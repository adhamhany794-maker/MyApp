package com.example.automation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import com.example.models.SmartRoutine

data class ScheduleResult(
    val scheduledCount: Int = 0,
    val exactAlarmPermissionDenied: Boolean = false
)

interface RoutineAlarmSchedulerGateway {
    fun canScheduleExactAlarms(): Boolean = true
    fun rescheduleAll(
        routines: List<SmartRoutine>,
        nowMillis: Long = System.currentTimeMillis()
    ): ScheduleResult
    fun scheduleImmediateEndAlarm(routine: SmartRoutine, endAtMillis: Long): ScheduleResult
    fun cancelRoutineAlarms(routineId: String)
}

class NoOpRoutineAlarmScheduler : RoutineAlarmSchedulerGateway {
    val rescheduledSnapshots = mutableListOf<List<SmartRoutine>>()
    val immediateEndAlarms = mutableListOf<Pair<String, Long>>()
    val cancelledRoutineIds = mutableListOf<String>()
    var exactAlarmAllowed: Boolean = true

    override fun canScheduleExactAlarms(): Boolean = exactAlarmAllowed

    override fun rescheduleAll(routines: List<SmartRoutine>, nowMillis: Long): ScheduleResult {
        rescheduledSnapshots.add(routines)
        if (!exactAlarmAllowed) {
            return ScheduleResult(scheduledCount = 0, exactAlarmPermissionDenied = true)
        }
        val count = routines.sumOf { RoutineAlarmCalculator.computeNextAlarms(it, nowMillis).size }
        return ScheduleResult(scheduledCount = count, exactAlarmPermissionDenied = false)
    }

    override fun scheduleImmediateEndAlarm(routine: SmartRoutine, endAtMillis: Long): ScheduleResult {
        immediateEndAlarms.add(routine.id to endAtMillis)
        return if (!exactAlarmAllowed) {
            ScheduleResult(scheduledCount = 0, exactAlarmPermissionDenied = true)
        } else {
            ScheduleResult(scheduledCount = 1, exactAlarmPermissionDenied = false)
        }
    }

    override fun cancelRoutineAlarms(routineId: String) {
        cancelledRoutineIds.add(routineId)
    }
}

class AndroidRoutineAlarmScheduler(
    private val context: Context
) : RoutineAlarmSchedulerGateway {

    private val alarmManager: AlarmManager?
        get() = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    private val prefs: SharedPreferences =
        context.getSharedPreferences("routine_alarm_scheduler_prefs", Context.MODE_PRIVATE)

    override fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() == true
        } else {
            true
        }
    }

    override fun rescheduleAll(routines: List<SmartRoutine>, nowMillis: Long): ScheduleResult {
        val am = alarmManager ?: return ScheduleResult(0, exactAlarmPermissionDenied = false)

        // Cancel previously tracked recurring alarm intents
        val previousEntries = prefs.getStringSet(KEY_RECURRING_ENTRIES, emptySet())?.toSet() ?: emptySet()
        previousEntries.forEach { entry ->
            cancelPendingIntentByKey(am, entry)
        }

        // Also cancel immediate-end alarms for routines that no longer exist or are disabled
        val enabledIds = routines.filter { it.isEnabled }.map { it.id }.toSet()
        val prevImmediate = prefs.getStringSet(KEY_IMMEDIATE_ENTRIES, emptySet())?.toMutableSet() ?: mutableSetOf()
        val keptImmediate = mutableSetOf<String>()
        prevImmediate.forEach { entry ->
            val routineId = entry.substringBefore("|")
            if (routineId in enabledIds) {
                keptImmediate.add(entry)
            } else {
                cancelPendingIntentByKey(am, entry)
            }
        }
        prefs.edit().putStringSet(KEY_IMMEDIATE_ENTRIES, keptImmediate).commit()

        val allEvents = routines.flatMap { routine ->
            RoutineAlarmCalculator.computeNextAlarms(routine, nowMillis)
        }
        if (allEvents.isEmpty()) {
            prefs.edit().putStringSet(KEY_RECURRING_ENTRIES, emptySet()).commit()
            return ScheduleResult(scheduledCount = 0, exactAlarmPermissionDenied = false)
        }

        if (!canScheduleExactAlarms()) {
            prefs.edit().putStringSet(KEY_RECURRING_ENTRIES, emptySet()).commit()
            return ScheduleResult(scheduledCount = 0, exactAlarmPermissionDenied = true)
        }

        val scheduledKeys = mutableSetOf<String>()
        var denied = false

        allEvents.forEach { event ->
            val key = buildEntryKey(
                routineId = event.routineId,
                windowIndex = event.windowIndex,
                eventType = if (event.isStart) EVENT_START else EVENT_END
            )
            val pi = buildAlarmPendingIntent(
                routineId = event.routineId,
                windowIndex = event.windowIndex,
                eventType = if (event.isStart) EVENT_START else EVENT_END,
                isImmediateEnd = false,
                entryKey = key
            )
            val scheduled = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, event.triggerAtMillis, pi)
                } else {
                    am.setExact(AlarmManager.RTC_WAKEUP, event.triggerAtMillis, pi)
                }
                true
            } catch (_: SecurityException) {
                denied = true
                false
            }
            if (scheduled) {
                scheduledKeys.add(key)
            }
        }

        prefs.edit().putStringSet(KEY_RECURRING_ENTRIES, scheduledKeys).commit()
        return ScheduleResult(
            scheduledCount = scheduledKeys.size,
            exactAlarmPermissionDenied = denied
        )
    }

    override fun scheduleImmediateEndAlarm(routine: SmartRoutine, endAtMillis: Long): ScheduleResult {
        val am = alarmManager ?: return ScheduleResult(0, exactAlarmPermissionDenied = false)
        if (!canScheduleExactAlarms()) {
            return ScheduleResult(0, exactAlarmPermissionDenied = true)
        }

        val key = buildEntryKey(
            routineId = routine.id,
            windowIndex = IMMEDIATE_WINDOW_INDEX,
            eventType = EVENT_END
        )
        val pi = buildAlarmPendingIntent(
            routineId = routine.id,
            windowIndex = IMMEDIATE_WINDOW_INDEX,
            eventType = EVENT_END,
            isImmediateEnd = true,
            entryKey = key
        )
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAtMillis, pi)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, endAtMillis, pi)
            }
            val current = prefs.getStringSet(KEY_IMMEDIATE_ENTRIES, emptySet())?.toMutableSet() ?: mutableSetOf()
            current.add(key)
            prefs.edit().putStringSet(KEY_IMMEDIATE_ENTRIES, current).commit()
            ScheduleResult(scheduledCount = 1, exactAlarmPermissionDenied = false)
        } catch (_: SecurityException) {
            ScheduleResult(scheduledCount = 0, exactAlarmPermissionDenied = true)
        }
    }

    override fun cancelRoutineAlarms(routineId: String) {
        val am = alarmManager ?: return
        val recurring = prefs.getStringSet(KEY_RECURRING_ENTRIES, emptySet())?.toMutableSet() ?: mutableSetOf()
        val remainingRecurring = mutableSetOf<String>()
        recurring.forEach { entry ->
            if (entry.startsWith("$routineId|")) {
                cancelPendingIntentByKey(am, entry)
            } else {
                remainingRecurring.add(entry)
            }
        }

        val immediate = prefs.getStringSet(KEY_IMMEDIATE_ENTRIES, emptySet())?.toMutableSet() ?: mutableSetOf()
        val remainingImmediate = mutableSetOf<String>()
        immediate.forEach { entry ->
            if (entry.startsWith("$routineId|")) {
                cancelPendingIntentByKey(am, entry)
            } else {
                remainingImmediate.add(entry)
            }
        }

        prefs.edit()
            .putStringSet(KEY_RECURRING_ENTRIES, remainingRecurring)
            .putStringSet(KEY_IMMEDIATE_ENTRIES, remainingImmediate)
            .commit()
    }

    private fun cancelPendingIntentByKey(am: AlarmManager, entryKey: String) {
        val parts = entryKey.split("|")
        if (parts.size < 3) return
        val routineId = parts[0]
        val windowIndex = parts[1].toIntOrNull() ?: 0
        val eventType = parts[2]
        val intent = Intent(context, RoutineAlarmReceiver::class.java).apply {
            action = ACTION_ROUTINE_ALARM
            data = Uri.parse("smartcompanion://alarm/$routineId/$windowIndex/$eventType")
        }
        val pi = PendingIntent.getBroadcast(
            context,
            entryKey.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            am.cancel(pi)
            pi.cancel()
        }
    }

    private fun buildAlarmPendingIntent(
        routineId: String,
        windowIndex: Int,
        eventType: String,
        isImmediateEnd: Boolean,
        entryKey: String
    ): PendingIntent {
        val intent = Intent(context, RoutineAlarmReceiver::class.java).apply {
            action = ACTION_ROUTINE_ALARM
            data = Uri.parse("smartcompanion://alarm/$routineId/$windowIndex/$eventType")
            putExtra(EXTRA_ROUTINE_ID, routineId)
            putExtra(EXTRA_WINDOW_INDEX, windowIndex)
            putExtra(EXTRA_EVENT_TYPE, eventType)
            putExtra(EXTRA_IS_IMMEDIATE_END, isImmediateEnd)
        }
        return PendingIntent.getBroadcast(
            context,
            entryKey.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildEntryKey(routineId: String, windowIndex: Int, eventType: String): String {
        return "$routineId|$windowIndex|$eventType"
    }

    companion object {
        const val ACTION_ROUTINE_ALARM = "com.example.ACTION_ROUTINE_ALARM"
        const val EXTRA_ROUTINE_ID = "extra_routine_id"
        const val EXTRA_WINDOW_INDEX = "extra_window_index"
        const val EXTRA_EVENT_TYPE = "extra_event_type"
        const val EXTRA_IS_IMMEDIATE_END = "extra_is_immediate_end"
        const val EVENT_START = "START"
        const val EVENT_END = "END"

        private const val IMMEDIATE_WINDOW_INDEX = 999
        private const val KEY_RECURRING_ENTRIES = "recurring_alarm_entries"
        private const val KEY_IMMEDIATE_ENTRIES = "immediate_alarm_entries"
    }
}
