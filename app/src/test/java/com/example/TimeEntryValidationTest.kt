package com.example

import android.content.Context
import android.os.Looper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.DemoSeedData
import com.example.data.PreferencesManager
import com.example.data.local.AppDatabase
import com.example.models.ActionType
import com.example.models.DayFilter
import com.example.models.RuleAction
import com.example.models.RuleTrigger
import com.example.models.SmartRoutine
import com.example.models.SoundMode
import com.example.models.TriggerType
import com.example.repositories.SmartCompanionRepository
import com.example.utils.ClockTime
import com.example.utils.RoutineTimeInputValidator
import com.example.viewmodels.SmartCompanionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TimeEntryValidationTest {

    @Test
    fun `minute validation accepts 0 to 59 and rejects invalid minutes with clear Arabic error`() {
        val validZero = RoutineTimeInputValidator.validateClockInput(
            hourText = "3",
            minuteText = "0",
            is24Hour = false,
            isPm = true,
            isArabic = true
        )
        assertNull(validZero.errorMessage)
        assertEquals(ClockTime(15, 0), validZero.clockTime)

        val validFiftyNine = RoutineTimeInputValidator.validateClockInput(
            hourText = "3",
            minuteText = "59",
            is24Hour = false,
            isPm = true,
            isArabic = true
        )
        assertNull(validFiftyNine.errorMessage)
        assertEquals(ClockTime(15, 59), validFiftyNine.clockTime)

        val invalidSixty = RoutineTimeInputValidator.validateClockInput(
            hourText = "3",
            minuteText = "60",
            is24Hour = false,
            isPm = true,
            isArabic = true
        )
        assertNull(invalidSixty.clockTime)
        assertEquals("الدقائق يجب أن تكون بين 0 و 59", invalidSixty.errorMessage)

        val invalidEmpty = RoutineTimeInputValidator.validateClockInput(
            hourText = "3",
            minuteText = "",
            is24Hour = false,
            isPm = true,
            isArabic = true
        )
        assertNull(invalidEmpty.clockTime)
        assertEquals("الدقائق يجب أن تكون بين 0 و 59", invalidEmpty.errorMessage)
    }

    @Test
    fun `hour validation respects 12-hour vs 24-hour setting and converts AM PM accurately`() {
        val pmThreeForty = RoutineTimeInputValidator.validateClockInput(
            hourText = "3",
            minuteText = "40",
            is24Hour = false,
            isPm = true,
            isArabic = true
        )
        assertEquals(ClockTime(15, 40), pmThreeForty.clockTime)

        val midnight12Am = RoutineTimeInputValidator.validateClockInput(
            hourText = "12",
            minuteText = "15",
            is24Hour = false,
            isPm = false,
            isArabic = true
        )
        assertEquals(ClockTime(0, 15), midnight12Am.clockTime)

        val invalidZeroIn12h = RoutineTimeInputValidator.validateClockInput(
            hourText = "0",
            minuteText = "10",
            is24Hour = false,
            isPm = false,
            isArabic = true
        )
        assertNull(invalidZeroIn12h.clockTime)
        assertEquals("الساعة يجب أن تكون بين 1 و 12", invalidZeroIn12h.errorMessage)

        val validZeroIn24h = RoutineTimeInputValidator.validateClockInput(
            hourText = "0",
            minuteText = "10",
            is24Hour = true,
            isPm = false,
            isArabic = true
        )
        assertEquals(ClockTime(0, 10), validZeroIn24h.clockTime)

        val invalid24In24h = RoutineTimeInputValidator.validateClockInput(
            hourText = "24",
            minuteText = "00",
            is24Hour = true,
            isPm = false,
            isArabic = true
        )
        assertNull(invalid24In24h.clockTime)
        assertEquals("الساعة يجب أن تكون بين 0 و 23", invalid24In24h.errorMessage)
    }

    @Test
    fun `duration validation requires at least 1 minute and supports quick chips and custom minutes`() {
        assertEquals(listOf(15, 30, 60, 120), RoutineTimeInputValidator.QUICK_DURATION_OPTIONS)

        val validOne = RoutineTimeInputValidator.validateDurationMinutes("1", isArabic = true)
        assertNull(validOne.errorMessage)
        assertEquals(1, validOne.minutes)

        val validNinety = RoutineTimeInputValidator.validateDurationMinutes("90", isArabic = true)
        assertNull(validNinety.errorMessage)
        assertEquals(90, validNinety.minutes)

        val invalidZero = RoutineTimeInputValidator.validateDurationMinutes("0", isArabic = true)
        assertNull(invalidZero.minutes)
        assertEquals("المدة يجب أن تكون دقيقة واحدة على الأقل", invalidZero.errorMessage)

        val invalidBlank = RoutineTimeInputValidator.validateDurationMinutes("", isArabic = true)
        assertNull(invalidBlank.minutes)
        assertEquals("المدة يجب أن تكون دقيقة واحدة على الأقل", invalidBlank.errorMessage)
    }

    @Test
    fun `duration addition computes exact end time including midnight wraparound`() {
        val endAfternoon = RoutineTimeInputValidator.addMinutes(hour24 = 15, minute = 40, durationMinutes = 90)
        assertEquals(ClockTime(17, 10), endAfternoon)

        val endOvernight = RoutineTimeInputValidator.addMinutes(hour24 = 23, minute = 30, durationMinutes = 60)
        assertEquals(ClockTime(0, 30), endOvernight)
    }

    @Test
    fun `routine card range formats start and end clearly in Arabic and English`() {
        val routine = SmartRoutine(
            id = "r_test_range",
            name = "دراسة مركزة",
            description = "وقت الدراسة",
            isEnabled = true,
            trigger = RuleTrigger(
                type = TriggerType.SPECIFIC_TIME,
                hour = 15,
                minute = 40,
                dayFilter = DayFilter.EVERY_DAY,
                summary = ""
            ),
            conditions = emptyList(),
            actions = listOf(RuleAction(ActionType.SET_SOUND_MODE, SoundMode.SILENT.name, "صامت")),
            exceptions = emptyList(),
            priority = 6,
            scheduleStartTime = "3:40 م",
            scheduleEndTime = "5:10 م",
            targetProfileId = null,
            lastExecutionResult = "جاهز",
            lastExecutedAtMillis = null,
            executionCount = 0,
            skippedCount = 0
        )

        val arabicCardText = RoutineTimeInputValidator.formatRoutineRange(
            routine = routine,
            isArabic = true,
            is24Hour = false
        )
        assertEquals("من 3:40 م إلى 5:10 م", arabicCardText)

        val englishCardText = RoutineTimeInputValidator.formatRoutineRange(
            routine = routine,
            isArabic = false,
            is24Hour = false
        )
        assertEquals("From 3:40 PM to 5:10 PM", englishCardText)
    }

    @Test
    fun `Now selection fills current clock time, recalculates duration end time, and marks startImmediately`() {
        val fixedNow = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 40)
        }

        val nowWithDuration = RoutineTimeInputValidator.applyNowSelection(
            endModeIsDuration = true,
            durationMinutes = 90,
            calendar = fixedNow
        )

        assertEquals(ClockTime(15, 40), nowWithDuration.start)
        assertEquals(ClockTime(17, 10), nowWithDuration.end)
        assertTrue(nowWithDuration.startImmediately)

        val nowExactEnd = RoutineTimeInputValidator.applyNowSelection(
            endModeIsDuration = false,
            durationMinutes = 90,
            calendar = fixedNow
        )
        assertEquals(ClockTime(15, 40), nowExactEnd.start)
        assertNull(nowExactEnd.end)
        assertTrue(nowExactEnd.startImmediately)
    }

    @Test
    fun `saving a routine created with Now starts the routine immediately in ViewModel`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val prefs = PreferencesManager(context)
            val repository = SmartCompanionRepository(db, prefs)
            val viewModel = SmartCompanionViewModel(repository)
            val collectorJob = launch(Dispatchers.Unconfined) {
                viewModel.uiState.collect {}
            }

            try {
                // Wait for seed initialization to complete
                var attempts = 0
                while (!viewModel.uiState.value.isReady && attempts < 100) {
                    Shadows.shadowOf(Looper.getMainLooper()).idle()
                    delay(50)
                    attempts++
                }
                assertTrue("ViewModel should become ready", viewModel.uiState.value.isReady)

                val nowRoutine = SmartRoutine(
                    id = "routine_now_immediate",
                    name = "روتين فوري",
                    description = "يبدأ الآن",
                    isEnabled = true,
                    trigger = RuleTrigger(
                        type = TriggerType.SPECIFIC_TIME,
                        hour = 15,
                        minute = 40,
                        dayFilter = DayFilter.EVERY_DAY,
                        summary = "كل يوم • من 3:40 م إلى 5:10 م"
                    ),
                    conditions = emptyList(),
                    actions = listOf(
                        RuleAction(ActionType.ACTIVATE_PROFILE, "profile_study", "تفعيل وضع الدراسة"),
                        RuleAction(ActionType.SET_SOUND_MODE, SoundMode.SILENT.name, "صامت")
                    ),
                    exceptions = emptyList(),
                    priority = 8,
                    scheduleStartTime = "3:40 م",
                    scheduleEndTime = "5:10 م",
                    targetProfileId = "profile_study",
                    lastExecutionResult = "محفوظ",
                    lastExecutedAtMillis = null,
                    executionCount = 0,
                    skippedCount = 0
                )

                viewModel.saveRoutine(nowRoutine, startImmediately = true)

                var saved: SmartRoutine? = null
                var waitCount = 0
                while (waitCount < 60) {
                    Shadows.shadowOf(Looper.getMainLooper()).idle()
                    saved = repository.routines.first().firstOrNull { it.id == "routine_now_immediate" }
                    if (saved != null && saved.executionCount >= 1) break
                    delay(50)
                    waitCount++
                }

                assertNotNull(saved)
                assertEquals(1, saved!!.executionCount)
                assertEquals("من 3:40 م إلى 5:10 م", RoutineTimeInputValidator.formatRoutineRange(saved, isArabic = true))
                val latestTrace = viewModel.uiState.value.latestSimulationTrace
                assertNotNull(latestTrace)
                assertTrue(latestTrace!!.matchingRoutineNames.contains("روتين فوري"))
            } finally {
                collectorJob.cancel()
            }
        } finally {
            db.close()
        }
    }
}
