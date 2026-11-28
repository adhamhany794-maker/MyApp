package com.example.automation

import android.Manifest
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.example.models.ContactPriority
import com.example.models.EmergencyBypassOutcome
import com.example.models.InterruptionType
import com.example.models.PriorityContact
import com.example.models.SoundMode
import com.example.simulation.PhoneControlManager

data class PickedPhoneContact(
    val name: String,
    val phoneNumber: String,
    val isStarredInSystem: Boolean = false,
    val systemContactId: String = ""
)

data class EmergencyBypassBackup(
    val previousRingerMode: SoundMode,
    val previousInterruptionFilter: Int,
    val notificationKey: String? = null,
    val savedAtMillis: Long = System.currentTimeMillis()
)

interface EmergencyBypassBackupStore {
    fun getBackup(): EmergencyBypassBackup?
    fun saveIfAbsent(backup: EmergencyBypassBackup)
    fun consumeBackup(notificationKey: String? = null): EmergencyBypassBackup?
    fun clear()
}

class InMemoryEmergencyBypassBackupStore : EmergencyBypassBackupStore {
    private var current: EmergencyBypassBackup? = null

    override fun getBackup(): EmergencyBypassBackup? = current

    override fun saveIfAbsent(backup: EmergencyBypassBackup) {
        if (current == null) {
            current = backup
        }
    }

    override fun consumeBackup(notificationKey: String?): EmergencyBypassBackup? {
        val existing = current ?: return null
        if (notificationKey != null && existing.notificationKey != null && existing.notificationKey != notificationKey) {
            return null
        }
        current = null
        return existing
    }

    override fun clear() {
        current = null
    }
}

class SharedPreferencesEmergencyBypassBackupStore(
    context: Context
) : EmergencyBypassBackupStore {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("emergency_bypass_backup_prefs", Context.MODE_PRIVATE)

    override fun getBackup(): EmergencyBypassBackup? {
        if (!prefs.getBoolean(KEY_HAS_BACKUP, false)) return null
        val ringer = prefs.getString(KEY_PREV_RINGER, SoundMode.SOUND.name)
            ?.let { runCatching { SoundMode.valueOf(it) }.getOrNull() }
            ?: SoundMode.SOUND
        val filter = prefs.getInt(KEY_PREV_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL)
        val key = prefs.getString(KEY_NOTIFICATION_KEY, null)
        val savedAt = prefs.getLong(KEY_SAVED_AT, 0L)
        return EmergencyBypassBackup(
            previousRingerMode = ringer,
            previousInterruptionFilter = filter,
            notificationKey = key,
            savedAtMillis = savedAt
        )
    }

    override fun saveIfAbsent(backup: EmergencyBypassBackup) {
        if (getBackup() != null) return
        prefs.edit()
            .putBoolean(KEY_HAS_BACKUP, true)
            .putString(KEY_PREV_RINGER, backup.previousRingerMode.name)
            .putInt(KEY_PREV_FILTER, backup.previousInterruptionFilter)
            .putString(KEY_NOTIFICATION_KEY, backup.notificationKey)
            .putLong(KEY_SAVED_AT, backup.savedAtMillis)
            .commit()
    }

    override fun consumeBackup(notificationKey: String?): EmergencyBypassBackup? {
        val existing = getBackup() ?: return null
        if (notificationKey != null && existing.notificationKey != null && existing.notificationKey != notificationKey) {
            return null
        }
        clear()
        return existing
    }

    override fun clear() {
        prefs.edit().clear().commit()
    }

    private companion object {
        const val KEY_HAS_BACKUP = "has_bypass_backup"
        const val KEY_PREV_RINGER = "bypass_prev_ringer"
        const val KEY_PREV_FILTER = "bypass_prev_filter"
        const val KEY_NOTIFICATION_KEY = "bypass_notification_key"
        const val KEY_SAVED_AT = "bypass_saved_at"
    }
}

object EmergencyPhoneNumberUtils {

    fun normalizePhoneNumber(raw: String): String {
        if (raw.isBlank()) return ""
        val sb = StringBuilder()
        for (ch in raw.trim()) {
            when {
                ch == '+' && sb.isEmpty() -> sb.append('+')
                ch in '0'..'9' -> sb.append(ch)
                ch in '٠'..'٩' -> sb.append(('0'.code + (ch.code - '٠'.code)).toChar())
                ch in '۰'..'۹' -> sb.append(('0'.code + (ch.code - '۰'.code)).toChar())
            }
        }
        val cleaned = sb.toString()
        return if (cleaned.startsWith("00") && cleaned.length > 2) {
            "+" + cleaned.substring(2)
        } else {
            cleaned
        }
    }

    fun numbersMatch(first: String?, second: String?): Boolean {
        val a = normalizePhoneNumber(first.orEmpty())
        val b = normalizePhoneNumber(second.orEmpty())
        if (a.isBlank() || b.isBlank()) return false
        if (a == b) return true

        val digitsA = a.removePrefix("+")
        val digitsB = b.removePrefix("+")
        if (digitsA == digitsB) return true

        val trimmedA = digitsA.trimStart('0')
        val trimmedB = digitsB.trimStart('0')
        if (trimmedA.length >= 9 && trimmedB.length >= 9) {
            if (trimmedA == trimmedB) return true
            if (digitsA.endsWith(trimmedB) || digitsB.endsWith(trimmedA)) return true
            val minTail = minOf(10, trimmedA.length, trimmedB.length)
            if (minTail >= 9 && trimmedA.takeLast(minTail) == trimmedB.takeLast(minTail)) {
                return true
            }
        }
        return false
    }

    fun findDuplicateByNumber(
        phoneNumber: String,
        existingContacts: List<PriorityContact>,
        excludingId: String? = null
    ): PriorityContact? {
        if (normalizePhoneNumber(phoneNumber).isBlank()) return null
        return existingContacts.firstOrNull { existing ->
            existing.id != excludingId &&
                existing.phoneNumber.isNotBlank() &&
                numbersMatch(existing.phoneNumber, phoneNumber)
        }
    }

    /**
     * Matches emergency contacts strictly by phone number OR exact contact name.
     * Never performs partial substring matching.
     */
    fun findMatchingEmergencyContact(
        senderName: String?,
        senderPhoneNumber: String?,
        contacts: List<PriorityContact>
    ): PriorityContact? {
        val emergencyContacts = contacts.filter {
            it.priorityLevel == ContactPriority.EMERGENCY && it.isEnabled
        }
        if (emergencyContacts.isEmpty()) return null

        if (!senderPhoneNumber.isNullOrBlank()) {
            emergencyContacts.firstOrNull { contact ->
                contact.phoneNumber.isNotBlank() && numbersMatch(contact.phoneNumber, senderPhoneNumber)
            }?.let { return it }
        }

        val cleanSender = senderName?.trim().orEmpty()
        if (cleanSender.isNotBlank()) {
            emergencyContacts.firstOrNull { contact ->
                contact.name.trim().equals(cleanSender, ignoreCase = true)
            }?.let { return it }
        }

        return null
    }
}

object PhoneContactPickerResolver {

    fun hasReadContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasWriteContactsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun resolveFromUri(contentResolver: ContentResolver, uri: Uri): PickedPhoneContact? {
        // 1. Check if the URI directly exposes CommonDataKinds.Phone columns
        runCatching {
            contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val contactId = if (idIdx >= 0) cursor.getString(idIdx).orEmpty().trim() else ""
                    val number = if (numIdx >= 0) cursor.getString(numIdx).orEmpty().trim() else ""
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx).orEmpty().trim() else ""
                    if (number.isNotBlank()) {
                        return PickedPhoneContact(
                            name = name.ifBlank { number },
                            phoneNumber = number,
                            systemContactId = contactId
                        )
                    }
                }
            }
        }

        // 2. Standard PickContact URI (ContactsContract.Contacts): query ID, DISPLAY_NAME, STARRED
        var contactId: String? = null
        var displayName = ""
        var isStarred = false

        runCatching {
            contentResolver.query(
                uri,
                arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.STARRED
                ),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                    val starredIdx = cursor.getColumnIndex(ContactsContract.Contacts.STARRED)
                    if (idIdx >= 0) contactId = cursor.getString(idIdx)
                    if (nameIdx >= 0) displayName = cursor.getString(nameIdx).orEmpty().trim()
                    if (starredIdx >= 0) isStarred = cursor.getInt(starredIdx) == 1
                }
            }
        }

        val resolvedId = contactId ?: return null
        var resolvedNumber = ""
        runCatching {
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.IS_PRIMARY
                ),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(resolvedId),
                "${ContactsContract.CommonDataKinds.Phone.IS_PRIMARY} DESC"
            )?.use { phoneCursor ->
                if (phoneCursor.moveToFirst()) {
                    val numIdx = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (numIdx >= 0) {
                        resolvedNumber = phoneCursor.getString(numIdx).orEmpty().trim()
                    }
                }
            }
        }

        if (resolvedNumber.isBlank() && displayName.isBlank()) return null
        return PickedPhoneContact(
            name = displayName.ifBlank { resolvedNumber },
            phoneNumber = resolvedNumber,
            isStarredInSystem = isStarred,
            systemContactId = resolvedId
        )
    }

    fun setContactStarred(context: Context, contact: PriorityContact, starred: Boolean): Boolean {
        if (!hasWriteContactsPermission(context)) return false
        val targetIds = linkedSetOf<String>()
        if (contact.systemContactId.isNotBlank()) {
            targetIds.add(contact.systemContactId.trim())
        }
        if (contact.phoneNumber.isNotBlank()) {
            targetIds.addAll(findSystemContactIdsByPhone(context.contentResolver, contact.phoneNumber))
        }
        if (targetIds.isEmpty()) return false

        val values = ContentValues().apply {
            put(ContactsContract.Contacts.STARRED, if (starred) 1 else 0)
        }
        var totalUpdated = 0
        for (id in targetIds) {
            runCatching {
                totalUpdated += context.contentResolver.update(
                    ContactsContract.Contacts.CONTENT_URI,
                    values,
                    "${ContactsContract.Contacts._ID} = ?",
                    arrayOf(id)
                )
            }
        }
        return totalUpdated > 0
    }

    fun findSystemContactIdsByPhone(contentResolver: ContentResolver, phoneNumber: String): Set<String> {
        if (phoneNumber.isBlank()) return emptySet()
        val ids = linkedSetOf<String>()

        runCatching {
            val lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            contentResolver.query(
                lookupUri,
                arrayOf(ContactsContract.PhoneLookup.CONTACT_ID, ContactsContract.PhoneLookup._ID),
                null,
                null,
                null
            )?.use { cursor ->
                val contactIdIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.CONTACT_ID)
                val idIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup._ID)
                while (cursor.moveToNext()) {
                    val cid = if (contactIdIdx >= 0) cursor.getString(contactIdIdx) else null
                    val id = if (idIdx >= 0) cursor.getString(idIdx) else null
                    val picked = cid?.takeIf { it.isNotBlank() } ?: id?.takeIf { it.isNotBlank() }
                    if (picked != null) ids.add(picked)
                }
            }
        }

        if (ids.isEmpty()) {
            runCatching {
                contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    ),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val cidIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    while (cursor.moveToNext()) {
                        val num = if (numIdx >= 0) cursor.getString(numIdx).orEmpty() else ""
                        if (EmergencyPhoneNumberUtils.numbersMatch(num, phoneNumber)) {
                            val cid = if (cidIdx >= 0) cursor.getString(cidIdx).orEmpty().trim() else ""
                            if (cid.isNotBlank()) ids.add(cid)
                        }
                    }
                }
            }
        }
        return ids
    }

    fun isNumberStarredInSystemContacts(context: Context, phoneNumber: String): Boolean {
        if (phoneNumber.isBlank() || !hasReadContactsPermission(context)) return false
        return runCatching {
            val lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
            )
            context.contentResolver.query(
                lookupUri,
                arrayOf(ContactsContract.PhoneLookup.STARRED),
                null,
                null,
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val starredIdx = cursor.getColumnIndex(ContactsContract.PhoneLookup.STARRED)
                    if (starredIdx >= 0 && cursor.getInt(starredIdx) == 1) {
                        return@runCatching true
                    }
                }
                false
            } ?: false
        }.getOrDefault(false)
    }
}

class EmergencyBypassController(
    private val phoneControlManager: PhoneControlManager,
    private val backupStore: EmergencyBypassBackupStore = InMemoryEmergencyBypassBackupStore(),
    private val onBypassAlert: (contact: PriorityContact, type: InterruptionType, isArabic: Boolean) -> Unit = { _, _, _ -> }
) {
    fun handleIncomingCommunication(
        senderName: String?,
        senderPhoneNumber: String?,
        interruptionType: InterruptionType,
        contacts: List<PriorityContact>,
        isStarredInOs: Boolean,
        isArabic: Boolean,
        notificationKey: String? = null
    ): EmergencyBypassOutcome {
        val matched = EmergencyPhoneNumberUtils.findMatchingEmergencyContact(
            senderName = senderName,
            senderPhoneNumber = senderPhoneNumber,
            contacts = contacts
        )
        if (matched == null) {
            return EmergencyBypassOutcome(
                matchedContact = null,
                isEmergency = false,
                allowed = false,
                usedOsDndMechanism = false,
                usedAppFallback = false,
                bypassAppliedOnDevice = false,
                explanation = if (isArabic) {
                    "المرسل ليس ضمن جهات الطوارئ."
                } else {
                    "Sender is not an emergency contact."
                }
            )
        }

        val deviceState = phoneControlManager.refreshPermissionAndDeviceState()
        val osDndCoversInterruption = deviceState.hasDndAccess &&
            deviceState.starredContactsAllowedInDnd &&
            isStarredInOs &&
            deviceState.currentRingerMode == SoundMode.SOUND &&
            (interruptionType == InterruptionType.CALL ||
                interruptionType == InterruptionType.MESSAGE)

        if (osDndCoversInterruption) {
            return EmergencyBypassOutcome(
                matchedContact = matched,
                isEmergency = true,
                allowed = true,
                usedOsDndMechanism = true,
                usedAppFallback = false,
                bypassAppliedOnDevice = false,
                explanation = if (isArabic) {
                    "تم السماح لجهة الطوارئ '${matched.name}' عبر آلية عدم الإزعاج للنظام (جهات الاتصال المميزة) — هيوصلك دايما."
                } else {
                    "Emergency contact '${matched.name}' allowed via OS DND starred/priority contacts policy."
                }
            )
        }

        // Fall back to the app's own emergency check & bypass when OS DND priority mechanism
        // cannot allow it (e.g. Silent/Vibrate/Sleep active, unstarred number, or OS policy blocked).
        // Save the device's state BEFORE applying the bypass so it can be restored afterwards.
        var deviceBypassed = false
        if (deviceState.hasDndAccess &&
            (deviceState.currentRingerMode != SoundMode.SOUND || deviceState.isDndEnabled)
        ) {
            backupStore.saveIfAbsent(
                EmergencyBypassBackup(
                    previousRingerMode = deviceState.currentRingerMode,
                    previousInterruptionFilter = deviceState.currentInterruptionFilter,
                    notificationKey = notificationKey,
                    savedAtMillis = System.currentTimeMillis()
                )
            )
            val res = phoneControlManager.applyEmergencyBypass()
            deviceBypassed = res.success
            if (!res.success) {
                backupStore.consumeBackup(notificationKey)
            }
        }

        onBypassAlert(matched, interruptionType, isArabic)

        return EmergencyBypassOutcome(
            matchedContact = matched,
            isEmergency = true,
            allowed = true,
            usedOsDndMechanism = false,
            usedAppFallback = true,
            bypassAppliedOnDevice = deviceBypassed,
            explanation = if (isArabic) {
                "تم تجاوز الروتين والوضع الصامت عبر فحص التطبيق لجهة الطوارئ '${matched.name}' — هيوصلك دايما."
            } else {
                "Emergency contact '${matched.name}' bypassed active Silent/DND/Sleep mode via app fallback check."
            }
        )
    }

    fun restoreAfterBypass(notificationKey: String? = null): Boolean {
        val saved = backupStore.consumeBackup(notificationKey) ?: return false
        val result = phoneControlManager.restoreDeviceState(
            ringerMode = saved.previousRingerMode,
            interruptionFilter = saved.previousInterruptionFilter
        )
        return result.success
    }
}
