package com.example.ui.screens

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.automation.RoutineAlarmCalculator
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
import com.example.models.SmartProfile
import com.example.models.SmartRoutine
import com.example.models.SoundMode
import com.example.models.TimeWindow
import com.example.models.TriggerType
import com.example.simulation.AutomationSimulationEngine
import com.example.simulation.SimulationEvent
import com.example.ui.components.CompanionCard
import com.example.ui.components.FeatureExplanationDialog
import com.example.ui.components.ScreenIntroHeader
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusEmerald
import com.example.utils.AppStrings
import com.example.utils.ClockTime
import com.example.utils.FeatureExplanations
import com.example.utils.FeatureHelpTopic
import com.example.utils.RoutineTimeInputValidator
import com.example.utils.TimeUtils
import com.example.viewmodels.SmartCompanionUiState
import com.example.viewmodels.SmartCompanionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

private enum class BuilderStepSheet {
    WHEN,
    ONLY_IF,
    DO_THIS,
    EXCEPT
}

@Stable
private class RuleBuilderFormState(
    existingRoutine: SmartRoutine?,
    initial: SmartRoutine?,
    strings: AppStrings
) {
    var activeSheet by mutableStateOf<BuilderStepSheet?>(null)
    var showHelp by mutableStateOf(false)

    var name by mutableStateOf(
        if (existingRoutine != null) strings.localizedRoutineName(existingRoutine)
        else initial?.name ?: ""
    )
    var description by mutableStateOf(
        if (existingRoutine != null) strings.localizedRoutineDescription(existingRoutine)
        else initial?.description ?: ""
    )
    var priority by mutableIntStateOf(initial?.priority ?: 6)

    var triggerType by mutableStateOf(initial?.trigger?.type ?: TriggerType.SPECIFIC_TIME)
    var triggerHour by mutableIntStateOf(initial?.trigger?.hour ?: 18)
    var triggerMinute by mutableIntStateOf(initial?.trigger?.minute ?: 0)
    var dayFilter by mutableStateOf(initial?.trigger?.dayFilter ?: DayFilter.WEEKDAYS)
    var location by mutableStateOf(initial?.trigger?.location ?: SimulatedLocation.LIBRARY)
    var activity by mutableStateOf(initial?.trigger?.activity ?: SimulatedActivity.STUDYING)

    private val parsedInitialEnd = initial?.scheduleEndTime?.let {
        RoutineTimeInputValidator.parseStoredClock(it)
    }
    var endHour by mutableIntStateOf(parsedInitialEnd?.hour24 ?: (((initial?.trigger?.hour ?: 18) + 2) % 24))
    var endMinute by mutableIntStateOf(parsedInitialEnd?.minute ?: (initial?.trigger?.minute ?: 0))
    var endByDuration by mutableStateOf(false)
    var selectedDurationMinutes by mutableIntStateOf(60)
    var customDurationText by mutableStateOf("60")
    var startImmediately by mutableStateOf(false)
    var startTimeError by mutableStateOf<String?>(null)
    var endTimeError by mutableStateOf<String?>(null)

    var customDays by mutableStateOf(initial?.customDays ?: emptySet())
    var isOneTime by mutableStateOf(initial?.isOneTime ?: false)
    var skipDates by mutableStateOf(initial?.skipDates ?: emptyList())
    var extraTimeWindows by mutableStateOf(initial?.extraTimeWindows ?: emptyList())
    var showSkipDatePicker by mutableStateOf(false)

    var selectedConditions by mutableStateOf(
        initial?.conditions?.map { it.type }?.toSet()
            ?: setOf(ConditionType.AUTOMATION_ENABLED)
    )

    var selectedActionTypes by mutableStateOf(
        initial?.actions?.map { it.type }?.toSet()
            ?: setOf(ActionType.ACTIVATE_PROFILE, ActionType.SET_SOUND_MODE)
    )
    var targetProfileId by mutableStateOf(initial?.targetProfileId ?: "profile_study")
    var targetSoundMode by mutableStateOf(
        initial?.actions?.firstOrNull { it.type == ActionType.SET_SOUND_MODE }
            ?.let { runCatching { SoundMode.valueOf(it.targetValue) }.getOrNull() }
            ?: SoundMode.SILENT
    )
    var enableDoNotDisturb by mutableStateOf(initial?.enableDoNotDisturb ?: false)

    private val initialFocusMins = initial?.actions?.firstOrNull { it.type == ActionType.START_FOCUS_SESSION }
        ?.targetValue?.toIntOrNull() ?: 45
    var focusDuration by mutableIntStateOf(initialFocusMins)
    var focusDurationText by mutableStateOf(initialFocusMins.toString())
    var focusDurationError by mutableStateOf<String?>(null)

    var selectedExceptions by mutableStateOf(
        initial?.exceptions?.map { it.type }?.toSet()
            ?: setOf(ExceptionType.EMERGENCY_CONTACTS)
    )

    var validationError by mutableStateOf<String?>(null)
    var sheetValidationError by mutableStateOf<String?>(null)
    var testResultText by mutableStateOf<String?>(null)
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RuleBuilderScreen(
    existingRoutine: SmartRoutine?,
    strings: AppStrings,
    state: SmartCompanionUiState,
    viewModel: SmartCompanionViewModel,
    onRequestNotificationPermission: () -> Unit = {},
    onSaved: () -> Unit
) {
    val initial = existingRoutine ?: state.routineDraftFromAssistant
    val form = remember(initial?.id, existingRoutine?.id) {
        RuleBuilderFormState(
            existingRoutine = existingRoutine,
            initial = initial,
            strings = strings
        )
    }

    val context = LocalContext.current
    val is24Hour = remember(context) { DateFormat.is24HourFormat(context) }

    BackHandler(enabled = form.activeSheet != null) {
        form.activeSheet = null
    }

    val chosenTimeRangeText = RoutineTimeInputValidator.formatTimeRange(
        start = ClockTime(form.triggerHour, form.triggerMinute),
        end = ClockTime(form.endHour, form.endMinute),
        isArabic = strings.isArabic,
        is24Hour = is24Hour
    )

    val daysDisplaySummary = if (form.customDays.isNotEmpty()) {
        form.customDays.joinToString(", ") { strings.dayFilterLabel(it, short = true) }
    } else {
        strings.dayFilterLabel(form.dayFilter)
    }
    val extraWinSummary = if (form.extraTimeWindows.isNotEmpty()) " (+${form.extraTimeWindows.size})" else ""
    val triggerSummary = when (form.triggerType) {
        TriggerType.SPECIFIC_TIME ->
            "$daysDisplaySummary • $chosenTimeRangeText$extraWinSummary"
        TriggerType.SPECIFIC_DAYS -> daysDisplaySummary
        TriggerType.DATE_AND_TIME ->
            "$daysDisplaySummary • $chosenTimeRangeText$extraWinSummary"
        TriggerType.MANUAL -> strings.triggerTypeLabel(TriggerType.MANUAL)
        TriggerType.FOCUS_STARTED -> strings.triggerTypeLabel(TriggerType.FOCUS_STARTED)
        TriggerType.FOCUS_ENDED -> strings.triggerTypeLabel(TriggerType.FOCUS_ENDED)
        TriggerType.LOCATION_ENTERED ->
            "${strings.triggerTypeLabel(TriggerType.LOCATION_ENTERED)}: ${strings.locationLabel(form.location)}"
        TriggerType.LOCATION_EXITED ->
            "${strings.triggerTypeLabel(TriggerType.LOCATION_EXITED)}: ${strings.locationLabel(form.location)}"
        TriggerType.ACTIVITY_STARTED ->
            "${strings.triggerTypeLabel(TriggerType.ACTIVITY_STARTED)}: ${strings.activityLabel(form.activity)}"
        TriggerType.ACTIVITY_ENDED ->
            "${strings.triggerTypeLabel(TriggerType.ACTIVITY_ENDED)}: ${strings.activityLabel(form.activity)}"
    }

    val conditionSummary = form.selectedConditions.joinToString(" • ") { strings.conditionTypeLabel(it) }
        .ifEmpty { if (strings.isArabic) "مثال: فقط أثناء أيام الدراسة" else "e.g., Only when automation is active" }

    val targetProfileName = state.profiles.firstOrNull { it.id == form.targetProfileId }
        ?.let { strings.localizedProfileName(it) } ?: "Study"
    val actionParts = form.selectedActionTypes.map {
        when (it) {
            ActionType.ACTIVATE_PROFILE -> targetProfileName
            ActionType.SET_SOUND_MODE -> strings.soundModeLabel(form.targetSoundMode)
            else -> strings.actionTypeLabel(it)
        }
    } + (if (form.enableDoNotDisturb) listOf(strings.phoneControlDndStatusPrefix) else emptyList())
    val actionSummary = actionParts.joinToString(" • ")
        .ifEmpty { if (strings.isArabic) "مثال: تفعيل وضع الدراسة وصامت" else "e.g., Turn on Study mode & Silent" }

    val exceptionSummary = form.selectedExceptions.joinToString(" • ") { strings.exceptionTypeLabel(it) }
        .ifEmpty { if (strings.isArabic) "مثال: جهات الطوارئ تصلك دائماً" else "e.g., Emergency contacts always get through" }

    fun buildCandidateRoutine(): SmartRoutine {
        val resolvedName = form.name.trim()
        val actions = form.selectedActionTypes.map { type ->
            when (type) {
                ActionType.ACTIVATE_PROFILE -> RuleAction(type, form.targetProfileId, "${strings.actionTypeLabel(type)}: $targetProfileName")
                ActionType.SET_SOUND_MODE -> RuleAction(type, form.targetSoundMode.name, "${strings.actionTypeLabel(type)}: ${strings.soundModeLabel(form.targetSoundMode)}")
                ActionType.START_FOCUS_SESSION -> RuleAction(type, form.focusDuration.toString(), "${strings.actionTypeLabel(type)} (${form.focusDuration}m)")
                ActionType.END_FOCUS_SESSION -> RuleAction(type, "end", strings.actionTypeLabel(type))
                ActionType.PAUSE_AUTOMATION -> RuleAction(type, "pause", strings.actionTypeLabel(type))
                ActionType.RESUME_AUTOMATION -> RuleAction(type, "resume", strings.actionTypeLabel(type))
                ActionType.SHOW_NOTIFICATION -> RuleAction(type, "$resolvedName activated", strings.actionTypeLabel(type))
                ActionType.RECORD_ACTIVITY -> RuleAction(type, "log", strings.actionTypeLabel(type))
            }
        }
        val conditions = form.selectedConditions.map { c ->
            val v = when (c) {
                ConditionType.DAY_OF_WEEK -> daysDisplaySummary
                ConditionType.TIME_RANGE -> "${TimeUtils.formatTime12h(form.triggerHour, form.triggerMinute, strings.isArabic)}–${TimeUtils.formatTime12h(form.endHour, form.endMinute, strings.isArabic)}"
                ConditionType.ACTIVE_PROFILE -> form.targetProfileId
                ConditionType.AUTOMATION_ENABLED -> strings.activeLabel
                ConditionType.SIMULATED_LOCATION -> strings.locationLabel(form.location)
                ConditionType.SIMULATED_ACTIVITY -> strings.activityLabel(form.activity)
                ConditionType.FOCUS_SESSION_ACTIVE -> strings.activeLabel
            }
            RuleCondition(c, v)
        }
        val exceptions = form.selectedExceptions.map { e ->
            RuleException(e, strings.exceptionTypeLabel(e))
        }
        return SmartRoutine(
            id = existingRoutine?.id ?: "routine_${UUID.randomUUID().toString().take(6)}",
            name = resolvedName,
            description = form.description.trim().ifEmpty { triggerSummary },
            isEnabled = existingRoutine?.isEnabled ?: true,
            trigger = RuleTrigger(
                type = form.triggerType,
                hour = form.triggerHour,
                minute = form.triggerMinute,
                dayFilter = form.dayFilter,
                location = form.location,
                activity = form.activity,
                summary = triggerSummary
            ),
            conditions = conditions,
            actions = actions,
            exceptions = exceptions,
            priority = form.priority,
            scheduleStartTime = TimeUtils.formatTime12h(form.triggerHour, form.triggerMinute, strings.isArabic),
            scheduleEndTime = TimeUtils.formatTime12h(form.endHour, form.endMinute, strings.isArabic),
            targetProfileId = if (ActionType.ACTIVATE_PROFILE in form.selectedActionTypes) form.targetProfileId else null,
            lastExecutionResult = existingRoutine?.lastExecutionResult
                ?: (if (strings.isArabic) "محفوظ — جاهز للتشغيل" else "Saved — ready to evaluate"),
            lastExecutedAtMillis = existingRoutine?.lastExecutedAtMillis,
            executionCount = existingRoutine?.executionCount ?: 0,
            skippedCount = existingRoutine?.skippedCount ?: 0,
            enableDoNotDisturb = form.enableDoNotDisturb,
            isOneTime = form.isOneTime,
            skipDates = form.skipDates,
            extraTimeWindows = form.extraTimeWindows,
            customDays = form.customDays
        )
    }

    fun validateBeforeSubmit(): String? {
        val timeErr = if (form.triggerType in listOf(TriggerType.SPECIFIC_TIME, TriggerType.DATE_AND_TIME)) {
            form.startTimeError ?: form.endTimeError
        } else {
            null
        }
        val focusErr = if (ActionType.START_FOCUS_SESSION in form.selectedActionTypes) {
            form.focusDurationError
        } else {
            null
        }
        val combinedTimeErr = timeErr ?: focusErr
        if (combinedTimeErr != null) return combinedTimeErr
        if (form.name.isBlank()) return strings.errRuleNameRequired
        if (form.selectedActionTypes.isEmpty() && !form.enableDoNotDisturb) {
            return strings.errRuleActionRequired
        }
        return null
    }

    val currentSheet = form.activeSheet
    if (currentSheet != null) {
        RuleBuilderActiveSheetView(
            activeSheet = currentSheet,
            form = form,
            strings = strings,
            is24Hour = is24Hour,
            profiles = state.profiles,
            chosenTimeRangeText = chosenTimeRangeText
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("rule_builder_list"),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ScreenIntroHeader(
                    title = if (existingRoutine != null) "${strings.edit}: ${strings.localizedRoutineName(existingRoutine)}" else strings.screenRuleBuilder,
                    subtitle = strings.ruleBuilderSubtitle,
                    strings = strings,
                    onInfoClick = { form.showHelp = true }
                )
            }

            item {
                BuilderOverviewRow(
                    title = strings.builderRowWhenTitle,
                    example = triggerSummary,
                    icon = Icons.Rounded.Schedule,
                    accent = MaterialTheme.colorScheme.primary,
                    onClick = { form.activeSheet = BuilderStepSheet.WHEN },
                    modifier = Modifier.testTag("builder_step_0")
                )
            }

            item {
                BuilderOverviewRow(
                    title = strings.builderRowOnlyIfTitle,
                    example = conditionSummary,
                    icon = Icons.Rounded.FilterList,
                    accent = MaterialTheme.colorScheme.tertiary,
                    onClick = { form.activeSheet = BuilderStepSheet.ONLY_IF },
                    modifier = Modifier.testTag("builder_step_1")
                )
            }

            item {
                BuilderOverviewRow(
                    title = strings.builderRowDoThisTitle,
                    example = actionSummary,
                    icon = Icons.Rounded.Bolt,
                    accent = StatusAmber,
                    onClick = { form.activeSheet = BuilderStepSheet.DO_THIS },
                    modifier = Modifier.testTag("builder_step_2")
                )
            }

            item {
                BuilderOverviewRow(
                    title = strings.builderRowExceptTitle,
                    example = exceptionSummary,
                    icon = Icons.Rounded.Shield,
                    accent = StatusEmerald,
                    onClick = { form.activeSheet = BuilderStepSheet.EXCEPT },
                    modifier = Modifier.testTag("builder_step_3")
                )
            }

            item {
                CompanionCard {
                    Text(strings.stepReviewTitle, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = form.name,
                        onValueChange = { form.name = it; form.validationError = null },
                        label = { Text(strings.ruleNameLabel) },
                        placeholder = { Text(strings.ruleNameHint) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rule_name_input")
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = form.description,
                        onValueChange = { form.description = it },
                        label = { Text(strings.ruleDescLabel) },
                        placeholder = { Text(strings.ruleDescHint) },
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(strings.advancedRuleOptions, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            3 to strings.priorityLowLabel,
                            6 to strings.priorityMediumLabel,
                            9 to strings.priorityHighLabel
                        ).forEach { (tierValue, tierLabel) ->
                            val selected = strings.priorityTierLabel(form.priority) == tierLabel
                            FilterChip(
                                selected = selected,
                                onClick = { form.priority = tierValue },
                                label = { Text(tierLabel) },
                                modifier = Modifier.testTag("priority_tier_$tierValue")
                            )
                        }
                    }

                    if (form.validationError != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = form.validationError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("rule_validation_error")
                        )
                    }

                    if (form.testResultText != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = form.testResultText!!,
                            color = StatusEmerald,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.testTag("rule_test_result")
                        )
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val err = validateBeforeSubmit()
                            if (err != null) {
                                form.validationError = err
                                return@Button
                            }
                            val candidate = buildCandidateRoutine()
                            if (RoutineAlarmCalculator.isTimeSchedulable(candidate) || form.startImmediately) {
                                onRequestNotificationPermission()
                            }
                            viewModel.saveRoutine(
                                routine = candidate,
                                startImmediately = form.startImmediately
                            )
                            onSaved()
                        },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("save_rule_button")
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(strings.saveRuleBtn)
                    }

                    OutlinedButton(
                        onClick = {
                            val err = validateBeforeSubmit()
                            if (err != null) {
                                form.validationError = err
                                return@OutlinedButton
                            }
                            form.validationError = null
                            val candidate = buildCandidateRoutine()
                            val engine = AutomationSimulationEngine()
                            val simEvent = when (form.triggerType) {
                                TriggerType.LOCATION_ENTERED -> SimulationEvent.LocationEntered(form.location.displayName)
                                TriggerType.LOCATION_EXITED -> SimulationEvent.LocationExited(form.location.displayName)
                                TriggerType.ACTIVITY_STARTED -> SimulationEvent.ActivityStarted(form.activity.displayName)
                                TriggerType.ACTIVITY_ENDED -> SimulationEvent.ActivityEnded(form.activity.displayName)
                                TriggerType.MANUAL -> SimulationEvent.ManualRoutine(candidate.id)
                                TriggerType.FOCUS_STARTED -> SimulationEvent.FocusLifecycle(true)
                                TriggerType.FOCUS_ENDED -> SimulationEvent.FocusLifecycle(false)
                                else -> SimulationEvent.TimeAndDay(form.triggerHour, form.triggerMinute, form.dayFilter)
                            }
                            val ctx = state.preferences.simulatedContext.copy(
                                simulatedHour = form.triggerHour,
                                simulatedMinute = form.triggerMinute,
                                simulatedDay = form.dayFilter,
                                location = form.location,
                                activity = form.activity
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
                            form.testResultText = if (strings.isArabic) {
                                "نجح الاختبار: ${eval.trace.finalResult}"
                            } else {
                                "Test Passed: ${eval.trace.finalResult}"
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
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

    if (form.showHelp) {
        FeatureExplanationDialog(
            explanation = FeatureExplanations.get(FeatureHelpTopic.RULE_BUILDER, strings.isArabic),
            strings = strings,
            onDismiss = { form.showHelp = false }
        )
    }

    if (form.showSkipDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { form.showSkipDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val formatted = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }.format(Date(millis))
                            if (formatted !in form.skipDates) {
                                form.skipDates = (form.skipDates + formatted).sorted()
                            }
                        }
                        form.showSkipDatePicker = false
                    }
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { form.showSkipDatePicker = false }) {
                    Text(strings.cancel)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun RuleBuilderActiveSheetView(
    activeSheet: BuilderStepSheet,
    form: RuleBuilderFormState,
    strings: AppStrings,
    is24Hour: Boolean,
    profiles: List<SmartProfile>,
    chosenTimeRangeText: String
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            when (activeSheet) {
                BuilderStepSheet.WHEN -> BuilderWhenSheetCard(
                    strings = strings,
                    is24Hour = is24Hour,
                    triggerType = form.triggerType,
                    onTriggerTypeChange = { form.triggerType = it },
                    dayFilter = form.dayFilter,
                    onDayFilterChange = { form.dayFilter = it },
                    customDays = form.customDays,
                    onCustomDaysChange = { form.customDays = it },
                    triggerHour = form.triggerHour,
                    triggerMinute = form.triggerMinute,
                    onStartTimeChanged = { newH, newM ->
                        form.triggerHour = newH
                        form.triggerMinute = newM
                        form.startImmediately = false
                        form.sheetValidationError = null
                        if (form.endByDuration) {
                            RoutineTimeInputValidator.validateDurationMinutes(
                                form.customDurationText,
                                strings.isArabic
                            ).minutes?.let { mins ->
                                val end = RoutineTimeInputValidator.addMinutes(newH, newM, mins)
                                form.endHour = end.hour24
                                form.endMinute = end.minute
                            }
                        }
                    },
                    onStartValidationError = {
                        form.startTimeError = it
                        if (it == null) form.sheetValidationError = null
                    },
                    onNowClicked = {
                        val durationForNow = if (form.endByDuration) {
                            RoutineTimeInputValidator.validateDurationMinutes(
                                form.customDurationText,
                                strings.isArabic
                            ).minutes
                        } else {
                            null
                        }
                        val nowResult = RoutineTimeInputValidator.applyNowSelection(
                            endModeIsDuration = form.endByDuration,
                            durationMinutes = durationForNow
                        )
                        form.triggerHour = nowResult.start.hour24
                        form.triggerMinute = nowResult.start.minute
                        nowResult.end?.let {
                            form.endHour = it.hour24
                            form.endMinute = it.minute
                        }
                        form.startImmediately = nowResult.startImmediately
                        form.startTimeError = null
                        form.sheetValidationError = null
                        form.validationError = null
                    },
                    startImmediately = form.startImmediately,
                    endByDuration = form.endByDuration,
                    onEndModeExact = {
                        form.endByDuration = false
                        form.endTimeError = null
                        form.sheetValidationError = null
                    },
                    onEndModeDuration = {
                        form.endByDuration = true
                        val durRes = RoutineTimeInputValidator.validateDurationMinutes(
                            form.customDurationText,
                            strings.isArabic
                        )
                        form.endTimeError = durRes.errorMessage
                        durRes.minutes?.let { mins ->
                            form.selectedDurationMinutes = mins
                            val end = RoutineTimeInputValidator.addMinutes(
                                form.triggerHour,
                                form.triggerMinute,
                                mins
                            )
                            form.endHour = end.hour24
                            form.endMinute = end.minute
                        }
                        form.sheetValidationError = null
                    },
                    endHour = form.endHour,
                    endMinute = form.endMinute,
                    onEndTimeChanged = { newH, newM ->
                        form.endHour = newH
                        form.endMinute = newM
                        form.sheetValidationError = null
                    },
                    onEndValidationError = {
                        form.endTimeError = it
                        if (it == null) form.sheetValidationError = null
                    },
                    selectedDurationMinutes = form.selectedDurationMinutes,
                    customDurationText = form.customDurationText,
                    endTimeError = form.endTimeError,
                    onDurationMinutesSelected = { mins ->
                        form.selectedDurationMinutes = mins
                        form.customDurationText = mins.toString()
                        form.endTimeError = null
                        form.sheetValidationError = null
                        val end = RoutineTimeInputValidator.addMinutes(
                            form.triggerHour,
                            form.triggerMinute,
                            mins
                        )
                        form.endHour = end.hour24
                        form.endMinute = end.minute
                    },
                    onDurationCustomTextChanged = { raw ->
                        form.customDurationText = raw
                        val res = RoutineTimeInputValidator.validateDurationMinutes(
                            raw,
                            strings.isArabic
                        )
                        form.endTimeError = res.errorMessage
                        if (res.minutes != null) {
                            form.selectedDurationMinutes = res.minutes
                            val end = RoutineTimeInputValidator.addMinutes(
                                form.triggerHour,
                                form.triggerMinute,
                                res.minutes
                            )
                            form.endHour = end.hour24
                            form.endMinute = end.minute
                            form.sheetValidationError = null
                        }
                    },
                    chosenTimeRangeText = chosenTimeRangeText,
                    extraTimeWindows = form.extraTimeWindows,
                    onExtraTimeWindowsChange = { form.extraTimeWindows = it },
                    onAddExtraWindow = {
                        val nextStart = RoutineTimeInputValidator.addMinutes(form.endHour, form.endMinute, 60)
                        val nextEnd = RoutineTimeInputValidator.addMinutes(nextStart.hour24, nextStart.minute, 60)
                        form.extraTimeWindows = form.extraTimeWindows + TimeWindow(
                            startHour = nextStart.hour24,
                            startMinute = nextStart.minute,
                            endHour = nextEnd.hour24,
                            endMinute = nextEnd.minute
                        )
                    },
                    isOneTime = form.isOneTime,
                    onIsOneTimeChange = { form.isOneTime = it },
                    skipDates = form.skipDates,
                    onRemoveSkipDate = { dateStr -> form.skipDates = form.skipDates - dateStr },
                    onAddSkipDateClick = { form.showSkipDatePicker = true },
                    location = form.location,
                    onLocationChange = { form.location = it },
                    activity = form.activity,
                    onActivityChange = { form.activity = it }
                )

                BuilderStepSheet.ONLY_IF -> BuilderOnlyIfSheetCard(
                    strings = strings,
                    selectedConditions = form.selectedConditions,
                    onToggleCondition = { cond ->
                        form.selectedConditions = if (cond in form.selectedConditions) {
                            form.selectedConditions - cond
                        } else {
                            form.selectedConditions + cond
                        }
                    }
                )

                BuilderStepSheet.DO_THIS -> BuilderDoThisSheetCard(
                    strings = strings,
                    profiles = profiles,
                    selectedActionTypes = form.selectedActionTypes,
                    onToggleActionType = { act ->
                        form.selectedActionTypes = if (act in form.selectedActionTypes) {
                            form.selectedActionTypes - act
                        } else {
                            form.selectedActionTypes + act
                        }
                        form.validationError = null
                    },
                    targetProfileId = form.targetProfileId,
                    onTargetProfileChange = { form.targetProfileId = it },
                    targetSoundMode = form.targetSoundMode,
                    onTargetSoundModeChange = { form.targetSoundMode = it },
                    focusDuration = form.focusDuration,
                    focusDurationText = form.focusDurationText,
                    focusDurationError = form.focusDurationError,
                    onFocusMinutesSelected = { mins ->
                        form.focusDuration = mins
                        form.focusDurationText = mins.toString()
                        form.focusDurationError = null
                        form.sheetValidationError = null
                    },
                    onFocusCustomTextChanged = { raw ->
                        form.focusDurationText = raw
                        val res = RoutineTimeInputValidator.validateDurationMinutes(
                            raw,
                            strings.isArabic
                        )
                        form.focusDurationError = res.errorMessage
                        if (res.minutes != null) {
                            form.focusDuration = res.minutes
                            form.sheetValidationError = null
                        }
                    },
                    enableDoNotDisturb = form.enableDoNotDisturb,
                    onEnableDoNotDisturbChange = {
                        form.enableDoNotDisturb = it
                        form.validationError = null
                    }
                )

                BuilderStepSheet.EXCEPT -> BuilderExceptSheetCard(
                    strings = strings,
                    selectedExceptions = form.selectedExceptions,
                    onToggleException = { exc ->
                        form.selectedExceptions = if (exc in form.selectedExceptions) {
                            form.selectedExceptions - exc
                        } else {
                            form.selectedExceptions + exc
                        }
                    }
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (form.sheetValidationError != null) {
                    Text(
                        text = form.sheetValidationError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("builder_sheet_error")
                    )
                }
                Button(
                    onClick = {
                        val err = when (activeSheet) {
                            BuilderStepSheet.WHEN ->
                                if (form.triggerType in listOf(TriggerType.SPECIFIC_TIME, TriggerType.DATE_AND_TIME)) {
                                    form.startTimeError ?: form.endTimeError
                                } else {
                                    null
                                }
                            BuilderStepSheet.DO_THIS ->
                                if (ActionType.START_FOCUS_SESSION in form.selectedActionTypes) form.focusDurationError else null
                            else -> null
                        }
                        if (err != null) {
                            form.sheetValidationError = err
                        } else {
                            form.sheetValidationError = null
                            form.activeSheet = null
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("builder_step_done_btn")
                ) {
                    Text(strings.builderDoneBtn)
                }
            }
        }
    }
}

@Composable
private fun BuilderOverviewRow(
    title: String,
    example: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    CompanionCard(
        onClick = onClick,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = example,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
