package com.example.automation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telephony.TelephonyManager
import com.example.data.PreferencesManager
import com.example.data.local.AppDatabase
import com.example.models.AppLanguage
import com.example.models.EventCategory
import com.example.models.PriorityContact
import com.example.models.RepeatCallerRule
import com.example.models.SoundMode
import com.example.repositories.SmartCompanionRepository
import com.example.simulation.AndroidPhoneControlGateway
import com.example.simulation.PhoneControlManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class IncomingCallBypassResult(
    val evaluation: RepeatCallEvaluation,
    val dndLiftedByApp: Boolean,
    val usedOsPolicy: Boolean
)

class IncomingCallBypassCoordinator(
    private val phoneControlManager: PhoneControlManager,
    private val bypassBackupStore: EmergencyBypassBackupStore,
    private val repeatCallTracker: RepeatCallTracker
) {
    fun onIncomingCallRinging(
        phoneNumber: String,
        timestampMillis: Long,
        emergencyContacts: List<PriorityContact>,
        perPersonRules: List<RepeatCallerRule>,
        globalEnabled: Boolean,
        globalRequiredCalls: Int,
        globalWindowMinutes: Int,
        seedCallLogTimestamps: List<Long> = emptyList(),
        isStarredInOs: Boolean = false
    ): IncomingCallBypassResult {
        val evaluation = repeatCallTracker.recordAndEvaluateIncomingCall(
            phoneNumber = phoneNumber,
            timestampMillis = timestampMillis,
            emergencyContacts = emergencyContacts,
            perPersonRules = perPersonRules,
            globalEnabled = globalEnabled,
            globalRequiredCalls = globalRequiredCalls,
            globalWindowMinutes = globalWindowMinutes,
            seedCallLogTimestamps = seedCallLogTimestamps
        )

        if (!evaluation.shouldAllowBypass) {
            return IncomingCallBypassResult(
                evaluation = evaluation,
                dndLiftedByApp = false,
                usedOsPolicy = false
            )
        }

        val deviceState = phoneControlManager.refreshPermissionAndDeviceState()

        val coveredByOsEmergency = evaluation.source == RepeatCallRuleSource.EMERGENCY_CONTACT &&
            deviceState.hasDndAccess &&
            deviceState.starredContactsAllowedInDnd &&
            isStarredInOs &&
            deviceState.currentRingerMode == SoundMode.SOUND

        val coveredByOsRepeat = evaluation.usedOsRepeatCallersPolicy &&
            deviceState.hasDndAccess &&
            deviceState.repeatCallersAllowedInDnd &&
            deviceState.currentRingerMode == SoundMode.SOUND

        if (coveredByOsEmergency || coveredByOsRepeat) {
            return IncomingCallBypassResult(
                evaluation = evaluation,
                dndLiftedByApp = false,
                usedOsPolicy = true
            )
        }

        var dndLifted = false
        if (deviceState.hasDndAccess &&
            (deviceState.isDndEnabled || deviceState.currentRingerMode != SoundMode.SOUND)
        ) {
            bypassBackupStore.saveIfAbsent(
                EmergencyBypassBackup(
                    previousRingerMode = deviceState.currentRingerMode,
                    previousInterruptionFilter = deviceState.currentInterruptionFilter,
                    notificationKey = REPEAT_CALL_BYPASS_KEY,
                    savedAtMillis = timestampMillis
                )
            )
            val bypassRes = phoneControlManager.applyEmergencyBypass()
            dndLifted = bypassRes.success
            if (!bypassRes.success) {
                bypassBackupStore.consumeBackup(REPEAT_CALL_BYPASS_KEY)
            }
        }

        return IncomingCallBypassResult(
            evaluation = evaluation,
            dndLiftedByApp = dndLifted,
            usedOsPolicy = false
        )
    }

    fun onCallEnded(): Boolean {
        val pending = bypassBackupStore.getBackup() ?: return false
        if (pending.notificationKey != REPEAT_CALL_BYPASS_KEY) return false
        val consumed = bypassBackupStore.consumeBackup(REPEAT_CALL_BYPASS_KEY) ?: return false
        val result = phoneControlManager.restoreDeviceState(
            ringerMode = consumed.previousRingerMode,
            interruptionFilter = consumed.previousInterruptionFilter
        )
        return result.success
    }

    companion object {
        const val REPEAT_CALL_BYPASS_KEY = "repeat_call_bypass"
    }
}

class IncomingCallStateReceiver : BroadcastReceiver() {

    @Suppress("DEPRECATION")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        if (state != TelephonyManager.EXTRA_STATE_RINGING && state != TelephonyManager.EXTRA_STATE_IDLE) {
            return
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val nowMillis = System.currentTimeMillis()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                handlePhoneStateChange(
                    context = appContext,
                    phoneState = state,
                    incomingNumber = incomingNumber,
                    nowMillis = nowMillis
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    internal suspend fun handlePhoneStateChange(
        context: Context,
        phoneState: String,
        incomingNumber: String?,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val phoneControlManager = PhoneControlManager(AndroidPhoneControlGateway(context))
        val backupStore = SharedPreferencesEmergencyBypassBackupStore(context)
        val tracker = RepeatCallTracker(SharedPreferencesRepeatCallHistoryStore(context))
        val coordinator = IncomingCallBypassCoordinator(phoneControlManager, backupStore, tracker)

        val db = AppDatabase.getInstance(context)
        val prefs = PreferencesManager(context)
        val repository = SmartCompanionRepository(db, prefs)
        val appPrefs = repository.preferences.first()
        val isArabic = appPrefs.language == AppLanguage.ARABIC

        when (phoneState) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                val number = incomingNumber?.trim().orEmpty()
                if (number.isBlank()) return
                val contacts = repository.getAllContactsOnce()
                val rules = repository.getAllRepeatCallerRulesOnce()
                val seedLogs = CallLogRepeatReader.readRecentCallTimestampsForNumber(
                    context = context,
                    phoneNumber = number,
                    windowMinutes = maxOf(appPrefs.repeatCallWindowMinutes, rules.maxOfOrNull { it.windowMinutes } ?: 15),
                    nowMillis = nowMillis
                )
                val isStarred = PhoneContactPickerResolver.isNumberStarredInSystemContacts(context, number)
                val result = coordinator.onIncomingCallRinging(
                    phoneNumber = number,
                    timestampMillis = nowMillis,
                    emergencyContacts = contacts,
                    perPersonRules = rules,
                    globalEnabled = appPrefs.repeatCallsEnabled,
                    globalRequiredCalls = appPrefs.repeatCallCount,
                    globalWindowMinutes = appPrefs.repeatCallWindowMinutes,
                    seedCallLogTimestamps = seedLogs,
                    isStarredInOs = isStarred
                )
                if (result.evaluation.shouldAllowBypass && result.evaluation.source != RepeatCallRuleSource.EMERGENCY_CONTACT) {
                    val callerLabel = result.evaluation.matchedRule?.name ?: number
                    val title = if (isArabic) {
                        "تجاوز المكالمات المتكررة: $callerLabel"
                    } else {
                        "Repeat Call Bypass: $callerLabel"
                    }
                    val detail = if (isArabic) {
                        "وصلت المكالمة رقم ${result.evaluation.callCountInWindow} من أصل ${result.evaluation.requiredCalls} خلال ${result.evaluation.windowMinutes} دقيقة — تم السماح بالرنين."
                    } else {
                        "Call ${result.evaluation.callCountInWindow}/${result.evaluation.requiredCalls} within ${result.evaluation.windowMinutes} min allowed through DND."
                    }
                    repository.logEvent(
                        title = title,
                        category = EventCategory.CONTACT_EXCEPTION,
                        triggerSource = if (result.usedOsPolicy) "OS Repeat Callers Policy" else "App Call Counter",
                        result = if (isArabic) "رنين مسموح" else "Allowed to ring",
                        explanation = detail
                    )
                }
            }

            TelephonyManager.EXTRA_STATE_IDLE -> {
                if (coordinator.onCallEnded()) {
                    repository.logEvent(
                        title = if (isArabic) "استعادة الحالة بعد انتهاء المكالمة" else "Restored State After Call Ended",
                        category = EventCategory.CONTACT_EXCEPTION,
                        triggerSource = "App Call Counter",
                        result = if (isArabic) "تمت استعادة الحالة السابقة" else "Previous state restored",
                        explanation = if (isArabic) {
                            "تمت إعادة وضع الصوت وعدم الإزعاج للحالة المحفوظة بعد انتهاء المكالمة."
                        } else {
                            "Restored saved ringer mode and DND state after call ended."
                        }
                    )
                }
            }
        }
    }
}

class EmergencyCallScreeningService : CallScreeningService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        val isIncoming = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            callDetails.callDirection == Call.Details.DIRECTION_INCOMING
        } else {
            true
        }
        val incomingNumber = callDetails.handle?.schemeSpecificPart?.trim().orEmpty()
        val nowMillis = System.currentTimeMillis()

        if (!isIncoming || incomingNumber.isBlank()) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        serviceScope.launch {
            var responseBuilder = CallResponse.Builder()
            runCatching {
                val db = AppDatabase.getInstance(applicationContext)
                val prefs = PreferencesManager(applicationContext)
                val repository = SmartCompanionRepository(db, prefs)
                val appPrefs = repository.preferences.first()
                val contacts = repository.getAllContactsOnce()
                val rules = repository.getAllRepeatCallerRulesOnce()

                val phoneControlManager = PhoneControlManager(AndroidPhoneControlGateway(applicationContext))
                val backupStore = SharedPreferencesEmergencyBypassBackupStore(applicationContext)
                val tracker = RepeatCallTracker(SharedPreferencesRepeatCallHistoryStore(applicationContext))
                val coordinator = IncomingCallBypassCoordinator(phoneControlManager, backupStore, tracker)

                val maxWindow = maxOf(
                    appPrefs.repeatCallWindowMinutes,
                    rules.maxOfOrNull { it.windowMinutes } ?: 15
                )
                val seedLogs = CallLogRepeatReader.readRecentCallTimestampsForNumber(
                    context = applicationContext,
                    phoneNumber = incomingNumber,
                    windowMinutes = maxWindow,
                    nowMillis = nowMillis
                )
                val isStarred = PhoneContactPickerResolver.isNumberStarredInSystemContacts(
                    context = applicationContext,
                    phoneNumber = incomingNumber
                )

                val result = coordinator.onIncomingCallRinging(
                    phoneNumber = incomingNumber,
                    timestampMillis = nowMillis,
                    emergencyContacts = contacts,
                    perPersonRules = rules,
                    globalEnabled = appPrefs.repeatCallsEnabled,
                    globalRequiredCalls = appPrefs.repeatCallCount,
                    globalWindowMinutes = appPrefs.repeatCallWindowMinutes,
                    seedCallLogTimestamps = seedLogs,
                    isStarredInOs = isStarred
                )

                if (result.evaluation.shouldAllowBypass) {
                    responseBuilder = responseBuilder
                        .setDisallowCall(false)
                        .setRejectCall(false)
                        .setSilenceCall(false)
                }
            }
            respondToCall(callDetails, responseBuilder.build())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
