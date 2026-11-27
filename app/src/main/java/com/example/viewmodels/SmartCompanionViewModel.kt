package com.example.viewmodels

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.assistant.AssistantStateSnapshot
import com.example.assistant.CompanionAssistantEngine
import com.example.assistant.LocalDemoAssistantEngine
import com.example.assistant.RoutineTimeParser
import com.example.automation.InMemoryRoutineStateBackupStore
import com.example.automation.NoOpRoutineAlarmScheduler
import com.example.automation.RoutineAlarmCalculator
import com.example.automation.RoutineAlarmSchedulerGateway
import com.example.automation.RoutinePhoneController
import com.example.automation.RoutineStateBackupStore
import com.example.automation.SavedPhoneBackup
import com.example.data.AppPreferences
import com.example.models.ActionType
import com.example.models.ActiveFocusSession
import com.example.models.ActivityEvent
import com.example.models.AppLanguage
import com.example.models.AssistantMessage
import com.example.models.ConflictResolutionPolicy
import com.example.models.DayFilter
import com.example.models.EventCategory
import com.example.models.ExceptionType
import com.example.models.FocusSessionRecord
import com.example.models.InterruptionDecision
import com.example.models.InterruptionType
import com.example.models.NotificationStat
import com.example.models.PriorityContact
import com.example.models.RoutineLogEntry
import com.example.models.RoutineLogEventType
import com.example.models.RoutineProposal
import com.example.models.RuleAction
import com.example.models.RuleException
import com.example.models.RuleTrigger
import com.example.models.SimulatedActivity
import com.example.models.SimulatedLocation
import com.example.models.SimulationContextState
import com.example.models.SimulationTrace
import com.example.models.SmartProfile
import com.example.models.SmartRoutine
import com.example.models.SoundMode
import com.example.models.ThemeMode
import com.example.models.TriggerType
import com.example.repositories.SmartCompanionRepository
import com.example.simulation.AutomationSimulationEngine
import com.example.simulation.DeviceActionCommand
import com.example.simulation.DeviceActionExecutor
import com.example.simulation.EngineEvaluationOutput
import com.example.simulation.PhoneControlManager
import com.example.simulation.PhoneControlState
import com.example.simulation.SimulatedDeviceActionExecutor
import com.example.simulation.SimulationEvent
import com.example.utils.TimeUtils
import com.example.utils.stringsFor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class PendingConflictPrompt(
    val trace: SimulationTrace,
    val candidateRoutines: List<SmartRoutine>,
    val triggerLabel: String
)

data class DemoScenarioStepResult(
    val scenarioTitle: String,
    val steps: List<String>,
    val finalOutcome: String
)

data class SavePermissionDialogState(
    val routineName: String,
    val missingDndAccess: Boolean,
    val missingExactAlarm: Boolean,
    val realChangeFailed: Boolean = false,
    val permissionRequiredForRun: Boolean = false,
    val failureNote: String? = null
)

data class SmartCompanionUiState(
    val isReady: Boolean = false,
    val preferences: AppPreferences = AppPreferences(),
    val profiles: List<SmartProfile> = emptyList(),
    val routines: List<SmartRoutine> = emptyList(),
    val contacts: List<PriorityContact> = emptyList(),
    val activityEvents: List<ActivityEvent> = emptyList(),
    val focusSessions: List<FocusSessionRecord> = emptyList(),
    val notificationStats: List<NotificationStat> = emptyList(),
    val activeFocus: ActiveFocusSession = ActiveFocusSession(),
    val lastCompletedFocusSummary: String? = null,
    val latestSimulationTrace: SimulationTrace? = null,
    val lastInterruptionDecision: InterruptionDecision? = null,
    val pendingConflict: PendingConflictPrompt? = null,
    val assistantMessages: List<AssistantMessage> = emptyList(),
    val isAssistantThinking: Boolean = false,
    val routineDraftFromAssistant: SmartRoutine? = null,
    val demoScenarioResult: DemoScenarioStepResult? = null,
    val feedbackBanner: String? = null,
    val phoneControl: PhoneControlState = PhoneControlState(),
    val routineLogs: List<RoutineLogEntry> = emptyList(),
    val dndPermissionAlertRoutine: String? = null,
    val exactAlarmDeniedNotice: Boolean = false,
    val savePermissionDialog: SavePermissionDialogState? = null,
    val activePhoneBackup: SavedPhoneBackup? = null,
    val toastMessage: String? = null
) {
    val activeProfile: SmartProfile?
        get() = profiles.firstOrNull { it.id == preferences.activeProfileId } ?: profiles.firstOrNull()
}

class SmartCompanionViewModel(
    private val repository: SmartCompanionRepository,
    private val simulationEngine: AutomationSimulationEngine = AutomationSimulationEngine(),
    private val deviceActionExecutor: DeviceActionExecutor = SimulatedDeviceActionExecutor(),
    private val assistantEngine: CompanionAssistantEngine = LocalDemoAssistantEngine(),
    private val phoneControlManager: PhoneControlManager = PhoneControlManager(),
    private val routineBackupStore: RoutineStateBackupStore = InMemoryRoutineStateBackupStore(),
    private val alarmScheduler: RoutineAlarmSchedulerGateway = NoOpRoutineAlarmScheduler(),
    private val onDndPermissionMissingNotification: (routineName: String, isArabic: Boolean) -> Unit = { _, _ -> }
) : ViewModel() {

    private val routinePhoneController = RoutinePhoneController(phoneControlManager, routineBackupStore)

    private val _activeFocus = MutableStateFlow(ActiveFocusSession())
    private val _lastCompletedFocusSummary = MutableStateFlow<String?>(null)
    private val _latestTrace = MutableStateFlow<SimulationTrace?>(null)
    private val _lastInterruption = MutableStateFlow<InterruptionDecision?>(null)
    private val _pendingConflict = MutableStateFlow<PendingConflictPrompt?>(null)
    private val _assistantMessages = MutableStateFlow(initialAssistantMessages(AppLanguage.ENGLISH))
    private val _isAssistantThinking = MutableStateFlow(false)
    private val _routineDraftFromAssistant = MutableStateFlow<SmartRoutine?>(null)
    private val _demoScenarioResult = MutableStateFlow<DemoScenarioStepResult?>(null)
    private val _feedbackBanner = MutableStateFlow<String?>(null)
    private val _isReady = MutableStateFlow(false)
    private val _dndPermissionAlertRoutine = MutableStateFlow<String?>(null)
    private val _exactAlarmDeniedNotice = MutableStateFlow(false)
    private val _savePermissionDialog = MutableStateFlow<SavePermissionDialogState?>(null)
    private val _activePhoneBackup = MutableStateFlow<SavedPhoneBackup?>(routineBackupStore.getBackup())
    private val _toastMessage = MutableStateFlow<String?>(null)

    private var focusTimerJob: Job? = null

    private val coreDataFlow = combine(
        repository.preferences,
        repository.profiles,
        repository.routines,
        repository.contacts,
        repository.activityEvents
    ) { prefs, profiles, routines, contacts, events ->
        CoreBundle(prefs, profiles, routines, contacts, events)
    }

    private val secondaryDataFlow = combine(
        repository.focusSessions,
        repository.notificationStats,
        _activeFocus,
        _latestTrace,
        _lastInterruption
    ) { focus, stats, activeFocus, trace, interruption ->
        SecondaryBundle(focus, stats, activeFocus, trace, interruption)
    }

    private val interactiveDataFlow = combine(
        _pendingConflict,
        _assistantMessages,
        _isAssistantThinking,
        _feedbackBanner,
        _isReady
    ) { conflict, messages, thinking, banner, ready ->
        InteractiveBundle(conflict, messages, thinking, banner, ready)
    }

    private val extraDataFlow = combine(
        _lastCompletedFocusSummary,
        _demoScenarioResult,
        _routineDraftFromAssistant,
        phoneControlManager.state,
        repository.routineLogs
    ) { focusSummary, scenarioResult, draftRoutine, phoneControl, routineLogs ->
        ExtraBundle(focusSummary, scenarioResult, draftRoutine, phoneControl, routineLogs)
    }

    private val alertDataFlow = combine(
        _dndPermissionAlertRoutine,
        _exactAlarmDeniedNotice,
        _savePermissionDialog,
        _activePhoneBackup,
        _toastMessage
    ) { dndAlert, exactDenied, saveDialog, phoneBackup, toastMessage ->
        AlertBundle(dndAlert, exactDenied, saveDialog, phoneBackup, toastMessage)
    }

    val uiState: StateFlow<SmartCompanionUiState> = combine(
        coreDataFlow,
        secondaryDataFlow,
        interactiveDataFlow,
        extraDataFlow,
        alertDataFlow
    ) { core, sec, inter, extra, alerts ->
        SmartCompanionUiState(
            isReady = inter.isReady,
            preferences = core.prefs,
            profiles = core.profiles,
            routines = core.routines,
            contacts = core.contacts,
            activityEvents = core.events,
            focusSessions = sec.focusSessions,
            notificationStats = sec.notificationStats,
            activeFocus = sec.activeFocus,
            lastCompletedFocusSummary = extra.focusSummary,
            latestSimulationTrace = sec.trace,
            lastInterruptionDecision = sec.interruption,
            pendingConflict = inter.pendingConflict,
            assistantMessages = inter.assistantMessages,
            isAssistantThinking = inter.isAssistantThinking,
            routineDraftFromAssistant = extra.draftRoutine,
            demoScenarioResult = extra.scenarioResult,
            feedbackBanner = inter.feedbackBanner,
            phoneControl = extra.phoneControl,
            routineLogs = extra.routineLogs,
            dndPermissionAlertRoutine = alerts.dndAlert,
            exactAlarmDeniedNotice = alerts.exactDenied,
            savePermissionDialog = alerts.saveDialog,
            activePhoneBackup = alerts.phoneBackup,
            toastMessage = alerts.toastMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SmartCompanionUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            rescheduleRoutinesInternal(repository.getAllRoutinesOnce())
            refreshPhoneControlState()
            _isReady.value = true
        }
    }

    private fun isArabic(): Boolean = uiState.value.preferences.language == AppLanguage.ARABIC

    fun clearFeedbackBanner() {
        _feedbackBanner.value = null
    }

    private fun showBanner(msg: String) {
        _feedbackBanner.value = msg
    }

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    private fun showRestoreToast(msg: String) {
        _toastMessage.value = msg
    }

    private fun formatArabicRestoredToast(
        ringer: SoundMode,
        interruptionFilter: Int,
        extraSuffix: String = ""
    ): String {
        val dndOn = interruptionFilter != android.app.NotificationManager.INTERRUPTION_FILTER_ALL &&
            interruptionFilter != android.app.NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        val dndAr = if (dndOn) "عدم الإزعاج مفعّل" else "عدم الإزعاج متوقف"
        return "تمت استعادة الحالة السابقة: ${ringer.arabicName} • $dndAr$extraSuffix"
    }

    fun refreshPhoneControlState() {
        val refreshed = phoneControlManager.refreshPermissionAndDeviceState()
        if (refreshed.hasDndAccess) {
            routineBackupStore.setDndPermissionMissing(false, null)
            _dndPermissionAlertRoutine.value = null
        } else if (routineBackupStore.isDndPermissionMissing()) {
            _dndPermissionAlertRoutine.value = routineBackupStore.getDndMissingRoutineName() ?: ""
        }
        if (refreshed.canScheduleExactAlarms) {
            _exactAlarmDeniedNotice.value = false
        }
    }

    fun dismissDndPermissionAlert() {
        routineBackupStore.setDndPermissionMissing(false, null)
        _dndPermissionAlertRoutine.value = null
    }

    fun dismissSavePermissionDialog() {
        _savePermissionDialog.value = null
    }

    fun clearRoutineLogs() {
        viewModelScope.launch {
            repository.clearRoutineLogs()
            showBanner(if (isArabic()) "تم مسح سجل الروتين" else "Routine log cleared")
        }
    }

    fun createDndAccessSettingsIntent(context: Context): Intent {
        return phoneControlManager.createDndAccessSettingsIntent(context)
    }

    fun createExactAlarmSettingsIntent(context: Context): Intent {
        return phoneControlManager.createExactAlarmSettingsIntent(context)
    }

    fun rescheduleAllAlarms() {
        viewModelScope.launch {
            rescheduleRoutinesInternal(repository.getAllRoutinesOnce())
        }
    }

    private fun rescheduleRoutinesInternal(routines: List<SmartRoutine>): Boolean {
        val result = alarmScheduler.rescheduleAll(routines)
        _exactAlarmDeniedNotice.value = result.exactAlarmPermissionDenied
        phoneControlManager.refreshPermissionAndDeviceState()
        return result.exactAlarmPermissionDenied
    }

    fun setRealRingerMode(mode: SoundMode) {
        val result = phoneControlManager.applyRingerMode(mode)
        val ar = isArabic()
        if (result.permissionRequired) {
            showBanner(
                if (ar) "يلزم منح إذن عدم الإزعاج أولاً"
                else "Do Not Disturb access is required first"
            )
            return
        }
        val modeLabel = if (ar) mode.arabicName else mode.displayName
        showBanner(
            if (result.success) {
                if (ar) "تم تغيير وضع رنين الهاتف الحقيقي إلى: $modeLabel"
                else "Phone ringer mode set to: $modeLabel"
            } else {
                if (ar) "تعذّر تغيير وضع الرنين على هذا الجهاز"
                else "Could not update device ringer mode"
            }
        )
    }

    fun toggleRealDoNotDisturb() {
        val result = phoneControlManager.toggleDoNotDisturb(allowStarredContacts = true)
        val ar = isArabic()
        if (result.permissionRequired) {
            showBanner(
                if (ar) "يلزم منح إذن عدم الإزعاج أولاً"
                else "Do Not Disturb access is required first"
            )
            return
        }
        showBanner(
            if (result.success) {
                if (result.state.isDndEnabled) {
                    if (ar) "تم تشغيل عدم الإزعاج (مع السماح لجهات الطوارئ المميزة بنجمة)"
                    else "Do Not Disturb turned On (starred contacts allowed)"
                } else {
                    if (ar) "تم إيقاف عدم الإزعاج"
                    else "Do Not Disturb turned Off"
                }
            } else {
                if (ar) "تعذّر تبديل وضع عدم الإزعاج"
                else "Could not toggle Do Not Disturb"
            }
        )
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            repository.setOnboardingCompleted(true)
            repository.logEvent(
                title = "Entered Demo Mode",
                category = EventCategory.SYSTEM,
                triggerSource = "Onboarding",
                result = "Demo workspace ready",
                explanation = "User completed onboarding and entered Smart Companion Demo Mode."
            )
        }
    }

    fun reopenOnboarding() {
        viewModelScope.launch {
            repository.setOnboardingCompleted(false)
        }
    }

    fun activateProfile(profile: SmartProfile, triggerSource: String = "User Dashboard") {
        viewModelScope.launch {
            val current = uiState.value
            val appStrings = stringsFor(current.preferences.language)
            val localizedName = appStrings.localizedProfileName(profile)
            val ar = current.preferences.language == AppLanguage.ARABIC
            val permState = phoneControlManager.refreshPermissionAndDeviceState()
            if (!permState.hasDndAccess) {
                _savePermissionDialog.value = SavePermissionDialogState(
                    routineName = localizedName,
                    missingDndAccess = true,
                    missingExactAlarm = false,
                    realChangeFailed = true,
                    failureNote = if (ar) "تعذر تفعيل الوضع: إذن الوصول لعدم الإزعاج غير ممنوح" else "Failed to activate profile: Do Not Disturb access not granted"
                )
                return@launch
            }

            val wantsDnd = profile.soundMode == SoundMode.SILENT || profile.id in listOf("profile_study", "profile_sleep")
            val allowStarred = profile.allowHighPriorityContacts || profile.priorityContactBehavior.contains("Emergency", ignoreCase = true)

            // Save previous state before changing if no active backup exists
            if (routineBackupStore.getBackup() == null) {
                val prevRinger = phoneControlManager.currentRingerMode()
                val prevFilter = phoneControlManager.currentInterruptionFilter()
                routineBackupStore.onRoutineStarted(
                    routineId = "mode_${profile.id}",
                    currentRingerMode = prevRinger,
                    currentInterruptionFilter = prevFilter,
                    timestampMillis = System.currentTimeMillis()
                )
                _activePhoneBackup.value = routineBackupStore.getBackup()
            }

            val ringerRes = phoneControlManager.applyRingerMode(profile.soundMode)
            val dndRes = phoneControlManager.setDoNotDisturbEnabled(wantsDnd, allowStarredContacts = allowStarred)

            if (!ringerRes.success || !dndRes.success) {
                _savePermissionDialog.value = SavePermissionDialogState(
                    routineName = localizedName,
                    missingDndAccess = ringerRes.permissionRequired || dndRes.permissionRequired,
                    missingExactAlarm = false,
                    realChangeFailed = true,
                    failureNote = if (ar) "تعذر تطبيق وضع الصوت أو عدم الإزعاج على الهاتف" else "Failed to apply sound mode or Do Not Disturb on device"
                )
                return@launch
            }

            val execResult = deviceActionExecutor.execute(
                DeviceActionCommand.ActivateProfileState(profile.id, localizedName, profile.soundMode)
            )
            repository.savePhoneState(
                activeProfileId = profile.id,
                soundMode = profile.soundMode,
                isAutomationPaused = current.preferences.isAutomationPaused
            )

            val soundLabel = appStrings.soundModeLabel(profile.soundMode)
            val dndStr = if (wantsDnd) (if (ar) " • عدم الإزعاج مفعّل" else " • DND On") else ""

            repository.addRoutineLog(
                RoutineLogEntry(
                    id = "rlog_${UUID.randomUUID().toString().take(8)}",
                    routineId = "mode_${profile.id}",
                    routineName = localizedName,
                    eventType = RoutineLogEventType.STARTED,
                    timestampMillis = System.currentTimeMillis(),
                    ringerMode = profile.soundMode,
                    dndEnabled = wantsDnd,
                    interruptionFilter = phoneControlManager.currentInterruptionFilter(),
                    note = if (ar) "تم تفعيل وضع $localizedName ($soundLabel$dndStr)" else "Activated $localizedName ($soundLabel$dndStr)"
                )
            )

            repository.logEvent(
                title = if (ar) "تم تفعيل وضع: $localizedName" else "$localizedName Profile Activated",
                category = EventCategory.PROFILE,
                triggerSource = triggerSource,
                result = "$soundLabel$dndStr",
                explanation = if (ar) "تم تفعيل وضع $localizedName ($soundLabel$dndStr)." else "$localizedName profile enabled ($soundLabel$dndStr).",
                relatedProfileName = localizedName
            )
            showBanner(
                if (ar) "تم تفعيل وضع $localizedName ($soundLabel$dndStr)"
                else "$localizedName Profile activated ($soundLabel$dndStr)"
            )
        }
    }

    fun setSimulatedSoundMode(mode: SoundMode, triggerSource: String = "Manual Override") {
        viewModelScope.launch {
            val current = uiState.value
            val execResult = deviceActionExecutor.execute(
                DeviceActionCommand.ApplySoundMode(mode, triggerSource)
            )
            repository.savePhoneState(
                activeProfileId = current.preferences.activeProfileId,
                soundMode = mode,
                isAutomationPaused = current.preferences.isAutomationPaused
            )
            repository.logEvent(
                title = "Simulated Sound Mode -> ${mode.displayName}",
                category = EventCategory.SOUND_MODE,
                triggerSource = triggerSource,
                result = execResult.summary,
                explanation = "User changed simulated sound state without modifying real device audio.",
                relatedProfileName = current.activeProfile?.name
            )
            showBanner(
                if (isArabic()) "تم ضبط الصوت المحاكى على: ${mode.arabicName}"
                else "Simulated phone sound set to ${mode.displayName}"
            )
        }
    }

    fun toggleGlobalAutomationPause() {
        viewModelScope.launch {
            val current = uiState.value
            val ar = current.preferences.language == AppLanguage.ARABIC
            val appStrings = stringsFor(current.preferences.language)
            val newPaused = !current.preferences.isAutomationPaused
            var nextProfileId = current.preferences.activeProfileId
            var nextSoundMode = current.preferences.simulatedSoundMode

            if (newPaused) {
                val backup = routineBackupStore.getBackup()
                val allRoutines = repository.getAllRoutinesOnce()
                var restoredRinger: SoundMode? = null
                var restoredFilter: Int? = null
                var restoredModeBackup = false
                backup?.activeRoutineIds?.toList()?.forEach { id ->
                    val name = if (id.startsWith("mode_")) {
                        val pId = id.removePrefix("mode_")
                        val prof = current.profiles.firstOrNull { it.id == pId }
                        if (prof != null) appStrings.localizedProfileName(prof) else appStrings.localizedProfileName(pId, pId)
                    } else {
                        val r = allRoutines.firstOrNull { it.id == id }
                        if (r != null) appStrings.localizedRoutineName(r) else id
                    }
                    val outcome = routinePhoneController.applyRoutineEnd(id, name, ar)
                    repository.addRoutineLog(outcome.logEntry)
                    if (outcome.permissionRequired) {
                        _savePermissionDialog.value = SavePermissionDialogState(
                            routineName = name,
                            missingDndAccess = true,
                            missingExactAlarm = false,
                            realChangeFailed = true,
                            failureNote = outcome.logEntry.note
                        )
                    } else if (outcome.success && outcome.logEntry.ringerMode != null) {
                        restoredRinger = outcome.logEntry.ringerMode
                        restoredFilter = outcome.logEntry.interruptionFilter
                        if (id.startsWith("mode_")) restoredModeBackup = true
                    }
                }
                _activePhoneBackup.value = routineBackupStore.getBackup()
                if (restoredRinger != null) {
                    if (restoredModeBackup) {
                        nextProfileId = "profile_personal"
                        nextSoundMode = restoredRinger!!
                    }
                    showRestoreToast(
                        formatArabicRestoredToast(
                            restoredRinger!!,
                            restoredFilter ?: android.app.NotificationManager.INTERRUPTION_FILTER_ALL
                        )
                    )
                }
            }

            repository.savePhoneState(
                activeProfileId = nextProfileId,
                soundMode = nextSoundMode,
                isAutomationPaused = newPaused
            )

            val activeProfileName = current.activeProfile?.let { appStrings.localizedProfileName(it) }
            repository.logEvent(
                title = if (ar) {
                    if (newPaused) "تم إيقاف الأتمتة مؤقتاً" else "تم استئناف الأتمتة"
                } else {
                    if (newPaused) "Global Automation Paused" else "Automation Resumed"
                },
                category = EventCategory.AUTOMATION,
                triggerSource = if (ar) "أدوات الأمان" else "Safety Control",
                result = if (ar) {
                    if (newPaused) "تم إيقاف المشغلات التلقائية" else "المشغلات المجدولة والسياقية نشطة"
                } else {
                    if (newPaused) "All automatic triggers paused" else "Scheduled & context triggers active"
                },
                explanation = if (newPaused) {
                    "Safety pause suspended automatic routine triggers while preserving existing rules."
                } else {
                    "Smart Companion automation resumed normal evaluation."
                },
                relatedProfileName = activeProfileName
            )
            showBanner(
                if (ar) {
                    if (newPaused) "تم إيقاف جميع الأتمتة مؤقتاً" else "تم استئناف الأتمتة"
                } else {
                    if (newPaused) "All automations paused" else "Automations resumed"
                }
            )
        }
    }

    fun restorePreviousPhoneState() {
        viewModelScope.launch {
            val current = uiState.value
            val ar = current.preferences.language == AppLanguage.ARABIC
            val appStrings = stringsFor(current.preferences.language)
            val prev = current.preferences.previousSnapshot
            val prevProfile = current.profiles.firstOrNull { it.id == prev.activeProfileId }
            val prevProfileName = prevProfile?.let { appStrings.localizedProfileName(it) }
                ?: appStrings.localizedProfileName(prev.activeProfileId, "Personal")
            val prevSoundLabel = appStrings.soundModeLabel(prev.soundMode)
            deviceActionExecutor.execute(
                DeviceActionCommand.ActivateProfileState(prev.activeProfileId, prevProfileName, prev.soundMode)
            )
            repository.savePhoneState(
                activeProfileId = prev.activeProfileId,
                soundMode = prev.soundMode,
                isAutomationPaused = prev.isAutomationPaused,
                recordPreviousSnapshot = false
            )
            repository.logEvent(
                title = if (ar) "تمت استعادة الحالة السابقة" else "Previous State Restored",
                category = EventCategory.SYSTEM,
                triggerSource = if (ar) "استرداد الأمان" else "Safety Recovery",
                result = "$prevProfileName • $prevSoundLabel",
                explanation = "Safety Center rolled back phone profile and sound state to the previous snapshot.",
                relatedProfileName = prevProfileName
            )
            showBanner(
                if (ar) "تمت استعادة الحالة السابقة: $prevProfileName ($prevSoundLabel)"
                else "Restored previous state: $prevProfileName ($prevSoundLabel)"
            )
            showRestoreToast("تمت استعادة الحالة السابقة: $prevProfileName • ${prev.soundMode.arabicName}")
        }
    }

    // --- Profile CRUD ---
    fun saveProfile(profile: SmartProfile) {
        viewModelScope.launch {
            repository.upsertProfile(profile)
            repository.logEvent(
                title = "Saved Profile: ${profile.name}",
                category = EventCategory.PROFILE,
                triggerSource = "Smart Profiles",
                result = "Sound=${profile.soundMode.displayName}",
                explanation = "Updated profile configuration and interruption rules.",
                relatedProfileName = profile.name
            )
            showBanner(if (isArabic()) "تم حفظ الوضع '${profile.name}'" else "Saved profile '${profile.name}'")
        }
    }

    fun duplicateProfile(profile: SmartProfile) {
        viewModelScope.launch {
            val copy = profile.copy(
                id = "profile_custom_${UUID.randomUUID().toString().take(6)}",
                name = if (isArabic()) "${profile.name} (نسخة)" else "${profile.name} Copy",
                isDefault = false,
                isProtected = false
            )
            repository.upsertProfile(copy)
            repository.logEvent(
                title = "Duplicated Profile: ${copy.name}",
                category = EventCategory.PROFILE,
                triggerSource = "Smart Profiles",
                result = "Created customizable copy",
                explanation = "Duplicated ${profile.name} so user can customize rules safely.",
                relatedProfileName = copy.name
            )
            showBanner(if (isArabic()) "تم تكرار '${profile.name}'" else "Duplicated '${profile.name}'")
        }
    }

    fun deleteProfile(profile: SmartProfile) {
        viewModelScope.launch {
            if (profile.isProtected) {
                showBanner(
                    if (isArabic()) "لا يمكن حذف الأوضاع الافتراضية المحمية. يمكنك تعديلها أو تكرارها."
                    else "Default protected profiles cannot be deleted. You can edit or duplicate them."
                )
                return@launch
            }
            if (uiState.value.preferences.activeProfileId == profile.id) {
                val fallback = uiState.value.profiles.firstOrNull { it.id == "profile_personal" }
                if (fallback != null) {
                    repository.savePhoneState(
                        fallback.id,
                        fallback.soundMode,
                        uiState.value.preferences.isAutomationPaused
                    )
                }
            }
            repository.deleteProfile(profile.id)
            repository.logEvent(
                title = "Deleted Custom Profile: ${profile.name}",
                category = EventCategory.PROFILE,
                triggerSource = "Smart Profiles",
                result = "Removed custom profile",
                explanation = "Custom profile deleted after user confirmation."
            )
            showBanner(if (isArabic()) "تم حذف الوضع '${profile.name}'" else "Deleted profile '${profile.name}'")
        }
    }

    fun restoreDefaultProfiles() {
        viewModelScope.launch {
            val current = uiState.value
            val ar = current.preferences.language == AppLanguage.ARABIC
            val appStrings = stringsFor(current.preferences.language)
            val backup = routineBackupStore.getBackup()
            var restoredDetails = ""
            var targetSoundMode = SoundMode.SOUND
            var permissionMissingForRestore = false

            if (backup != null) {
                val stateNow = phoneControlManager.refreshPermissionAndDeviceState()
                if (stateNow.hasDndAccess) {
                    phoneControlManager.restoreDeviceState(
                        ringerMode = backup.previousRingerMode,
                        interruptionFilter = backup.previousInterruptionFilter
                    )
                    val dndOn = backup.previousInterruptionFilter != android.app.NotificationManager.INTERRUPTION_FILTER_ALL &&
                        backup.previousInterruptionFilter != android.app.NotificationManager.INTERRUPTION_FILTER_UNKNOWN
                    val soundName = appStrings.soundModeLabel(backup.previousRingerMode)
                    val dndStr = if (dndOn) (if (ar) " • عدم الإزعاج مفعّل" else " • DND On") else (if (ar) " • عدم الإزعاج متوقف" else " • DND Off")
                    restoredDetails = if (ar) " — تمت استعادة وضع الصوت ($soundName$dndStr)" else " — Restored audio ($soundName$dndStr)"
                    targetSoundMode = backup.previousRingerMode
                    routineBackupStore.clearAll()
                    _activePhoneBackup.value = null

                    repository.addRoutineLog(
                        RoutineLogEntry(
                            id = "rlog_${UUID.randomUUID().toString().take(8)}",
                            routineId = "restore_defaults",
                            routineName = if (ar) "استعادة الافتراضي" else "Restore Defaults",
                            eventType = RoutineLogEventType.ENDED,
                            timestampMillis = System.currentTimeMillis(),
                            ringerMode = backup.previousRingerMode,
                            dndEnabled = dndOn,
                            interruptionFilter = backup.previousInterruptionFilter,
                            note = formatArabicRestoredToast(backup.previousRingerMode, backup.previousInterruptionFilter, " والأوضاع الافتراضية")
                        )
                    )
                    showRestoreToast(
                        formatArabicRestoredToast(
                            backup.previousRingerMode,
                            backup.previousInterruptionFilter,
                            " والأوضاع الافتراضية"
                        )
                    )
                } else {
                    permissionMissingForRestore = true
                    _savePermissionDialog.value = SavePermissionDialogState(
                        routineName = if (ar) "استعادة الافتراضي" else "Restore Defaults",
                        missingDndAccess = true,
                        missingExactAlarm = false,
                        realChangeFailed = true,
                        failureNote = if (ar) "تعذرت استعادة صوت الهاتف: إذن الوصول لعدم الإزعاج غير ممنوح" else "Could not restore phone state: Do Not Disturb access is not granted"
                    )
                }
            } else {
                showRestoreToast("تمت استعادة الأوضاع الافتراضية")
            }

            repository.restoreDefaultProfiles()
            repository.savePhoneState(
                activeProfileId = "profile_personal",
                soundMode = targetSoundMode,
                isAutomationPaused = current.preferences.isAutomationPaused,
                recordPreviousSnapshot = false
            )
            repository.logEvent(
                title = if (ar) "تمت استعادة الأوضاع الافتراضية" else "Default Profiles Restored",
                category = EventCategory.SYSTEM,
                triggerSource = if (ar) "الأمان والاسترداد" else "Safety & Recovery",
                result = if (ar) "تمت استعادة 5 أوضاع افتراضية$restoredDetails" else "5 core profiles restored$restoredDetails",
                explanation = if (ar) "تمت استعادة أوضاع الدراسة والعمل والنوم والشخصي والمخصص إلى إعداداتها الأصلية." else "Restored Study, Work, Sleep, Personal, and Custom Focus defaults.",
                relatedProfileName = appStrings.localizedProfileName("profile_personal", "Personal")
            )
            if (!permissionMissingForRestore) {
                val msg = if (ar) "تمت استعادة الأوضاع الافتراضية$restoredDetails" else "Default profiles restored$restoredDetails"
                showBanner(msg)
            }
        }
    }

    // --- Routine CRUD & Simulation Engine ---
    @JvmOverloads
    fun saveRoutine(routine: SmartRoutine, startImmediately: Boolean = false) {
        viewModelScope.launch {
            saveRoutineInternal(routine, startImmediately)
        }
    }

    private suspend fun saveRoutineInternal(
        routine: SmartRoutine,
        startImmediately: Boolean = false
    ): Boolean {
        val current = uiState.value
        val appStrings = stringsFor(current.preferences.language)
        val localizedRoutineName = appStrings.localizedRoutineName(routine)
        val permStateBeforeSave = phoneControlManager.refreshPermissionAndDeviceState()
        val missingDndBeforeSave = !permStateBeforeSave.hasDndAccess
        val missingExactBeforeSave = !(permStateBeforeSave.canScheduleExactAlarms && alarmScheduler.canScheduleExactAlarms())
        if (missingExactBeforeSave) {
            _exactAlarmDeniedNotice.value = true
        }

        repository.upsertRoutine(routine)
        _routineDraftFromAssistant.value = null
        repository.logEvent(
            title = if (isArabic()) "تم حفظ الروتين: $localizedRoutineName" else "Saved Routine: $localizedRoutineName",
            category = EventCategory.ROUTINE,
            triggerSource = if (isArabic()) "منشئ القواعد" else "Visual Rule Builder",
            result = routine.trigger.summary,
            explanation = "Rule saved with ${routine.actions.size} actions and ${routine.exceptions.size} exceptions.",
            relatedRoutineName = localizedRoutineName
        )
        val allRoutines = repository.getAllRoutinesOnce()
        val exactDenied = rescheduleRoutinesInternal(allRoutines) || missingExactBeforeSave

        if (startImmediately && routine.isEnabled) {
            val state = uiState.value
            // 1. Apply real phone control BEFORE the simulation banner
            val realOutcome = applyRealPhoneControlForRoutine(
                routine = routine,
                profiles = state.profiles,
                scheduleEndAlarm = true,
                showMissingDndBanner = false
            )
            _activePhoneBackup.value = routineBackupStore.getBackup()

            val routinesWithSaved = state.routines.filterNot { it.id == routine.id } + routine
            val output = simulationEngine.evaluate(
                event = SimulationEvent.ManualRoutine(routine.id),
                routines = routinesWithSaved,
                profiles = state.profiles,
                context = state.preferences.simulatedContext,
                isAutomationPaused = state.preferences.isAutomationPaused,
                activeProfileId = state.preferences.activeProfileId,
                isFocusActive = state.activeFocus.isActive,
                conflictPolicy = state.preferences.conflictPolicy
            )
            _latestTrace.value = output.trace
            applyEngineOutput(output, showCompletionBanner = false)

            if (realOutcome.success) {
                val activatedMsg = if (isArabic()) {
                    "تم تفعيل '$localizedRoutineName' على الهاتف"
                } else {
                    output.inAppNotice ?: "$localizedRoutineName activated"
                }
                showBanner(activatedMsg)
                if (exactDenied) {
                    _savePermissionDialog.value = SavePermissionDialogState(
                        routineName = localizedRoutineName,
                        missingDndAccess = false,
                        missingExactAlarm = true
                    )
                }
                return true
            } else {
                // Never show an "activated" message when real change failed; show persistent dialog
                _savePermissionDialog.value = SavePermissionDialogState(
                    routineName = localizedRoutineName,
                    missingDndAccess = realOutcome.permissionRequired || missingDndBeforeSave,
                    missingExactAlarm = exactDenied,
                    realChangeFailed = true,
                    permissionRequiredForRun = realOutcome.permissionRequired,
                    failureNote = realOutcome.logEntry.note
                )
                return false
            }
        } else if (missingDndBeforeSave || exactDenied) {
            _savePermissionDialog.value = SavePermissionDialogState(
                routineName = localizedRoutineName,
                missingDndAccess = missingDndBeforeSave,
                missingExactAlarm = exactDenied
            )
            return true
        } else {
            showBanner(if (isArabic()) "تم حفظ الروتين '$localizedRoutineName'" else "Saved routine '$localizedRoutineName'")
            return true
        }
    }

    private suspend fun applyRealPhoneControlForRoutine(
        routine: SmartRoutine,
        profiles: List<com.example.models.SmartProfile>,
        scheduleEndAlarm: Boolean,
        showMissingDndBanner: Boolean = false
    ): com.example.automation.RoutineControlOutcome {
        val now = System.currentTimeMillis()
        val ar = isArabic()
        val outcome = routinePhoneController.applyRoutineStart(
            routine = routine,
            profiles = profiles,
            isArabic = ar,
            timestampMillis = now
        )
        if (outcome.permissionRequired) {
            repository.addRoutineLog(outcome.logEntry)
            _dndPermissionAlertRoutine.value = routine.name
            onDndPermissionMissingNotification(routine.name, ar)
            if (showMissingDndBanner) {
                showBanner(
                    if (ar) "مطلوب إذن الوصول لعدم الإزعاج لتشغيل '${routine.name}'"
                    else "Do Not Disturb access is required to apply '${routine.name}'"
                )
            }
            return outcome
        }

        if (outcome.success && scheduleEndAlarm && RoutineAlarmCalculator.isTimeSchedulable(routine)) {
            val endAt = RoutineAlarmCalculator.computeImmediateEndTimeMillis(routine, now)
            val endRes = alarmScheduler.scheduleImmediateEndAlarm(routine, endAt)
            if (endRes.exactAlarmPermissionDenied) {
                _exactAlarmDeniedNotice.value = true
            }
        }
        repository.addRoutineLog(outcome.logEntry)
        return outcome
    }

    fun testSilentFor60Seconds() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val endAt = now + 60_000L
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = now }
            val endCal = java.util.Calendar.getInstance().apply { timeInMillis = endAt }
            val ar = isArabic()
            val debugRoutine = SmartRoutine(
                id = "routine_debug_silent_60s",
                name = if (ar) "اختبار صامت 60 ثانية (Debug)" else "Debug Silent 60s",
                description = if (ar) "اختبار مؤقت لتطبيق الصامت لمدة 60 ثانية ثم استعادة الوضع السابق"
                else "Temporary test applying Silent for 60 seconds then restoring previous state",
                isEnabled = true,
                trigger = RuleTrigger(
                    type = TriggerType.SPECIFIC_TIME,
                    hour = cal.get(java.util.Calendar.HOUR_OF_DAY),
                    minute = cal.get(java.util.Calendar.MINUTE),
                    dayFilter = DayFilter.EVERY_DAY,
                    summary = "Debug 60s"
                ),
                conditions = emptyList(),
                actions = listOf(
                    RuleAction(
                        type = ActionType.SET_SOUND_MODE,
                        targetValue = SoundMode.SILENT.name,
                        label = if (ar) "صامت" else "Silent"
                    )
                ),
                exceptions = emptyList(),
                priority = 10,
                scheduleStartTime = TimeUtils.formatTime12h(
                    cal.get(java.util.Calendar.HOUR_OF_DAY),
                    cal.get(java.util.Calendar.MINUTE),
                    ar
                ),
                scheduleEndTime = TimeUtils.formatTime12h(
                    endCal.get(java.util.Calendar.HOUR_OF_DAY),
                    endCal.get(java.util.Calendar.MINUTE),
                    ar
                ),
                targetProfileId = null,
                lastExecutionResult = if (ar) "اختبار 60 ثانية" else "60s test",
                lastExecutedAtMillis = now,
                executionCount = 0,
                skippedCount = 0,
                enableDoNotDisturb = false,
                isOneTime = true
            )

            repository.upsertRoutine(debugRoutine)
            val outcome = routinePhoneController.applyRoutineStart(
                routine = debugRoutine,
                profiles = uiState.value.profiles,
                isArabic = ar,
                timestampMillis = now
            )
            repository.addRoutineLog(outcome.logEntry)

            if (outcome.permissionRequired || !outcome.success) {
                val missingExact = !(phoneControlManager.refreshPermissionAndDeviceState().canScheduleExactAlarms &&
                    alarmScheduler.canScheduleExactAlarms())
                if (outcome.permissionRequired) {
                    _dndPermissionAlertRoutine.value = debugRoutine.name
                    onDndPermissionMissingNotification(debugRoutine.name, ar)
                }
                _savePermissionDialog.value = SavePermissionDialogState(
                    routineName = debugRoutine.name,
                    missingDndAccess = outcome.permissionRequired || !phoneControlManager.state.value.hasDndAccess,
                    missingExactAlarm = missingExact,
                    realChangeFailed = true,
                    permissionRequiredForRun = outcome.permissionRequired,
                    failureNote = outcome.logEntry.note
                )
                return@launch
            }

            val endRes = alarmScheduler.scheduleImmediateEndAlarm(debugRoutine, endAt)
            if (endRes.exactAlarmPermissionDenied) {
                _exactAlarmDeniedNotice.value = true
                _savePermissionDialog.value = SavePermissionDialogState(
                    routineName = debugRoutine.name,
                    missingDndAccess = false,
                    missingExactAlarm = true
                )
            }
            showBanner(
                if (ar) "تم تطبيق الصامت الحقيقي لمدة 60 ثانية — ستتم الاستعادة تلقائياً"
                else "Applied real Silent mode for 60 seconds — auto-restore scheduled"
            )
        }
    }

    fun toggleRoutineEnabled(routine: SmartRoutine) {
        viewModelScope.launch {
            val current = uiState.value
            val ar = current.preferences.language == AppLanguage.ARABIC
            val appStrings = stringsFor(current.preferences.language)
            val localizedName = appStrings.localizedRoutineName(routine)
            val updated = routine.copy(isEnabled = !routine.isEnabled)
            repository.upsertRoutine(updated)
            if (!updated.isEnabled) {
                alarmScheduler.cancelRoutineAlarms(updated.id)
                val backup = routineBackupStore.getBackup()
                if (backup != null && backup.activeRoutineIds.contains(updated.id)) {
                    val outcome = routinePhoneController.applyRoutineEnd(updated.id, localizedName, ar)
                    repository.addRoutineLog(outcome.logEntry)
                    _activePhoneBackup.value = routineBackupStore.getBackup()
                    if (outcome.permissionRequired) {
                        _savePermissionDialog.value = SavePermissionDialogState(
                            routineName = localizedName,
                            missingDndAccess = true,
                            missingExactAlarm = false,
                            realChangeFailed = true,
                            failureNote = outcome.logEntry.note
                        )
                    } else if (outcome.success && outcome.logEntry.ringerMode != null) {
                        showRestoreToast(
                            formatArabicRestoredToast(
                                outcome.logEntry.ringerMode,
                                outcome.logEntry.interruptionFilter ?: android.app.NotificationManager.INTERRUPTION_FILTER_ALL
                            )
                        )
                    }
                }
            }
            val exactDenied = rescheduleRoutinesInternal(repository.getAllRoutinesOnce())
            repository.logEvent(
                title = if (ar) "$localizedName: ${if (updated.isEnabled) "مفعّل" else "معطّل"}" else "${updated.name} ${if (updated.isEnabled) "Enabled" else "Disabled"}",
                category = EventCategory.ROUTINE,
                triggerSource = if (ar) "الروتينات الذكية" else "Smart Routines",
                result = if (ar) {
                    if (updated.isEnabled) "نشط في محرك الأتمتة" else "متوقف عن الأتمتة"
                } else {
                    if (updated.isEnabled) "Active in automation engine" else "Excluded from automation"
                },
                explanation = "Routine toggle updated by user.",
                relatedRoutineName = localizedName
            )
            if (exactDenied && updated.isEnabled) {
                showBanner(
                    if (ar) "تعذّر جدولة المنبه الدقيق: يرجى السماح بإذن المنبهات الدقيقة"
                    else "Exact alarm permission denied — enable Alarms & reminders"
                )
            } else {
                showBanner(
                    if (ar) {
                        "$localizedName: ${if (updated.isEnabled) "مفعّل" else "معطّل"}"
                    } else {
                        "${updated.name} ${if (updated.isEnabled) "enabled" else "disabled"}"
                    }
                )
            }
        }
    }

    fun duplicateRoutine(routine: SmartRoutine) {
        viewModelScope.launch {
            val copy = routine.copy(
                id = "routine_${UUID.randomUUID().toString().take(6)}",
                name = if (isArabic()) "${routine.name} (نسخة)" else "${routine.name} (Copy)",
                executionCount = 0,
                skippedCount = 0,
                lastExecutionResult = if (isArabic()) "منسوخ — جاهز للتشغيل" else "Duplicated — ready to run",
                lastExecutedAtMillis = null
            )
            repository.upsertRoutine(copy)
            rescheduleRoutinesInternal(repository.getAllRoutinesOnce())
            repository.logEvent(
                title = "Duplicated Routine: ${copy.name}",
                category = EventCategory.ROUTINE,
                triggerSource = "Smart Routines",
                result = "New routine copy created",
                explanation = "Cloned configuration from ${routine.name}.",
                relatedRoutineName = copy.name
            )
            showBanner(if (isArabic()) "تم تكرار '${routine.name}'" else "Duplicated '${routine.name}'")
        }
    }

    fun deleteRoutine(routine: SmartRoutine) {
        viewModelScope.launch {
            alarmScheduler.cancelRoutineAlarms(routine.id)
            repository.deleteRoutine(routine.id)
            rescheduleRoutinesInternal(repository.getAllRoutinesOnce())
            repository.logEvent(
                title = "Deleted Routine: ${routine.name}",
                category = EventCategory.ROUTINE,
                triggerSource = "Smart Routines",
                result = "Routine removed",
                explanation = "Confirmed deletion of ${routine.name}.",
                relatedRoutineName = routine.name
            )
            showBanner(if (isArabic()) "تم حذف الروتين '${routine.name}'" else "Deleted routine '${routine.name}'")
        }
    }

    fun resetDemoRoutines() {
        viewModelScope.launch {
            repository.resetDemoRoutines()
            rescheduleRoutinesInternal(repository.getAllRoutinesOnce())
            repository.logEvent(
                title = "Demo Routines Reset",
                category = EventCategory.SYSTEM,
                triggerSource = "Safety & Recovery",
                result = "Initial demonstration routines restored",
                explanation = "Restored Study Time, Sleep Schedule, Work Hours, Personal Time, and Conflict Demo."
            )
            showBanner(if (isArabic()) "تمت استعادة روتينات العرض" else "Demo routines restored")
        }
    }

    fun runRoutineManually(routine: SmartRoutine) {
        viewModelScope.launch {
            val state = uiState.value
            val realOutcome = applyRealPhoneControlForRoutine(
                routine = routine,
                profiles = state.profiles,
                scheduleEndAlarm = true,
                showMissingDndBanner = false
            )
            val output = simulationEngine.evaluate(
                event = SimulationEvent.ManualRoutine(routine.id),
                routines = state.routines,
                profiles = state.profiles,
                context = state.preferences.simulatedContext,
                isAutomationPaused = state.preferences.isAutomationPaused,
                activeProfileId = state.preferences.activeProfileId,
                isFocusActive = state.activeFocus.isActive,
                conflictPolicy = state.preferences.conflictPolicy
            )
            _latestTrace.value = output.trace
            applyEngineOutput(output, showCompletionBanner = false)

            if (realOutcome.success) {
                _activePhoneBackup.value = routineBackupStore.getBackup()
                showBanner(
                    output.inAppNotice
                        ?: if (isArabic()) "تم تفعيل '${routine.name}' على الهاتف" else "${routine.name} activated"
                )
            } else {
                val missingExact = !(phoneControlManager.refreshPermissionAndDeviceState().canScheduleExactAlarms &&
                    alarmScheduler.canScheduleExactAlarms())
                _savePermissionDialog.value = SavePermissionDialogState(
                    routineName = routine.name,
                    missingDndAccess = realOutcome.permissionRequired || !phoneControlManager.state.value.hasDndAccess,
                    missingExactAlarm = missingExact,
                    realChangeFailed = true,
                    permissionRequiredForRun = realOutcome.permissionRequired,
                    failureNote = realOutcome.logEntry.note
                )
            }
        }
    }

    fun setRealManualRingerMode(mode: SoundMode) {
        viewModelScope.launch {
            val permState = phoneControlManager.refreshPermissionAndDeviceState()
            if (!permState.hasDndAccess) {
                _savePermissionDialog.value = SavePermissionDialogState(
                    routineName = if (isArabic()) "التحكم اليدوي" else "Manual Control",
                    missingDndAccess = true,
                    missingExactAlarm = false
                )
                return@launch
            }
            val backup = routineBackupStore.getBackup()
            if (backup == null) {
                routineBackupStore.onRoutineStarted(
                    routineId = "manual_override",
                    currentRingerMode = permState.currentRingerMode,
                    currentInterruptionFilter = permState.currentInterruptionFilter,
                    timestampMillis = System.currentTimeMillis()
                )
                _activePhoneBackup.value = routineBackupStore.getBackup()
            }
            val res = phoneControlManager.applyRingerMode(mode)
            if (res.success) {
                val msg = if (isArabic()) "تم تطبيق وضع الرنين: ${mode.arabicName}" else "Applied ringer mode: ${mode.displayName}"
                showBanner(msg)
                repository.addRoutineLog(
                    RoutineLogEntry(
                        id = "rlog_${UUID.randomUUID().toString().take(8)}",
                        routineId = "manual_override",
                        routineName = if (isArabic()) "التحكم اليدوي" else "Manual Control",
                        eventType = RoutineLogEventType.STARTED,
                        timestampMillis = System.currentTimeMillis(),
                        ringerMode = mode,
                        dndEnabled = null,
                        interruptionFilter = phoneControlManager.currentInterruptionFilter(),
                        note = msg
                    )
                )
            } else {
                _savePermissionDialog.value = SavePermissionDialogState(
                    routineName = if (isArabic()) "التحكم اليدوي" else "Manual Control",
                    missingDndAccess = res.permissionRequired,
                    missingExactAlarm = false,
                    realChangeFailed = true,
                    failureNote = if (isArabic()) "تعذر تطبيق وضع الرنين" else "Failed to apply ringer mode"
                )
            }
        }
    }

    fun restoreRealPreviousState() {
        viewModelScope.launch {
            val backup = routineBackupStore.getBackup()
            if (backup == null) {
                showBanner(if (isArabic()) "لا توجد حالة سابقة محفوظة للاستعادة" else "No previous saved state to restore")
                return@launch
            }
            val stateNow = phoneControlManager.refreshPermissionAndDeviceState()
            if (!stateNow.hasDndAccess) {
                _savePermissionDialog.value = SavePermissionDialogState(
                    routineName = if (isArabic()) "استعادة الحالة" else "Restore State",
                    missingDndAccess = true,
                    missingExactAlarm = false,
                    realChangeFailed = true
                )
                return@launch
            }
            phoneControlManager.restoreDeviceState(
                ringerMode = backup.previousRingerMode,
                interruptionFilter = backup.previousInterruptionFilter
            )
            val hadModeBackup = backup.activeRoutineIds.any { it.startsWith("mode_") }
            routineBackupStore.clearAll()
            _activePhoneBackup.value = null
            if (hadModeBackup) {
                repository.savePhoneState(
                    activeProfileId = "profile_personal",
                    soundMode = backup.previousRingerMode,
                    isAutomationPaused = uiState.value.preferences.isAutomationPaused,
                    recordPreviousSnapshot = false
                )
            }
            val dndOn = backup.previousInterruptionFilter != android.app.NotificationManager.INTERRUPTION_FILTER_ALL &&
                backup.previousInterruptionFilter != android.app.NotificationManager.INTERRUPTION_FILTER_UNKNOWN
            val arabicToast = formatArabicRestoredToast(backup.previousRingerMode, backup.previousInterruptionFilter)
            val msg = if (isArabic()) {
                arabicToast
            } else {
                val dndStr = if (dndOn) "DND On" else "DND Off"
                "Restored previous state: ${backup.previousRingerMode.displayName} • $dndStr"
            }
            showBanner(msg)
            showRestoreToast(arabicToast)
            repository.addRoutineLog(
                RoutineLogEntry(
                    id = "rlog_${UUID.randomUUID().toString().take(8)}",
                    routineId = "manual_override",
                    routineName = if (isArabic()) "استعادة الحالة" else "Restore State",
                    eventType = RoutineLogEventType.ENDED,
                    timestampMillis = System.currentTimeMillis(),
                    ringerMode = backup.previousRingerMode,
                    dndEnabled = dndOn,
                    interruptionFilter = backup.previousInterruptionFilter,
                    note = msg
                )
            )
        }
    }

    fun runSimulationEvent(event: SimulationEvent) {
        viewModelScope.launch {
            val state = uiState.value
            val output = simulationEngine.evaluate(
                event = event,
                routines = state.routines,
                profiles = state.profiles,
                context = state.preferences.simulatedContext,
                isAutomationPaused = state.preferences.isAutomationPaused,
                activeProfileId = state.preferences.activeProfileId,
                isFocusActive = state.activeFocus.isActive,
                conflictPolicy = state.preferences.conflictPolicy
            )
            _latestTrace.value = output.trace

            if (output.trace.requiresUserConfirmation) {
                val conflictingRoutines = state.routines.filter { it.id in output.trace.conflictingRoutineIds }
                _pendingConflict.value = PendingConflictPrompt(
                    trace = output.trace,
                    candidateRoutines = conflictingRoutines,
                    triggerLabel = output.trace.triggerReceived
                )
                repository.logEvent(
                    title = "Rule Conflict Detected",
                    category = EventCategory.CONFLICT,
                    triggerSource = output.trace.triggerReceived,
                    result = "Awaiting confirmation",
                    explanation = output.trace.conflictExplanation ?: "Multiple matching routines have conflicting actions."
                )
                showBanner(
                    if (isArabic()) "تم اكتشاف تعارض! اختر الروتين المراد تنفيذه."
                    else "Conflict detected! Choose which routine to execute."
                )
                return@launch
            }

            applyEngineOutput(output)
        }
    }

    fun resolvePendingConflict(chosenRoutine: SmartRoutine?) {
        viewModelScope.launch {
            val pending = _pendingConflict.value ?: return@launch
            _pendingConflict.value = null

            if (chosenRoutine == null) {
                repository.logEvent(
                    title = "Conflict Resolution Cancelled",
                    category = EventCategory.CONFLICT,
                    triggerSource = pending.triggerLabel,
                    result = "Current phone state preserved",
                    explanation = "User chose to keep current state without running conflicting routines."
                )
                showBanner(if (isArabic()) "تم الإبقاء على الحالة الحالية" else "Kept current state")
                return@launch
            }

            pending.candidateRoutines.filter { it.id != chosenRoutine.id }.forEach { skipped ->
                repository.upsertRoutine(
                    skipped.copy(
                        skippedCount = skipped.skippedCount + 1,
                        lastExecutionResult = "Skipped after user selected '${chosenRoutine.name}'",
                        lastExecutedAtMillis = System.currentTimeMillis()
                    )
                )
            }

            val output = simulationEngine.executeConfirmedRoutine(
                winner = chosenRoutine,
                conflictingNames = pending.candidateRoutines.map { it.name },
                triggerLabel = pending.triggerLabel,
                conflictExplanation = pending.trace.conflictExplanation
            )
            _latestTrace.value = output.trace
            applyEngineOutput(output)
        }
    }

    private suspend fun applyEngineOutput(
        output: EngineEvaluationOutput,
        showCompletionBanner: Boolean = true
    ) {
        val state = uiState.value
        val routine = output.selectedRoutine

        if (routine == null) {
            repository.logEvent(
                title = "Simulation Evaluated",
                category = EventCategory.AUTOMATION,
                triggerSource = output.trace.triggerReceived,
                result = output.trace.finalResult,
                explanation = output.trace.conditionsEvaluated.joinToString("; ").ifEmpty { output.trace.finalResult }
            )
            if (showCompletionBanner) {
                showBanner(output.trace.finalResult)
            }
            return
        }

        val appStrings = stringsFor(state.preferences.language)
        val ar = state.preferences.language == AppLanguage.ARABIC
        val localizedRoutineName = appStrings.localizedRoutineName(routine)
        val targetProfileId = output.targetProfileId ?: state.preferences.activeProfileId
        val targetProfile = state.profiles.firstOrNull { it.id == targetProfileId }
        val localizedProfileName = targetProfile?.let { appStrings.localizedProfileName(it) }
            ?: appStrings.localizedProfileName(targetProfileId, "Profile")
        val newSoundMode = output.targetSoundMode ?: targetProfile?.soundMode ?: state.preferences.simulatedSoundMode
        val localizedSoundLabel = appStrings.soundModeLabel(newSoundMode)
        val newPaused = output.setAutomationPaused ?: state.preferences.isAutomationPaused

        // Route execution through the DeviceActionExecutor abstraction
        deviceActionExecutor.execute(
            DeviceActionCommand.ActivateProfileState(
                profileId = targetProfileId,
                profileName = localizedProfileName,
                soundMode = newSoundMode
            )
        )
        if (output.inAppNotice != null) {
            deviceActionExecutor.execute(
                DeviceActionCommand.DeliverInAppNotice(localizedRoutineName, output.inAppNotice)
            )
        }

        repository.savePhoneState(
            activeProfileId = targetProfileId,
            soundMode = newSoundMode,
            isAutomationPaused = newPaused
        )

        val updatedRoutine = routine.copy(
            executionCount = routine.executionCount + 1,
            lastExecutionResult = output.trace.finalResult,
            lastExecutedAtMillis = System.currentTimeMillis()
        )
        repository.upsertRoutine(updatedRoutine)

        if (output.startFocusMinutes != null) {
            startFocusSessionInternal(
                minutes = output.startFocusMinutes,
                profile = targetProfile ?: state.activeProfile,
                triggerSource = if (ar) "روتين: $localizedRoutineName" else "Routine: $localizedRoutineName"
            )
        } else if (output.endFocusSession && state.activeFocus.isActive) {
            endFocusSession(completedNaturally = false)
        }

        repository.logEvent(
            title = if (ar) "تم تشغيل الروتين: $localizedRoutineName" else "Routine Executed: $localizedRoutineName",
            category = if (output.trace.conflictDetected) EventCategory.CONFLICT else EventCategory.ROUTINE,
            triggerSource = output.trace.triggerReceived,
            result = "$localizedProfileName • $localizedSoundLabel",
            explanation = output.trace.finalResult,
            relatedRoutineName = localizedRoutineName,
            relatedProfileName = localizedProfileName
        )

        if (showCompletionBanner) {
            showBanner(
                output.inAppNotice ?: "$localizedRoutineName -> $localizedProfileName ($localizedSoundLabel)"
            )
        }
    }

    // --- Context Awareness ---
    fun updateSimulatedContext(
        hour: Int? = null,
        minute: Int? = null,
        day: DayFilter? = null,
        location: SimulatedLocation? = null,
        customLocationName: String? = null,
        activity: SimulatedActivity? = null,
        customActivityName: String? = null,
        repeatCallerDetected: Boolean? = null,
        evaluateEngine: Boolean = false
    ) {
        viewModelScope.launch {
            val prev = uiState.value.preferences.simulatedContext
            val next = prev.copy(
                simulatedHour = hour ?: prev.simulatedHour,
                simulatedMinute = minute ?: prev.simulatedMinute,
                simulatedDay = day ?: prev.simulatedDay,
                location = location ?: prev.location,
                customLocationName = customLocationName ?: prev.customLocationName,
                activity = activity ?: prev.activity,
                customActivityName = customActivityName ?: prev.customActivityName,
                repeatCallerDetected = repeatCallerDetected ?: prev.repeatCallerDetected
            )
            repository.setSimulatedContext(next)
            repository.logEvent(
                title = "Simulated Context Updated",
                category = EventCategory.CONTEXT,
                triggerSource = "Context Awareness",
                result = "${next.location.displayName} • ${next.activity.displayName} • ${TimeUtils.formatTime12h(next.simulatedHour, next.simulatedMinute)}",
                explanation = "Updated simulated environment context for automation testing."
            )

            if (evaluateEngine) {
                when {
                    location != null && location != prev.location -> {
                        val locName = if (location == SimulatedLocation.CUSTOM) next.customLocationName else location.displayName
                        runSimulationEvent(SimulationEvent.LocationEntered(locName))
                    }
                    activity != null && activity != prev.activity -> {
                        val actName = if (activity == SimulatedActivity.CUSTOM) next.customActivityName else activity.displayName
                        runSimulationEvent(SimulationEvent.ActivityStarted(actName))
                    }
                    else -> {
                        runSimulationEvent(SimulationEvent.TimeAndDay(next.simulatedHour, next.simulatedMinute, next.simulatedDay))
                    }
                }
            }
        }
    }

    // --- Priority Contacts & Interruption Simulator ---
    fun saveContact(contact: PriorityContact) {
        viewModelScope.launch {
            repository.upsertContact(contact)
            repository.logEvent(
                title = "Updated Contact: ${contact.name}",
                category = EventCategory.CONTACT_EXCEPTION,
                triggerSource = "Priority Contacts",
                result = "${contact.priorityLevel.displayName} (${if (contact.isEnabled) "Enabled" else "Paused"})",
                explanation = "Configured simulated exception behavior for ${contact.name}."
            )
            showBanner(if (isArabic()) "تم حفظ جهة الاتصال '${contact.name}'" else "Saved contact '${contact.name}'")
        }
    }

    fun deleteContact(contact: PriorityContact) {
        viewModelScope.launch {
            repository.deleteContact(contact.id)
            repository.logEvent(
                title = "Removed Contact: ${contact.name}",
                category = EventCategory.CONTACT_EXCEPTION,
                triggerSource = "Priority Contacts",
                result = "Contact deleted",
                explanation = "Removed synthetic contact after user confirmation."
            )
            showBanner(if (isArabic()) "تمت إزالة '${contact.name}'" else "Removed '${contact.name}'")
        }
    }

    fun simulateIncomingInterruption(
        contact: PriorityContact,
        type: InterruptionType,
        isRepeatCaller: Boolean
    ): InterruptionDecision {
        val currentLanguage = uiState.value.preferences.language
        val appStrings = stringsFor(currentLanguage)
        val activeProfile = uiState.value.activeProfile ?: SmartProfile(
            id = "profile_personal",
            name = appStrings.localizedProfileName("profile_personal", "Personal"),
            description = "",
            iconKey = com.example.models.ProfileIconKey.PERSONAL,
            accentColorHex = 0xFF0D7C7B,
            soundMode = SoundMode.SOUND,
            allowedCategories = emptyList(),
            priorityContactBehavior = "",
            allowHighPriorityContacts = true,
            defaultFocusMinutes = 25,
            isDefault = true,
            isProtected = true
        )
        val localizedActiveProfileName = appStrings.localizedProfileName(activeProfile)
        val decision = simulationEngine.simulateIncomingInterruption(
            contact = contact,
            interruptionType = type,
            isRepeatCaller = isRepeatCaller,
            activeProfile = activeProfile
        )
        _lastInterruption.value = decision

        if (!decision.isAllowed && _activeFocus.value.isActive) {
            _activeFocus.update {
                it.copy(simulatedInterruptionsBlocked = it.simulatedInterruptionsBlocked + 1)
            }
        }

        viewModelScope.launch {
            val ar = currentLanguage == AppLanguage.ARABIC
            repository.logEvent(
                title = if (ar) {
                    if (decision.isAllowed) "مقاطعة مسموحة: ${contact.name}" else "مقاطعة محجوبة: ${contact.name}"
                } else {
                    if (decision.isAllowed) "Interruption Allowed: ${contact.name}" else "Interruption Blocked: ${contact.name}"
                },
                category = EventCategory.CONTACT_EXCEPTION,
                triggerSource = "Simulated Incoming ${type.displayName}",
                result = if (decision.isAllowed) "Allowed (${decision.priorityLevel.displayName})" else "Blocked by $localizedActiveProfileName",
                explanation = decision.explanation,
                relatedProfileName = localizedActiveProfileName
            )
        }
        return decision
    }

    // --- Focus Session Timer ---
    fun startFocusSession(minutes: Int, profile: SmartProfile, activateProfileNow: Boolean = true) {
        viewModelScope.launch {
            val currentLang = uiState.value.preferences.language
            val appStrings = stringsFor(currentLang)
            val localizedProfileName = appStrings.localizedProfileName(profile)
            if (activateProfileNow) {
                deviceActionExecutor.execute(
                    DeviceActionCommand.ActivateProfileState(profile.id, localizedProfileName, profile.soundMode)
                )
                repository.savePhoneState(
                    activeProfileId = profile.id,
                    soundMode = profile.soundMode,
                    isAutomationPaused = uiState.value.preferences.isAutomationPaused
                )
            }
            startFocusSessionInternal(minutes, profile, if (currentLang == AppLanguage.ARABIC) "التحكم بالتركيز" else "Focus Session Control")
            showBanner(
                if (currentLang == AppLanguage.ARABIC) "بدأت جلسة التركيز: ${minutes} د ($localizedProfileName)"
                else "Focus session started: ${minutes}m ($localizedProfileName)"
            )
        }
    }

    private suspend fun startFocusSessionInternal(
        minutes: Int,
        profile: SmartProfile?,
        triggerSource: String
    ) {
        val safeProfile = profile ?: return
        val currentLang = uiState.value.preferences.language
        val ar = currentLang == AppLanguage.ARABIC
        val appStrings = stringsFor(currentLang)
        val localizedProfileName = appStrings.localizedProfileName(safeProfile)
        val localizedSoundLabel = appStrings.soundModeLabel(safeProfile.soundMode)
        val totalSecs = (minutes.coerceIn(1, 240)) * 60
        focusTimerJob?.cancel()
        _lastCompletedFocusSummary.value = null
        _activeFocus.value = ActiveFocusSession(
            isActive = true,
            isPaused = false,
            profileId = safeProfile.id,
            profileName = localizedProfileName,
            totalSeconds = totalSecs,
            remainingSeconds = totalSecs,
            simulatedInterruptionsBlocked = 0,
            startedAtMillis = System.currentTimeMillis()
        )
        repository.logEvent(
            title = if (ar) "بدء جلسة تركيز (${minutes} د)" else "Focus Session Started (${minutes}m)",
            category = EventCategory.FOCUS,
            triggerSource = triggerSource,
            result = "$localizedProfileName • $localizedSoundLabel",
            explanation = if (ar) "بدأ العد التنازلي لمدة $minutes دقيقة ضمن وضع $localizedProfileName." else "Countdown started for $minutes minutes under $localizedProfileName profile.",
            relatedProfileName = localizedProfileName
        )
        startTimerTicker()
    }

    private fun startTimerTicker() {
        focusTimerJob?.cancel()
        focusTimerJob = viewModelScope.launch {
            while (_activeFocus.value.isActive) {
                delay(1000L)
                val cur = _activeFocus.value
                if (!cur.isActive || cur.isPaused) continue
                if (cur.remainingSeconds <= 1) {
                    endFocusSession(completedNaturally = true)
                    break
                } else {
                    _activeFocus.value = cur.copy(remainingSeconds = cur.remainingSeconds - 1)
                }
            }
        }
    }

    fun pauseOrResumeFocusSession() {
        val cur = _activeFocus.value
        if (!cur.isActive) return
        val nextPaused = !cur.isPaused
        _activeFocus.value = cur.copy(isPaused = nextPaused)
        viewModelScope.launch {
            repository.logEvent(
                title = if (nextPaused) "Focus Session Paused" else "Focus Session Resumed",
                category = EventCategory.FOCUS,
                triggerSource = "Focus Timer",
                result = "${TimeUtils.formatTimer(cur.remainingSeconds)} remaining",
                explanation = if (nextPaused) "User paused active focus timer." else "User resumed active focus timer.",
                relatedProfileName = cur.profileName
            )
        }
    }

    fun resetFocusSession() {
        val cur = _activeFocus.value
        if (!cur.isActive) return
        _activeFocus.value = cur.copy(
            remainingSeconds = cur.totalSeconds,
            isPaused = true,
            simulatedInterruptionsBlocked = 0
        )
        viewModelScope.launch {
            repository.logEvent(
                title = "Focus Timer Reset",
                category = EventCategory.FOCUS,
                triggerSource = "Focus Timer",
                result = "Reset to ${TimeUtils.formatTimer(cur.totalSeconds)}",
                explanation = "User reset the focus countdown timer back to full duration.",
                relatedProfileName = cur.profileName
            )
            showBanner(
                if (isArabic()) "تمت إعادة ضبط مؤقت التركيز"
                else "Focus timer reset"
            )
        }
    }

    fun simulateBlockedInterruptionDuringFocus() {
        val cur = _activeFocus.value
        if (!cur.isActive) return
        _activeFocus.value = cur.copy(simulatedInterruptionsBlocked = cur.simulatedInterruptionsBlocked + 1)
        showBanner(
            if (isArabic()) "تم كتم إشعار محاكى غير مهم أثناء التركيز"
            else "Simulated non-priority notification muted during focus"
        )
    }

    fun endFocusSession(completedNaturally: Boolean = false) {
        focusTimerJob?.cancel()
        val cur = _activeFocus.value
        if (!cur.isActive) return

        val elapsedSecs = (cur.totalSeconds - cur.remainingSeconds).coerceAtLeast(60)
        val plannedMins = (cur.totalSeconds / 60).coerceAtLeast(1)
        val completedMins = if (completedNaturally) plannedMins else (elapsedSecs / 60).coerceAtLeast(1)
        val status = if (completedNaturally) "Completed" else "Ended Early"

        _activeFocus.value = ActiveFocusSession()
        val summary = if (isArabic()) {
            "تم إنهاء الجلسة: ${completedMins} د في وضع ${cur.profileName} • تم حجب ${cur.simulatedInterruptionsBlocked} مقاطعة محاكاة."
        } else {
            "$status: ${completedMins}m in ${cur.profileName} Profile • ${cur.simulatedInterruptionsBlocked} simulated distractions blocked."
        }
        _lastCompletedFocusSummary.value = summary

        viewModelScope.launch {
            repository.addFocusSession(
                FocusSessionRecord(
                    id = "fs_${UUID.randomUUID().toString().take(8)}",
                    profileId = cur.profileId,
                    profileName = cur.profileName,
                    plannedMinutes = plannedMins,
                    completedMinutes = completedMins,
                    startedAtMillis = cur.startedAtMillis,
                    endedAtMillis = System.currentTimeMillis(),
                    simulatedInterruptionsBlocked = cur.simulatedInterruptionsBlocked,
                    status = status
                )
            )
            repository.logEvent(
                title = "Focus Session $status (${completedMins}m)",
                category = EventCategory.FOCUS,
                triggerSource = "Focus Timer",
                result = "${cur.simulatedInterruptionsBlocked} simulated interruptions blocked",
                explanation = summary,
                relatedProfileName = cur.profileName
            )
            showBanner(summary)
        }
    }

    private val _pendingRoutineDraft = MutableStateFlow<SmartRoutine?>(null)
    private val _clarificationTurnCount = MutableStateFlow(0)

    // --- AI Assistant ---
    fun sendAssistantPrompt(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return

        val userMsg = AssistantMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = trimmed,
            timestampMillis = System.currentTimeMillis()
        )
        _assistantMessages.update { it + userMsg }
        _isAssistantThinking.value = true

        viewModelScope.launch {
            val lower = trimmed.lowercase().trim()
            val promptDetailedRange = RoutineTimeParser.parseDetailedTimeRange(lower)
            val isClarificationAnswer = isClarificationFollowUpAnswer(lower) ||
                (_clarificationTurnCount.value > 0 && promptDetailedRange != null && !isStandaloneRoutineCreation(lower))
            val isStandalone = !isClarificationAnswer && (promptDetailedRange != null || isStandaloneRoutineCreation(lower))

            // Requirement 3: A new standalone request (not a follow-up answer) must start a fresh draft
            if (isStandalone) {
                _pendingRoutineDraft.value = null
                _clarificationTurnCount.value = 0
            }

            val state = uiState.value
            val history = _assistantMessages.value
                .filter { it.id != "welcome_1" }
                .dropLast(1)
                .takeLast(6)
                .map { it.isUser to it.text }
            val snapshot = AssistantStateSnapshot(
                language = state.preferences.language,
                activeProfile = state.activeProfile ?: state.profiles.first(),
                soundMode = state.preferences.simulatedSoundMode,
                isAutomationPaused = state.preferences.isAutomationPaused,
                routines = state.routines,
                contacts = state.contacts,
                notificationStats = state.notificationStats,
                focusSessions = state.focusSessions,
                recentEvents = state.activityEvents,
                recentConversation = history,
                pendingRoutineDraft = _pendingRoutineDraft.value,
                clarificationTurnCount = _clarificationTurnCount.value
            )
            val reply = assistantEngine.processPrompt(trimmed, snapshot)

            val effectiveProposal = if (reply.proposal == null) {
                _clarificationTurnCount.update { it + 1 }
                if (_clarificationTurnCount.value >= 2) {
                    val fallbackProposal = LocalDemoAssistantEngine().processPrompt(trimmed, snapshot).proposal
                    fallbackProposal ?: run {
                        val draft = _pendingRoutineDraft.value
                        if (draft != null && draft.scheduleStartTime.isNotBlank() && draft.scheduleEndTime.isNotBlank()) {
                            val isAr = isArabic()
                            val soundAction = draft.actions.firstOrNull { it.type == ActionType.SET_SOUND_MODE }
                            val soundMode = soundAction?.let { action ->
                                runCatching { SoundMode.valueOf(action.targetValue) }.getOrNull()
                            } ?: SoundMode.SILENT
                            val actionSummary = if (isAr) {
                                when (soundMode) {
                                    SoundMode.SILENT -> "تفعيل الوضع الصامت"
                                    SoundMode.VIBRATE -> "تفعيل وضع الاهتزاز"
                                    SoundMode.SOUND -> "تفعيل وضع الصوت العادي"
                                }
                            } else {
                                when (soundMode) {
                                    SoundMode.SILENT -> "Set phone to Silent"
                                    SoundMode.VIBRATE -> "Set phone to Vibrate"
                                    SoundMode.SOUND -> "Set phone to Sound"
                                }
                            }
                            val startLabel = draft.scheduleStartTime
                            val endLabel = draft.scheduleEndTime
                            val triggerSummary = if (isAr) "يومياً من $startLabel إلى $endLabel" else "Daily from $startLabel to $endLabel"
                            val exceptionSummary = if (isAr) "جهات الطوارئ مستثناة دائماً ولا يمكن حجبها" else "Emergency contacts always allowed"
                            RoutineProposal(
                                routine = draft.copy(enableDoNotDisturb = draft.enableDoNotDisturb || soundMode == SoundMode.SILENT),
                                triggerSummary = triggerSummary,
                                endTimeLabel = endLabel,
                                actionSummary = actionSummary,
                                exceptionSummary = exceptionSummary
                            )
                        } else null
                    }
                } else null
            } else {
                reply.proposal
            }

            // Requirement 3: Clear the draft after the confirmation card is shown
            if (effectiveProposal != null) {
                _pendingRoutineDraft.value = null
                _clarificationTurnCount.value = 0
            } else if (_pendingRoutineDraft.value == null && reply.proposal == null) {
                val soundMode = RoutineTimeParser.parseExplicitSoundMode(lower) ?: SoundMode.SILENT
                val hasExplicitRange = promptDetailedRange != null
                val startH = promptDetailedRange?.startHour ?: 0
                val startM = promptDetailedRange?.startMinute ?: 0
                val endH = promptDetailedRange?.endHour ?: 0
                val endM = promptDetailedRange?.endMinute ?: 0
                val isAr = isArabic()
                _pendingRoutineDraft.value = SmartRoutine(
                    id = "routine_draft_${UUID.randomUUID().toString().take(6)}",
                    name = if (isAr) "روتين مخصص" else "Custom Routine",
                    description = trimmed,
                    isEnabled = true,
                    trigger = RuleTrigger(
                        type = if (hasExplicitRange) TriggerType.SPECIFIC_TIME else TriggerType.SPECIFIC_DAYS,
                        hour = startH,
                        minute = startM,
                        dayFilter = DayFilter.EVERY_DAY,
                        summary = ""
                    ),
                    conditions = emptyList(),
                    actions = listOf(
                        RuleAction(ActionType.ACTIVATE_PROFILE, "profile_study", if (isAr) "تفعيل وضع الدراسة" else "Activate Study profile"),
                        RuleAction(ActionType.SET_SOUND_MODE, soundMode.name, if (isAr) "ضبط صوت الهاتف: ${soundMode.arabicName}" else "Set ${soundMode.displayName} Mode")
                    ),
                    exceptions = listOf(RuleException(ExceptionType.EMERGENCY_CONTACTS, "Allow Emergency contacts")),
                    priority = 5,
                    scheduleStartTime = if (hasExplicitRange) TimeUtils.formatTime12h(startH, startM, isAr) else "",
                    scheduleEndTime = if (hasExplicitRange) TimeUtils.formatTime12h(endH, endM, isAr) else "",
                    targetProfileId = "profile_study",
                    lastExecutionResult = "",
                    lastExecutedAtMillis = null,
                    executionCount = 0,
                    skippedCount = 0,
                    enableDoNotDisturb = (soundMode == SoundMode.SILENT)
                )
            }

            if (BuildConfig.DEBUG && !reply.debugErrorLog.isNullOrBlank()) {
                repository.logEvent(
                    title = "Gemini Debug Error Log",
                    category = EventCategory.ASSISTANT,
                    triggerSource = "GeminiAssistantEngine",
                    result = reply.debugErrorLog,
                    explanation = "Debug diagnostic captured when Gemini fell back to the local assistant."
                )
            }

            val assistantMsg = AssistantMessage(
                id = UUID.randomUUID().toString(),
                isUser = false,
                text = if (effectiveProposal != null && reply.proposal == null) {
                    if (isArabic()) "بعد عدة توضيحات، جهّزت لك هذا الروتين للمراجعة:"
                    else "After a couple of clarifications, I prepared this routine for your review:"
                } else {
                    reply.text
                },
                timestampMillis = System.currentTimeMillis(),
                proposal = effectiveProposal ?: reply.proposal,
                noticeText = reply.noticeText
            )
            _assistantMessages.update { it + assistantMsg }
            _isAssistantThinking.value = false
        }
    }

    fun confirmAssistantProposal(messageId: String, proposal: RoutineProposal) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val hasSilent = proposal.routine.actions.any {
                it.type == ActionType.SET_SOUND_MODE && it.targetValue == SoundMode.SILENT.name
            }
            val routineToSave = proposal.routine.copy(
                enableDoNotDisturb = proposal.routine.enableDoNotDisturb || hasSilent
            )
            val isActive = RoutineAlarmCalculator.isCurrentlyActive(routineToSave, now)
            val appliedReal = saveRoutineInternal(routineToSave, startImmediately = isActive)
            _pendingRoutineDraft.value = null
            _clarificationTurnCount.value = 0

            val appStrings = stringsFor(uiState.value.preferences.language)
            val localizedRoutineName = appStrings.localizedRoutineName(routineToSave)
            val note = if (isArabic()) {
                when {
                    isActive && appliedReal -> "تم التأكيد وإضافة '$localizedRoutineName' وتطبيقه فوراً على الهاتف."
                    isActive && !appliedReal -> "تم حفظ '$localizedRoutineName' — يلزم منح إذن عدم الإزعاج لتطبيقه فوراً على الهاتف."
                    else -> "تم التأكيد وجدولة '$localizedRoutineName' ضمن الروتينات الذكية."
                }
            } else {
                when {
                    isActive && appliedReal -> "Confirmed, scheduled & applied '$localizedRoutineName' on your phone immediately."
                    isActive && !appliedReal -> "Saved '$localizedRoutineName' — grant Do Not Disturb permission to apply it immediately on your phone."
                    else -> "Confirmed & scheduled '$localizedRoutineName' in Smart Routines."
                }
            }
            _assistantMessages.update { list ->
                list.map { msg ->
                    if (msg.id == messageId) {
                        msg.copy(proposalResolved = true, proposalStatusNote = note)
                    } else msg
                }
            }
            repository.logEvent(
                title = if (isArabic()) "أنشأ المساعد روتين: $localizedRoutineName" else "Assistant Created Routine: $localizedRoutineName",
                category = EventCategory.ASSISTANT,
                triggerSource = if (isArabic()) "تأكيد المساعد الذكي" else "AI Assistant Confirmation",
                result = proposal.triggerSummary,
                explanation = "Routine confirmed by user from natural-language proposal.${if (isActive && appliedReal) " Applied immediately on device." else ""}",
                relatedRoutineName = localizedRoutineName
            )
        }
    }

    fun editAssistantProposal(messageId: String, proposal: RoutineProposal) {
        _routineDraftFromAssistant.value = proposal.routine
        _pendingRoutineDraft.value = null
        _clarificationTurnCount.value = 0
        val note = if (isArabic()) {
            "تم فتح '${proposal.routine.name}' في منشئ القواعد المرئي للتعديل."
        } else {
            "Opened '${proposal.routine.name}' in Visual Rule Builder for customization."
        }
        _assistantMessages.update { list ->
            list.map { msg ->
                if (msg.id == messageId) {
                    msg.copy(proposalResolved = true, proposalStatusNote = note)
                } else msg
            }
        }
    }

    fun cancelAssistantProposal(messageId: String) {
        _pendingRoutineDraft.value = null
        _clarificationTurnCount.value = 0
        val note = if (isArabic()) "تم الإلغاء — لم يتم إنشاء الروتين." else "Cancelled — routine was not created."
        _assistantMessages.update { list ->
            list.map { msg ->
                if (msg.id == messageId) {
                    msg.copy(proposalResolved = true, proposalStatusNote = note)
                } else msg
            }
        }
    }

    fun clearAssistantDraft() {
        _routineDraftFromAssistant.value = null
    }

    fun clearAssistantConversation() {
        _pendingRoutineDraft.value = null
        _clarificationTurnCount.value = 0
        _assistantMessages.value = initialAssistantMessages(uiState.value.preferences.language)
        showBanner(if (isArabic()) "تم مسح المحادثة" else "Assistant conversation cleared")
    }

    private fun isClarificationFollowUpAnswer(lower: String): Boolean {
        val trimmed = lower.trim()
        val periods = setOf("مساء", "مساءً", "صباح", "صباحاً", "صباحا", "صبح", "الصبح", "pm", "am", "evening", "morning", "م", "ص", "ليل", "بليل", "بالليل", "الليل")
        if (trimmed in periods) return true
        val modes = setOf("صامت", "اهتزاز", "عادي", "صوت", "وضع صامت", "وضع اهتزاز", "وضع عادي", "silent", "vibrate", "sound")
        if (trimmed in modes) return true
        val days = setOf("يومياً", "كل يوم", "أيام الأسبوع", "عطلة نهاية الأسبوع", "السبت", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "daily", "weekdays", "weekends")
        if (trimmed in days) return true
        return false
    }

    private fun isStandaloneRoutineCreation(lower: String): Boolean {
        val creationKeywords = listOf(
            "اعمل", "أنشئ", "روتين", "ساعات", "جدول",
            "create", "routine", "schedule", "keep", "make", "set"
        )
        return creationKeywords.any { lower.contains(it) }
    }

    // --- Settings, History & Safety Recovery ---
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            repository.setLanguage(language)
            // Keep conversation intact, only update welcome message if user hasn't chatted yet
            _assistantMessages.update { current ->
                if (current.size == 1 && current.first().id == "welcome_1") {
                    initialAssistantMessages(language)
                } else current
            }
        }
    }

    fun setRequireConfirmation(require: Boolean) {
        viewModelScope.launch { repository.setRequireConfirmation(require) }
    }

    fun setDefaultProfileId(profileId: String) {
        viewModelScope.launch { repository.setDefaultProfileId(profileId) }
    }

    fun setConflictPolicy(policy: ConflictResolutionPolicy) {
        viewModelScope.launch { repository.setConflictPolicy(policy) }
    }

    fun clearActivityHistory() {
        viewModelScope.launch {
            repository.clearActivityHistory()
            repository.logEvent(
                title = "Activity History Cleared",
                category = EventCategory.SYSTEM,
                triggerSource = "Activity History",
                result = "Fresh timeline started",
                explanation = "User cleared previous demo logs."
            )
            showBanner(if (isArabic()) "تم مسح سجل النشاط" else "Activity history cleared")
        }
    }

    fun resetAllDemoData(resetOnboarding: Boolean = false) {
        viewModelScope.launch {
            focusTimerJob?.cancel()
            _activeFocus.value = ActiveFocusSession()
            _latestTrace.value = null
            _lastInterruption.value = null
            _pendingConflict.value = null
            _demoScenarioResult.value = null
            routineBackupStore.clearAll()
            _dndPermissionAlertRoutine.value = null
            _assistantMessages.value = initialAssistantMessages(uiState.value.preferences.language)
            repository.resetAllDemoData(resetOnboarding = resetOnboarding)
            rescheduleRoutinesInternal(repository.getAllRoutinesOnce())
            showBanner(if (isArabic()) "تمت استعادة جميع بيانات العرض الافتراضية" else "All demo data restored to initial state")
        }
    }

    // --- End-to-End Demo Scenarios (Demo Playground) ---
    fun executeDemoScenario(scenarioNumber: Int) {
        viewModelScope.launch {
            val state = uiState.value
            val studyProfile = state.profiles.firstOrNull { it.id == "profile_study" } ?: return@launch
            val sleepProfile = state.profiles.firstOrNull { it.id == "profile_sleep" } ?: return@launch
            val normalContact = state.contacts.firstOrNull { it.priorityLevel == com.example.models.ContactPriority.NORMAL }
                ?: state.contacts.first()
            val emergencyContact = state.contacts.firstOrNull { it.priorityLevel == com.example.models.ContactPriority.EMERGENCY }
                ?: state.contacts.first()
            val ar = isArabic()

            when (scenarioNumber) {
                1 -> {
                    deviceActionExecutor.execute(
                        DeviceActionCommand.ActivateProfileState(studyProfile.id, studyProfile.name, SoundMode.SILENT)
                    )
                    repository.savePhoneState(studyProfile.id, SoundMode.SILENT, isAutomationPaused = false)
                    val studyRoutine = state.routines.firstOrNull { it.id == "routine_study_time" }
                    if (studyRoutine != null) {
                        repository.upsertRoutine(
                            studyRoutine.copy(
                                executionCount = studyRoutine.executionCount + 1,
                                lastExecutionResult = "Executed in Demo Scenario 1",
                                lastExecutedAtMillis = System.currentTimeMillis()
                            )
                        )
                    }
                    val normalDecision = simulateIncomingInterruption(normalContact, InterruptionType.CALL, false)
                    val emergencyDecision = simulateIncomingInterruption(emergencyContact, InterruptionType.CALL, false)
                    _demoScenarioResult.value = DemoScenarioStepResult(
                        scenarioTitle = if (ar) "السيناريو 1: وقت الدراسة" else "Scenario 1: Study Time",
                        steps = if (ar) listOf(
                            "1. تفعيل وضع الدراسة (الصوت المحاكى: صامت).",
                            "2. تشغيل روتين 'وقت الدراسة'.",
                            "3. مكالمة واردة من ${normalContact.name}: ${if (normalDecision.isAllowed) "مسموح" else "محجوب"}.",
                            "4. مكالمة طوارئ من ${emergencyContact.name}: ${if (emergencyDecision.isAllowed) "مسموح" else "محجوب"}."
                        ) else listOf(
                            "1. Activated Study Profile (Simulated Sound -> Silent).",
                            "2. Executed 'Study Time' routine.",
                            "3. Incoming call from ${normalContact.name} (${normalContact.priorityLevel.displayName}): ${if (normalDecision.isAllowed) "ALLOWED" else "BLOCKED"} — ${normalDecision.explanation}",
                            "4. Incoming call from ${emergencyContact.name} (Emergency): ${if (emergencyDecision.isAllowed) "ALLOWED" else "BLOCKED"} — ${emergencyDecision.explanation}"
                        ),
                        finalOutcome = if (ar) "تمت حماية التركيز مع السماح بمكالمات الطوارئ وتسجيل القرارين."
                        else "Study focus protected while emergency caller bypassed Silent mode. Both decisions logged in Activity History."
                    )
                }
                2 -> {
                    deviceActionExecutor.execute(
                        DeviceActionCommand.ActivateProfileState(sleepProfile.id, sleepProfile.name, SoundMode.SILENT)
                    )
                    repository.savePhoneState(sleepProfile.id, SoundMode.SILENT, isAutomationPaused = false)
                    val msgDecision = simulateIncomingInterruption(normalContact, InterruptionType.MESSAGE, false)
                    val callDecision = simulateIncomingInterruption(emergencyContact, InterruptionType.CALL, false)
                    _demoScenarioResult.value = DemoScenarioStepResult(
                        scenarioTitle = if (ar) "السيناريو 2: حماية النوم" else "Scenario 2: Sleep Protection",
                        steps = if (ar) listOf(
                            "1. تفعيل وضع النوم وجدول 11:00 م (صامت).",
                            "2. رسالة واردة من ${normalContact.name}: ${if (msgDecision.isAllowed) "مسموح" else "محجوب"}.",
                            "3. مكالمة طوارئ من ${emergencyContact.name}: ${if (callDecision.isAllowed) "مسموح" else "محجوب"}."
                        ) else listOf(
                            "1. Activated Sleep Profile & Sleep Schedule (11:00 PM Silent).",
                            "2. Incoming message from ${normalContact.name}: ${if (msgDecision.isAllowed) "ALLOWED" else "BLOCKED"}.",
                            "3. Incoming emergency call from ${emergencyContact.name}: ${if (callDecision.isAllowed) "ALLOWED" else "BLOCKED"}."
                        ),
                        finalOutcome = if (ar) "تم كتم الرسائل العادية ليلاً مع السماح بوصول العائلة عند الطوارئ."
                        else "Overnight notifications silenced; emergency family access preserved."
                    )
                }
                3 -> {
                    val ctx = state.preferences.simulatedContext.copy(
                        simulatedHour = 9,
                        simulatedMinute = 0,
                        simulatedDay = DayFilter.MONDAY
                    )
                    repository.setSimulatedContext(ctx)
                    val output = simulationEngine.evaluate(
                        event = SimulationEvent.TimeAndDay(9, 0, DayFilter.MONDAY),
                        routines = state.routines,
                        profiles = state.profiles,
                        context = ctx,
                        isAutomationPaused = false,
                        activeProfileId = state.preferences.activeProfileId,
                        isFocusActive = state.activeFocus.isActive,
                        conflictPolicy = ConflictResolutionPolicy.HIGHEST_PRIORITY
                    )
                    _latestTrace.value = output.trace
                    applyEngineOutput(output)
                    _demoScenarioResult.value = DemoScenarioStepResult(
                        scenarioTitle = if (ar) "السيناريو 3: الجدولة الذكية" else "Scenario 3: Smart Scheduling",
                        steps = if (ar) listOf(
                            "1. محاكاة الوصول إلى الإثنين 9:00 صباحاً.",
                            "2. الروتينات المطابقة: ${output.trace.matchingRoutineNames.joinToString()}",
                            "3. تم تنفيذ: ${output.selectedRoutine?.name ?: "لا شيء"}",
                            "4. تم تحديث الرئيسية وسجل النشاط."
                        ) else listOf(
                            "1. Simulated reaching Monday 9:00 AM.",
                            "2. Matched routines: ${output.trace.matchingRoutineNames.joinToString()}",
                            "3. Selected & executed: ${output.selectedRoutine?.name ?: "None"}",
                            "4. Updated Dashboard & Activity History."
                        ),
                        finalOutcome = output.trace.finalResult
                    )
                }
                4 -> {
                    val prompt = if (ar) {
                        "اكتم الإشعارات يومياً من 8 مساءً إلى 10 مساءً مع السماح بجهات الطوارئ."
                    } else {
                        "Keep my phone silent every day from 8 PM to 10 PM, but allow emergency contacts."
                    }
                    val snapshot = AssistantStateSnapshot(
                        language = state.preferences.language,
                        activeProfile = state.activeProfile ?: studyProfile,
                        soundMode = state.preferences.simulatedSoundMode,
                        isAutomationPaused = state.preferences.isAutomationPaused,
                        routines = state.routines,
                        contacts = state.contacts,
                        notificationStats = state.notificationStats,
                        focusSessions = state.focusSessions,
                        recentEvents = state.activityEvents
                    )
                    val reply = assistantEngine.processPrompt(prompt, snapshot)
                    val proposal = reply.proposal
                    if (proposal != null) {
                        val demoRoutine = proposal.routine.copy(id = "routine_ai_demo_8pm")
                        repository.upsertRoutine(demoRoutine)
                        repository.logEvent(
                            title = "Demo Scenario 4: AI Created '${demoRoutine.name}'",
                            category = EventCategory.ASSISTANT,
                            triggerSource = "Demo Playground (Assistant)",
                            result = proposal.triggerSummary,
                            explanation = "Generated and confirmed structured rule from natural language.",
                            relatedRoutineName = demoRoutine.name
                        )
                    }
                    _demoScenarioResult.value = DemoScenarioStepResult(
                        scenarioTitle = if (ar) "السيناريو 4: إنشاء روتين بالمساعد" else "Scenario 4: AI Routine Creation",
                        steps = if (ar) listOf(
                            "1. الطلب: \"$prompt\"",
                            "2. استخرج المساعد المُشغّل (${proposal?.triggerSummary}) والانتهاء (${proposal?.endTimeLabel}) والاستثناء (${proposal?.exceptionSummary}).",
                            "3. تم تأكيد الحفظ في قائمة الروتينات الذكية."
                        ) else listOf(
                            "1. Prompt: \"$prompt\"",
                            "2. Assistant parsed Trigger (${proposal?.triggerSummary}), End (${proposal?.endTimeLabel}), Action (${proposal?.actionSummary}), Exception (${proposal?.exceptionSummary}).",
                            "3. Confirmed creation and persisted routine to Smart Routines."
                        ),
                        finalOutcome = if (ar) "تمت إضافة '${proposal?.routine?.name}' إلى الروتينات الذكية."
                        else "'${proposal?.routine?.name}' is now visible and testable in Smart Routines."
                    )
                }
                5 -> {
                    val ctx = state.preferences.simulatedContext.copy(
                        simulatedHour = 18,
                        simulatedMinute = 0,
                        simulatedDay = DayFilter.WEDNESDAY
                    )
                    repository.setSimulatedContext(ctx)
                    val output = simulationEngine.evaluate(
                        event = SimulationEvent.TimeAndDay(18, 0, DayFilter.WEDNESDAY),
                        routines = state.routines,
                        profiles = state.profiles,
                        context = ctx,
                        isAutomationPaused = false,
                        activeProfileId = state.preferences.activeProfileId,
                        isFocusActive = state.activeFocus.isActive,
                        conflictPolicy = ConflictResolutionPolicy.ASK_USER
                    )
                    _latestTrace.value = output.trace
                    val conflicting = state.routines.filter { it.id in output.trace.conflictingRoutineIds }
                    if (output.trace.requiresUserConfirmation && conflicting.isNotEmpty()) {
                        _pendingConflict.value = PendingConflictPrompt(
                            trace = output.trace,
                            candidateRoutines = conflicting,
                            triggerLabel = output.trace.triggerReceived
                        )
                    }
                    _demoScenarioResult.value = DemoScenarioStepResult(
                        scenarioTitle = if (ar) "السيناريو 5: حل تعارض القواعد" else "Scenario 5: Conflict Resolution",
                        steps = if (ar) listOf(
                            "1. محاكاة الأربعاء 6:00 مساءً.",
                            "2. روتينات متقاطعة: ${output.trace.matchingRoutineNames.joinToString()}",
                            "3. تم فتح نافذة حل التعارض لتختار بأمان."
                        ) else listOf(
                            "1. Simulated Wednesday 6:00 PM trigger.",
                            "2. Detected overlapping routines: ${output.trace.matchingRoutineNames.joinToString()}",
                            "3. Conflict: ${output.trace.conflictExplanation ?: "Conflicting actions detected"}",
                            "4. Opened Conflict Resolution dialog so you can choose safely."
                        ),
                        finalOutcome = if (ar) "اختر الروتين الفائز من نافذة التأكيد."
                        else "Conflict surfaced cleanly — pick a winning routine in the dialog."
                    )
                }
                6 -> {
                    repository.savePhoneState(
                        activeProfileId = state.preferences.activeProfileId,
                        soundMode = state.preferences.simulatedSoundMode,
                        isAutomationPaused = true
                    )
                    val pausedEval = simulationEngine.evaluate(
                        event = SimulationEvent.TimeAndDay(9, 0, DayFilter.MONDAY),
                        routines = state.routines,
                        profiles = state.profiles,
                        context = state.preferences.simulatedContext,
                        isAutomationPaused = true,
                        activeProfileId = state.preferences.activeProfileId,
                        isFocusActive = state.activeFocus.isActive,
                        conflictPolicy = state.preferences.conflictPolicy
                    )
                    repository.savePhoneState(
                        activeProfileId = state.preferences.activeProfileId,
                        soundMode = state.preferences.simulatedSoundMode,
                        isAutomationPaused = false,
                        recordPreviousSnapshot = false
                    )
                    repository.logEvent(
                        title = "Demo Scenario 6: Global Pause & Recovery Verified",
                        category = EventCategory.AUTOMATION,
                        triggerSource = "Demo Playground",
                        result = "Paused -> Blocked trigger -> Restored",
                        explanation = "Verified automation stopped under Global Pause and resumed safely."
                    )
                    _demoScenarioResult.value = DemoScenarioStepResult(
                        scenarioTitle = if (ar) "السيناريو 6: الإيقاف الشامل والاستعادة" else "Scenario 6: Global Pause & Recovery",
                        steps = if (ar) listOf(
                            "1. تفعيل الإيقاف الشامل للأتمتة.",
                            "2. اختبار مشغّل الإثنين 9:00 ص -> توقف بأمان.",
                            "3. استعادة الأتمتة مع الحفاظ على جميع القواعد."
                        ) else listOf(
                            "1. Activated Global Automation Pause.",
                            "2. Tested Monday 9:00 AM trigger -> ${pausedEval.trace.finalResult}",
                            "3. Restored Automation back to active state with all routines intact."
                        ),
                        finalOutcome = if (ar) "أوقف الإيقاف الشامل التنفيذ بأمان وتمت استعادة الإعدادات."
                        else "Global Pause safely halted execution and restored configuration."
                    )
                }
            }
        }
    }

    private fun initialAssistantMessages(language: AppLanguage): List<AssistantMessage> {
        val welcome = if (language == AppLanguage.ARABIC) {
            "مرحباً! صف الروتين الذي تريده بالعربية أو الإنجليزية (مثلاً: \"اجعل هاتفي صامتاً أثناء الدراسة 8 إلى 10 مساءً واسمح لعائلتي بالاتصال\"). سأجهّز لك بطاقة روتين للموافقة أو التعديل أو الرفض."
        } else {
            "Hello! Describe any routine you want in English or Arabic (for example: \"Silence my phone while I study 8 to 10 but let my family call me\"). I'll prepare a routine card for you to Approve, Edit, or Reject."
        }
        return listOf(
            AssistantMessage(
                id = "welcome_1",
                isUser = false,
                text = welcome,
                timestampMillis = System.currentTimeMillis()
            )
        )
    }

    private data class CoreBundle(
        val prefs: AppPreferences,
        val profiles: List<SmartProfile>,
        val routines: List<SmartRoutine>,
        val contacts: List<PriorityContact>,
        val events: List<ActivityEvent>
    )

    private data class SecondaryBundle(
        val focusSessions: List<FocusSessionRecord>,
        val notificationStats: List<NotificationStat>,
        val activeFocus: ActiveFocusSession,
        val trace: SimulationTrace?,
        val interruption: InterruptionDecision?
    )

    private data class InteractiveBundle(
        val pendingConflict: PendingConflictPrompt?,
        val assistantMessages: List<AssistantMessage>,
        val isAssistantThinking: Boolean,
        val feedbackBanner: String?,
        val isReady: Boolean
    )

    private data class ExtraBundle(
        val focusSummary: String?,
        val scenarioResult: DemoScenarioStepResult?,
        val draftRoutine: SmartRoutine?,
        val phoneControl: PhoneControlState,
        val routineLogs: List<RoutineLogEntry>
    )

    private data class AlertBundle(
        val dndAlert: String?,
        val exactDenied: Boolean,
        val saveDialog: SavePermissionDialogState?,
        val phoneBackup: SavedPhoneBackup?,
        val toastMessage: String?
    )

    class Factory(
        private val repository: SmartCompanionRepository,
        private val assistantEngine: CompanionAssistantEngine = LocalDemoAssistantEngine(),
        private val phoneControlManager: PhoneControlManager = PhoneControlManager(),
        private val routineBackupStore: RoutineStateBackupStore = InMemoryRoutineStateBackupStore(),
        private val alarmScheduler: RoutineAlarmSchedulerGateway = NoOpRoutineAlarmScheduler(),
        private val onDndPermissionMissingNotification: (routineName: String, isArabic: Boolean) -> Unit = { _, _ -> }
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SmartCompanionViewModel(
                repository = repository,
                assistantEngine = assistantEngine,
                phoneControlManager = phoneControlManager,
                routineBackupStore = routineBackupStore,
                alarmScheduler = alarmScheduler,
                onDndPermissionMissingNotification = onDndPermissionMissingNotification
            ) as T
        }
    }
}
