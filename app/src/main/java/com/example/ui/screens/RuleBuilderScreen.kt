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
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
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
import com.example.models.SmartRoutine
import com.example.models.SoundMode
import com.example.models.TimeWindow
import com.example.models.TriggerType
import com.example.simulation.AutomationSimulationEngine
import com.example.simulation.SimulationEvent
import com.example.ui.components.CompanionCard
import com.example.ui.components.DurationMinutesSelector
import com.example.ui.components.ExactTimeEntrySection
import com.example.ui.components.FeatureExplanationDialog
import com.example.ui.components.ScreenIntroHeader
import com.example.ui.theme.JetBrainsMonoFamily
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

    var activeSheet by remember { mutableStateOf<BuilderStepSheet?>(null) }
    var showHelp by remember { mutableStateOf(false) }

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
    var priority by remember(initial?.id) { mutableIntStateOf(initial?.priority ?: 6) }

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
    val context = LocalContext.current
    val is24Hour = remember(context) { DateFormat.is24HourFormat(context) }

    val parsedInitialEnd = remember(initial?.id) {
        initial?.scheduleEndTime?.let { RoutineTimeInputValidator.parseStoredClock(it) }
    }
    var endHour by remember(initial?.id) {
        mutableIntStateOf(parsedInitialEnd?.hour24 ?: ((triggerHour + 2) % 24))
    }
    var endMinute by remember(initial?.id) {
        mutableIntStateOf(parsedInitialEnd?.minute ?: triggerMinute)
    }
    var endByDuration by remember(initial?.id) { mutableStateOf(false) }
    var selectedDurationMinutes by remember(initial?.id) { mutableIntStateOf(60) }
    var customDurationText by remember(initial?.id) { mutableStateOf("60") }
    var startImmediately by remember(initial?.id) { mutableStateOf(false) }
    var startTimeError by remember { mutableStateOf<String?>(null) }
    var endTimeError by remember { mutableStateOf<String?>(null) }

    var customDays by remember(initial?.id) { mutableStateOf(initial?.customDays ?: emptySet()) }
    var isOneTime by remember(initial?.id) { mutableStateOf(initial?.isOneTime ?: false) }
    var skipDates by remember(initial?.id) { mutableStateOf(initial?.skipDates ?: emptyList()) }
    var extraTimeWindows by remember(initial?.id) { mutableStateOf(initial?.extraTimeWindows ?: emptyList()) }
    var showSkipDatePicker by remember { mutableStateOf(false) }

    // 2. ONLY IF (Conditions)
    var selectedConditions by remember(initial?.id) {
        mutableStateOf(
            initial?.conditions?.map { it.type }?.toSet()
                ?: setOf(ConditionType.AUTOMATION_ENABLED)
        )
    }

    // 3. DO THIS (Actions)
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
    var enableDoNotDisturb by remember(initial?.id) {
        mutableStateOf(initial?.enableDoNotDisturb ?: false)
    }
    val initialFocusMins = remember(initial?.id) {
        initial?.actions?.firstOrNull { it.type == ActionType.START_FOCUS_SESSION }
            ?.targetValue?.toIntOrNull() ?: 45
    }
    var focusDuration by remember(initial?.id) { mutableIntStateOf(initialFocusMins) }
    var focusDurationText by remember(initial?.id) { mutableStateOf(initialFocusMins.toString()) }
    var focusDurationError by remember { mutableStateOf<String?>(null) }

    // 4. EXCEPT (Exceptions)
    var selectedExceptions by remember(initial?.id) {
        mutableStateOf(
            initial?.exceptions?.map { it.type }?.toSet()
                ?: setOf(ExceptionType.EMERGENCY_CONTACTS)
        )
    }

    var validationError by remember { mutableStateOf<String?>(null) }
    var sheetValidationError by remember { mutableStateOf<String?>(null) }
    var testResultText by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = activeSheet != null) {
        activeSheet = null
    }

    val chosenTimeRangeText = RoutineTimeInputValidator.formatTimeRange(
        start = ClockTime(triggerHour, triggerMinute),
        end = ClockTime(endHour, endMinute),
        isArabic = strings.isArabic,
        is24Hour = is24Hour
    )

    val daysDisplaySummary = if (customDays.isNotEmpty()) {
        customDays.joinToString(", ") { strings.dayFilterLabel(it, short = true) }
    } else {
        strings.dayFilterLabel(dayFilter)
    }
    val extraWinSummary = if (extraTimeWindows.isNotEmpty()) " (+${extraTimeWindows.size})" else ""
    val triggerSummary = when (triggerType) {
        TriggerType.SPECIFIC_TIME ->
            "$daysDisplaySummary • $chosenTimeRangeText$extraWinSummary"
        TriggerType.SPECIFIC_DAYS -> daysDisplaySummary
        TriggerType.DATE_AND_TIME ->
            "$daysDisplaySummary • $chosenTimeRangeText$extraWinSummary"
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

    val conditionSummary = selectedConditions.joinToString(" • ") { strings.conditionTypeLabel(it) }
        .ifEmpty { if (strings.isArabic) "مثال: فقط أثناء أيام الدراسة" else "e.g., Only when automation is active" }

    val targetProfileName = state.profiles.firstOrNull { it.id == targetProfileId }
        ?.let { strings.localizedProfileName(it) } ?: "Study"
    val actionParts = selectedActionTypes.map {
        when (it) {
            ActionType.ACTIVATE_PROFILE -> targetProfileName
            ActionType.SET_SOUND_MODE -> strings.soundModeLabel(targetSoundMode)
            else -> strings.actionTypeLabel(it)
        }
    } + (if (enableDoNotDisturb) listOf(strings.phoneControlDndStatusPrefix) else emptyList())
    val actionSummary = actionParts.joinToString(" • ")
        .ifEmpty { if (strings.isArabic) "مثال: تفعيل وضع الدراسة وصامت" else "e.g., Turn on Study mode & Silent" }

    val exceptionSummary = selectedExceptions.joinToString(" • ") { strings.exceptionTypeLabel(it) }
        .ifEmpty { if (strings.isArabic) "مثال: جهات الطوارئ تصلك دائماً" else "e.g., Emergency contacts always get through" }

    fun buildCandidateRoutine(): SmartRoutine {
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
                ConditionType.DAY_OF_WEEK -> daysDisplaySummary
                ConditionType.TIME_RANGE -> "${TimeUtils.formatTime12h(triggerHour, triggerMinute, strings.isArabic)}–${TimeUtils.formatTime12h(endHour, endMinute, strings.isArabic)}"
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
            scheduleStartTime = TimeUtils.formatTime12h(triggerHour, triggerMinute, strings.isArabic),
            scheduleEndTime = TimeUtils.formatTime12h(endHour, endMinute, strings.isArabic),
            targetProfileId = if (ActionType.ACTIVATE_PROFILE in selectedActionTypes) targetProfileId else null,
            lastExecutionResult = existingRoutine?.lastExecutionResult
                ?: (if (strings.isArabic) "محفوظ — جاهز للتشغيل" else "Saved — ready to evaluate"),
            lastExecutedAtMillis = existingRoutine?.lastExecutedAtMillis,
            executionCount = existingRoutine?.executionCount ?: 0,
            skippedCount = existingRoutine?.skippedCount ?: 0,
            enableDoNotDisturb = enableDoNotDisturb,
            isOneTime = isOneTime,
            skipDates = skipDates,
            extraTimeWindows = extraTimeWindows,
            customDays = customDays
        )
    }

    if (activeSheet != null) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (activeSheet) {
                BuilderStepSheet.WHEN -> item {
                    CompanionCard {
                        Text(strings.stepWhenTitle, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            strings.whenHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
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

                        Spacer(Modifier.height(14.dp))
                        if (triggerType in listOf(TriggerType.SPECIFIC_TIME, TriggerType.DATE_AND_TIME, TriggerType.SPECIFIC_DAYS)) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    DayFilter.EVERY_DAY,
                                    DayFilter.WEEKDAYS,
                                    DayFilter.WEEKENDS
                                ).forEach { df ->
                                    FilterChip(
                                        selected = customDays.isEmpty() && dayFilter == df,
                                        onClick = {
                                            customDays = emptySet()
                                            dayFilter = df
                                        },
                                        label = { Text(strings.dayFilterLabel(df)) },
                                        modifier = Modifier.testTag("day_preset_${df.name}")
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = strings.customDaysLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    DayFilter.MONDAY,
                                    DayFilter.TUESDAY,
                                    DayFilter.WEDNESDAY,
                                    DayFilter.THURSDAY,
                                    DayFilter.FRIDAY,
                                    DayFilter.SATURDAY,
                                    DayFilter.SUNDAY
                                ).forEach { day ->
                                    val selected = day in customDays || (customDays.isEmpty() && dayFilter == day)
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            val updated = if (day in customDays) {
                                                customDays - day
                                            } else {
                                                customDays + day
                                            }
                                            customDays = updated
                                            if (updated.size == 1) {
                                                dayFilter = updated.first()
                                            }
                                        },
                                        label = { Text(strings.dayFilterLabel(day, short = true)) },
                                        modifier = Modifier.testTag("custom_day_${day.name}")
                                    )
                                }
                            }
                            if (triggerType != TriggerType.SPECIFIC_DAYS) {
                                Spacer(Modifier.height(14.dp))
                                ExactTimeEntrySection(
                                    label = strings.startTimeLabel,
                                    hour24 = triggerHour,
                                    minute = triggerMinute,
                                    strings = strings,
                                    testTagPrefix = "start_time",
                                    onTimeChanged = { newH, newM ->
                                        triggerHour = newH
                                        triggerMinute = newM
                                        startImmediately = false
                                        sheetValidationError = null
                                        if (endByDuration) {
                                            RoutineTimeInputValidator.validateDurationMinutes(
                                                customDurationText,
                                                strings.isArabic
                                            ).minutes?.let { mins ->
                                                val end = RoutineTimeInputValidator.addMinutes(newH, newM, mins)
                                                endHour = end.hour24
                                                endMinute = end.minute
                                            }
                                        }
                                    },
                                    onValidationErrorChanged = {
                                        startTimeError = it
                                        if (it == null) sheetValidationError = null
                                    },
                                    trailingAction = {
                                        FilledTonalButton(
                                            onClick = {
                                                val durationForNow = if (endByDuration) {
                                                    RoutineTimeInputValidator.validateDurationMinutes(
                                                        customDurationText,
                                                        strings.isArabic
                                                    ).minutes
                                                } else {
                                                    null
                                                }
                                                val nowResult = RoutineTimeInputValidator.applyNowSelection(
                                                    endModeIsDuration = endByDuration,
                                                    durationMinutes = durationForNow
                                                )
                                                triggerHour = nowResult.start.hour24
                                                triggerMinute = nowResult.start.minute
                                                nowResult.end?.let {
                                                    endHour = it.hour24
                                                    endMinute = it.minute
                                                }
                                                startImmediately = nowResult.startImmediately
                                                startTimeError = null
                                                sheetValidationError = null
                                                validationError = null
                                            },
                                            modifier = Modifier
                                                .defaultMinSize(minHeight = 48.dp)
                                                .testTag("start_time_now_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.MyLocation,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(strings.nowBtn)
                                        }
                                    }
                                )

                                if (startImmediately) {
                                    Spacer(Modifier.height(6.dp))
                                    Surface(
                                        color = StatusEmerald.copy(alpha = 0.14f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("starts_immediately_badge")
                                    ) {
                                        Text(
                                            text = strings.startsImmediatelyHint,
                                            color = StatusEmerald,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(14.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(Modifier.height(12.dp))

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    FilterChip(
                                        selected = !endByDuration,
                                        onClick = {
                                            endByDuration = false
                                            endTimeError = null
                                            sheetValidationError = null
                                        },
                                        label = { Text(strings.endAtExactTimeLabel) },
                                        modifier = Modifier
                                            .defaultMinSize(minHeight = 48.dp)
                                            .testTag("end_mode_exact")
                                    )
                                    FilterChip(
                                        selected = endByDuration,
                                        onClick = {
                                            endByDuration = true
                                            val durRes = RoutineTimeInputValidator.validateDurationMinutes(
                                                customDurationText,
                                                strings.isArabic
                                            )
                                            endTimeError = durRes.errorMessage
                                            durRes.minutes?.let { mins ->
                                                selectedDurationMinutes = mins
                                                val end = RoutineTimeInputValidator.addMinutes(
                                                    triggerHour,
                                                    triggerMinute,
                                                    mins
                                                )
                                                endHour = end.hour24
                                                endMinute = end.minute
                                            }
                                            sheetValidationError = null
                                        },
                                        label = { Text(strings.endAfterDurationLabel) },
                                        modifier = Modifier
                                            .defaultMinSize(minHeight = 48.dp)
                                            .testTag("end_mode_duration")
                                    )
                                }

                                Spacer(Modifier.height(10.dp))
                                if (!endByDuration) {
                                    ExactTimeEntrySection(
                                        label = strings.endTimeLabel,
                                        hour24 = endHour,
                                        minute = endMinute,
                                        strings = strings,
                                        testTagPrefix = "end_time",
                                        onTimeChanged = { newH, newM ->
                                            endHour = newH
                                            endMinute = newM
                                            sheetValidationError = null
                                        },
                                        onValidationErrorChanged = {
                                            endTimeError = it
                                            if (it == null) sheetValidationError = null
                                        }
                                    )
                                } else {
                                    DurationMinutesSelector(
                                        label = strings.endAfterDurationLabel,
                                        selectedMinutes = selectedDurationMinutes,
                                        minutesText = customDurationText,
                                        strings = strings,
                                        testTagPrefix = "end_duration",
                                        onMinutesSelected = { mins ->
                                            selectedDurationMinutes = mins
                                            customDurationText = mins.toString()
                                            endTimeError = null
                                            sheetValidationError = null
                                            val end = RoutineTimeInputValidator.addMinutes(
                                                triggerHour,
                                                triggerMinute,
                                                mins
                                            )
                                            endHour = end.hour24
                                            endMinute = end.minute
                                        },
                                        onCustomTextChanged = { raw ->
                                            customDurationText = raw
                                            val res = RoutineTimeInputValidator.validateDurationMinutes(
                                                raw,
                                                strings.isArabic
                                            )
                                            endTimeError = res.errorMessage
                                            if (res.minutes != null) {
                                                selectedDurationMinutes = res.minutes
                                                val end = RoutineTimeInputValidator.addMinutes(
                                                    triggerHour,
                                                    triggerMinute,
                                                    res.minutes
                                                )
                                                endHour = end.hour24
                                                endMinute = end.minute
                                                sheetValidationError = null
                                            }
                                        },
                                        errorMessage = endTimeError
                                    )
                                }

                                Spacer(Modifier.height(12.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("when_range_preview")
                                ) {
                                    Text(
                                        text = chosenTimeRangeText,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontFamily = JetBrainsMonoFamily,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                    )
                                }

                                Spacer(Modifier.height(14.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(Modifier.height(12.dp))

                                // Additional Time Windows
                                Text(
                                    text = strings.extraTimeWindowsLabel,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Spacer(Modifier.height(6.dp))
                                extraTimeWindows.forEachIndexed { index, win ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .testTag("extra_window_card_$index")
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = RoutineTimeInputValidator.formatTimeRange(
                                                        start = ClockTime(win.startHour, win.startMinute),
                                                        end = ClockTime(win.endHour, win.endMinute),
                                                        isArabic = strings.isArabic,
                                                        is24Hour = is24Hour
                                                    ),
                                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFamily),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                IconButton(
                                                    onClick = {
                                                        extraTimeWindows = extraTimeWindows.filterIndexed { i, _ -> i != index }
                                                    },
                                                    modifier = Modifier.testTag("remove_extra_window_$index")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.DeleteOutline,
                                                        contentDescription = strings.removeWindowDesc,
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                            ExactTimeEntrySection(
                                                label = strings.startTimeLabel,
                                                hour24 = win.startHour,
                                                minute = win.startMinute,
                                                strings = strings,
                                                testTagPrefix = "extra_start_$index",
                                                onTimeChanged = { newH, newM ->
                                                    extraTimeWindows = extraTimeWindows.mapIndexed { i, old ->
                                                        if (i == index) old.copy(startHour = newH, startMinute = newM) else old
                                                    }
                                                }
                                            )
                                            Spacer(Modifier.height(6.dp))
                                            ExactTimeEntrySection(
                                                label = strings.endTimeLabel,
                                                hour24 = win.endHour,
                                                minute = win.endMinute,
                                                strings = strings,
                                                testTagPrefix = "extra_end_$index",
                                                onTimeChanged = { newH, newM ->
                                                    extraTimeWindows = extraTimeWindows.mapIndexed { i, old ->
                                                        if (i == index) old.copy(endHour = newH, endMinute = newM) else old
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                                OutlinedButton(
                                    onClick = {
                                        val nextStart = RoutineTimeInputValidator.addMinutes(endHour, endMinute, 60)
                                        val nextEnd = RoutineTimeInputValidator.addMinutes(nextStart.hour24, nextStart.minute, 60)
                                        extraTimeWindows = extraTimeWindows + TimeWindow(
                                            startHour = nextStart.hour24,
                                            startMinute = nextStart.minute,
                                            endHour = nextEnd.hour24,
                                            endMinute = nextEnd.minute
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("builder_add_time_window_btn")
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.addTimeWindowBtn)
                                }

                                Spacer(Modifier.height(14.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(Modifier.height(12.dp))

                                // One-time routine switch
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(strings.oneTimeRoutineLabel, style = MaterialTheme.typography.titleSmall)
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            strings.oneTimeRoutineHint,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Switch(
                                        checked = isOneTime,
                                        onCheckedChange = { isOneTime = it },
                                        modifier = Modifier.testTag("builder_one_time_switch")
                                    )
                                }

                                Spacer(Modifier.height(12.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(Modifier.height(12.dp))

                                // Skip Dates
                                Text(
                                    text = strings.skipDatesLabel,
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Spacer(Modifier.height(6.dp))
                                if (skipDates.isNotEmpty()) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        skipDates.forEach { dateStr ->
                                            InputChip(
                                                selected = true,
                                                onClick = { skipDates = skipDates - dateStr },
                                                label = { Text(dateStr) },
                                                trailingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Close,
                                                        contentDescription = strings.delete,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                },
                                                modifier = Modifier.testTag("skip_date_chip_$dateStr")
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(6.dp))
                                }
                                OutlinedButton(
                                    onClick = { showSkipDatePicker = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 48.dp)
                                        .testTag("builder_add_skip_date_btn")
                                ) {
                                    Icon(Icons.Rounded.EventBusy, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(strings.addSkipDateBtn)
                                }
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

                BuilderStepSheet.ONLY_IF -> item {
                    CompanionCard {
                        Text(strings.stepConditionsTitle, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            strings.alwaysRunNote,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
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

                BuilderStepSheet.DO_THIS -> item {
                    CompanionCard {
                        Text(strings.stepActionsTitle, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            strings.actionsHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                            Spacer(Modifier.height(14.dp))
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
                            Spacer(Modifier.height(14.dp))
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
                            Spacer(Modifier.height(14.dp))
                            DurationMinutesSelector(
                                label = strings.focusDurationLabel,
                                selectedMinutes = focusDuration,
                                minutesText = focusDurationText,
                                strings = strings,
                                testTagPrefix = "focus_duration",
                                onMinutesSelected = { mins ->
                                    focusDuration = mins
                                    focusDurationText = mins.toString()
                                    focusDurationError = null
                                    sheetValidationError = null
                                },
                                onCustomTextChanged = { raw ->
                                    focusDurationText = raw
                                    val res = RoutineTimeInputValidator.validateDurationMinutes(
                                        raw,
                                        strings.isArabic
                                    )
                                    focusDurationError = res.errorMessage
                                    if (res.minutes != null) {
                                        focusDuration = res.minutes
                                        sheetValidationError = null
                                    }
                                },
                                errorMessage = focusDurationError
                            )
                        }

                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(strings.dndEnableForRoutineLabel, style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    strings.dndEnableForRoutineHint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = enableDoNotDisturb,
                                onCheckedChange = {
                                    enableDoNotDisturb = it
                                    validationError = null
                                },
                                modifier = Modifier.testTag("builder_enable_dnd_switch")
                            )
                        }
                    }
                }

                BuilderStepSheet.EXCEPT -> item {
                    CompanionCard {
                        Text(strings.stepExceptionsTitle, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            strings.exceptionsHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
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

                null -> Unit
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (sheetValidationError != null) {
                        Text(
                            text = sheetValidationError!!,
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
                                    if (triggerType in listOf(TriggerType.SPECIFIC_TIME, TriggerType.DATE_AND_TIME)) {
                                        startTimeError ?: endTimeError
                                    } else {
                                        null
                                    }
                                BuilderStepSheet.DO_THIS ->
                                    if (ActionType.START_FOCUS_SESSION in selectedActionTypes) focusDurationError else null
                                else -> null
                            }
                            if (err != null) {
                                sheetValidationError = err
                            } else {
                                sheetValidationError = null
                                activeSheet = null
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
        return
    }

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
                onInfoClick = { showHelp = true }
            )
        }

        // Big Rounded Rows: When, Only if, Do this, Except
        item {
            BuilderOverviewRow(
                title = strings.builderRowWhenTitle,
                example = triggerSummary,
                icon = Icons.Rounded.Schedule,
                accent = MaterialTheme.colorScheme.primary,
                onClick = { activeSheet = BuilderStepSheet.WHEN },
                modifier = Modifier.testTag("builder_step_0")
            )
        }

        item {
            BuilderOverviewRow(
                title = strings.builderRowOnlyIfTitle,
                example = conditionSummary,
                icon = Icons.Rounded.FilterList,
                accent = MaterialTheme.colorScheme.tertiary,
                onClick = { activeSheet = BuilderStepSheet.ONLY_IF },
                modifier = Modifier.testTag("builder_step_1")
            )
        }

        item {
            BuilderOverviewRow(
                title = strings.builderRowDoThisTitle,
                example = actionSummary,
                icon = Icons.Rounded.Bolt,
                accent = StatusAmber,
                onClick = { activeSheet = BuilderStepSheet.DO_THIS },
                modifier = Modifier.testTag("builder_step_2")
            )
        }

        item {
            BuilderOverviewRow(
                title = strings.builderRowExceptTitle,
                example = exceptionSummary,
                icon = Icons.Rounded.Shield,
                accent = StatusEmerald,
                onClick = { activeSheet = BuilderStepSheet.EXCEPT },
                modifier = Modifier.testTag("builder_step_3")
            )
        }

        // Routine Name, Importance (High/Medium/Low), and Save
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
                    shape = MaterialTheme.shapes.small,
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
                        val selected = strings.priorityTierLabel(priority) == tierLabel
                        FilterChip(
                            selected = selected,
                            onClick = { priority = tierValue },
                            label = { Text(tierLabel) },
                            modifier = Modifier.testTag("priority_tier_$tierValue")
                        )
                    }
                }

                if (validationError != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = validationError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.testTag("rule_validation_error")
                    )
                }

                if (testResultText != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = testResultText!!,
                        color = StatusEmerald,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("rule_test_result")
                    )
                }
            }
        }

        // One Primary Orange Action + Secondary Test Link
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        val timeErr = if (triggerType in listOf(TriggerType.SPECIFIC_TIME, TriggerType.DATE_AND_TIME)) {
                            startTimeError ?: endTimeError
                        } else {
                            null
                        }
                        val focusErr = if (ActionType.START_FOCUS_SESSION in selectedActionTypes) {
                            focusDurationError
                        } else {
                            null
                        }
                        val combinedTimeErr = timeErr ?: focusErr
                        if (combinedTimeErr != null) {
                            validationError = combinedTimeErr
                            return@Button
                        }
                        if (name.isBlank()) {
                            validationError = strings.errRuleNameRequired
                            return@Button
                        }
                        if (selectedActionTypes.isEmpty() && !enableDoNotDisturb) {
                            validationError = strings.errRuleActionRequired
                            return@Button
                        }
                        val candidate = buildCandidateRoutine()
                        if (RoutineAlarmCalculator.isTimeSchedulable(candidate) || startImmediately) {
                            onRequestNotificationPermission()
                        }
                        viewModel.saveRoutine(
                            routine = candidate,
                            startImmediately = startImmediately
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
                        val timeErr = if (triggerType in listOf(TriggerType.SPECIFIC_TIME, TriggerType.DATE_AND_TIME)) {
                            startTimeError ?: endTimeError
                        } else {
                            null
                        }
                        val focusErr = if (ActionType.START_FOCUS_SESSION in selectedActionTypes) {
                            focusDurationError
                        } else {
                            null
                        }
                        val combinedTimeErr = timeErr ?: focusErr
                        if (combinedTimeErr != null) {
                            validationError = combinedTimeErr
                            return@OutlinedButton
                        }
                        if (name.isBlank()) {
                            validationError = strings.errRuleNameRequired
                            return@OutlinedButton
                        }
                        if (selectedActionTypes.isEmpty() && !enableDoNotDisturb) {
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

    if (showHelp) {
        FeatureExplanationDialog(
            explanation = FeatureExplanations.get(FeatureHelpTopic.RULE_BUILDER, strings.isArabic),
            strings = strings,
            onDismiss = { showHelp = false }
        )
    }

    if (showSkipDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showSkipDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val formatted = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }.format(Date(millis))
                            if (formatted !in skipDates) {
                                skipDates = (skipDates + formatted).sorted()
                            }
                        }
                        showSkipDatePicker = false
                    }
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSkipDatePicker = false }) {
                    Text(strings.cancel)
                }
            }
        ) {
            DatePicker(state = datePickerState)
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
