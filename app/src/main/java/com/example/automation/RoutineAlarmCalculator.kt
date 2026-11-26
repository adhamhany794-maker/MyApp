package com.example.automation

import com.example.models.DayFilter
import com.example.models.SmartRoutine
import com.example.models.TimeWindow
import com.example.models.TriggerType
import com.example.utils.RoutineTimeInputValidator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class ScheduledAlarmEvent(
    val routineId: String,
    val windowIndex: Int,
    val isStart: Boolean,
    val triggerAtMillis: Long
)

object RoutineAlarmCalculator {

    fun allTimeWindows(routine: SmartRoutine): List<TimeWindow> {
        val startHour = routine.trigger.hour.coerceIn(0, 23)
        val startMinute = routine.trigger.minute.coerceIn(0, 59)
        val parsedEnd = RoutineTimeInputValidator.parseStoredClock(routine.scheduleEndTime)
        val fallbackEnd = RoutineTimeInputValidator.addMinutes(startHour, startMinute, 60)
        val primary = TimeWindow(
            startHour = startHour,
            startMinute = startMinute,
            endHour = parsedEnd?.hour24 ?: fallbackEnd.hour24,
            endMinute = parsedEnd?.minute ?: fallbackEnd.minute
        )
        return listOf(primary) + routine.extraTimeWindows
    }

    fun isTimeSchedulable(routine: SmartRoutine): Boolean {
        return routine.isEnabled && routine.trigger.type in listOf(
            TriggerType.SPECIFIC_TIME,
            TriggerType.DATE_AND_TIME
        )
    }

    fun isCurrentlyActive(
        routine: SmartRoutine,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Boolean {
        if (!routine.isEnabled || routine.trigger.type !in listOf(
                TriggerType.SPECIFIC_TIME,
                TriggerType.DATE_AND_TIME
            )
        ) {
            return false
        }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            this.timeZone = timeZone
        }
        val skipSet = routine.skipDates.toSet()
        val windows = allTimeWindows(routine)
        for (window in windows) {
            for (dayOffset in listOf(-1, 0)) {
                val startCal = calendarForDayOffset(nowMillis, dayOffset, window.startHour, window.startMinute, timeZone)
                if (!matchesRoutineDay(routine, startCal)) continue
                val dateStr = dateFormat.format(startCal.time)
                if (dateStr in skipSet) continue
                val startMillis = startCal.timeInMillis
                val endMillis = computeWindowEndMillis(startCal, window)
                if (nowMillis in startMillis until endMillis) {
                    return true
                }
            }
        }
        return false
    }

    fun computeNextAlarms(
        routine: SmartRoutine,
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): List<ScheduledAlarmEvent> {
        if (!isTimeSchedulable(routine)) return emptyList()

        val windows = allTimeWindows(routine)
        if (windows.isEmpty()) return emptyList()

        val events = mutableListOf<ScheduledAlarmEvent>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            this.timeZone = timeZone
        }
        val skipSet = routine.skipDates.toSet()

        if (routine.isOneTime) {
            // Find the single earliest active end (if inside a window) or next upcoming start + paired end
            var bestCandidate: Pair<Long, List<ScheduledAlarmEvent>>? = null
            windows.forEachIndexed { index, window ->
                val pair = findEarliestWindowOccurrence(
                    routine = routine,
                    windowIndex = index,
                    window = window,
                    nowMillis = nowMillis,
                    timeZone = timeZone,
                    dateFormat = dateFormat,
                    skipSet = skipSet
                )
                if (pair != null) {
                    val sortKey = pair.minOf { it.triggerAtMillis }
                    if (bestCandidate == null || sortKey < bestCandidate!!.first) {
                        bestCandidate = sortKey to pair
                    }
                }
            }
            return bestCandidate?.second ?: emptyList()
        }

        windows.forEachIndexed { index, window ->
            var nextStart: Long? = null
            var nextEnd: Long? = null

            for (dayOffset in -1..366) {
                val startCal = calendarForDayOffset(nowMillis, dayOffset, window.startHour, window.startMinute, timeZone)
                if (!matchesRoutineDay(routine, startCal)) continue
                val dateStr = dateFormat.format(startCal.time)
                if (dateStr in skipSet) continue

                val startMillis = startCal.timeInMillis
                val endMillis = computeWindowEndMillis(startCal, window)

                if (dayOffset == -1) {
                    // Yesterday's window: only schedule its END if it started <= now and ends in the future
                    if (startMillis <= nowMillis && endMillis > nowMillis && nextEnd == null) {
                        nextEnd = endMillis
                    }
                    continue
                }

                if (startMillis > nowMillis && nextStart == null) {
                    nextStart = startMillis
                }
                if (endMillis > nowMillis && nextEnd == null) {
                    nextEnd = endMillis
                }
                if (nextStart != null && nextEnd != null) break
            }

            nextStart?.let {
                events.add(ScheduledAlarmEvent(routine.id, index, isStart = true, triggerAtMillis = it))
            }
            nextEnd?.let {
                events.add(ScheduledAlarmEvent(routine.id, index, isStart = false, triggerAtMillis = it))
            }
        }

        return events.sortedBy { it.triggerAtMillis }
    }

    fun computeImmediateEndTimeMillis(
        routine: SmartRoutine,
        nowMillis: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val window = allTimeWindows(routine).first()
        val endCal = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, window.endHour.coerceIn(0, 23))
            set(Calendar.MINUTE, window.endMinute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (endCal.timeInMillis <= nowMillis) {
            endCal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return endCal.timeInMillis
    }

    fun matchesRoutineDay(routine: SmartRoutine, calendar: Calendar): Boolean {
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        if (routine.customDays.isNotEmpty()) {
            return routine.customDays.any { matchesDayFilter(it, dayOfWeek) }
        }
        return matchesDayFilter(routine.trigger.dayFilter, dayOfWeek)
    }

    fun matchesDayFilter(dayFilter: DayFilter, calendarDayOfWeek: Int): Boolean {
        return when (dayFilter) {
            DayFilter.EVERY_DAY -> true
            DayFilter.WEEKDAYS -> calendarDayOfWeek in listOf(
                Calendar.MONDAY,
                Calendar.TUESDAY,
                Calendar.WEDNESDAY,
                Calendar.THURSDAY,
                Calendar.FRIDAY
            )
            DayFilter.WEEKENDS -> calendarDayOfWeek in listOf(
                Calendar.SATURDAY,
                Calendar.SUNDAY
            )
            DayFilter.MONDAY -> calendarDayOfWeek == Calendar.MONDAY
            DayFilter.TUESDAY -> calendarDayOfWeek == Calendar.TUESDAY
            DayFilter.WEDNESDAY -> calendarDayOfWeek == Calendar.WEDNESDAY
            DayFilter.THURSDAY -> calendarDayOfWeek == Calendar.THURSDAY
            DayFilter.FRIDAY -> calendarDayOfWeek == Calendar.FRIDAY
            DayFilter.SATURDAY -> calendarDayOfWeek == Calendar.SATURDAY
            DayFilter.SUNDAY -> calendarDayOfWeek == Calendar.SUNDAY
        }
    }

    private fun findEarliestWindowOccurrence(
        routine: SmartRoutine,
        windowIndex: Int,
        window: TimeWindow,
        nowMillis: Long,
        timeZone: TimeZone,
        dateFormat: SimpleDateFormat,
        skipSet: Set<String>
    ): List<ScheduledAlarmEvent>? {
        for (dayOffset in -1..366) {
            val startCal = calendarForDayOffset(nowMillis, dayOffset, window.startHour, window.startMinute, timeZone)
            if (!matchesRoutineDay(routine, startCal)) continue
            if (dateFormat.format(startCal.time) in skipSet) continue

            val startMillis = startCal.timeInMillis
            val endMillis = computeWindowEndMillis(startCal, window)

            if (dayOffset == -1) {
                if (startMillis <= nowMillis && endMillis > nowMillis) {
                    return listOf(
                        ScheduledAlarmEvent(routine.id, windowIndex, isStart = false, triggerAtMillis = endMillis)
                    )
                }
                continue
            }

            if (startMillis > nowMillis) {
                return listOf(
                    ScheduledAlarmEvent(routine.id, windowIndex, isStart = true, triggerAtMillis = startMillis),
                    ScheduledAlarmEvent(routine.id, windowIndex, isStart = false, triggerAtMillis = endMillis)
                )
            }
        }
        return null
    }

    private fun calendarForDayOffset(
        baseMillis: Long,
        dayOffset: Int,
        hour: Int,
        minute: Int,
        timeZone: TimeZone
    ): Calendar {
        return Calendar.getInstance(timeZone).apply {
            timeInMillis = baseMillis
            add(Calendar.DAY_OF_YEAR, dayOffset)
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun computeWindowEndMillis(startCal: Calendar, window: TimeWindow): Long {
        val endCal = (startCal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, window.endHour.coerceIn(0, 23))
            set(Calendar.MINUTE, window.endMinute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTotal = window.startHour * 60 + window.startMinute
        val endTotal = window.endHour * 60 + window.endMinute
        if (endTotal <= startTotal) {
            endCal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return endCal.timeInMillis
    }
}
