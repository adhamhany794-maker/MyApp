package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.models.ActionType
import com.example.models.ConditionType
import com.example.models.DayFilter
import com.example.models.ExceptionType
import com.example.models.SimulatedActivity
import com.example.models.SimulatedLocation
import com.example.models.SmartProfile
import com.example.models.SoundMode
import com.example.models.TimeWindow
import com.example.models.TriggerType
import com.example.ui.components.CompanionCard
import com.example.ui.components.DurationMinutesSelector
import com.example.ui.components.ExactTimeEntrySection
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StatusEmerald
import com.example.utils.AppStrings
import com.example.utils.ClockTime
import com.example.utils.RoutineTimeInputValidator

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BuilderWhenSheetCard(
    strings: AppStrings,
    is24Hour: Boolean,
    triggerType: TriggerType,
    onTriggerTypeChange: (TriggerType) -> Unit,
    dayFilter: DayFilter,
    onDayFilterChange: (DayFilter) -> Unit,
    customDays: Set<DayFilter>,
    onCustomDaysChange: (Set<DayFilter>) -> Unit,
    triggerHour: Int,
    triggerMinute: Int,
    onStartTimeChanged: (Int, Int) -> Unit,
    onStartValidationError: (String?) -> Unit,
    onNowClicked: () -> Unit,
    startImmediately: Boolean,
    endByDuration: Boolean,
    onEndModeExact: () -> Unit,
    onEndModeDuration: () -> Unit,
    endHour: Int,
    endMinute: Int,
    onEndTimeChanged: (Int, Int) -> Unit,
    onEndValidationError: (String?) -> Unit,
    selectedDurationMinutes: Int,
    customDurationText: String,
    endTimeError: String?,
    onDurationMinutesSelected: (Int) -> Unit,
    onDurationCustomTextChanged: (String) -> Unit,
    chosenTimeRangeText: String,
    extraTimeWindows: List<TimeWindow>,
    onExtraTimeWindowsChange: (List<TimeWindow>) -> Unit,
    onAddExtraWindow: () -> Unit,
    isOneTime: Boolean,
    onIsOneTimeChange: (Boolean) -> Unit,
    skipDates: List<String>,
    onRemoveSkipDate: (String) -> Unit,
    onAddSkipDateClick: () -> Unit,
    location: SimulatedLocation,
    onLocationChange: (SimulatedLocation) -> Unit,
    activity: SimulatedActivity,
    onActivityChange: (SimulatedActivity) -> Unit
) {
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
                    onClick = { onTriggerTypeChange(t) },
                    label = { Text(strings.triggerTypeLabel(t)) },
                    modifier = Modifier.testTag("trigger_${t.name}")
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        if (triggerType in listOf(TriggerType.SPECIFIC_TIME, TriggerType.DATE_AND_TIME, TriggerType.SPECIFIC_DAYS)) {
            DayFilterSelectorSection(
                strings = strings,
                dayFilter = dayFilter,
                customDays = customDays,
                onSelectPreset = { df ->
                    onCustomDaysChange(emptySet())
                    onDayFilterChange(df)
                },
                onToggleDay = { day ->
                    val updated = if (day in customDays) customDays - day else customDays + day
                    onCustomDaysChange(updated)
                    if (updated.size == 1) {
                        onDayFilterChange(updated.first())
                    }
                }
            )
            if (triggerType != TriggerType.SPECIFIC_DAYS) {
                Spacer(Modifier.height(14.dp))
                ExactTimeEntrySection(
                    label = strings.startTimeLabel,
                    hour24 = triggerHour,
                    minute = triggerMinute,
                    strings = strings,
                    testTagPrefix = "start_time",
                    onTimeChanged = onStartTimeChanged,
                    onValidationErrorChanged = onStartValidationError,
                    trailingAction = {
                        FilledTonalButton(
                            onClick = onNowClicked,
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
                        onClick = onEndModeExact,
                        label = { Text(strings.endAtExactTimeLabel) },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("end_mode_exact")
                    )
                    FilterChip(
                        selected = endByDuration,
                        onClick = onEndModeDuration,
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
                        onTimeChanged = onEndTimeChanged,
                        onValidationErrorChanged = onEndValidationError
                    )
                } else {
                    DurationMinutesSelector(
                        label = strings.endAfterDurationLabel,
                        selectedMinutes = selectedDurationMinutes,
                        minutesText = customDurationText,
                        strings = strings,
                        testTagPrefix = "end_duration",
                        onMinutesSelected = onDurationMinutesSelected,
                        onCustomTextChanged = onDurationCustomTextChanged,
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

                ExtraWindowsAndScheduleOptionsSection(
                    strings = strings,
                    is24Hour = is24Hour,
                    extraTimeWindows = extraTimeWindows,
                    onExtraTimeWindowsChange = onExtraTimeWindowsChange,
                    onAddExtraWindow = onAddExtraWindow,
                    isOneTime = isOneTime,
                    onIsOneTimeChange = onIsOneTimeChange,
                    skipDates = skipDates,
                    onRemoveSkipDate = onRemoveSkipDate,
                    onAddSkipDateClick = onAddSkipDateClick
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
                        onClick = { onLocationChange(loc) },
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
                        onClick = { onActivityChange(act) },
                        label = { Text(strings.activityLabel(act)) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayFilterSelectorSection(
    strings: AppStrings,
    dayFilter: DayFilter,
    customDays: Set<DayFilter>,
    onSelectPreset: (DayFilter) -> Unit,
    onToggleDay: (DayFilter) -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listOf(DayFilter.EVERY_DAY, DayFilter.WEEKDAYS, DayFilter.WEEKENDS).forEach { df ->
            FilterChip(
                selected = customDays.isEmpty() && dayFilter == df,
                onClick = { onSelectPreset(df) },
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
                onClick = { onToggleDay(day) },
                label = { Text(strings.dayFilterLabel(day, short = true)) },
                modifier = Modifier.testTag("custom_day_${day.name}")
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExtraWindowsAndScheduleOptionsSection(
    strings: AppStrings,
    is24Hour: Boolean,
    extraTimeWindows: List<TimeWindow>,
    onExtraTimeWindowsChange: (List<TimeWindow>) -> Unit,
    onAddExtraWindow: () -> Unit,
    isOneTime: Boolean,
    onIsOneTimeChange: (Boolean) -> Unit,
    skipDates: List<String>,
    onRemoveSkipDate: (String) -> Unit,
    onAddSkipDateClick: () -> Unit
) {
    Text(text = strings.extraTimeWindowsLabel, style = MaterialTheme.typography.labelLarge)
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
                            onExtraTimeWindowsChange(extraTimeWindows.filterIndexed { i, _ -> i != index })
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
                        onExtraTimeWindowsChange(
                            extraTimeWindows.mapIndexed { i, old ->
                                if (i == index) old.copy(startHour = newH, startMinute = newM) else old
                            }
                        )
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
                        onExtraTimeWindowsChange(
                            extraTimeWindows.mapIndexed { i, old ->
                                if (i == index) old.copy(endHour = newH, endMinute = newM) else old
                            }
                        )
                    }
                )
            }
        }
    }
    OutlinedButton(
        onClick = onAddExtraWindow,
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
            onCheckedChange = onIsOneTimeChange,
            modifier = Modifier.testTag("builder_one_time_switch")
        )
    }

    Spacer(Modifier.height(12.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Spacer(Modifier.height(12.dp))

    Text(text = strings.skipDatesLabel, style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(6.dp))
    if (skipDates.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            skipDates.forEach { dateStr ->
                InputChip(
                    selected = true,
                    onClick = { onRemoveSkipDate(dateStr) },
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
        onClick = onAddSkipDateClick,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BuilderOnlyIfSheetCard(
    strings: AppStrings,
    selectedConditions: Set<ConditionType>,
    onToggleCondition: (ConditionType) -> Unit
) {
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
                FilterChip(
                    selected = cond in selectedConditions,
                    onClick = { onToggleCondition(cond) },
                    label = { Text(strings.conditionTypeLabel(cond)) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BuilderDoThisSheetCard(
    strings: AppStrings,
    profiles: List<SmartProfile>,
    selectedActionTypes: Set<ActionType>,
    onToggleActionType: (ActionType) -> Unit,
    targetProfileId: String,
    onTargetProfileChange: (String) -> Unit,
    targetSoundMode: SoundMode,
    onTargetSoundModeChange: (SoundMode) -> Unit,
    focusDuration: Int,
    focusDurationText: String,
    focusDurationError: String?,
    onFocusMinutesSelected: (Int) -> Unit,
    onFocusCustomTextChanged: (String) -> Unit,
    enableDoNotDisturb: Boolean,
    onEnableDoNotDisturbChange: (Boolean) -> Unit
) {
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
                FilterChip(
                    selected = act in selectedActionTypes,
                    onClick = { onToggleActionType(act) },
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
                profiles.forEach { p ->
                    FilterChip(
                        selected = targetProfileId == p.id,
                        onClick = { onTargetProfileChange(p.id) },
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
                        onClick = { onTargetSoundModeChange(sm) },
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
                onMinutesSelected = onFocusMinutesSelected,
                onCustomTextChanged = onFocusCustomTextChanged,
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
                onCheckedChange = onEnableDoNotDisturbChange,
                modifier = Modifier.testTag("builder_enable_dnd_switch")
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BuilderExceptSheetCard(
    strings: AppStrings,
    selectedExceptions: Set<ExceptionType>,
    onToggleException: (ExceptionType) -> Unit
) {
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
                FilterChip(
                    selected = exc in selectedExceptions,
                    onClick = { onToggleException(exc) },
                    label = { Text(strings.exceptionTypeLabel(exc)) }
                )
            }
        }
    }
}
