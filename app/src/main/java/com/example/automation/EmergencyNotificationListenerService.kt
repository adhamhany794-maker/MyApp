package com.example.automation

import android.app.Notification
import android.app.Person
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.data.PreferencesManager
import com.example.data.local.AppDatabase
import com.example.models.AppLanguage
import com.example.models.EventCategory
import com.example.models.InterruptionType
import com.example.repositories.SmartCompanionRepository
import com.example.simulation.AndroidPhoneControlGateway
import com.example.simulation.PhoneControlManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ExtractedNotificationSender(
    val senderName: String? = null,
    val senderPhoneNumber: String? = null
)

object EmergencyNotificationExtractor {
    /**
     * Extracts sender identity ONLY from structured Person / URI metadata.
     * Never uses Notification.EXTRA_TEXT or Notification.EXTRA_TITLE as a contact name or phone number.
     */
    @Suppress("DEPRECATION")
    fun extractSenderIdentity(notification: Notification): ExtractedNotificationSender {
        val extras = notification.extras ?: return ExtractedNotificationSender()
        var extractedName: String? = null
        var extractedPhone: String? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val messagingPerson = extras.getParcelable(Notification.EXTRA_MESSAGING_PERSON) as? Person
            if (messagingPerson != null) {
                val uri = messagingPerson.uri
                if (uri != null && uri.startsWith("tel:", ignoreCase = true)) {
                    extractedPhone = uri.substringAfter("tel:", "").trim().takeIf { it.isNotBlank() }
                }
                extractedName = messagingPerson.name?.toString()?.trim()?.takeIf { it.isNotBlank() }
            }

            val people = extras.getParcelableArrayList<Person>(Notification.EXTRA_PEOPLE_LIST)
            if (!people.isNullOrEmpty()) {
                for (person in people) {
                    val uri = person.uri
                    if (extractedPhone == null && uri != null && uri.startsWith("tel:", ignoreCase = true)) {
                        extractedPhone = uri.substringAfter("tel:", "").trim().takeIf { it.isNotBlank() }
                    }
                    if (extractedName == null) {
                        extractedName = person.name?.toString()?.trim()?.takeIf { it.isNotBlank() }
                    }
                }
            }
        }

        if (extractedPhone == null) {
            val legacyPeople = extras.getStringArray(Notification.EXTRA_PEOPLE)
            if (legacyPeople != null) {
                for (uri in legacyPeople) {
                    if (uri != null && uri.startsWith("tel:", ignoreCase = true)) {
                        extractedPhone = uri.substringAfter("tel:", "").trim().takeIf { it.isNotBlank() }
                        if (extractedPhone != null) break
                    }
                }
            }
        }

        return ExtractedNotificationSender(
            senderName = extractedName,
            senderPhoneNumber = extractedPhone
        )
    }
}

class EmergencyNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val posted = sbn ?: return
        if (posted.packageName == packageName) return

        val notification = posted.notification ?: return
        val identity = EmergencyNotificationExtractor.extractSenderIdentity(notification)
        if (identity.senderName.isNullOrBlank() && identity.senderPhoneNumber.isNullOrBlank()) {
            return
        }

        val interruptionType = if (notification.category == Notification.CATEGORY_CALL) {
            InterruptionType.CALL
        } else {
            InterruptionType.MESSAGE
        }

        serviceScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            val prefs = PreferencesManager(applicationContext)
            val repository = SmartCompanionRepository(db, prefs)
            val contacts = repository.getAllContactsOnce()
            val appPrefs = repository.preferences.first()
            val isArabic = appPrefs.language == AppLanguage.ARABIC

            val matched = EmergencyPhoneNumberUtils.findMatchingEmergencyContact(
                senderName = identity.senderName,
                senderPhoneNumber = identity.senderPhoneNumber,
                contacts = contacts
            ) ?: return@launch

            val phoneControlManager = PhoneControlManager(AndroidPhoneControlGateway(applicationContext))
            val bypassBackupStore = SharedPreferencesEmergencyBypassBackupStore(applicationContext)
            val controller = EmergencyBypassController(
                phoneControlManager = phoneControlManager,
                backupStore = bypassBackupStore,
                onBypassAlert = { contact, type, ar ->
                    RoutineNotificationHelper.showEmergencyBypassAlert(
                        context = applicationContext,
                        contactName = contact.name,
                        phoneNumber = contact.phoneNumber,
                        isCall = type == InterruptionType.CALL,
                        isArabic = ar
                    )
                }
            )

            val isStarredInOs = PhoneContactPickerResolver.isNumberStarredInSystemContacts(
                context = applicationContext,
                phoneNumber = matched.phoneNumber
            )

            val outcome = controller.handleIncomingCommunication(
                senderName = identity.senderName,
                senderPhoneNumber = identity.senderPhoneNumber ?: matched.phoneNumber,
                interruptionType = interruptionType,
                contacts = contacts,
                isStarredInOs = isStarredInOs,
                isArabic = isArabic,
                notificationKey = posted.key
            )

            if (outcome.isEmergency && outcome.allowed) {
                repository.logEvent(
                    title = if (isArabic) "تجاوز طوارئ: ${matched.name}" else "Emergency Bypass: ${matched.name}",
                    category = EventCategory.CONTACT_EXCEPTION,
                    triggerSource = if (outcome.usedOsDndMechanism) "OS DND Priority" else "Notification Listener Backup",
                    result = if (isArabic) "هيوصلك دايما" else "Always gets through",
                    explanation = outcome.explanation
                )
            }

            if (outcome.bypassAppliedOnDevice) {
                val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
                if (!isOngoing) {
                    delay(BACKUP_AUTO_RESTORE_DELAY_MS)
                    if (controller.restoreAfterBypass(posted.key)) {
                        repository.logEvent(
                            title = if (isArabic) "استعادة الحالة بعد طوارئ: ${matched.name}" else "Restored State After Emergency: ${matched.name}",
                            category = EventCategory.CONTACT_EXCEPTION,
                            triggerSource = "Notification Listener Backup",
                            result = if (isArabic) "تمت استعادة الحالة السابقة" else "Previous state restored",
                            explanation = if (isArabic) "تمت إعادة الصوت وعدم الإزعاج للحالة المحفوظة بعد تجاوز الطوارئ." else "Restored saved phone sound and DND state after emergency bypass."
                        )
                    }
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        val removed = sbn ?: return
        if (removed.packageName == packageName) return

        val bypassBackupStore = SharedPreferencesEmergencyBypassBackupStore(applicationContext)
        val pendingBackup = bypassBackupStore.getBackup() ?: return
        if (pendingBackup.notificationKey != null && pendingBackup.notificationKey != removed.key) {
            return
        }

        serviceScope.launch {
            val db = AppDatabase.getInstance(applicationContext)
            val prefs = PreferencesManager(applicationContext)
            val repository = SmartCompanionRepository(db, prefs)
            val appPrefs = repository.preferences.first()
            val isArabic = appPrefs.language == AppLanguage.ARABIC
            val phoneControlManager = PhoneControlManager(AndroidPhoneControlGateway(applicationContext))
            val controller = EmergencyBypassController(
                phoneControlManager = phoneControlManager,
                backupStore = bypassBackupStore
            )
            if (controller.restoreAfterBypass(removed.key)) {
                repository.logEvent(
                    title = if (isArabic) "استعادة الحالة بعد انتهاء تنبيه الطوارئ" else "Restored State After Emergency Ended",
                    category = EventCategory.CONTACT_EXCEPTION,
                    triggerSource = "Notification Listener Backup",
                    result = if (isArabic) "تمت استعادة الحالة السابقة" else "Previous state restored",
                    explanation = if (isArabic) "تمت استعادة وضع الصوت وعدم الإزعاج بعد انتهاء مكالمة أو إشعار الطوارئ." else "Restored previous ringer and DND filter after emergency notification cleared."
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private companion object {
        const val BACKUP_AUTO_RESTORE_DELAY_MS = 30_000L
    }
}
