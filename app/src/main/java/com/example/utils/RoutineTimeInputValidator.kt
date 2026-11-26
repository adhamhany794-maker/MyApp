package com.example.utils

import com.example.models.SmartRoutine
import com.example.models.TriggerType
import java.util.Calendar
import java.util.Locale

data class ClockTime(
    val hour24: Int,
    val minute: Int
)

data class TimeValidationResult(
    val clockTime: ClockTime? = null,
    val errorMessage: String? = null
)

data class DurationValidationResult(
    val minutes: Int? = null,
    val errorMessage: String? = null
)

data class DisplayClockFields(
    val hourText: String,
    val minuteText: String,
    val isPm: Boolean
)

data class NowSelectionResult(
    val start: ClockTime,
    val end: ClockTime?,
    val startImmediately: Boolean
)

object RoutineTimeInputValidator {

    val QUICK_DURATION_OPTIONS = listOf(15, 30, 60, 120)

    fun validateClockInput(
        hourText: String,
        minuteText: String,
        is24Hour: Boolean,
        isPm: Boolean,
        isArabic: Boolean
    ): TimeValidationResult {
        val parsedHour = hourText.trim().toIntOrNull()
        if (parsedHour == null) {
            val msg = if (is24Hour) {
                if (isArabic) "الساعة يجب أن تكون بين 0 و 23" else "Hour must be between 0 and 23."
            } else {
                if (isArabic) "الساعة يجب أن تكون بين 1 و 12" else "Hour must be between 1 and 12."
            }
            return TimeValidationResult(errorMessage = msg)
        }

        if (is24Hour && parsedHour !in 0..23) {
            return TimeValidationResult(
                errorMessage = if (isArabic) "الساعة يجب أن تكون بين 0 و 23" else "Hour must be between 0 and 23."
            )
        }
        if (!is24Hour && parsedHour !in 1..12) {
            return TimeValidationResult(
                errorMessage = if (isArabic) "الساعة يجب أن تكون بين 1 و 12" else "Hour must be between 1 and 12."
            )
        }

        val parsedMinute = minuteText.trim().toIntOrNull()
        if (parsedMinute == null || parsedMinute !in 0..59) {
            return TimeValidationResult(
                errorMessage = if (isArabic) "الدقائق يجب أن تكون بين 0 و 59" else "Minutes must be between 0 and 59."
            )
        }

        val hour24 = if (is24Hour) {
            parsedHour
        } else {
            when {
                isPm && parsedHour < 12 -> parsedHour + 12
                !isPm && parsedHour == 12 -> 0
                else -> parsedHour
            }
        }
        return TimeValidationResult(clockTime = ClockTime(hour24 = hour24, minute = parsedMinute))
    }

    fun validateDurationMinutes(minutesText: String, isArabic: Boolean): DurationValidationResult {
        val parsed = minutesText.trim().toIntOrNull()
        if (parsed == null || parsed < 1) {
            return DurationValidationResult(
                errorMessage = if (isArabic) "المدة يجب أن تكون دقيقة واحدة على الأقل" else "Duration must be at least 1 minute."
            )
        }
        return DurationValidationResult(minutes = parsed)
    }

    fun addMinutes(hour24: Int, minute: Int, durationMinutes: Int): ClockTime {
        val safeStart = ((hour24 % 24) + 24) % 24 * 60 + minute.coerceIn(0, 59)
        val total = ((safeStart + durationMinutes) % 1440 + 1440) % 1440
        return ClockTime(hour24 = total / 60, minute = total % 60)
    }

    fun toDisplayFields(hour24: Int, minute: Int, is24Hour: Boolean): DisplayClockFields {
        val safeHour = ((hour24 % 24) + 24) % 24
        val safeMinute = minute.coerceIn(0, 59)
        val isPm = safeHour >= 12
        val displayHour = if (is24Hour) {
            safeHour
        } else {
            when {
                safeHour == 0 -> 12
                safeHour > 12 -> safeHour - 12
                else -> safeHour
            }
        }
        return DisplayClockFields(
            hourText = displayHour.toString(),
            minuteText = String.format(Locale.US, "%02d", safeMinute),
            isPm = isPm
        )
    }

    fun currentClockTime(calendar: Calendar = Calendar.getInstance()): ClockTime {
        return ClockTime(
            hour24 = calendar.get(Calendar.HOUR_OF_DAY),
            minute = calendar.get(Calendar.MINUTE)
        )
    }

    fun applyNowSelection(
        endModeIsDuration: Boolean,
        durationMinutes: Int?,
        calendar: Calendar = Calendar.getInstance()
    ): NowSelectionResult {
        val now = currentClockTime(calendar)
        val end = if (endModeIsDuration && durationMinutes != null && durationMinutes >= 1) {
            addMinutes(now.hour24, now.minute, durationMinutes)
        } else {
            null
        }
        return NowSelectionResult(
            start = now,
            end = end,
            startImmediately = true
        )
    }

    fun parseStoredClock(label: String): ClockTime? {
        val trimmed = label.trim()
        if (trimmed.isBlank()) return null

        val m24 = Regex("^(\\d{1,2}):(\\d{2})$").matchEntire(trimmed)
        if (m24 != null) {
            val h = m24.groupValues[1].toIntOrNull() ?: return null
            val min = m24.groupValues[2].toIntOrNull() ?: return null
            if (h in 0..23 && min in 0..59) return ClockTime(h, min)
        }

        val m12 = Regex("^(\\d{1,2}):(\\d{2})\\s*(AM|PM|ص|م)$", RegexOption.IGNORE_CASE).matchEntire(trimmed)
        if (m12 != null) {
            val rawH = m12.groupValues[1].toIntOrNull() ?: return null
            val min = m12.groupValues[2].toIntOrNull() ?: return null
            val period = m12.groupValues[3].uppercase(Locale.US)
            if (rawH !in 1..12 || min !in 0..59) return null
            val h24 = when {
                (period == "PM" || period == "م") && rawH < 12 -> rawH + 12
                (period == "AM" || period == "ص") && rawH == 12 -> 0
                else -> rawH
            }
            return ClockTime(h24, min)
        }
        return null
    }

    fun formatTimeForSetting(
        hour24: Int,
        minute: Int,
        is24Hour: Boolean,
        isArabic: Boolean
    ): String {
        val safeHour = ((hour24 % 24) + 24) % 24
        val safeMinute = minute.coerceIn(0, 59)
        return if (is24Hour) {
            String.format(Locale.US, "%02d:%02d", safeHour, safeMinute)
        } else {
            TimeUtils.formatTime12h(safeHour, safeMinute, isArabic)
        }
    }

    fun formatTimeRange(
        start: ClockTime,
        end: ClockTime,
        isArabic: Boolean,
        is24Hour: Boolean = false
    ): String {
        val startStr = formatTimeForSetting(start.hour24, start.minute, is24Hour, isArabic)
        val endStr = formatTimeForSetting(end.hour24, end.minute, is24Hour, isArabic)
        return if (isArabic) "من $startStr إلى $endStr" else "From $startStr to $endStr"
    }

    fun formatRoutineRange(
        routine: SmartRoutine,
        isArabic: Boolean,
        is24Hour: Boolean = false
    ): String? {
        val start = parseStoredClock(routine.scheduleStartTime) ?: if (
            routine.trigger.type in listOf(TriggerType.SPECIFIC_TIME, TriggerType.DATE_AND_TIME)
        ) {
            ClockTime(routine.trigger.hour, routine.trigger.minute)
        } else {
            null
        }
        val end = parseStoredClock(routine.scheduleEndTime)
        return when {
            start != null && end != null -> formatTimeRange(start, end, isArabic, is24Hour)
            start != null -> formatTimeForSetting(start.hour24, start.minute, is24Hour, isArabic)
            else -> null
        }
    }
}
