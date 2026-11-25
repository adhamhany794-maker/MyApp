package com.example

import com.example.assistant.AssistantStateSnapshot
import com.example.assistant.CompanionAssistantEngine
import com.example.assistant.AssistantReply
import com.example.assistant.GeminiAssistantEngine
import com.example.assistant.LocalDemoAssistantEngine
import com.example.assistant.RoutineConflictChecker
import com.example.assistant.RoutineJsonValidationResult
import com.example.assistant.RoutineJsonValidator
import com.example.data.DemoSeedData
import com.example.models.ActionType
import com.example.models.AppLanguage
import com.example.models.ConditionType
import com.example.models.DayFilter
import com.example.models.ExceptionType
import com.example.models.SimulatedLocation
import com.example.models.SoundMode
import com.example.models.TriggerType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoutineJsonValidatorTest {

    private val profiles = DemoSeedData.defaultProfiles()
    private val routines = DemoSeedData.defaultRoutines()
    private val contacts = DemoSeedData.defaultContacts()

    private fun baseSnapshot(language: AppLanguage = AppLanguage.ENGLISH) = AssistantStateSnapshot(
        language = language,
        activeProfile = profiles.first { it.id == "profile_study" },
        soundMode = SoundMode.SILENT,
        isAutomationPaused = false,
        routines = routines,
        contacts = contacts,
        notificationStats = DemoSeedData.defaultNotificationStats(),
        focusSessions = DemoSeedData.defaultFocusSessions(),
        recentEvents = DemoSeedData.defaultActivityEvents()
    )

    @Test
    fun `valid time_range JSON builds routine proposal and enforces emergency contacts even if omitted`() {
        val rawJson = """
            {
              "status": "ok",
              "followUpQuestion": null,
              "name": "Evening Study Silence",
              "trigger": {
                "type": "time_range",
                "startTime": "20:00",
                "endTime": "22:00",
                "days": ["WEEKDAYS"]
              },
              "conditions": ["AUTOMATION_ENABLED", "DAY_OF_WEEK"],
              "action": {
                "mode": "silent"
              },
              "exceptions": []
            }
        """.trimIndent()

        val result = RoutineJsonValidator.parseAndValidate(
            rawJson = rawJson,
            originalPrompt = "silence my phone while I study 8 to 10",
            existingRoutines = emptyList(),
            isArabic = false
        )

        assertTrue(result is RoutineJsonValidationResult.Valid)
        val valid = result as RoutineJsonValidationResult.Valid
        val routine = valid.proposal.routine

        assertEquals("Evening Study Silence", routine.name)
        assertEquals(TriggerType.SPECIFIC_TIME, routine.trigger.type)
        assertEquals(20, routine.trigger.hour)
        assertEquals(0, routine.trigger.minute)
        assertEquals(DayFilter.WEEKDAYS, routine.trigger.dayFilter)
        assertEquals("8:00 PM", routine.scheduleStartTime)
        assertEquals("10:00 PM", valid.proposal.endTimeLabel)
        assertTrue(routine.actions.any { it.type == ActionType.SET_SOUND_MODE && it.targetValue == SoundMode.SILENT.name })
        assertTrue(routine.conditions.any { it.type == ConditionType.DAY_OF_WEEK })
        assertTrue(routine.exceptions.any { it.type == ExceptionType.EMERGENCY_CONTACTS })
        assertNull(valid.proposal.conflictWarning)
    }

    @Test
    fun `valid Arabic place_enter and focus JSON sets location trigger and focus action`() {
        val rawJson = """
            {
              "status": "ok",
              "name": "تركيز المكتبة",
              "trigger": {
                "type": "place_enter",
                "place": "LIBRARY",
                "days": ["EVERY_DAY"]
              },
              "conditions": ["SIMULATED_LOCATION"],
              "action": {
                "mode": "focus",
                "focusMinutes": 50
              },
              "exceptions": ["PRIORITY_CONTACTS"]
            }
        """.trimIndent()

        val result = RoutineJsonValidator.parseAndValidate(
            rawJson = rawJson,
            originalPrompt = "فعّل وضع التركيز 50 دقيقة عند دخول المكتبة",
            existingRoutines = emptyList(),
            isArabic = true
        )

        assertTrue(result is RoutineJsonValidationResult.Valid)
        val valid = result as RoutineJsonValidationResult.Valid
        val routine = valid.proposal.routine

        assertEquals(TriggerType.LOCATION_ENTERED, routine.trigger.type)
        assertEquals(SimulatedLocation.LIBRARY, routine.trigger.location)
        assertTrue(routine.actions.any { it.type == ActionType.START_FOCUS_SESSION && it.targetValue == "50" })
        assertTrue(routine.exceptions.any { it.type == ExceptionType.EMERGENCY_CONTACTS })
        assertTrue(routine.exceptions.any { it.type == ExceptionType.PRIORITY_CONTACTS })
        assertTrue(valid.proposal.exceptionSummary.contains("الطوارئ"))
    }

    @Test
    fun `valid days and place_exit vibrate JSON map properly`() {
        val daysJson = """
            {
              "status": "ok",
              "name": "Weekend Quiet",
              "trigger": {
                "type": "days",
                "days": ["SATURDAY", "SUNDAY"]
              },
              "conditions": [],
              "action": { "mode": "vibrate" },
              "exceptions": ["EMERGENCY_CONTACTS"]
            }
        """.trimIndent()
        val daysRes = RoutineJsonValidator.parseAndValidate(daysJson, "vibrate on weekends", emptyList(), false)
        assertTrue(daysRes is RoutineJsonValidationResult.Valid)
        val daysRoutine = (daysRes as RoutineJsonValidationResult.Valid).proposal.routine
        assertEquals(TriggerType.SPECIFIC_DAYS, daysRoutine.trigger.type)
        assertEquals(DayFilter.WEEKENDS, daysRoutine.trigger.dayFilter)
        assertTrue(daysRoutine.actions.any { it.type == ActionType.SET_SOUND_MODE && it.targetValue == SoundMode.VIBRATE.name })

        val exitJson = """
            {
              "status": "ok",
              "name": "Restore Sound Leaving Work",
              "trigger": {
                "type": "place_exit",
                "place": "WORKPLACE",
                "days": ["WEEKDAYS"]
              },
              "conditions": ["AUTOMATION_ENABLED"],
              "action": { "mode": "sound" },
              "exceptions": ["EMERGENCY_CONTACTS"]
            }
        """.trimIndent()
        val exitRes = RoutineJsonValidator.parseAndValidate(exitJson, "sound when I leave work", emptyList(), false)
        assertTrue(exitRes is RoutineJsonValidationResult.Valid)
        val exitRoutine = (exitRes as RoutineJsonValidationResult.Valid).proposal.routine
        assertEquals(TriggerType.LOCATION_EXITED, exitRoutine.trigger.type)
        assertEquals(SimulatedLocation.WORKPLACE, exitRoutine.trigger.location)
        assertTrue(exitRoutine.actions.any { it.type == ActionType.SET_SOUND_MODE && it.targetValue == SoundMode.SOUND.name })
    }

    @Test
    fun `malformed or missing fields return NeedsClarification with follow-up question instead of guessing`() {
        val malformed = RoutineJsonValidator.parseAndValidate("not json at all", "hello", emptyList(), false)
        assertTrue(malformed is RoutineJsonValidationResult.NeedsClarification)

        val missingName = """
            {
              "status": "ok",
              "name": "",
              "trigger": { "type": "time_range", "startTime": "20:00", "endTime": "22:00", "days": ["EVERY_DAY"] },
              "conditions": [],
              "action": { "mode": "silent" },
              "exceptions": ["EMERGENCY_CONTACTS"]
            }
        """.trimIndent()
        assertTrue(RoutineJsonValidator.parseAndValidate(missingName, "silent 8 to 10", emptyList(), false) is RoutineJsonValidationResult.NeedsClarification)

        val invalidTime = """
            {
              "status": "ok",
              "name": "Study",
              "trigger": { "type": "time_range", "startTime": "25:99", "endTime": "22:00", "days": ["EVERY_DAY"] },
              "conditions": [],
              "action": { "mode": "silent" },
              "exceptions": ["EMERGENCY_CONTACTS"]
            }
        """.trimIndent()
        assertTrue(RoutineJsonValidator.parseAndValidate(invalidTime, "study", emptyList(), false) is RoutineJsonValidationResult.NeedsClarification)

        val equalStartEnd = """
            {
              "status": "ok",
              "name": "Study",
              "trigger": { "type": "time_range", "startTime": "20:00", "endTime": "20:00", "days": ["EVERY_DAY"] },
              "conditions": [],
              "action": { "mode": "silent" },
              "exceptions": ["EMERGENCY_CONTACTS"]
            }
        """.trimIndent()
        assertTrue(RoutineJsonValidator.parseAndValidate(equalStartEnd, "study", emptyList(), false) is RoutineJsonValidationResult.NeedsClarification)

        val missingPlace = """
            {
              "status": "ok",
              "name": "Cafe Silent",
              "trigger": { "type": "place_enter", "place": "COFFEE_SHOP", "days": ["EVERY_DAY"] },
              "conditions": [],
              "action": { "mode": "silent" },
              "exceptions": ["EMERGENCY_CONTACTS"]
            }
        """.trimIndent()
        assertTrue(RoutineJsonValidator.parseAndValidate(missingPlace, "silent at cafe", emptyList(), false) is RoutineJsonValidationResult.NeedsClarification)

        val invalidAction = """
            {
              "status": "ok",
              "name": "Bad Action",
              "trigger": { "type": "time_range", "startTime": "20:00", "endTime": "22:00", "days": ["EVERY_DAY"] },
              "conditions": [],
              "action": { "mode": "send_sms" },
              "exceptions": ["EMERGENCY_CONTACTS"]
            }
        """.trimIndent()
        assertTrue(RoutineJsonValidator.parseAndValidate(invalidAction, "send sms at 8", emptyList(), false) is RoutineJsonValidationResult.NeedsClarification)

        val invalidFocusMinutes = """
            {
              "status": "ok",
              "name": "Too Long Focus",
              "trigger": { "type": "time_range", "startTime": "20:00", "endTime": "22:00", "days": ["EVERY_DAY"] },
              "conditions": [],
              "action": { "mode": "focus", "focusMinutes": 999 },
              "exceptions": ["EMERGENCY_CONTACTS"]
            }
        """.trimIndent()
        assertTrue(RoutineJsonValidator.parseAndValidate(invalidFocusMinutes, "focus 999m", emptyList(), false) is RoutineJsonValidationResult.NeedsClarification)

        val invalidCondition = """
            {
              "status": "ok",
              "name": "Unknown Cond",
              "trigger": { "type": "time_range", "startTime": "20:00", "endTime": "22:00", "days": ["EVERY_DAY"] },
              "conditions": ["UNKNOWN_COND"],
              "action": { "mode": "silent" },
              "exceptions": ["EMERGENCY_CONTACTS"]
            }
        """.trimIndent()
        assertTrue(RoutineJsonValidator.parseAndValidate(invalidCondition, "silent", emptyList(), false) is RoutineJsonValidationResult.NeedsClarification)
    }

    @Test
    fun `needs_clarification status preserves short follow-up question from model`() {
        val clarificationJson = """
            {
              "status": "needs_clarification",
              "followUpQuestion": "Do you mean 8:00 AM to 10:00 AM or 8:00 PM to 10:00 PM?",
              "name": "Study",
              "trigger": { "type": "time_range", "days": ["EVERY_DAY"] },
              "conditions": [],
              "action": { "mode": "silent" },
              "exceptions": ["EMERGENCY_CONTACTS"]
            }
        """.trimIndent()

        val result = RoutineJsonValidator.parseAndValidate(clarificationJson, "study 8 to 10", emptyList(), false)
        assertTrue(result is RoutineJsonValidationResult.NeedsClarification)
        assertEquals(
            "Do you mean 8:00 AM to 10:00 AM or 8:00 PM to 10:00 PM?",
            (result as RoutineJsonValidationResult.NeedsClarification).question
        )
    }

    @Test
    fun `conflict checker warns in plain language when proposed routine overlaps existing enabled routine`() {
        val rawJson = """
            {
              "status": "ok",
              "name": "Deep Work",
              "trigger": {
                "type": "time_range",
                "startTime": "10:00",
                "endTime": "12:00",
                "days": ["WEEKDAYS"]
              },
              "conditions": ["AUTOMATION_ENABLED"],
              "action": { "mode": "silent" },
              "exceptions": ["EMERGENCY_CONTACTS"]
            }
        """.trimIndent()

        val enRes = RoutineJsonValidator.parseAndValidate(rawJson, "silent 10am to 12pm weekdays", routines, isArabic = false)
        assertTrue(enRes is RoutineJsonValidationResult.Valid)
        val enWarning = (enRes as RoutineJsonValidationResult.Valid).proposal.conflictWarning
        assertNotNull(enWarning)
        assertTrue(enWarning!!.contains("Work Hours"))

        val arRes = RoutineJsonValidator.parseAndValidate(rawJson, "صامت من 10 إلى 12", routines, isArabic = true)
        val arWarning = (arRes as RoutineJsonValidationResult.Valid).proposal.conflictWarning
        assertNotNull(arWarning)
        assertTrue(arWarning!!.contains("تنبيه تعارض"))
        assertTrue(arWarning.contains("Work Hours"))
    }

    @Test
    fun `GeminiAssistantEngine falls back to LocalDemoAssistantEngine when offline or model fails and enforces routine-only + emergency exception`() = runTest {
        val offlineEngine = GeminiAssistantEngine(
            fallbackEngine = LocalDemoAssistantEngine(),
            connectivityChecker = { false }
        )

        val offlineReply = offlineEngine.processPrompt(
            "Keep my phone silent every day from 8 PM to 10 PM",
            baseSnapshot()
        )
        assertNotNull(offlineReply.noticeText)
        assertTrue(offlineReply.noticeText!!.contains("offline", ignoreCase = true))
        assertNotNull(offlineReply.proposal)
        assertNull(offlineReply.focusMinutesToStart)
        assertTrue(offlineReply.proposal!!.routine.exceptions.any { it.type == ExceptionType.EMERGENCY_CONTACTS })
        assertNotNull(offlineReply.proposal!!.conflictWarning)

        // Direct phone-action prompt in offline mode must NOT return focusMinutesToStart (rule 8: routine creation only)
        val directActionReply = offlineEngine.processPrompt("Start a 45 minute focus session", baseSnapshot())
        assertNull(directActionReply.focusMinutesToStart)
        assertNotNull(directActionReply.noticeText)

        // Model exception fallback in Arabic
        val failingEngine = GeminiAssistantEngine(
            fallbackEngine = LocalDemoAssistantEngine(),
            connectivityChecker = { true },
            modelInvoker = { throw IllegalStateException("Network error") }
        )
        val failedArReply = failingEngine.processPrompt(
            "اكتم الإشعارات يومياً من 8 مساءً إلى 10 مساءً",
            baseSnapshot(AppLanguage.ARABIC)
        )
        assertNotNull(failedArReply.noticeText)
        assertTrue(failedArReply.noticeText!!.contains("المساعد المحلي"))
        assertNotNull(failedArReply.proposal)
        assertTrue(failedArReply.proposal!!.routine.exceptions.any { it.type == ExceptionType.EMERGENCY_CONTACTS })
    }

    @Test
    fun `GeminiAssistantEngine uses validated JSON from modelInvoker when online`() = runTest {
        val fakeModelEngine = GeminiAssistantEngine(
            connectivityChecker = { true },
            modelInvoker = {
                """
                {
                  "status": "ok",
                  "followUpQuestion": null,
                  "name": "Study 8 to 10",
                  "trigger": {
                    "type": "time_range",
                    "startTime": "20:00",
                    "endTime": "22:00",
                    "days": ["EVERY_DAY"]
                  },
                  "conditions": ["AUTOMATION_ENABLED"],
                  "action": { "mode": "silent" },
                  "exceptions": ["EMERGENCY_CONTACTS"]
                }
                """.trimIndent()
            }
        )

        val reply = fakeModelEngine.processPrompt(
            "silence my phone while I study 8 to 10 but let my family call me",
            baseSnapshot()
        )
        assertNull(reply.noticeText)
        assertNull(reply.focusMinutesToStart)
        assertNotNull(reply.proposal)
        assertEquals("Study 8 to 10", reply.proposal!!.routine.name)
        assertTrue(reply.proposal!!.routine.exceptions.any { it.type == ExceptionType.EMERGENCY_CONTACTS })
    }
}
