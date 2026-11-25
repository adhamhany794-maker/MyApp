package com.example.simulation

import com.example.models.ActionType
import com.example.models.ConditionType
import com.example.models.ConflictResolutionPolicy
import com.example.models.ContactPriority
import com.example.models.DayFilter
import com.example.models.ExceptionType
import com.example.models.InterruptionDecision
import com.example.models.InterruptionType
import com.example.models.PriorityContact
import com.example.models.RuleAction
import com.example.models.SimulationContextState
import com.example.models.SimulationTrace
import com.example.models.SmartProfile
import com.example.models.SmartRoutine
import com.example.models.SoundMode
import com.example.models.TriggerType
import com.example.utils.TimeUtils
import java.util.UUID

sealed interface SimulationEvent {
    data class TimeAndDay(val hour: Int, val minute: Int, val day: DayFilter) : SimulationEvent
    data class LocationEntered(val locationName: String) : SimulationEvent
    data class LocationExited(val locationName: String) : SimulationEvent
    data class ActivityStarted(val activityName: String) : SimulationEvent
    data class ActivityEnded(val activityName: String) : SimulationEvent
    data class ManualRoutine(val routineId: String) : SimulationEvent
    data class FocusLifecycle(val started: Boolean) : SimulationEvent
}

data class EngineEvaluationOutput(
    val trace: SimulationTrace,
    val selectedRoutine: SmartRoutine?,
    val targetProfileId: String?,
    val targetSoundMode: SoundMode?,
    val startFocusMinutes: Int?,
    val endFocusSession: Boolean,
    val setAutomationPaused: Boolean?,
    val inAppNotice: String?
)

class AutomationSimulationEngine {

    fun evaluate(
        event: SimulationEvent,
        routines: List<SmartRoutine>,
        profiles: List<SmartProfile>,
        context: SimulationContextState,
        isAutomationPaused: Boolean,
        activeProfileId: String,
        isFocusActive: Boolean,
        conflictPolicy: ConflictResolutionPolicy
    ): EngineEvaluationOutput {
        val triggerLabel = describeEvent(event)

        if (isAutomationPaused && event !is SimulationEvent.ManualRoutine) {
            return EngineEvaluationOutput(
                trace = SimulationTrace(
                    id = UUID.randomUUID().toString(),
                    triggerReceived = triggerLabel,
                    matchingRoutineNames = emptyList(),
                    conditionsEvaluated = listOf("Global Automation Pause is ON"),
                    exceptionsChecked = listOf("All automatic triggers suspended"),
                    actionsSelected = emptyList(),
                    conflictDetected = false,
                    finalResult = "No actions executed because Global Automation Pause is active."
                ),
                selectedRoutine = null,
                targetProfileId = null,
                targetSoundMode = null,
                startFocusMinutes = null,
                endFocusSession = false,
                setAutomationPaused = null,
                inAppNotice = null
            )
        }

        val matching = routines.filter { routine ->
            routine.isEnabled && matchesTrigger(routine, event, context)
        }

        if (matching.isEmpty()) {
            return EngineEvaluationOutput(
                trace = SimulationTrace(
                    id = UUID.randomUUID().toString(),
                    triggerReceived = triggerLabel,
                    matchingRoutineNames = emptyList(),
                    conditionsEvaluated = listOf("Checked ${routines.count { it.isEnabled }} enabled routines"),
                    exceptionsChecked = emptyList(),
                    actionsSelected = emptyList(),
                    conflictDetected = false,
                    finalResult = "No enabled routine matched the simulated trigger."
                ),
                selectedRoutine = null,
                targetProfileId = null,
                targetSoundMode = null,
                startFocusMinutes = null,
                endFocusSession = false,
                setAutomationPaused = null,
                inAppNotice = null
            )
        }

        val conditionLogs = mutableListOf<String>()
        val conditionPassing = matching.filter { routine ->
            val passed = evaluateConditions(
                routine = routine,
                context = context,
                activeProfileId = activeProfileId,
                isAutomationPaused = isAutomationPaused,
                isFocusActive = isFocusActive,
                profiles = profiles
            )
            conditionLogs.add("${routine.name}: ${if (passed) "Conditions PASSED" else "Conditions FAILED"}")
            passed
        }

        if (conditionPassing.isEmpty()) {
            return EngineEvaluationOutput(
                trace = SimulationTrace(
                    id = UUID.randomUUID().toString(),
                    triggerReceived = triggerLabel,
                    matchingRoutineNames = matching.map { it.name },
                    conditionsEvaluated = conditionLogs,
                    exceptionsChecked = emptyList(),
                    actionsSelected = emptyList(),
                    conflictDetected = false,
                    finalResult = "Matching routines did not satisfy all required conditions."
                ),
                selectedRoutine = null,
                targetProfileId = null,
                targetSoundMode = null,
                startFocusMinutes = null,
                endFocusSession = false,
                setAutomationPaused = null,
                inAppNotice = null
            )
        }

        val exceptionLogs = mutableListOf<String>()
        val eligible = conditionPassing.filter { routine ->
            val blockedByException = checkBlockingException(routine, context)
            if (blockedByException != null) {
                exceptionLogs.add("${routine.name}: Skipped ($blockedByException)")
                false
            } else {
                val passThrough = routine.exceptions.joinToString { it.type.displayName }
                    .ifEmpty { "None" }
                exceptionLogs.add("${routine.name}: Active exceptions -> $passThrough")
                true
            }
        }

        if (eligible.isEmpty()) {
            return EngineEvaluationOutput(
                trace = SimulationTrace(
                    id = UUID.randomUUID().toString(),
                    triggerReceived = triggerLabel,
                    matchingRoutineNames = matching.map { it.name },
                    conditionsEvaluated = conditionLogs,
                    exceptionsChecked = exceptionLogs,
                    actionsSelected = emptyList(),
                    conflictDetected = false,
                    finalResult = "Eligible routines were skipped due to rule exception windows."
                ),
                selectedRoutine = null,
                targetProfileId = null,
                targetSoundMode = null,
                startFocusMinutes = null,
                endFocusSession = false,
                setAutomationPaused = null,
                inAppNotice = null
            )
        }

        val sorted = eligible.sortedByDescending { it.priority }
        val conflictDetails = detectConflicts(sorted)
        val hasConflict = conflictDetails != null && sorted.size > 1

        val winner = when {
            !hasConflict -> sorted.first()
            conflictPolicy == ConflictResolutionPolicy.KEEP_CURRENT -> null
            else -> sorted.first()
        }

        val requiresConfirmation = hasConflict && conflictPolicy == ConflictResolutionPolicy.ASK_USER

        if (requiresConfirmation && winner != null) {
            return EngineEvaluationOutput(
                trace = SimulationTrace(
                    id = UUID.randomUUID().toString(),
                    triggerReceived = triggerLabel,
                    matchingRoutineNames = sorted.map { it.name },
                    conditionsEvaluated = conditionLogs,
                    exceptionsChecked = exceptionLogs,
                    actionsSelected = listOf("Awaiting user confirmation before executing"),
                    conflictDetected = true,
                    conflictExplanation = conflictDetails,
                    conflictingRoutineIds = sorted.map { it.id },
                    requiresUserConfirmation = true,
                    finalResult = "Conflict detected between ${sorted.joinToString(" & ") { it.name }}. Confirmation requested."
                ),
                selectedRoutine = winner,
                targetProfileId = null,
                targetSoundMode = null,
                startFocusMinutes = null,
                endFocusSession = false,
                setAutomationPaused = null,
                inAppNotice = null
            )
        }

        if (winner == null) {
            return EngineEvaluationOutput(
                trace = SimulationTrace(
                    id = UUID.randomUUID().toString(),
                    triggerReceived = triggerLabel,
                    matchingRoutineNames = sorted.map { it.name },
                    conditionsEvaluated = conditionLogs,
                    exceptionsChecked = exceptionLogs,
                    actionsSelected = listOf("Kept current state per conflict policy"),
                    conflictDetected = true,
                    conflictExplanation = conflictDetails,
                    conflictingRoutineIds = sorted.map { it.id },
                    requiresUserConfirmation = false,
                    finalResult = "Conflict detected; kept current phone state safely."
                ),
                selectedRoutine = null,
                targetProfileId = null,
                targetSoundMode = null,
                startFocusMinutes = null,
                endFocusSession = false,
                setAutomationPaused = null,
                inAppNotice = null
            )
        }

        return buildExecutionOutput(
            winner = winner,
            allMatched = sorted,
            triggerLabel = triggerLabel,
            conditionLogs = conditionLogs,
            exceptionLogs = exceptionLogs,
            hasConflict = hasConflict,
            conflictDetails = conflictDetails
        )
    }

    fun executeConfirmedRoutine(
        winner: SmartRoutine,
        conflictingNames: List<String>,
        triggerLabel: String,
        conflictExplanation: String?
    ): EngineEvaluationOutput {
        return buildExecutionOutput(
            winner = winner,
            allMatched = listOf(winner),
            triggerLabel = triggerLabel,
            conditionLogs = listOf("${winner.name}: Confirmed by user"),
            exceptionLogs = listOf("Exceptions preserved: ${winner.exceptions.joinToString { it.type.displayName }.ifEmpty { "None" }}"),
            hasConflict = conflictingNames.size > 1,
            conflictDetails = conflictExplanation
        )
    }

    fun simulateIncomingInterruption(
        contact: PriorityContact,
        interruptionType: InterruptionType,
        isRepeatCaller: Boolean,
        activeProfile: SmartProfile
    ): InterruptionDecision {
        val isSoundNormal = activeProfile.soundMode == SoundMode.SOUND
        val contactActive = contact.isEnabled

        val isAllowed: Boolean
        val reason: String

        when {
            contact.priorityLevel == ContactPriority.EMERGENCY -> {
                isAllowed = true
                reason = "Emergency contacts always get through — ${interruptionType.displayName} from ${contact.name} bypassed ${activeProfile.name} (${activeProfile.soundMode.displayName})."
            }
            isSoundNormal -> {
                isAllowed = true
                reason = "${activeProfile.name} uses Sound mode, so interruptions from ${contact.name} are allowed."
            }
            isRepeatCaller && contact.allowRepeatCallers -> {
                isAllowed = true
                reason = "Repeat-caller emergency safeguard allowed ${contact.name} through ${activeProfile.name} (${activeProfile.soundMode.displayName})."
            }
            contactActive && contact.priorityLevel == ContactPriority.HIGH_PRIORITY && activeProfile.allowHighPriorityContacts -> {
                if (contact.allowedInterruptions.contains(interruptionType)) {
                    isAllowed = true
                    reason = "${activeProfile.name} permits High-Priority ${interruptionType.displayName} from ${contact.name}."
                } else {
                    isAllowed = false
                    reason = "${contact.name} does not have ${interruptionType.displayName} enabled as an exception."
                }
            }
            else -> {
                isAllowed = false
                reason = "Blocked by ${activeProfile.name} (${activeProfile.soundMode.displayName}). ${contact.name} (${contact.priorityLevel.displayName}) does not bypass this profile's filter."
            }
        }

        return InterruptionDecision(
            contactName = contact.name,
            priorityLevel = contact.priorityLevel,
            interruptionType = interruptionType,
            isAllowed = isAllowed,
            activeProfileName = activeProfile.name,
            explanation = reason
        )
    }

    private fun buildExecutionOutput(
        winner: SmartRoutine,
        allMatched: List<SmartRoutine>,
        triggerLabel: String,
        conditionLogs: List<String>,
        exceptionLogs: List<String>,
        hasConflict: Boolean,
        conflictDetails: String?
    ): EngineEvaluationOutput {
        var targetProfileId: String? = winner.targetProfileId
        var targetSoundMode: SoundMode? = null
        var startFocusMinutes: Int? = null
        var endFocusSession = false
        var setAutomationPaused: Boolean? = null
        var inAppNotice: String? = null

        winner.actions.forEach { action ->
            when (action.type) {
                ActionType.ACTIVATE_PROFILE -> targetProfileId = action.targetValue
                ActionType.SET_SOUND_MODE -> {
                    targetSoundMode = runCatching { SoundMode.valueOf(action.targetValue) }.getOrNull()
                }
                ActionType.START_FOCUS_SESSION -> {
                    startFocusMinutes = action.targetValue.toIntOrNull() ?: 25
                }
                ActionType.END_FOCUS_SESSION -> endFocusSession = true
                ActionType.PAUSE_AUTOMATION -> setAutomationPaused = true
                ActionType.RESUME_AUTOMATION -> setAutomationPaused = false
                ActionType.SHOW_NOTIFICATION -> inAppNotice = action.targetValue
                ActionType.RECORD_ACTIVITY -> Unit
            }
        }

        val summaryText = if (hasConflict) {
            "Resolved overlap: executed '${winner.name}'."
        } else {
            "Executed '${winner.name}' cleanly."
        }

        return EngineEvaluationOutput(
            trace = SimulationTrace(
                id = UUID.randomUUID().toString(),
                triggerReceived = triggerLabel,
                matchingRoutineNames = allMatched.map { it.name },
                conditionsEvaluated = conditionLogs,
                exceptionsChecked = exceptionLogs,
                actionsSelected = winner.actions.map { it.label },
                conflictDetected = hasConflict,
                conflictExplanation = conflictDetails,
                conflictingRoutineIds = if (hasConflict) allMatched.map { it.id } else emptyList(),
                requiresUserConfirmation = false,
                finalResult = summaryText
            ),
            selectedRoutine = winner,
            targetProfileId = targetProfileId,
            targetSoundMode = targetSoundMode,
            startFocusMinutes = startFocusMinutes,
            endFocusSession = endFocusSession,
            setAutomationPaused = setAutomationPaused,
            inAppNotice = inAppNotice
        )
    }

    private fun describeEvent(event: SimulationEvent): String = when (event) {
        is SimulationEvent.TimeAndDay ->
            "Simulated Clock -> ${event.day.label} at ${TimeUtils.formatTime12h(event.hour, event.minute)}"
        is SimulationEvent.LocationEntered -> "Simulated Location Entered -> ${event.locationName}"
        is SimulationEvent.LocationExited -> "Simulated Location Exited -> ${event.locationName}"
        is SimulationEvent.ActivityStarted -> "Simulated Activity Started -> ${event.activityName}"
        is SimulationEvent.ActivityEnded -> "Simulated Activity Ended -> ${event.activityName}"
        is SimulationEvent.ManualRoutine -> "Manual Routine Trigger"
        is SimulationEvent.FocusLifecycle ->
            if (event.started) "Focus Session Started" else "Focus Session Ended"
    }

    private fun matchesTrigger(
        routine: SmartRoutine,
        event: SimulationEvent,
        context: SimulationContextState
    ): Boolean {
        val t = routine.trigger
        return when (event) {
            is SimulationEvent.ManualRoutine -> routine.id == event.routineId
            is SimulationEvent.TimeAndDay -> {
                val timeMatch = (t.type == TriggerType.SPECIFIC_TIME || t.type == TriggerType.DATE_AND_TIME) &&
                    t.hour == event.hour && t.minute == event.minute && dayMatches(t.dayFilter, event.day)
                val dayOnlyMatch = t.type == TriggerType.SPECIFIC_DAYS && dayMatches(t.dayFilter, event.day)
                timeMatch || dayOnlyMatch
            }
            is SimulationEvent.LocationEntered ->
                t.type == TriggerType.LOCATION_ENTERED &&
                    (t.location.displayName.equals(event.locationName, ignoreCase = true) ||
                        context.customLocationName.equals(event.locationName, ignoreCase = true))
            is SimulationEvent.LocationExited ->
                t.type == TriggerType.LOCATION_EXITED &&
                    t.location.displayName.equals(event.locationName, ignoreCase = true)
            is SimulationEvent.ActivityStarted ->
                t.type == TriggerType.ACTIVITY_STARTED &&
                    t.activity.displayName.equals(event.activityName, ignoreCase = true)
            is SimulationEvent.ActivityEnded ->
                t.type == TriggerType.ACTIVITY_ENDED &&
                    t.activity.displayName.equals(event.activityName, ignoreCase = true)
            is SimulationEvent.FocusLifecycle ->
                (event.started && t.type == TriggerType.FOCUS_STARTED) ||
                    (!event.started && t.type == TriggerType.FOCUS_ENDED)
        }
    }

    private fun evaluateConditions(
        routine: SmartRoutine,
        context: SimulationContextState,
        activeProfileId: String,
        isAutomationPaused: Boolean,
        isFocusActive: Boolean,
        profiles: List<SmartProfile>
    ): Boolean {
        return routine.conditions.all { cond ->
            when (cond.type) {
                ConditionType.DAY_OF_WEEK -> {
                    val filter = DayFilter.entries.firstOrNull {
                        it.label.equals(cond.value, ignoreCase = true) || it.name.equals(cond.value, ignoreCase = true)
                    } ?: DayFilter.EVERY_DAY
                    dayMatches(filter, context.simulatedDay)
                }
                ConditionType.TIME_RANGE -> true
                ConditionType.ACTIVE_PROFILE -> {
                    val activeName = profiles.firstOrNull { it.id == activeProfileId }?.name.orEmpty()
                    activeProfileId == cond.value || activeName.equals(cond.value, ignoreCase = true)
                }
                ConditionType.AUTOMATION_ENABLED -> !isAutomationPaused
                ConditionType.SIMULATED_LOCATION ->
                    context.location.displayName.equals(cond.value, ignoreCase = true)
                ConditionType.SIMULATED_ACTIVITY ->
                    context.activity.displayName.equals(cond.value, ignoreCase = true)
                ConditionType.FOCUS_SESSION_ACTIVE ->
                    if (cond.value.contains("inactive", ignoreCase = true)) !isFocusActive else isFocusActive
            }
        }
    }

    private fun checkBlockingException(routine: SmartRoutine, context: SimulationContextState): String? {
        for (exc in routine.exceptions) {
            if (exc.type == ExceptionType.MANUALLY_DISABLED) {
                return "Manually disabled exception active"
            }
            if (exc.type == ExceptionType.SPECIFIC_DAYS) {
                val excludedDay = DayFilter.entries.firstOrNull {
                    it.label.equals(exc.value, ignoreCase = true)
                }
                if (excludedDay != null && dayMatches(excludedDay, context.simulatedDay)) {
                    return "Exception day '${exc.value}' matched"
                }
            }
        }
        return null
    }

    private fun detectConflicts(routines: List<SmartRoutine>): String? {
        if (routines.size < 2) return null
        val profileTargets = routines.mapNotNull { r ->
            r.actions.firstOrNull { it.type == ActionType.ACTIVATE_PROFILE }?.targetValue?.let { r.name to it }
        }
        val distinctProfiles = profileTargets.map { it.second }.distinct()
        val soundTargets = routines.mapNotNull { r ->
            r.actions.firstOrNull { it.type == ActionType.SET_SOUND_MODE }?.targetValue?.let { r.name to it }
        }
        val distinctSounds = soundTargets.map { it.second }.distinct()

        return when {
            distinctProfiles.size > 1 ->
                "Multiple matching routines attempt to activate different profiles (${profileTargets.joinToString { "${it.first} -> ${it.second.removePrefix("profile_")}" }})."
            distinctSounds.size > 1 ->
                "Conflicting sound modes requested (${soundTargets.joinToString { "${it.first} -> ${it.second}" }})."
            else -> null
        }
    }

    fun dayMatches(filter: DayFilter, actual: DayFilter): Boolean {
        return when (filter) {
            DayFilter.EVERY_DAY -> true
            DayFilter.WEEKDAYS -> actual in listOf(
                DayFilter.MONDAY, DayFilter.TUESDAY, DayFilter.WEDNESDAY, DayFilter.THURSDAY, DayFilter.FRIDAY, DayFilter.WEEKDAYS
            )
            DayFilter.WEEKENDS -> actual in listOf(DayFilter.SATURDAY, DayFilter.SUNDAY, DayFilter.WEEKENDS)
            else -> filter == actual
        }
    }

    fun findPotentialConflicts(routines: List<SmartRoutine>): List<Pair<SmartRoutine, SmartRoutine>> {
        val enabled = routines.filter { it.isEnabled }
        val pairs = mutableListOf<Pair<SmartRoutine, SmartRoutine>>()
        for (i in enabled.indices) {
            for (j in i + 1 until enabled.size) {
                val a = enabled[i]
                val b = enabled[j]
                val sameTime = a.trigger.type == TriggerType.SPECIFIC_TIME &&
                    b.trigger.type == TriggerType.SPECIFIC_TIME &&
                    a.trigger.hour == b.trigger.hour &&
                    a.trigger.minute == b.trigger.minute &&
                    (dayMatches(a.trigger.dayFilter, b.trigger.dayFilter) ||
                        dayMatches(b.trigger.dayFilter, a.trigger.dayFilter))
                if (sameTime && detectConflicts(listOf(a, b)) != null) {
                    pairs.add(a to b)
                }
            }
        }
        return pairs
    }
}
