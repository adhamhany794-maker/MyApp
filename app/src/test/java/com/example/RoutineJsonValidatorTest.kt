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
        assertNotNull(failedArReply.debugErrorLog)
        assertTrue(failedArReply.debugErrorLog!!.contains("Network error"))
        val failedProposal = failedArReply.proposal
        assertNotNull(failedProposal)
        assertTrue(failedProposal!!.routine.exceptions.any { it.type == ExceptionType.EMERGENCY_CONTACTS })
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
        assertNull(reply.debugErrorLog)
        assertNull(reply.focusMinutesToStart)
        val proposal = reply.proposal
        assertNotNull(proposal)
        assertEquals("Study 8 to 10", proposal!!.routine.name)
        assertTrue(proposal.routine.exceptions.any { it.type == ExceptionType.EMERGENCY_CONTACTS })
    }

    @Test
    fun `GeminiAssistantEngine retries primary once on 503 or per-attempt timeout before succeeding`() = runTest {
        val calledModels = mutableListOf<String>()
        val validJson = """
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

        // Case A: 503 / high-demand triggers 1 retry on primary
        val retryEngine = GeminiAssistantEngine(
            connectivityChecker = { true },
            retryDelayMs = 1L,
            attemptTimeoutMs = 200L,
            modelByNameInvoker = { modelName, _ ->
                calledModels += modelName
                if (calledModels.size == 1) {
                    throw IllegalStateException("The model is currently experiencing high demand.")
                } else {
                    validJson
                }
            }
        )

        val reply = retryEngine.processPrompt("silence my phone 8 to 10", baseSnapshot())
        assertEquals(
            listOf(
                GeminiAssistantEngine.PRIMARY_MODEL_NAME,
                GeminiAssistantEngine.PRIMARY_MODEL_NAME
            ),
            calledModels
        )
        assertNull(reply.noticeText)
        assertNotNull(reply.proposal)
        assertEquals("Study 8 to 10", reply.proposal!!.routine.name)

        // Case B: per-attempt timeout triggers 1 retry on primary with a fresh timeout window
        val timeoutCalls = mutableListOf<String>()
        val timeoutRecoveryEngine = GeminiAssistantEngine(
            connectivityChecker = { true },
            retryDelayMs = 1L,
            attemptTimeoutMs = 40L,
            modelByNameInvoker = { modelName, _ ->
                timeoutCalls += modelName
                if (timeoutCalls.size == 1) {
                    kotlinx.coroutines.delay(250L)
                }
                validJson
            }
        )
        val recoveredReply = timeoutRecoveryEngine.processPrompt("silence my phone 8 to 10", baseSnapshot())
        assertEquals(
            listOf(
                GeminiAssistantEngine.PRIMARY_MODEL_NAME,
                GeminiAssistantEngine.PRIMARY_MODEL_NAME
            ),
            timeoutCalls
        )
        assertNotNull(recoveredReply.proposal)
    }

    @Test
    fun `GeminiAssistantEngine follows primary then 1 retry then fallback model then local assistant with attempt timing in debug notice`() = runTest {
        val calledModels = mutableListOf<String>()
        val validJson = """
            {
              "status": "ok",
              "followUpQuestion": null,
              "name": "Fallback Model Routine",
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

        // Case A: primary fails initial + 1 retry, fallback model succeeds
        val fallbackSuccessEngine = GeminiAssistantEngine(
            connectivityChecker = { true },
            retryDelayMs = 1L,
            attemptTimeoutMs = 200L,
            modelByNameInvoker = { modelName, _ ->
                calledModels += modelName
                if (modelName == GeminiAssistantEngine.PRIMARY_MODEL_NAME) {
                    throw IllegalStateException("The model is currently experiencing high demand.")
                } else {
                    validJson
                }
            }
        )
        val fallbackReply = fallbackSuccessEngine.processPrompt("silence 8 to 10", baseSnapshot())
        assertEquals(
            listOf(
                GeminiAssistantEngine.PRIMARY_MODEL_NAME,
                GeminiAssistantEngine.PRIMARY_MODEL_NAME,
                GeminiAssistantEngine.FALLBACK_MODEL_NAME
            ),
            calledModels
        )
        assertNull(fallbackReply.noticeText)
        assertNotNull(fallbackReply.proposal)
        assertEquals("Fallback Model Routine", fallbackReply.proposal!!.routine.name)

        // Case B: primary, retry, and fallback all fail -> local assistant as last fallback with per-attempt details
        val allFailedCalls = mutableListOf<String>()
        val allFailEngine = GeminiAssistantEngine(
            fallbackEngine = LocalDemoAssistantEngine(),
            connectivityChecker = { true },
            retryDelayMs = 1L,
            attemptTimeoutMs = 200L,
            modelByNameInvoker = { modelName, _ ->
                allFailedCalls += modelName
                throw IllegalStateException("503: model is currently experiencing high demand")
            }
        )
        val localReply = allFailEngine.processPrompt(
            "Keep my phone silent every day from 8 PM to 10 PM",
            baseSnapshot()
        )
        assertEquals(
            listOf(
                GeminiAssistantEngine.PRIMARY_MODEL_NAME,
                GeminiAssistantEngine.PRIMARY_MODEL_NAME,
                GeminiAssistantEngine.FALLBACK_MODEL_NAME
            ),
            allFailedCalls
        )
        assertNotNull(localReply.noticeText)
        assertTrue(localReply.noticeText!!.contains("Gemini is unavailable"))
        assertTrue(localReply.noticeText!!.contains("Attempt 1 (primary) [${GeminiAssistantEngine.PRIMARY_MODEL_NAME}]"))
        assertTrue(localReply.noticeText!!.contains("Attempt 2 (primary retry) [${GeminiAssistantEngine.PRIMARY_MODEL_NAME}]"))
        assertTrue(localReply.noticeText!!.contains("Attempt 3 (fallback) [${GeminiAssistantEngine.FALLBACK_MODEL_NAME}]"))
        assertTrue(localReply.noticeText!!.contains("ms:"))
        assertNotNull(localReply.debugErrorLog)
        assertTrue(localReply.debugErrorLog!!.contains("503"))
        assertNotNull(localReply.proposal)
    }

    @Test
    fun `multiturn Arabic conversation flow reproduces steps 1 to 6 and succeeds`() = runTest {
        val engine = com.example.assistant.LocalDemoAssistantEngine()
        var snapshot = baseSnapshot(AppLanguage.ARABIC)

        // Step 1 & 2: User requests silence in evening, assistant asks for start/end times
        val reply1 = engine.processPrompt("اكتم هاتفي في المساء", snapshot)
        assertNull(reply1.proposal)

        val draft1 = com.example.models.SmartRoutine(
            id = "draft_1",
            name = "مساء هادئ",
            description = "Draft",
            isEnabled = true,
            trigger = com.example.models.RuleTrigger(type = TriggerType.SPECIFIC_TIME, hour = 8, minute = 0, dayFilter = DayFilter.EVERY_DAY, summary = "8:00"),
            conditions = emptyList(),
            actions = listOf(com.example.models.RuleAction(ActionType.SET_SOUND_MODE, SoundMode.SILENT.name, "Silent")),
            exceptions = emptyList(),
            priority = 5,
            scheduleStartTime = "",
            scheduleEndTime = "",
            targetProfileId = "profile_study",
            lastExecutionResult = "",
            lastExecutedAtMillis = null,
            executionCount = 0,
            skippedCount = 0
        )
        snapshot = snapshot.copy(pendingRoutineDraft = draft1, clarificationTurnCount = 1)

        // Step 3 & 4: User answers "8 to 11", assistant asks AM/PM
        val reply2 = engine.processPrompt("8 to 11", snapshot)
        val draft2 = draft1.copy(scheduleStartTime = "8:00 AM", scheduleEndTime = "11:00 AM")
        snapshot = snapshot.copy(pendingRoutineDraft = draft2, clarificationTurnCount = 2)

        // Step 5 & 6: User answers "مساء" (PM) -> should merge and produce proposal
        val reply3 = engine.processPrompt("مساء", snapshot)
        assertNotNull(reply3.proposal)
        assertEquals("8:00 م", reply3.proposal!!.routine.scheduleStartTime)
        assertEquals("11:00 م", reply3.proposal!!.endTimeLabel)
    }

    @Test
    fun `arabic routine case A extracts start and end times correctly and proposes silent routine`() = runTest {
        val engine = com.example.assistant.LocalDemoAssistantEngine()
        val snapshot = baseSnapshot(AppLanguage.ARABIC)
        val reply = engine.processPrompt("اعمل الفون صامت من 8 بليل ل10 الصبح", snapshot)
        assertNotNull(reply.proposal)
        assertEquals("8:00 م", reply.proposal!!.routine.scheduleStartTime)
        assertEquals("10:00 ص", reply.proposal!!.endTimeLabel)
        assertEquals(SoundMode.SILENT.name, reply.proposal!!.routine.actions.firstOrNull { it.type == ActionType.SET_SOUND_MODE }?.targetValue)
        assertEquals("تفعيل الوضع الصامت", reply.proposal!!.actionSummary)
        assertEquals("جهات الطوارئ مستثناة دائماً ولا يمكن حجبها", reply.proposal!!.exceptionSummary)
    }

    @Test
    fun `arabic routine case B handles multi-turn follow up with morning and evening correctly`() = runTest {
        val engine = com.example.assistant.LocalDemoAssistantEngine()
        var snapshot = baseSnapshot(AppLanguage.ARABIC)
        val reply1 = engine.processPrompt("لا عاوزه صامت", snapshot)
        assertNull(reply1.proposal)

        val draft1 = com.example.models.SmartRoutine(
            id = "draft_b",
            name = "روتين",
            description = "Draft B",
            isEnabled = true,
            trigger = com.example.models.RuleTrigger(type = TriggerType.SPECIFIC_TIME, hour = 9, minute = 0, dayFilter = DayFilter.EVERY_DAY, summary = ""),
            conditions = emptyList(),
            actions = listOf(com.example.models.RuleAction(ActionType.SET_SOUND_MODE, SoundMode.SILENT.name, "Silent")),
            exceptions = emptyList(),
            priority = 5,
            scheduleStartTime = "",
            scheduleEndTime = "",
            targetProfileId = "profile_study",
            lastExecutionResult = "",
            lastExecutedAtMillis = null,
            executionCount = 0,
            skippedCount = 0
        )
        snapshot = snapshot.copy(pendingRoutineDraft = draft1, clarificationTurnCount = 1)
        val reply2 = engine.processPrompt("9صباحا ل10 مساء", snapshot)
        assertNotNull(reply2.proposal)
        assertEquals("9:00 ص", reply2.proposal!!.routine.scheduleStartTime)
        assertEquals("10:00 م", reply2.proposal!!.endTimeLabel)
        assertEquals(SoundMode.SILENT.name, reply2.proposal!!.routine.actions.firstOrNull { it.type == ActionType.SET_SOUND_MODE }?.targetValue)
        assertEquals("تفعيل الوضع الصامت", reply2.proposal!!.actionSummary)
        assertEquals("جهات الطوارئ مستثناة دائماً ولا يمكن حجبها", reply2.proposal!!.exceptionSummary)
    }

    @Test
    fun `arabic routine overnight range handles cross midnight schedule correctly`() = runTest {
        val engine = com.example.assistant.LocalDemoAssistantEngine()
        val snapshot = baseSnapshot(AppLanguage.ARABIC)
        val reply = engine.processPrompt("من 11 بالليل ل 7 الصبح وضع صامت", snapshot)
        assertNotNull(reply.proposal)
        assertEquals("11:00 م", reply.proposal!!.routine.scheduleStartTime)
        assertEquals("7:00 ص", reply.proposal!!.endTimeLabel)
        assertEquals(SoundMode.SILENT.name, reply.proposal!!.routine.actions.firstOrNull { it.type == ActionType.SET_SOUND_MODE }?.targetValue)
        assertEquals("تفعيل الوضع الصامت", reply.proposal!!.actionSummary)
    }

    @Test
    fun `sequential standalone requests without clearing update routine card from 8pm-10am to 7pm-6am`() = runTest {
        val engine = com.example.assistant.LocalDemoAssistantEngine()
        val snapshot1 = baseSnapshot(AppLanguage.ARABIC)

        // 1. First standalone request: "اعمل صامت من 8 بليل ل10 الصبح"
        val reply1 = engine.processPrompt("اعمل صامت من 8 بليل ل10 الصبح", snapshot1)
        assertNotNull(reply1.proposal)
        assertEquals("8:00 م", reply1.proposal!!.routine.scheduleStartTime)
        assertEquals("10:00 ص", reply1.proposal!!.endTimeLabel)
        assertEquals(SoundMode.SILENT.name, reply1.proposal!!.routine.actions.firstOrNull { it.type == ActionType.SET_SOUND_MODE }?.targetValue)

        // 2. Without clearing, second standalone message: "7 مساء الى 6 صباح"
        val snapshot2 = snapshot1.copy(pendingRoutineDraft = reply1.proposal!!.routine)
        val reply2 = engine.processPrompt("7 مساء الى 6 صباح", snapshot2)
        assertNotNull(reply2.proposal)
        assertEquals("7:00 م", reply2.proposal!!.routine.scheduleStartTime)
        assertEquals("6:00 ص", reply2.proposal!!.endTimeLabel)
        assertEquals("يومياً من 7:00 م إلى 6:00 ص", reply2.proposal!!.triggerSummary)
    }

    @Test
    fun `vibrate routine request from 11pm to 7am gives vibrate mode and correct schedule`() = runTest {
        val engine = com.example.assistant.LocalDemoAssistantEngine()
        val snapshot = baseSnapshot(AppLanguage.ARABIC)

        val reply = engine.processPrompt("اهتزاز من 11 بالليل لـ 7 الصبح", snapshot)
        assertNotNull(reply.proposal)
        assertEquals("11:00 م", reply.proposal!!.routine.scheduleStartTime)
        assertEquals("7:00 ص", reply.proposal!!.endTimeLabel)
        assertEquals(SoundMode.VIBRATE.name, reply.proposal!!.routine.actions.firstOrNull { it.type == ActionType.SET_SOUND_MODE }?.targetValue)
        assertEquals("تفعيل وضع الاهتزاز", reply.proposal!!.actionSummary)
    }

    @Test
    fun `routine json validator overrides stale gemini default times with prompt times`() {
        val geminiJson = """
            {
              "status": "ok",
              "name": "ساعات الهدوء",
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

        val result = RoutineJsonValidator.parseAndValidate(
            rawJson = geminiJson,
            originalPrompt = "7 مساء الى 6 صباح",
            existingRoutines = emptyList(),
            isArabic = true
        )
        assertTrue(result is RoutineJsonValidationResult.Valid)
        val valid = result as RoutineJsonValidationResult.Valid
        assertEquals("7:00 م", valid.proposal.routine.scheduleStartTime)
        assertEquals("6:00 ص", valid.proposal.endTimeLabel)
    }
}

