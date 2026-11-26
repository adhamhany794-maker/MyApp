package com.example.ui.components

import android.text.format.DateFormat
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.utils.AppStrings
import com.example.utils.RoutineTimeInputValidator
import com.example.utils.TimeUtils

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExactTimeEntrySection(
    label: String,
    hour24: Int,
    minute: Int,
    strings: AppStrings,
    testTagPrefix: String,
    onTimeChanged: (hour24: Int, minute: Int) -> Unit,
    onValidationErrorChanged: (String?) -> Unit = {},
    modifier: Modifier = Modifier,
    trailingAction: (@Composable () -> Unit)? = null
) {
    val context = LocalContext.current
    val is24Hour = remember(context) { DateFormat.is24HourFormat(context) }

    val initialFields = remember(is24Hour) {
        RoutineTimeInputValidator.toDisplayFields(hour24, minute, is24Hour)
    }
    var hourText by remember(is24Hour) { mutableStateOf(initialFields.hourText) }
    var minuteText by remember(is24Hour) { mutableStateOf(initialFields.minuteText) }
    var isPm by remember(is24Hour) { mutableStateOf(initialFields.isPm) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showClockDialog by remember { mutableStateOf(false) }

    // Keep text fields synchronized when external actions (Now button, duration chips, presets, clock dialog) change hour24/minute
    LaunchedEffect(hour24, minute, is24Hour) {
        val currentValidated = RoutineTimeInputValidator.validateClockInput(
            hourText = hourText,
            minuteText = minuteText,
            is24Hour = is24Hour,
            isPm = isPm,
            isArabic = strings.isArabic
        ).clockTime
        if (currentValidated == null || currentValidated.hour24 != hour24 || currentValidated.minute != minute) {
            val updated = RoutineTimeInputValidator.toDisplayFields(hour24, minute, is24Hour)
            hourText = updated.hourText
            minuteText = updated.minuteText
            isPm = updated.isPm
            errorMessage = null
            onValidationErrorChanged(null)
        }
    }

    fun validateAndEmit(newHourText: String, newMinuteText: String, newIsPm: Boolean) {
        val result = RoutineTimeInputValidator.validateClockInput(
            hourText = newHourText,
            minuteText = newMinuteText,
            is24Hour = is24Hour,
            isPm = newIsPm,
            isArabic = strings.isArabic
        )
        errorMessage = result.errorMessage
        onValidationErrorChanged(result.errorMessage)
        result.clockTime?.let { onTimeChanged(it.hour24, it.minute) }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = RoutineTimeInputValidator.formatTimeForSetting(
                        hour24 = hour24,
                        minute = minute,
                        is24Hour = is24Hour,
                        isArabic = strings.isArabic
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("${testTagPrefix}_formatted_time")
                )
            }
            if (trailingAction != null) {
                trailingAction()
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Keep numeric Hour : Minute left-to-right visually in RTL layouts
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = hourText,
                        onValueChange = { raw ->
                            val filtered = raw.filter { it.isDigit() }.take(2)
                            hourText = filtered
                            validateAndEmit(filtered, minuteText, isPm)
                        },
                        label = { Text(strings.hourFieldLabel) },
                        singleLine = true,
                        isError = errorMessage != null && (
                            hourText.toIntOrNull()?.let { if (is24Hour) it !in 0..23 else it !in 1..12 } ?: true
                            ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("${testTagPrefix}_hour_input")
                    )

                    Text(
                        text = ":",
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFamily),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = minuteText,
                        onValueChange = { raw ->
                            val filtered = raw.filter { it.isDigit() }.take(2)
                            minuteText = filtered
                            validateAndEmit(hourText, filtered, isPm)
                        },
                        label = { Text(strings.minuteFieldLabel) },
                        singleLine = true,
                        isError = errorMessage != null && (
                            minuteText.toIntOrNull()?.let { it !in 0..59 } ?: true
                            ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("${testTagPrefix}_minute_input")
                    )
                }
            }

            if (!is24Hour) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = !isPm,
                        onClick = {
                            isPm = false
                            validateAndEmit(hourText, minuteText, false)
                        },
                        label = { Text(strings.amLabel) },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("${testTagPrefix}_am_chip")
                    )
                    FilterChip(
                        selected = isPm,
                        onClick = {
                            isPm = true
                            validateAndEmit(hourText, minuteText, true)
                        },
                        label = { Text(strings.pmLabel) },
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("${testTagPrefix}_pm_chip")
                    )
                }
            }

            FilledTonalIconButton(
                onClick = { showClockDialog = true },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("${testTagPrefix}_clock_btn")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Schedule,
                    contentDescription = strings.pickClockTimeDesc
                )
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.testTag("${testTagPrefix}_error")
            )
        }
    }

    if (showClockDialog) {
        TimePickerModalDialog(
            title = label,
            initialHour24 = hour24,
            initialMinute = minute,
            is24Hour = is24Hour,
            strings = strings,
            onConfirm = { pickedHour, pickedMinute ->
                val updated = RoutineTimeInputValidator.toDisplayFields(pickedHour, pickedMinute, is24Hour)
                hourText = updated.hourText
                minuteText = updated.minuteText
                isPm = updated.isPm
                errorMessage = null
                onValidationErrorChanged(null)
                onTimeChanged(pickedHour, pickedMinute)
                showClockDialog = false
            },
            onDismiss = { showClockDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerModalDialog(
    title: String,
    initialHour24: Int,
    initialMinute: Int,
    is24Hour: Boolean,
    strings: AppStrings,
    onConfirm: (hour24: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val pickerState = rememberTimePickerState(
        initialHour = ((initialHour24 % 24) + 24) % 24,
        initialMinute = initialMinute.coerceIn(0, 59),
        is24Hour = is24Hour
    )
    var useDial by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                IconButton(
                    onClick = { useDial = !useDial },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (useDial) Icons.Rounded.Keyboard else Icons.Rounded.AccessTime,
                        contentDescription = if (useDial) {
                            strings.switchToKeyboardInputDesc
                        } else {
                            strings.switchToDialInputDesc
                        }
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (useDial) {
                    TimePicker(state = pickerState)
                } else {
                    TimeInput(state = pickerState)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(pickerState.hour, pickerState.minute) },
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text(strings.confirm)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text(strings.cancel)
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DurationMinutesSelector(
    label: String,
    selectedMinutes: Int,
    minutesText: String,
    strings: AppStrings,
    testTagPrefix: String,
    onMinutesSelected: (Int) -> Unit,
    onCustomTextChanged: (String) -> Unit,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$label: ${TimeUtils.formatDurationMinutes(selectedMinutes.coerceAtLeast(1), strings.isArabic)}",
            style = MaterialTheme.typography.labelLarge
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RoutineTimeInputValidator.QUICK_DURATION_OPTIONS.forEach { mins ->
                val isSelected = minutesText.trim().toIntOrNull() == mins
                FilterChip(
                    selected = isSelected,
                    onClick = { onMinutesSelected(mins) },
                    label = { Text(TimeUtils.formatDurationMinutes(mins, strings.isArabic)) },
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("${testTagPrefix}_chip_$mins")
                )
            }
        }
        OutlinedTextField(
            value = minutesText,
            onValueChange = { raw ->
                val filtered = raw.filter { it.isDigit() }.take(4)
                onCustomTextChanged(filtered)
            },
            label = { Text(strings.customDurationMinutesLabel) },
            suffix = { Text(strings.minutesUnitLabel) },
            singleLine = true,
            isError = errorMessage != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("${testTagPrefix}_custom_input")
        )
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.testTag("${testTagPrefix}_error")
            )
        }
    }
}
