package com.example.models

data class SmartProfile(
    val id: String,
    val name: String,
    val description: String,
    val iconKey: ProfileIconKey,
    val accentColorHex: Long,
    val soundMode: SoundMode,
    val allowedCategories: List<NotificationCategory>,
    val priorityContactBehavior: String,
    val allowHighPriorityContacts: Boolean,
    val defaultFocusMinutes: Int,
    val isDefault: Boolean,
    val isProtected: Boolean
)

data class RuleTrigger(
    val type: TriggerType,
    val hour: Int = 18,
    val minute: Int = 0,
    val dayFilter: DayFilter = DayFilter.WEEKDAYS,
    val dateLabel: String = "",
    val location: SimulatedLocation = SimulatedLocation.LIBRARY,
    val activity: SimulatedActivity = SimulatedActivity.STUDYING,
    val summary: String = ""
)

data class RuleCondition(
    val type: ConditionType,
    val value: String
)

data class RuleAction(
    val type: ActionType,
    val targetValue: String,
    val label: String
)

data class RuleException(
    val type: ExceptionType,
    val value: String
)

data class SmartRoutine(
    val id: String,
    val name: String,
    val description: String,
    val isEnabled: Boolean,
    val trigger: RuleTrigger,
    val conditions: List<RuleCondition>,
    val actions: List<RuleAction>,
    val exceptions: List<RuleException>,
    val priority: Int,
    val scheduleStartTime: String,
    val scheduleEndTime: String,
    val targetProfileId: String?,
    val lastExecutionResult: String,
    val lastExecutedAtMillis: Long?,
    val executionCount: Int,
    val skippedCount: Int
)

data class PriorityContact(
    val id: String,
    val name: String,
    val initials: String,
    val relationship: String,
    val priorityLevel: ContactPriority,
    val allowedInterruptions: List<InterruptionType>,
    val isEnabled: Boolean,
    val allowRepeatCallers: Boolean
)

data class ActivityEvent(
    val id: String,
    val title: String,
    val timestampMillis: Long,
    val category: EventCategory,
    val triggerSource: String,
    val result: String,
    val explanation: String,
    val relatedRoutineName: String? = null,
    val relatedProfileName: String? = null
)

data class FocusSessionRecord(
    val id: String,
    val profileId: String,
    val profileName: String,
    val plannedMinutes: Int,
    val completedMinutes: Int,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val simulatedInterruptionsBlocked: Int,
    val status: String
)

data class NotificationStat(
    val id: String,
    val appName: String,
    val category: NotificationCategory,
    val totalCount: Int,
    val importantCount: Int,
    val blockedCount: Int,
    val peakHourRange: String,
    val timePeriod: TimePeriodFilter
)

data class SimulationContextState(
    val simulatedHour: Int = 18,
    val simulatedMinute: Int = 0,
    val simulatedDay: DayFilter = DayFilter.MONDAY,
    val location: SimulatedLocation = SimulatedLocation.HOME,
    val customLocationName: String = "Downtown Study Cafe",
    val activity: SimulatedActivity = SimulatedActivity.PERSONAL_TIME,
    val customActivityName: String = "Reading",
    val repeatCallerDetected: Boolean = false
)

data class PhoneSnapshot(
    val activeProfileId: String,
    val soundMode: SoundMode,
    val isAutomationPaused: Boolean
)

data class ActiveFocusSession(
    val isActive: Boolean = false,
    val isPaused: Boolean = false,
    val profileId: String = "profile_study",
    val profileName: String = "Study",
    val totalSeconds: Int = 25 * 60,
    val remainingSeconds: Int = 25 * 60,
    val simulatedInterruptionsBlocked: Int = 0,
    val startedAtMillis: Long = 0L
)

data class SimulationTrace(
    val id: String,
    val triggerReceived: String,
    val matchingRoutineNames: List<String>,
    val conditionsEvaluated: List<String>,
    val exceptionsChecked: List<String>,
    val actionsSelected: List<String>,
    val conflictDetected: Boolean,
    val conflictExplanation: String? = null,
    val conflictingRoutineIds: List<String> = emptyList(),
    val requiresUserConfirmation: Boolean = false,
    val finalResult: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

data class InterruptionDecision(
    val contactName: String,
    val priorityLevel: ContactPriority,
    val interruptionType: InterruptionType,
    val isAllowed: Boolean,
    val activeProfileName: String,
    val explanation: String
)

data class RoutineProposal(
    val routine: SmartRoutine,
    val triggerSummary: String,
    val endTimeLabel: String,
    val actionSummary: String,
    val exceptionSummary: String
)

data class AssistantMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val timestampMillis: Long,
    val proposal: RoutineProposal? = null,
    val proposalResolved: Boolean = false,
    val proposalStatusNote: String? = null
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
