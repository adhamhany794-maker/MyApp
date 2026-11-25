package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.models.ActionType
import com.example.models.ConditionType
import com.example.models.ConflictResolutionPolicy
import com.example.models.DayFilter
import com.example.models.ExceptionType
import com.example.models.RuleAction
import com.example.models.RuleCondition
import com.example.models.RuleException
import com.example.models.RuleTrigger
import com.example.models.SimulatedActivity
import com.example.models.SimulatedLocation
import com.example.models.SmartRoutine
import com.example.models.SoundMode
import com.example.models.TriggerType
import com.example.simulation.AutomationSimulationEngine
import com.example.simulation.SimulationEvent
import com.example.ui.components.CompanionCard
import com.example.ui.components.FeatureExplanationDialog
import com.example.ui.components.ScreenIntroHeader
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StatusEmerald
import com.example.utils.AppStrings
import com.example.utils.FeatureExplanations
import com.example.utils.FeatureHelpTopic
import com.example.utils.TimeUtils
import com.example.viewmodels.SmartCompanionUiState
import com.example.viewmodels.SmartCompanionViewModel
import java.util.UUID
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RuleBuilderScreen(
    existingRoutine: SmartRoutine?,
    strings: AppStrings,
    state: SmartCompanionUiState,
    viewModel: SmartCompanionViewModel,
    onSaved: () -> Unit
) {
    val initial = existingRoutine ?: state.routineDraftFromAssistant

    // If editing an existing rule, start on Review (or Step 1) - starting at Step 1 makes guided editing clear,
    // and clickable step tabs let users jump directly to Review (Step 5) anytime.
    var currentStep by remember(initial?.id) { mutableIntStateOf(if (existingRoutine != null) 4 else 0) }
    var showHelp by remember { mutableStateOf(false) }
    var showAdvancedOptions by remember { mutableStateOf(false) }

    var name by remember(initial?.id) {
        mutableStateOf(
            if (existingRoutine != null) strings.localizedRoutineName(existingRoutine)
            else initial?.name ?: ""
        )
    }
    var description by remember(initial?.id) {
        mutableStateOf(
            if (existingRoutine != null) strings.localizedRoutineDescription(existingRoutine)
            else initial?.description ?: ""
        )
    }
    var priority by remember(initial?.id) { mutableIntStateOf(initial?.priority ?: 7) }

    // 1. WHEN
    var triggerType by remember(initial?.id) {
        mutableStateOf(initial?.trigger?.type ?: TriggerType.SPECIFIC_TIME)
    }
    var triggerHour by remember(initial?.id) { mutableIntStateOf(initial?.trigger?.hour ?: 18) }
    var triggerMinute by remember(initial?.id) { mutableIntStateOf(initial?.trigger?.minute ?: 0) }
    var dayFilter by remember(initial?.id) {
        mutableStateOf(initial?.trigger?.dayFilter ?: DayFilter.WEEKDAYS)
    }
    var location by remember(initial?.id) {
        mutableStateOf(initial?.trigger?.location ?: SimulatedLocation.LIBRARY)
    }
    var activity by remember(initial?.id) {
        mutableStateOf(initial?.trigger?.activity ?: SimulatedActivity.STUDYING)
    }
    var endHour by remember(initial?.id) { mutableIntStateOf(20) }

    // 2. CONDITIONS
    var selectedConditions by remember(initial?.id) {
        mutableStateOf(
            initial?.conditions?.map { it.type }?.toSet()
                ?: setOf(ConditionType.AUTOMATION_ENABLED)
        )
    }

    // 3. ACTIONS
    var selectedActionTypes by remember(initial?.id) {
        mutableStateOf(
            initial?.actions?.map { it.type }?.toSet()
                ?: setOf(ActionType.ACTIVATE_PROFILE, ActionType.SET_SOUND_MODE)
        )
    }
    var targetProfileId by remember(initial?.id) {
        mutableStateOf(initial?.targetProfileId ?: "profile_study")
    }
    var targetSoundMode by remember(initial?.id) {
        mutableStateOf(
            initial?.actions?.firstOrNull { it.type == ActionType.SET_SOUND_MODE }
                ?.let { runCatching { SoundMode.valueOf(it.targetValue) }.getOrNull() }
                ?: SoundMode.SILENT
        )
    }
    var focusDuration by remember(initial?.id) { mutableIntStateOf(45) }

    // 4. EXCEPTIONS
    var selectedExceptions by remember(initial?.id) {
        mutableStateOf(
            initial?.exceptions?.map { it.type }?.toSet()
                ?: setOf(ExceptionType.EMERGENCY_CONTACTS)
        )
    }

    var validationError by remember { mutableStateOf<String?>(null) }
    var testResultText by remember { mutableStateOf<String?>(null) }

    val triggerSummary = when (triggerType) {
        TriggerType.SPECIFIC_TIME ->
            "${strings.dayFilterLabel(dayFilter)} • ${TimeUtils.formatTime12h(triggerHour, triggerMinute, strings.isArabic)}"
        TriggerType.SPECIFIC_DAYS -> strings.dayFilterLabel(dayFilter)
        TriggerType.DATE_AND_TIME ->
            "${strings.dayFilterLabel(dayFilter)} ${TimeUtils.formatTime12h(triggerHour, triggerMinute, strings.isArabic)}"
        TriggerType.MANUAL -> strings.triggerTypeLabel(TriggerType.MANUAL)
        TriggerType.FOCUS_STARTED -> strings.triggerTypeLabel(TriggerType.FOCUS_STARTED)
        TriggerType.FOCUS_ENDED -> strings.triggerTypeLabel(TriggerType.FOCUS_ENDED)
        TriggerType.LOCATION_ENTERED ->
            "${strings.triggerTypeLabel(TriggerType.LOCATION_ENTERED)}: ${strings.locationLabel(location)}"
        TriggerType.LOCATION_EXITED ->
            "${strings.triggerTypeLabel(TriggerType.LOCATION_EXITED)}: ${strings.locationLabel(location)}"
        TriggerType.ACTIVITY_STARTED ->
            "${strings.triggerTypeLabel(TriggerType.ACTIVITY_STARTED)}: ${strings.activityLabel(activity)}"
        TriggerType.ACTIVITY_ENDED ->
            "${strings.triggerTypeLabel(TriggerType.ACTIVITY_ENDED)}: ${strings.activityLabel(activity)}"
    }

    val stepLabels = if (strings.isArabic) {
        listOf("1. متى", "2. الشروط", "3. الإجراءات", "4. الاستثناءات", "5. المراجعة")
    } else {
        listOf("1. When", "2. Conditions", "3. Actions", "4. Exceptions", "5. Review")
    }

    fun buildCandidateRoutine(): SmartRoutine {
        val targetProfile = state.profiles.firstOrNull { it.id == targetProfileId }
        val targetProfileName = targetProfile?.let { strings.localizedProfileName(it) } ?: "Study"
        val resolvedName = name.trim()
        val actions = selectedActionTypes.map { type ->
            when (type) {
                ActionType.ACTIVATE_PROFILE -> RuleAction(type, targetProfileId, "${strings.actionTypeLabel(type)}: $targetProfileName")
                ActionType.SET_SOUND_MODE -> RuleAction(type, targetSoundMode.name, "${strings.actionTypeLabel(type)}: ${strings.soundModeLabel(targetSoundMode)}")
                ActionType.START_FOCUS_SESSION -> RuleAction(type, focusDuration.toString(), "${strings.actionTypeLabel(type)} (${focusDuration}m)")
                ActionType.END_FOCUS_SESSION -> RuleAction(type, "end", strings.actionTypeLabel(type))
                ActionType.PAUSE_AUTOMATION -> RuleAction(type, "pause", strings.actionTypeLabel(type))
                ActionType.RESUME_AUTOMATION -> RuleAction(type, "resume", strings.actionTypeLabel(type))
                ActionType.SHOW_NOTIFICATION -> RuleAction(type, "$resolvedName activated", strings.actionTypeLabel(type))
                ActionType.RECORD_ACTIVITY -> RuleAction(type, "log", strings.actionTypeLabel(type))
            }
        }
        val conditions = selectedConditions.map { c ->
            val v = when (c) {
                ConditionType.DAY_OF_WEEK -> strings.dayFilterLabel(dayFilter)
                ConditionType.TIME_RANGE -> "${TimeUtils.formatTime12h(triggerHour, 0, strings.isArabic)}–${TimeUtils.formatTime12h(endHour, 0, strings.isArabic)}"
                ConditionType.ACTIVE_PROFILE -> targetProfileId
                ConditionType.AUTOMATION_ENABLED -> strings.activeLabel
                ConditionType.SIMULATED_LOCATION -> strings.locationLabel(location)
                ConditionType.SIMULATED_ACTIVITY -> strings.activityLabel(activity)
                ConditionType.FOCUS_SESSION_ACTIVE -> strings.activeLabel
            }
            RuleCondition(c, v)
        }
        val exceptions = selectedExceptions.map { e ->
            RuleException(e, strings.exceptionTypeLabel(e))
        }
        return SmartRoutine(
            id = existingRoutine?.id ?: "routine_${UUID.randomUUID().toString().take(6)}",
            name = resolvedName,
            description = description.trim().ifEmpty { triggerSummary },
            isEnabled = existingRoutine?.isEnabled ?: true,
            trigger = RuleTrigger(
                type = triggerType,
                hour = triggerHour,
                minute = triggerMinute,
                dayFilter = dayFilter,
                location = location,
                activity = activity,
                summary = triggerSummary
            ),
            conditions = conditions,
            actions = actions,
            exceptions = exceptions,
            priority = priority,
            scheduleStartTime = TimeUtils.formatTime12h(triggerHour, triggerMinute),
            scheduleEndTime = TimeUtils.formatTime12h(endHour, 0),
            targetProfileId = if (ActionType.ACTIVATE_PROFILE in selectedActionTypes) targetProfileId else null,
            lastExecutionResult = existingRoutine?.lastExecutionResult
                ?: (if (strings.isArabic) "محفوظ — جاهز للتشغيل" else "Saved — ready to evaluate"),
            lastExecutedAtMillis = existingRoutine?.lastExecutedAtMillis,
            executionCount = existingRoutine?.executionCount ?: 0,
            skippedCount = existingRoutine?.skippedCount ?: 0
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ScreenIntroHeader(
                    title = if (existingRoutine != null) "${strings.edit}: ${strings.localizedRoutineName(existingRoutine)}" else strings.screenRuleBuilder,
                    subtitle = strings.ruleBuilderSubtitle,
                    strings = strings,
                    onInfoClick = { showHelp = true }
                )
            }

            // Guided Step Indicator + Progress
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(
                        progress = { (currentStep + 1) / 5f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(MaterialTheme.shapes.small)
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        stepLabels.forEachIndexed { idx, label ->
                            val selected = currentStep == idx
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    validationError = null
                                    currentStep = idx
                                },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("builder_step_$idx")
                            )
                        }
                    }
                }
            }

            when (currentStep) {
                // STEP 1: WHEN
                0 -> item {
                    CompanionCard {
                        Text(strings.stepWhenTitle, style = MaterialTheme.typography.titleMedium)
                        Text(
                            strings.whenHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TriggerType.entries.forEach { t ->
                                FilterChip(
                                    selected = triggerType == t,
                                    onClick = { triggerType = t },
                                    label = { Text(strings.triggerTypeLabel(t)) },
                                    modifier = Modifier.testTag("trigger_${t.name}")
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        if (triggerType in listOf(TriggerType.SPECIFIC_TIME, TriggerType.DATE_AND_TIME, TriggerType.SPECIFIC_DAYS)) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    DayFilter.EVERY_DAY,
                                    DayFilter.WEEKDAYS,
                                    DayFilter.WEEKENDS,
                                    DayFilter.MONDAY,
                                    DayFilter.FRIDAY
                                ).forEach { df ->
                                    FilterChip(
                                        selected = dayFilter == df,
                                        onClick = { dayFilter = df },
                                        label = { Text(strings.dayFilterLabel(df)) }
                                    )
                                }
                            }
                            if (triggerType != TriggerType.SPECIFIC_DAYS) {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = TimeUtils.formatTime12h(triggerHour, triggerMinute, strings.isArabic),
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Slider(
                                    value = triggerHour.toFloat(),
                                    onValueChange = { triggerHour = it.roundToInt().coerceIn(0, 23) },
                                    valueRange = 0f..23f
                                )
                                Text(
                                    text = "${if (strings.isArabic) "إلى" else "Until"}: ${TimeUtils.formatTime12h(endHour, 0, strings.isArabic)}",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Slider(
                                    value = endHour.toFloat(),
                                    onValueChange = { endHour = it.roundToInt().coerceIn(0, 23) },
                                    valueRange = 0f..23f
                                )
                            }
                        }

                        if (triggerType in listOf(TriggerType.LOCATION_ENTERED, TriggerType.LOCATION_EXITED)) {
                            Text(strings.simLocationTitle, style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(6.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                SimulatedLocation.entries.forEach { loc ->
                                    FilterChip(
                                        selected = location == loc,
                                        onClick = { location = loc },
                                        label = { Text(strings.locationLabel(loc)) }
                                    )
                                }
                            }
                        }

                        if (triggerType in listOf(TriggerType.ACTIVITY_STARTED, TriggerType.ACTIVITY_ENDED)) {
                            Text(strings.simActivityTitle, style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(6.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                SimulatedActivity.entries.forEach { act ->
                                    FilterChip(
                                        selected = activity == act,
                                        onClick = { activity = act },
                                        label = { Text(strings.activityLabel(act)) }
                                    )
                                }
                            }
                        }
                    }
                }

                // STEP 2: CONDITIONS
                1 -> item {
                    CompanionCard {
                        Text(strings.stepConditionsTitle, style = MaterialTheme.typography.titleMedium)
                        Text(
                            strings.alwaysRunNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ConditionType.entries.forEach { cond ->
                                val active = cond in selectedConditions
                                FilterChip(
                                    selected = active,
                                    onClick = {
                                        selectedConditions = if (active) selectedConditions - cond else selectedConditions + cond
                                    },
                                    label = { Text(strings.conditionTypeLabel(cond)) }
                                )
                            }
                        }
                    }
                }

                // STEP 3: ACTIONS
                2 -> item {
                    CompanionCard {
                        Text(strings.stepActionsTitle, style = MaterialTheme.typography.titleMedium)
                        Text(
                            strings.actionsHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ActionType.entries.forEach { act ->
                                val active = act in selectedActionTypes
                                FilterChip(
                                    selected = active,
                                    onClick = {
                                        selectedActionTypes = if (active) selectedActionTypes - act else selectedActionTypes + act
                                        validationError = null
                                    },
                                    label = { Text(strings.actionTypeLabel(act)) },
                                    modifier = Modifier.testTag("action_${act.name}")
                                )
                            }
                        }

                        if (ActionType.ACTIVATE_PROFILE in selectedActionTypes) {
                            Spacer(Modifier.height(12.dp))
                            Text(strings.targetProfileLabel, style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(6.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                state.profiles.forEach { p ->
                                    FilterChip(
                                        selected = targetProfileId == p.id,
                                        onClick = { targetProfileId = p.id },
                                        label = { Text(strings.localizedProfileName(p)) }
                                    )
                                }
                            }
                        }

                        if (ActionType.SET_SOUND_MODE in selectedActionTypes) {
                            Spacer(Modifier.height(12.dp))
                            Text(strings.simulatedPhoneStatus, style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(6.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                SoundMode.entries.forEach { sm ->
                                    FilterChip(
                                        selected = targetSoundMode == sm,
                                        onClick = { targetSoundMode = sm },
                                        label = { Text(strings.soundModeLabel(sm)) }
                                    )
                                }
                            }
                        }

                        if (ActionType.START_FOCUS_SESSION in selectedActionTypes) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "${strings.focusDurationLabel}: ${TimeUtils.formatDurationMinutes(focusDuration, strings.isArabic)}",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(Modifier.height(6.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(25, 45, 60, 90).forEach { mins ->
                                    FilterChip(
                                        selected = focusDuration == mins,
                                        onClick = { focusDuration = mins },
                                        label = { Text(TimeUtils.formatDurationMinutes(mins, strings.isArabic)) }
                                    )
                                }
                            }
                        }

                        if (validationError != null) {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = validationError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.testTag("rule_validation_error")
                            )
                        }
                    }
                }

                // STEP 4: EXCEPTIONS & ADVANCED PRIORITY
                3 -> {
                    item {
                        CompanionCard {
                            Text(strings.stepExceptionsTitle, style = MaterialTheme.typography.titleMedium)
                            Text(
                                strings.exceptionsHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(10.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                ExceptionType.entries.forEach { exc ->
                                    val active = exc in selectedExceptions
                                    FilterChip(
                                        selected = active,
                                        onClick = {
                                            selectedExceptions = if (active) selectedExceptions - exc else selectedExceptions + exc
                                        },
                                        label = { Text(strings.exceptionTypeLabel(exc)) }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        CompanionCard {
                            TextButton(
                                onClick = { showAdvancedOptions = !showAdvancedOptions },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(strings.advancedRuleOptions, modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = if (showAdvancedOptions) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                    contentDescription = null
                                )
                            }
                            AnimatedVisibility(visible = showAdvancedOptions) {
                                Column {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "${strings.priorityLevelPrefix}: $priority / 10",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                    Slider(
                                        value = priority.toFloat(),
                                        onValueChange = { priority = it.roundToInt().coerceIn(1, 10) },
                                        valueRange = 1f..10f,
                                        steps = 8
                                    )
                                }
                            }
                        }
                    }
                }

                // STEP 5: REVIEW & SAVE
                4 -> {
                    item {
                        CompanionCard {
                            Text(strings.stepReviewTitle, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it; validationError = null },
                                label = { Text(strings.ruleNameLabel) },
                                placeholder = { Text(strings.ruleNameHint) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("rule_name_input")
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text(strings.ruleDescLabel) },
                                placeholder = { Text(strings.ruleDescHint) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    item {
                        CompanionCard(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                            borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                        ) {
                            Text(strings.ruleSummaryTitle, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            val condList = selectedConditions.joinToString { strings.conditionTypeLabel(it) }
                                .ifEmpty { if (strings.isArabic) "دائماً" else "Always" }
                            val actList = selectedActionTypes.joinToString { strings.actionTypeLabel(it) }
                                .ifEmpty { strings.errRuleActionRequired }
                            val excList = selectedExceptions.joinToString { strings.exceptionTypeLabel(it) }
                                .ifEmpty { if (strings.isArabic) "لا يوجد" else "None" }

                            RuleSummaryRow(if (strings.isArabic) "متى" else "WHEN", triggerSummary)
                            RuleSummaryRow(if (strings.isArabic) "الشروط" else "CONDITIONS", condList)
                            RuleSummaryRow(if (strings.isArabic) "الإجراءات" else "ACTIONS", actList)
                            RuleSummaryRow(if (strings.isArabic) "الاستثناءات" else "EXCEPTIONS", excList)

                            if (validationError != null) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = validationError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.testTag("rule_validation_error")
                                )
                            }

                            if (testResultText != null) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = testResultText!!,
                                    color = StatusEmerald,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.testTag("rule_test_result")
                                )
                            }

                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    if (name.isBlank()) {
                                        validationError = strings.errRuleNameRequired
                                        return@OutlinedButton
                                    }
                                    if (selectedActionTypes.isEmpty()) {
                                        validationError = strings.errRuleActionRequired
                                        return@OutlinedButton
                                    }
                                    validationError = null
                                    val candidate = buildCandidateRoutine()
                                    val engine = AutomationSimulationEngine()
                                    val simEvent = when (triggerType) {
                                        TriggerType.LOCATION_ENTERED -> SimulationEvent.LocationEntered(location.displayName)
                                        TriggerType.LOCATION_EXITED -> SimulationEvent.LocationExited(location.displayName)
                                        TriggerType.ACTIVITY_STARTED -> SimulationEvent.ActivityStarted(activity.displayName)
                                        TriggerType.ACTIVITY_ENDED -> SimulationEvent.ActivityEnded(activity.displayName)
                                        TriggerType.MANUAL -> SimulationEvent.ManualRoutine(candidate.id)
                                        TriggerType.FOCUS_STARTED -> SimulationEvent.FocusLifecycle(true)
                                        TriggerType.FOCUS_ENDED -> SimulationEvent.FocusLifecycle(false)
                                        else -> SimulationEvent.TimeAndDay(triggerHour, triggerMinute, dayFilter)
                                    }
                                    val ctx = state.preferences.simulatedContext.copy(
                                        simulatedHour = triggerHour,
                                        simulatedMinute = triggerMinute,
                                        simulatedDay = dayFilter,
                                        location = location,
                                        activity = activity
                                    )
                                    val eval = engine.evaluate(
                                        event = simEvent,
                                        routines = listOf(candidate),
                                        profiles = state.profiles,
                                        context = ctx,
                                        isAutomationPaused = false,
                                        activeProfileId = state.preferences.activeProfileId,
                                        isFocusActive = state.activeFocus.isActive,
                                        conflictPolicy = ConflictResolutionPolicy.HIGHEST_PRIORITY
                                    )
                                    testResultText = if (strings.isArabic) {
                                        "نجح الاختبار: ${eval.trace.finalResult}"
                                    } else {
                                        "Test Passed: ${eval.trace.finalResult}"
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("test_rule_button")
                            ) {
                                Icon(Icons.Rounded.Science, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(strings.testRuleBtn)
                            }
                        }
                    }
                }
            }
        }

        // Bottom Guided Step Navigation Footer
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 0) {
                    OutlinedButton(onClick = {
                        validationError = null
                        currentStep -= 1
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(strings.back)
                    }
                } else {
                    Spacer(Modifier.width(4.dp))
                }

                if (currentStep < 4) {
                    Button(
                        onClick = {
                            if (currentStep == 2 && selectedActionTypes.isEmpty()) {
                                validationError = strings.errRuleActionRequired
                                return@Button
                            }
                            validationError = null
                            currentStep += 1
                        },
                        modifier = Modifier.testTag("builder_next_button")
                    ) {
                        Text(strings.next)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                } else {
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                validationError = strings.errRuleNameRequired
                                return@Button
                            }
                            if (selectedActionTypes.isEmpty()) {
                                validationError = strings.errRuleActionRequired
                                return@Button
                            }
                            viewModel.saveRoutine(buildCandidateRoutine())
                            onSaved()
                        },
                        modifier = Modifier.testTag("save_rule_button")
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.saveRuleBtn)
                    }
                }
            }
        }
    }

    if (showHelp) {
        FeatureExplanationDialog(
            explanation = FeatureExplanations.get(FeatureHelpTopic.RULE_BUILDER, strings.isArabic),
            strings = strings,
            onDismiss = { showHelp = false }
        )
    }
}

@Composable
private fun RuleSummaryRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFamily),
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 6.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        )
    }
}
