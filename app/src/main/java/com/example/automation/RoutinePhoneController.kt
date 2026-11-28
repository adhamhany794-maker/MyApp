package com.example.automation

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import com.example.models.ActionType
import com.example.models.ExceptionType
import com.example.models.RoutineLogEntry
import com.example.models.RoutineLogEventType
import com.example.models.SmartProfile
import com.example.models.SmartRoutine
import com.example.models.SoundMode
import com.example.simulation.PhoneControlManager
import java.util.UUID

data class SavedPhoneBackup(
    val activeRoutineIds: Set<String>,
    val previousRingerMode: SoundMode,
    val previousInterruptionFilter: Int,
    val savedAtMillis: Long
)

sealed interface EndRestoreDecision {
    data object NoBackup : EndRestoreDecision
    data class StillOtherRoutinesActive(val remainingRoutineIds: Set<String>) : EndRestoreDecision
    data class Restore(
        val previousRingerMode: SoundMode,
        val previousInterruptionFilter: Int
    ) : EndRestoreDecision
}

interface RoutineStateBackupStore {
    fun getBackup(): SavedPhoneBackup?
    fun onRoutineStarted(
        routineId: String,
        currentRingerMode: SoundMode,
        currentInterruptionFilter: Int,
        timestampMillis: Long
    )
    fun onRoutineEnded(routineId: String): EndRestoreDecision
    fun setDndPermissionMissing(missing: Boolean, routineName: String? = null)
    fun isDndPermissionMissing(): Boolean
    fun getDndMissingRoutineName(): String?
    fun clearAll()
}

class InMemoryRoutineStateBackupStore : RoutineStateBackupStore {
    private var backup: SavedPhoneBackup? = null
    private var dndMissing: Boolean = false
    private var dndMissingRoutine: String? = null

    override fun getBackup(): SavedPhoneBackup? = backup

    override fun onRoutineStarted(
        routineId: String,
        currentRingerMode: SoundMode,
        currentInterruptionFilter: Int,
        timestampMillis: Long
    ) {
        val existing = backup
        backup = if (existing == null) {
            SavedPhoneBackup(
                activeRoutineIds = setOf(routineId),
                previousRingerMode = currentRingerMode,
                previousInterruptionFilter = currentInterruptionFilter,
                savedAtMillis = timestampMillis
            )
        } else {
            existing.copy(activeRoutineIds = existing.activeRoutineIds + routineId)
        }
    }

    override fun onRoutineEnded(routineId: String): EndRestoreDecision {
        val existing = backup ?: return EndRestoreDecision.NoBackup
        val remaining = existing.activeRoutineIds - routineId
        return if (remaining.isEmpty()) {
            backup = null
            EndRestoreDecision.Restore(
                previousRingerMode = existing.previousRingerMode,
                previousInterruptionFilter = existing.previousInterruptionFilter
            )
        } else {
            backup = existing.copy(activeRoutineIds = remaining)
            EndRestoreDecision.StillOtherRoutinesActive(remaining)
        }
    }

    override fun setDndPermissionMissing(missing: Boolean, routineName: String?) {
        dndMissing = missing
        dndMissingRoutine = if (missing) routineName else null
    }

    override fun isDndPermissionMissing(): Boolean = dndMissing

    override fun getDndMissingRoutineName(): String? = dndMissingRoutine

    override fun clearAll() {
        backup = null
        dndMissing = false
        dndMissingRoutine = null
    }
}

class SharedPreferencesRoutineStateBackupStore(
    context: Context
) : RoutineStateBackupStore {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("routine_phone_backup_prefs", Context.MODE_PRIVATE)

    override fun getBackup(): SavedPhoneBackup? {
        if (!prefs.getBoolean(KEY_HAS_BACKUP, false)) return null
        val ids = prefs.getStringSet(KEY_ACTIVE_IDS, emptySet())?.toSet() ?: emptySet()
        val mode = prefs.getString(KEY_PREV_RINGER, SoundMode.SOUND.name)
            ?.let { runCatching { SoundMode.valueOf(it) }.getOrNull() }
            ?: SoundMode.SOUND
        val filter = prefs.getInt(KEY_PREV_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL)
        val savedAt = prefs.getLong(KEY_SAVED_AT, 0L)
        return SavedPhoneBackup(
            activeRoutineIds = ids,
            previousRingerMode = mode,
            previousInterruptionFilter = filter,
            savedAtMillis = savedAt
        )
    }

    override fun onRoutineStarted(
        routineId: String,
        currentRingerMode: SoundMode,
        currentInterruptionFilter: Int,
        timestampMillis: Long
    ) {
        val existing = getBackup()
        val editor = prefs.edit()
        if (existing == null) {
            editor
                .putBoolean(KEY_HAS_BACKUP, true)
                .putString(KEY_PREV_RINGER, currentRingerMode.name)
                .putInt(KEY_PREV_FILTER, currentInterruptionFilter)
                .putLong(KEY_SAVED_AT, timestampMillis)
                .putStringSet(KEY_ACTIVE_IDS, setOf(routineId))
        } else {
            editor.putStringSet(KEY_ACTIVE_IDS, existing.activeRoutineIds + routineId)
        }
        editor.commit()
    }

    override fun onRoutineEnded(routineId: String): EndRestoreDecision {
        val existing = getBackup() ?: return EndRestoreDecision.NoBackup
        val remaining = existing.activeRoutineIds - routineId
        return if (remaining.isEmpty()) {
            prefs.edit()
                .remove(KEY_HAS_BACKUP)
                .remove(KEY_PREV_RINGER)
                .remove(KEY_PREV_FILTER)
                .remove(KEY_SAVED_AT)
                .remove(KEY_ACTIVE_IDS)
                .commit()
            EndRestoreDecision.Restore(
                previousRingerMode = existing.previousRingerMode,
                previousInterruptionFilter = existing.previousInterruptionFilter
            )
        } else {
            prefs.edit()
                .putStringSet(KEY_ACTIVE_IDS, remaining)
                .commit()
            EndRestoreDecision.StillOtherRoutinesActive(remaining)
        }
    }

    override fun setDndPermissionMissing(missing: Boolean, routineName: String?) {
        prefs.edit()
            .putBoolean(KEY_DND_MISSING, missing)
            .putString(KEY_DND_MISSING_ROUTINE, if (missing) routineName else null)
            .commit()
    }

    override fun isDndPermissionMissing(): Boolean = prefs.getBoolean(KEY_DND_MISSING, false)

    override fun getDndMissingRoutineName(): String? = prefs.getString(KEY_DND_MISSING_ROUTINE, null)

    override fun clearAll() {
        prefs.edit().clear().commit()
    }

    private companion object {
        const val KEY_HAS_BACKUP = "has_backup"
        const val KEY_ACTIVE_IDS = "active_routine_ids"
        const val KEY_PREV_RINGER = "prev_ringer_mode"
        const val KEY_PREV_FILTER = "prev_interruption_filter"
        const val KEY_SAVED_AT = "saved_at_millis"
        const val KEY_DND_MISSING = "dnd_permission_missing"
        const val KEY_DND_MISSING_ROUTINE = "dnd_missing_routine"
    }
}

data class RoutineControlOutcome(
    val success: Boolean,
    val permissionRequired: Boolean,
    val logEntry: RoutineLogEntry
)

class RoutinePhoneController(
    private val phoneControlManager: PhoneControlManager,
    private val backupStore: RoutineStateBackupStore
) {

    fun applyRoutineStart(
        routine: SmartRoutine,
        profiles: List<SmartProfile>,
        isArabic: Boolean,
        timestampMillis: Long = System.currentTimeMillis(),
        hasEmergencyContacts: Boolean = true
    ): RoutineControlOutcome {
        val targetSoundMode = resolveTargetSoundMode(routine, profiles)
        val isMutingRoutine = targetSoundMode == SoundMode.SILENT ||
            targetSoundMode == SoundMode.VIBRATE ||
            routine.enableDoNotDisturb
        if (targetSoundMode == null && !routine.enableDoNotDisturb) {
            return RoutineControlOutcome(
                success = false,
                permissionRequired = false,
                logEntry = RoutineLogEntry(
                    id = "rlog_${UUID.randomUUID().toString().take(8)}",
                    routineId = routine.id,
                    routineName = routine.name,
                    eventType = RoutineLogEventType.NO_ACTION,
                    timestampMillis = timestampMillis,
                    ringerMode = null,
                    dndEnabled = null,
                    interruptionFilter = null,
                    note = "No phone action selected"
                )
            )
        }

        val stateBefore = phoneControlManager.refreshPermissionAndDeviceState()
        if (!stateBefore.hasDndAccess) {
            backupStore.setDndPermissionMissing(true, routine.name)
            val note = if (isArabic) {
                "تعذر التحكم بالهاتف: إذن الوصول لعدم الإزعاج غير ممنوح"
            } else {
                "Failed: Do Not Disturb access not granted"
            }
            return RoutineControlOutcome(
                success = false,
                permissionRequired = true,
                logEntry = RoutineLogEntry(
                    id = "rlog_${UUID.randomUUID().toString().take(8)}",
                    routineId = routine.id,
                    routineName = routine.name,
                    eventType = RoutineLogEventType.PERMISSION_DENIED,
                    timestampMillis = timestampMillis,
                    ringerMode = null,
                    dndEnabled = null,
                    interruptionFilter = null,
                    note = note
                )
            )
        }

        // Snapshot previous phone ringer mode and interruption filter BEFORE applying changes
        val prevRinger = phoneControlManager.currentRingerMode()
        val prevFilter = phoneControlManager.currentInterruptionFilter()
        backupStore.onRoutineStarted(
            routineId = routine.id,
            currentRingerMode = prevRinger,
            currentInterruptionFilter = prevFilter,
            timestampMillis = timestampMillis
        )
        backupStore.setDndPermissionMissing(false, null)

        var appliedRinger: SoundMode? = null
        var appliedDnd: Boolean? = null
        var ringerOk = true
        var dndOk = true
        var permRequiredDuringApply = false

        // Keep ringer on SOUND (NORMAL) so OS DND starred contacts ring through,
        // and mute non-priority calls/notifications via DND INTERRUPTION_FILTER_PRIORITY.
        val ringerRes = phoneControlManager.applyRingerMode(SoundMode.SOUND)
        ringerOk = ringerRes.success
        if (ringerRes.permissionRequired) permRequiredDuringApply = true
        if (ringerRes.success) {
            appliedRinger = SoundMode.SOUND
        }

        if (isMutingRoutine) {
            val dndRes = phoneControlManager.setDoNotDisturbEnabled(
                enabled = true,
                allowStarredContacts = true
            )
            dndOk = dndRes.success
            if (dndRes.permissionRequired) permRequiredDuringApply = true
            if (dndRes.success) {
                appliedDnd = true
            }
        }

        val overallSuccess = (appliedRinger != null || appliedDnd == true) && ringerOk && dndOk
        if (!overallSuccess) {
            // Roll back empty backup entry if nothing succeeded
            backupStore.onRoutineEnded(routine.id)
            if (permRequiredDuringApply) {
                backupStore.setDndPermissionMissing(true, routine.name)
            }
            val failNote = if (permRequiredDuringApply) {
                if (isArabic) "تعذر التحكم بالهاتف: إذن الوصول لعدم الإزعاج غير ممنوح"
                else "Failed: Do Not Disturb access not granted"
            } else {
                if (isArabic) "تعذر تطبيق تغيير الصوت أو عدم الإزعاج على الهاتف"
                else "Failed to apply ringer mode or Do Not Disturb on device"
            }
            return RoutineControlOutcome(
                success = false,
                permissionRequired = permRequiredDuringApply,
                logEntry = RoutineLogEntry(
                    id = "rlog_${UUID.randomUUID().toString().take(8)}",
                    routineId = routine.id,
                    routineName = routine.name,
                    eventType = RoutineLogEventType.PERMISSION_DENIED,
                    timestampMillis = timestampMillis,
                    ringerMode = null,
                    dndEnabled = null,
                    interruptionFilter = null,
                    note = failNote
                )
            )
        }

        val afterState = phoneControlManager.refreshPermissionAndDeviceState()
        val note = buildAppliedSummary(
            ringerMode = appliedRinger ?: SoundMode.SOUND,
            dndEnabled = appliedDnd ?: (if (isMutingRoutine) true else null),
            isArabic = isArabic
        )

        return RoutineControlOutcome(
            success = true,
            permissionRequired = false,
            logEntry = RoutineLogEntry(
                id = "rlog_${UUID.randomUUID().toString().take(8)}",
                routineId = routine.id,
                routineName = routine.name,
                eventType = RoutineLogEventType.STARTED,
                timestampMillis = timestampMillis,
                ringerMode = appliedRinger ?: afterState.currentRingerMode,
                dndEnabled = appliedDnd ?: afterState.isDndEnabled,
                interruptionFilter = afterState.currentInterruptionFilter,
                note = note
            )
        )
    }

    fun applyRoutineEnd(
        routineId: String,
        routineName: String,
        isArabic: Boolean,
        timestampMillis: Long = System.currentTimeMillis()
    ): RoutineControlOutcome {
        return when (val decision = backupStore.onRoutineEnded(routineId)) {
            is EndRestoreDecision.NoBackup -> {
                val note = if (isArabic) {
                    "انتهى الروتين (لم تكن هناك حالة سابقة محفوظة)"
                } else {
                    "Routine ended (no prior saved state to restore)"
                }
                RoutineControlOutcome(
                    success = true,
                    permissionRequired = false,
                    logEntry = RoutineLogEntry(
                        id = "rlog_${UUID.randomUUID().toString().take(8)}",
                        routineId = routineId,
                        routineName = routineName,
                        eventType = RoutineLogEventType.ENDED,
                        timestampMillis = timestampMillis,
                        ringerMode = null,
                        dndEnabled = null,
                        interruptionFilter = null,
                        note = note
                    )
                )
            }
            is EndRestoreDecision.StillOtherRoutinesActive -> {
                val note = if (isArabic) {
                    "انتهى الروتين، وبقيت حالة الهاتف محمية لروتين آخر نشط"
                } else {
                    "Routine ended; phone state kept for another active routine"
                }
                RoutineControlOutcome(
                    success = true,
                    permissionRequired = false,
                    logEntry = RoutineLogEntry(
                        id = "rlog_${UUID.randomUUID().toString().take(8)}",
                        routineId = routineId,
                        routineName = routineName,
                        eventType = RoutineLogEventType.ENDED,
                        timestampMillis = timestampMillis,
                        ringerMode = null,
                        dndEnabled = null,
                        interruptionFilter = null,
                        note = note
                    )
                )
            }
            is EndRestoreDecision.Restore -> {
                val stateNow = phoneControlManager.refreshPermissionAndDeviceState()
                if (!stateNow.hasDndAccess) {
                    backupStore.setDndPermissionMissing(true, routineName)
                    val note = if (isArabic) {
                        "تعذر استعادة الحالة السابقة: إذن الوصول لعدم الإزعاج غير ممنوح"
                    } else {
                        "Failed to restore previous state: Do Not Disturb access not granted"
                    }
                    return RoutineControlOutcome(
                        success = false,
                        permissionRequired = true,
                        logEntry = RoutineLogEntry(
                            id = "rlog_${UUID.randomUUID().toString().take(8)}",
                            routineId = routineId,
                            routineName = routineName,
                            eventType = RoutineLogEventType.PERMISSION_DENIED,
                            timestampMillis = timestampMillis,
                            ringerMode = null,
                            dndEnabled = null,
                            interruptionFilter = null,
                            note = note
                        )
                    )
                }

                val res = phoneControlManager.restoreDeviceState(
                    ringerMode = decision.previousRingerMode,
                    interruptionFilter = decision.previousInterruptionFilter
                )
                val restoredDndOn = decision.previousInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL &&
                    decision.previousInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
                val note = if (isArabic) {
                    val dndStr = if (restoredDndOn) "عدم الإزعاج مفعّل" else "عدم الإزعاج متوقف"
                    "تمت استعادة الحالة السابقة: ${decision.previousRingerMode.arabicName} • $dndStr"
                } else {
                    val dndStr = if (restoredDndOn) "DND On" else "DND Off"
                    "Restored previous state: ${decision.previousRingerMode.displayName} • $dndStr"
                }
                RoutineControlOutcome(
                    success = res.success,
                    permissionRequired = res.permissionRequired,
                    logEntry = RoutineLogEntry(
                        id = "rlog_${UUID.randomUUID().toString().take(8)}",
                        routineId = routineId,
                        routineName = routineName,
                        eventType = RoutineLogEventType.ENDED,
                        timestampMillis = timestampMillis,
                        ringerMode = decision.previousRingerMode,
                        dndEnabled = restoredDndOn,
                        interruptionFilter = decision.previousInterruptionFilter,
                        note = note
                    )
                )
            }
        }
    }

    fun resolveTargetSoundMode(routine: SmartRoutine, profiles: List<SmartProfile>): SoundMode? {
        val explicit = routine.actions.firstOrNull { it.type == ActionType.SET_SOUND_MODE }
            ?.let { runCatching { SoundMode.valueOf(it.targetValue) }.getOrNull() }
        if (explicit != null) return explicit

        val profileId = routine.targetProfileId
            ?: routine.actions.firstOrNull { it.type == ActionType.ACTIVATE_PROFILE }?.targetValue
        if (profileId != null) {
            return profiles.firstOrNull { it.id == profileId }?.soundMode
        }
        return null
    }

    private fun buildAppliedSummary(
        ringerMode: SoundMode?,
        dndEnabled: Boolean?,
        isArabic: Boolean
    ): String {
        val parts = mutableListOf<String>()
        if (ringerMode != null) {
            parts.add(if (isArabic) "وضع الصوت: ${ringerMode.arabicName}" else "Sound: ${ringerMode.displayName}")
        }
        if (dndEnabled == true) {
            parts.add(if (isArabic) "عدم الإزعاج: مفعّل" else "Do Not Disturb: On")
        }
        if (parts.isEmpty()) {
            return if (isArabic) "تم تشغيل الروتين" else "Routine applied"
        }
        return parts.joinToString(" • ")
    }
}
