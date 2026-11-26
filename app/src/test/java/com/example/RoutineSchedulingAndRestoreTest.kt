package com.example

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.automation.AndroidRoutineAlarmScheduler
import com.example.automation.EndRestoreDecision
import com.example.automation.InMemoryRoutineStateBackupStore
import com.example.automation.NoOpRoutineAlarmScheduler
import com.example.automation.RoutineAlarmCalculator
import com.example.automation.RoutineAlarmReceiver
import com.example.automation.RoutineBootReceiver
import com.example.automation.RoutinePhoneController
import com.example.automation.SharedPreferencesRoutineStateBackupStore
import com.example.data.PreferencesManager
import com.example.data.local.AppDatabase
import com.example.models.ActionType
import com.example.models.AppLanguage
import com.example.models.DayFilter
import com.example.models.ExceptionType
import com.example.models.RoutineLogEventType
import com.example.models.RuleAction
import com.example.models.RuleException
import com.example.models.RuleTrigger
import com.example.models.SmartProfile
import com.example.models.SmartRoutine
import com.example.models.SoundMode
import com.example.models.TimeWindow
import com.example.models.TriggerType
import com.example.repositories.SmartCompanionRepository
import com.example.simulation.AndroidPhoneControlGateway
import com.example.simulation.PhoneControlGateway
import com.example.simulation.PhoneControlManager
import com.example.viewmodels.SmartCompanionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoutineSchedulingAndRestoreTest {

    private class FakePhoneControlGateway(
        var granted: Boolean = true,
        var ringerMode: SoundMode = SoundMode.SOUND,
        var interruptionFilter: Int = NotificationManager.INTERRUPTION_FILTER_ALL,
        var starredAllowed: Boolean = false,
        var exactAlarmsAllowed: Boolean = true
    ) : PhoneControlGateway {
        val actionLog = mutableListOf<String>()

        override fun isNotificationPolicyAccessGranted(): Boolean = granted
        override fun getCurrentRingerMode(): SoundMode = ringerMode
        override fun setRingerMode(mode: SoundMode): Boolean {
            if (!granted) return false
            actionLog.add("ringer:${mode.name}")
            ringerMode = mode
            return true
        }

        override fun isDndEnabled(): Boolean {
            return interruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL &&
                interruptionFilter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        }

        override fun isStarredContactsAllowedInDnd(): Boolean = starredAllowed

        override fun setDoNotDisturb(enabled: Boolean, allowStarredContacts: Boolean): Boolean {
            if (!granted) return false
            interruptionFilter = if (enabled) {
                NotificationManager.INTERRUPTION_FILTER_PRIORITY
            } else {
                NotificationManager.INTERRUPTION_FILTER_ALL
            }
            if (enabled && allowStarredContacts) {
                starredAllowed = true
            }
            actionLog.add("dnd:$enabled")
            return true
        }

        override fun getCurrentInterruptionFilter(): Int = interruptionFilter

        override fun setInterruptionFilter(filter: Int): Boolean {
            if (!granted) return false
            interruptionFilter = filter
            actionLog.add("filter:$filter")
            return true
        }

        override fun canScheduleExactAlarms(): Boolean = exactAlarmsAllowed

        override fun createDndAccessSettingsIntent(context: Context): Intent = Intent()
    }

    private fun buildRoutine(
        id: String = "routine_test_1",
        name: String = "روتين اختبار",
        isEnabled: Boolean = true,
        startHour: Int = 20,
        startMinute: Int = 0,
        endTime: String = "22:00",
        dayFilter: DayFilter = DayFilter.EVERY_DAY,
        customDays: List<DayFilter> = emptyList(),
        isOneTime: Boolean = false,
        skipDates: List<String> = emptyList(),
        extraTimeWindows: List<TimeWindow> = emptyList(),
        soundMode: SoundMode = SoundMode.SILENT,
        enableDoNotDisturb: Boolean = false,
        exceptions: List<RuleException> = emptyList()
    ): SmartRoutine {
        return SmartRoutine(
            id = id,
            name = name,
            description = "وصف",
            isEnabled = isEnabled,
            trigger = RuleTrigger(
                type = TriggerType.SPECIFIC_TIME,
                hour = startHour,
                minute = startMinute,
                dayFilter = dayFilter,
                summary = "$startHour:$startMinute"
            ),
            conditions = emptyList(),
            actions = listOf(
                RuleAction(ActionType.SET_SOUND_MODE, soundMode.name, soundMode.arabicName)
            ),
            exceptions = exceptions,
            priority = 7,
            scheduleStartTime = String.format(Locale.US, "%02d:%02d", startHour, startMinute),
            scheduleEndTime = endTime,
            targetProfileId = null,
            lastExecutionResult = "جاهز",
            lastExecutedAtMillis = null,
            executionCount = 0,
            skippedCount = 0,
            enableDoNotDisturb = enableDoNotDisturb,
            isOneTime = isOneTime,
            skipDates = skipDates,
            extraTimeWindows = extraTimeWindows,
            customDays = customDays
        )
    }

    private fun utcCalendar(
        year: Int,
        monthZeroBased: Int,
        dayOfMonth: Int,
        hour: Int,
        minute: Int
    ): Calendar {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    // ---- 1. Scheduling Logic Tests ----

    @Test
    fun `computeNextAlarms respects DayFilter WEEKDAYS and customDays`() {
        val tz = TimeZone.getTimeZone("UTC")
        // 2026-11-27 is a Friday
        val fridayMorning = utcCalendar(2026, Calendar.NOVEMBER, 27, 8, 0).timeInMillis

        val weekdaysRoutine = buildRoutine(
            id = "r_weekdays",
            startHour = 10,
            startMinute = 0,
            endTime = "12:00",
            dayFilter = DayFilter.WEEKDAYS
        )
        val weekdayEvents = RoutineAlarmCalculator.computeNextAlarms(weekdaysRoutine, fridayMorning, tz)
        assertEquals(2, weekdayEvents.size)
        val expectedFridayStart = utcCalendar(2026, Calendar.NOVEMBER, 27, 10, 0).timeInMillis
        val expectedFridayEnd = utcCalendar(2026, Calendar.NOVEMBER, 27, 12, 0).timeInMillis
        assertEquals(expectedFridayStart, weekdayEvents[0].triggerAtMillis)
        assertTrue(weekdayEvents[0].isStart)
        assertEquals(expectedFridayEnd, weekdayEvents[1].triggerAtMillis)
        assertFalse(weekdayEvents[1].isStart)

        // Friday afternoon after 12:00 -> next WEEKDAYS occurrence must jump over Sat/Sun to Monday Nov 30
        val fridayAfternoon = utcCalendar(2026, Calendar.NOVEMBER, 27, 14, 0).timeInMillis
        val mondayEvents = RoutineAlarmCalculator.computeNextAlarms(weekdaysRoutine, fridayAfternoon, tz)
        assertEquals(
            utcCalendar(2026, Calendar.NOVEMBER, 30, 10, 0).timeInMillis,
            mondayEvents[0].triggerAtMillis
        )

        // customDays = Monday & Thursday only -> from Friday afternoon, next is Monday Nov 30
        val customDaysRoutine = buildRoutine(
            id = "r_custom_days",
            startHour = 9,
            startMinute = 30,
            endTime = "11:00",
            customDays = listOf(DayFilter.MONDAY, DayFilter.THURSDAY)
        )
        val customEvents = RoutineAlarmCalculator.computeNextAlarms(customDaysRoutine, fridayAfternoon, tz)
        assertEquals(
            utcCalendar(2026, Calendar.NOVEMBER, 30, 9, 30).timeInMillis,
            customEvents[0].triggerAtMillis
        )
    }

    @Test
    fun `computeNextAlarms skips configured skipDates`() {
        val tz = TimeZone.getTimeZone("UTC")
        // 2026-11-27 08:00 UTC -> skip 2026-11-27 and 2026-11-28 -> next valid is 2026-11-29
        val now = utcCalendar(2026, Calendar.NOVEMBER, 27, 8, 0).timeInMillis
        val routine = buildRoutine(
            id = "r_skip",
            startHour = 10,
            startMinute = 0,
            endTime = "11:30",
            skipDates = listOf("2026-11-27", "2026-11-28")
        )

        val events = RoutineAlarmCalculator.computeNextAlarms(routine, now, tz)
        assertEquals(2, events.size)
        assertEquals(utcCalendar(2026, Calendar.NOVEMBER, 29, 10, 0).timeInMillis, events[0].triggerAtMillis)
        assertEquals(utcCalendar(2026, Calendar.NOVEMBER, 29, 11, 30).timeInMillis, events[1].triggerAtMillis)
    }

    @Test
    fun `computeNextAlarms schedules multiple time windows and one-time selects only earliest`() {
        val tz = TimeZone.getTimeZone("UTC")
        val now = utcCalendar(2026, Calendar.NOVEMBER, 27, 7, 0).timeInMillis

        val multiWindowRecurring = buildRoutine(
            id = "r_multi",
            startHour = 9,
            startMinute = 0,
            endTime = "11:00",
            extraTimeWindows = listOf(TimeWindow(15, 0, 17, 0)),
            isOneTime = false
        )
        val recurringEvents = RoutineAlarmCalculator.computeNextAlarms(multiWindowRecurring, now, tz)
        assertEquals(4, recurringEvents.size)
        assertEquals(0, recurringEvents[0].windowIndex)
        assertEquals(0, recurringEvents[1].windowIndex)
        assertEquals(1, recurringEvents[2].windowIndex)
        assertEquals(1, recurringEvents[3].windowIndex)
        assertEquals(utcCalendar(2026, Calendar.NOVEMBER, 27, 15, 0).timeInMillis, recurringEvents[2].triggerAtMillis)
        assertEquals(utcCalendar(2026, Calendar.NOVEMBER, 27, 17, 0).timeInMillis, recurringEvents[3].triggerAtMillis)

        // With isOneTime = true, only window 0's start & end are scheduled
        val oneTimeRoutine = multiWindowRecurring.copy(isOneTime = true)
        val oneTimeEvents = RoutineAlarmCalculator.computeNextAlarms(oneTimeRoutine, now, tz)
        assertEquals(2, oneTimeEvents.size)
        assertEquals(0, oneTimeEvents[0].windowIndex)
        assertTrue(oneTimeEvents[0].isStart)
        assertFalse(oneTimeEvents[1].isStart)
    }

    @Test
    fun `computeNextAlarms handles overnight windows crossing midnight and computeImmediateEndTimeMillis`() {
        val tz = TimeZone.getTimeZone("UTC")
        // Current time: 2026-11-28 01:00 AM UTC (inside yesterday's 23:00 -> 07:00 window!)
        val insideOvernight = utcCalendar(2026, Calendar.NOVEMBER, 28, 1, 0).timeInMillis
        val sleepRoutine = buildRoutine(
            id = "r_sleep",
            startHour = 23,
            startMinute = 0,
            endTime = "07:00"
        )

        val events = RoutineAlarmCalculator.computeNextAlarms(sleepRoutine, insideOvernight, tz)
        // First upcoming event should be END at 2026-11-28 07:00, second should be START at 2026-11-28 23:00
        assertEquals(2, events.size)
        assertFalse(events[0].isStart)
        assertEquals(utcCalendar(2026, Calendar.NOVEMBER, 28, 7, 0).timeInMillis, events[0].triggerAtMillis)
        assertTrue(events[1].isStart)
        assertEquals(utcCalendar(2026, Calendar.NOVEMBER, 28, 23, 0).timeInMillis, events[1].triggerAtMillis)

        // Immediate "Now" end time when end is 01:30 and now is 23:45 -> wraps to next day 01:30
        val lateNight = utcCalendar(2026, Calendar.NOVEMBER, 27, 23, 45).timeInMillis
        val immediateRoutine = buildRoutine(startHour = 23, startMinute = 45, endTime = "01:30")
        val endMillis = RoutineAlarmCalculator.computeImmediateEndTimeMillis(immediateRoutine, lateNight, tz)
        assertEquals(utcCalendar(2026, Calendar.NOVEMBER, 28, 1, 30).timeInMillis, endMillis)
    }

    // ---- 2. Restore-Previous-State Logic Tests ----

    @Test
    fun `applyRoutineStart saves previous ringer and DND filter, and applyRoutineEnd restores them exactly`() {
        val gateway = FakePhoneControlGateway(
            granted = true,
            ringerMode = SoundMode.VIBRATE,
            interruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL
        )
        val manager = PhoneControlManager(gateway)
        val backupStore = InMemoryRoutineStateBackupStore()
        val controller = RoutinePhoneController(manager, backupStore)

        val routine = buildRoutine(
            id = "r_silent_dnd",
            name = "اجتماع هادئ",
            soundMode = SoundMode.SILENT,
            enableDoNotDisturb = true,
            exceptions = listOf(RuleException(ExceptionType.EMERGENCY_CONTACTS, "Starred", "الطوارئ"))
        )

        val startOutcome = controller.applyRoutineStart(
            routine = routine,
            profiles = emptyList(),
            isArabic = true,
            timestampMillis = 1_000_000L
        )
        assertTrue(startOutcome.success)
        assertFalse(startOutcome.permissionRequired)
        assertEquals(RoutineLogEventType.STARTED, startOutcome.logEntry.eventType)
        assertEquals(SoundMode.SILENT, gateway.ringerMode)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_PRIORITY, gateway.interruptionFilter)
        assertTrue(gateway.starredAllowed)

        // Backup must preserve pre-routine VIBRATE + INTERRUPTION_FILTER_ALL
        val savedBackup = backupStore.getBackup()
        assertNotNull(savedBackup)
        assertEquals(SoundMode.VIBRATE, savedBackup!!.previousRingerMode)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_ALL, savedBackup.previousInterruptionFilter)

        // When routine ends, exact previous ringerMode + interruptionFilter are restored
        val endOutcome = controller.applyRoutineEnd(
            routineId = routine.id,
            routineName = routine.name,
            isArabic = true,
            timestampMillis = 2_000_000L
        )
        assertTrue(endOutcome.success)
        assertEquals(RoutineLogEventType.ENDED, endOutcome.logEntry.eventType)
        assertEquals(SoundMode.VIBRATE, gateway.ringerMode)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_ALL, gateway.interruptionFilter)
        assertNull(backupStore.getBackup())

        // Verify interruption filter was restored BEFORE ringer mode so AudioManager doesn't reject normal/vibrate
        val filterRestoreIdx = gateway.actionLog.indexOfLast { it.startsWith("filter:") }
        val ringerRestoreIdx = gateway.actionLog.indexOfLast { it.startsWith("ringer:") }
        assertTrue(filterRestoreIdx >= 0 && filterRestoreIdx < ringerRestoreIdx)
    }

    @Test
    fun `overlapping routines preserve original baseline until last active routine ends`() {
        val gateway = FakePhoneControlGateway(
            granted = true,
            ringerMode = SoundMode.SOUND,
            interruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL
        )
        val manager = PhoneControlManager(gateway)
        val backupStore = InMemoryRoutineStateBackupStore()
        val controller = RoutinePhoneController(manager, backupStore)

        val r1 = buildRoutine(id = "r1", name = "الأول", soundMode = SoundMode.VIBRATE, enableDoNotDisturb = false)
        val r2 = buildRoutine(id = "r2", name = "الثاني", soundMode = SoundMode.SILENT, enableDoNotDisturb = true)

        controller.applyRoutineStart(r1, emptyList(), isArabic = true, timestampMillis = 1000L)
        assertEquals(SoundMode.VIBRATE, gateway.ringerMode)

        controller.applyRoutineStart(r2, emptyList(), isArabic = true, timestampMillis = 2000L)
        assertEquals(SoundMode.SILENT, gateway.ringerMode)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_PRIORITY, gateway.interruptionFilter)

        // Ending r1 while r2 is still active must NOT restore SOUND yet
        val end1 = controller.applyRoutineEnd("r1", "الأول", isArabic = true, timestampMillis = 3000L)
        assertTrue(end1.success)
        assertEquals(SoundMode.SILENT, gateway.ringerMode)
        assertNotNull(backupStore.getBackup())

        // Ending r2 restores original pre-r1 SOUND + ALL filter
        val end2 = controller.applyRoutineEnd("r2", "الثاني", isArabic = true, timestampMillis = 4000L)
        assertTrue(end2.success)
        assertEquals(SoundMode.SOUND, gateway.ringerMode)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_ALL, gateway.interruptionFilter)
        assertNull(backupStore.getBackup())
    }

    @Test
    fun `SharedPreferencesRoutineStateBackupStore persists backup across store instances`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store1 = SharedPreferencesRoutineStateBackupStore(context)
        store1.clearAll()

        store1.onRoutineStarted(
            routineId = "r_persisted",
            currentRingerMode = SoundMode.SOUND,
            currentInterruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALARMS,
            timestampMillis = 123456L
        )
        store1.setDndPermissionMissing(true, "روتين الدراسة")

        val store2 = SharedPreferencesRoutineStateBackupStore(context)
        val loaded = store2.getBackup()
        assertNotNull(loaded)
        assertEquals(setOf("r_persisted"), loaded!!.activeRoutineIds)
        assertEquals(SoundMode.SOUND, loaded.previousRingerMode)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_ALARMS, loaded.previousInterruptionFilter)
        assertTrue(store2.isDndPermissionMissing())
        assertEquals("روتين الدراسة", store2.getDndMissingRoutineName())

        val decision = store2.onRoutineEnded("r_persisted")
        assertTrue(decision is EndRestoreDecision.Restore)
        val restore = decision as EndRestoreDecision.Restore
        assertEquals(SoundMode.SOUND, restore.previousRingerMode)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_ALARMS, restore.previousInterruptionFilter)
        assertNull(store2.getBackup())
    }

    @Test
    fun `missing DND access produces PERMISSION_DENIED log entry and flags backup store`() {
        val gateway = FakePhoneControlGateway(granted = false)
        val manager = PhoneControlManager(gateway)
        val backupStore = InMemoryRoutineStateBackupStore()
        val controller = RoutinePhoneController(manager, backupStore)

        val routine = buildRoutine(id = "r_no_perm", name = "صامت ليلي", soundMode = SoundMode.SILENT)
        val outcome = controller.applyRoutineStart(routine, emptyList(), isArabic = true, timestampMillis = 5000L)

        assertFalse(outcome.success)
        assertTrue(outcome.permissionRequired)
        assertEquals(RoutineLogEventType.PERMISSION_DENIED, outcome.logEntry.eventType)
        assertTrue(backupStore.isDndPermissionMissing())
        assertEquals("صامت ليلي", backupStore.getDndMissingRoutineName())
    }

    // ---- 3. ViewModel, Exact Alarm Scheduler, & BroadcastReceiver Integration Tests ----

    @Test
    fun `AndroidRoutineAlarmScheduler schedules exact alarms and reports denied SCHEDULE_EXACT_ALARM`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val shadowAm = Shadows.shadowOf(am)
        shadowAm.setCanScheduleExactAlarms(true)

        val scheduler = AndroidRoutineAlarmScheduler(context)
        val now = utcCalendar(2026, Calendar.NOVEMBER, 27, 8, 0).timeInMillis
        val routine = buildRoutine(id = "r_alarm", startHour = 21, startMinute = 0, endTime = "23:00")

        val res = scheduler.rescheduleAll(listOf(routine), nowMillis = now)
        assertFalse(res.exactAlarmPermissionDenied)
        assertEquals(2, res.scheduledCount)
        assertTrue(shadowAm.scheduledAlarms.size >= 2)

        // Cancel when routine is deleted
        scheduler.cancelRoutineAlarms(routine.id)
        val remaining = shadowAm.scheduledAlarms.filter { scheduled ->
            val intent = Shadows.shadowOf(scheduled.operation).savedIntent
            intent.getStringExtra(AndroidRoutineAlarmScheduler.EXTRA_ROUTINE_ID) == routine.id
        }
        assertTrue(remaining.isEmpty())

        // Simulate denied SCHEDULE_EXACT_ALARM permission
        shadowAm.setCanScheduleExactAlarms(false)
        val deniedRes = scheduler.rescheduleAll(listOf(routine), nowMillis = now)
        assertTrue(deniedRes.exactAlarmPermissionDenied)
        assertEquals(0, deniedRes.scheduledCount)
    }

    @Test
    fun `ViewModel Now execution applies real phone control, logs entry, and triggers DND alert when missing permission`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val prefs = PreferencesManager(context)
            prefs.setLanguage(AppLanguage.ARABIC)
            val repository = SmartCompanionRepository(db, prefs)

            val fakeGateway = FakePhoneControlGateway(
                granted = true,
                ringerMode = SoundMode.SOUND,
                interruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL,
                exactAlarmsAllowed = false
            )
            val phoneControlManager = PhoneControlManager(fakeGateway)
            val backupStore = InMemoryRoutineStateBackupStore()
            val fakeScheduler = NoOpRoutineAlarmScheduler().apply { exactAlarmAllowed = true }
            val missingNotifications = mutableListOf<Pair<String, Boolean>>()

            val viewModel = SmartCompanionViewModel(
                repository = repository,
                phoneControlManager = phoneControlManager,
                routineBackupStore = backupStore,
                alarmScheduler = fakeScheduler,
                onDndPermissionMissingNotification = { name, ar -> missingNotifications.add(name to ar) }
            )
            val job = launch(Dispatchers.Unconfined) { viewModel.uiState.collect {} }

            try {
                var wait = 0
                while (!viewModel.uiState.value.isReady && wait < 80) {
                    Shadows.shadowOf(Looper.getMainLooper()).idle()
                    delay(40)
                    wait++
                }
                assertTrue(viewModel.uiState.value.isReady)

                // Save with "Now" -> applies real mode immediately, schedules end alarm, records RoutineLogEntry
                val nowRoutine = buildRoutine(
                    id = "r_vm_now",
                    name = "تركيز الآن",
                    soundMode = SoundMode.SILENT,
                    enableDoNotDisturb = true,
                    endTime = "23:45"
                )
                viewModel.saveRoutine(nowRoutine, startImmediately = true)

                var logs = emptyList<com.example.models.RoutineLogEntry>()
                wait = 0
                while (wait < 80) {
                    Shadows.shadowOf(Looper.getMainLooper()).idle()
                    logs = repository.routineLogs.first()
                    if (logs.any { it.routineId == "r_vm_now" && it.eventType == RoutineLogEventType.STARTED }) break
                    delay(40)
                    wait++
                }
                assertTrue(logs.any { it.routineId == "r_vm_now" && it.eventType == RoutineLogEventType.STARTED })
                assertEquals(SoundMode.SILENT, fakeGateway.ringerMode)
                assertEquals(NotificationManager.INTERRUPTION_FILTER_PRIORITY, fakeGateway.interruptionFilter)
                assertTrue(fakeScheduler.immediateEndAlarms.any { it.first == "r_vm_now" })

                // Exact alarm denied -> sets exactAlarmDeniedNotice
                fakeScheduler.exactAlarmAllowed = false
                viewModel.deleteRoutine(nowRoutine)
                wait = 0
                while (wait < 60 && !viewModel.uiState.value.exactAlarmDeniedNotice) {
                    Shadows.shadowOf(Looper.getMainLooper()).idle()
                    delay(40)
                    wait++
                }
                assertTrue(fakeScheduler.cancelledRoutineIds.contains("r_vm_now"))
                assertTrue(viewModel.uiState.value.exactAlarmDeniedNotice)

                // Missing DND access -> sets dndPermissionAlertRoutine + invokes notification callback + logs PERMISSION_DENIED
                fakeGateway.granted = false
                fakeScheduler.exactAlarmAllowed = true
                val blockedRoutine = buildRoutine(id = "r_vm_blocked", name = "صامت محجوب", soundMode = SoundMode.SILENT)
                viewModel.saveRoutine(blockedRoutine, startImmediately = true)

                wait = 0
                while (wait < 80 && viewModel.uiState.value.dndPermissionAlertRoutine == null) {
                    Shadows.shadowOf(Looper.getMainLooper()).idle()
                    delay(40)
                    wait++
                }
                assertEquals("صامت محجوب", viewModel.uiState.value.dndPermissionAlertRoutine)
                assertTrue(missingNotifications.any { it.first == "صامت محجوب" })
                val updatedLogs = repository.routineLogs.first()
                assertTrue(updatedLogs.any { it.routineId == "r_vm_blocked" && it.eventType == RoutineLogEventType.PERMISSION_DENIED })
            } finally {
                job.cancel()
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun `RoutineAlarmReceiver applies mode on START, restores previous state on END, disables one-time routine, and alerts on missing DND`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val prefs = PreferencesManager(context)
        prefs.setLanguage(AppLanguage.ARABIC)
        val repository = SmartCompanionRepository(db, prefs)
        repository.ensureSeeded()

        val backupStore = SharedPreferencesRoutineStateBackupStore(context)
        backupStore.clearAll()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val shadowNm = Shadows.shadowOf(nm)
        val shadowAm = Shadows.shadowOf(context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
        shadowAm.setCanScheduleExactAlarms(true)

        // Start in SOUND + ALL filter with DND access granted
        shadowNm.setNotificationPolicyAccessGranted(true)
        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        am.ringerMode = AudioManager.RINGER_MODE_NORMAL

        val oneTimeRoutine = buildRoutine(
            id = "r_receiver_onetime",
            name = "موعد واحد",
            startHour = 14,
            startMinute = 0,
            endTime = "15:00",
            isOneTime = true,
            soundMode = SoundMode.VIBRATE,
            enableDoNotDisturb = true
        )
        repository.upsertRoutine(oneTimeRoutine)

        val receiver = RoutineAlarmReceiver()
        val tStart = utcCalendar(2026, Calendar.NOVEMBER, 27, 14, 0).timeInMillis
        receiver.handleAlarmEvent(
            context = context,
            routineId = oneTimeRoutine.id,
            eventType = AndroidRoutineAlarmScheduler.EVENT_START,
            nowMillis = tStart
        )

        assertEquals(AudioManager.RINGER_MODE_VIBRATE, am.ringerMode)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_PRIORITY, nm.currentInterruptionFilter)

        val tEnd = utcCalendar(2026, Calendar.NOVEMBER, 27, 15, 0).timeInMillis
        receiver.handleAlarmEvent(
            context = context,
            routineId = oneTimeRoutine.id,
            eventType = AndroidRoutineAlarmScheduler.EVENT_END,
            nowMillis = tEnd
        )

        // Restored back to normal sound & ALL filter, and one-time routine disabled
        assertEquals(AudioManager.RINGER_MODE_NORMAL, am.ringerMode)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_ALL, nm.currentInterruptionFilter)
        val afterEndRoutine = repository.getRoutineById(oneTimeRoutine.id)
        assertNotNull(afterEndRoutine)
        assertFalse(afterEndRoutine!!.isEnabled)

        val logs = repository.routineLogs.first().filter { it.routineId == oneTimeRoutine.id }
        assertTrue(logs.any { it.eventType == RoutineLogEventType.STARTED })
        assertTrue(logs.any { it.eventType == RoutineLogEventType.ENDED })

        // Now revoke DND access and fire START on another routine -> posts notification + PERMISSION_DENIED log
        shadowNm.setNotificationPolicyAccessGranted(false)
        val blockedRoutine = buildRoutine(
            id = "r_receiver_blocked",
            name = "روتين بدون إذن",
            startHour = 16,
            startMinute = 0,
            endTime = "17:00",
            soundMode = SoundMode.SILENT
        )
        repository.upsertRoutine(blockedRoutine)
        receiver.handleAlarmEvent(
            context = context,
            routineId = blockedRoutine.id,
            eventType = AndroidRoutineAlarmScheduler.EVENT_START,
            nowMillis = tEnd + 60_000L
        )
        assertTrue(backupStore.isDndPermissionMissing())
        assertTrue(shadowNm.allNotifications.isNotEmpty())
    }
}
