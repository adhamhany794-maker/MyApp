package com.example

import com.example.assistant.AssistantStateSnapshot
import com.example.assistant.LocalDemoAssistantEngine
import com.example.data.DemoSeedData
import com.example.models.AppLanguage
import com.example.models.ConflictResolutionPolicy
import com.example.models.ContactPriority
import com.example.models.DayFilter
import com.example.models.InterruptionType
import com.example.models.SimulationContextState
import com.example.models.SoundMode
import com.example.simulation.AndroidDeviceActionExecutor
import com.example.simulation.AutomationSimulationEngine
import com.example.simulation.DeviceActionCommand
import com.example.simulation.SimulatedDeviceActionExecutor
import com.example.simulation.SimulationEvent
import com.example.utils.FeatureExplanations
import com.example.utils.FeatureHelpTopic
import com.example.utils.stringsFor
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartCompanionCoreTest {

    private val engine = AutomationSimulationEngine()
    private val assistant = LocalDemoAssistantEngine()
    private val profiles = DemoSeedData.defaultProfiles()
    private val routines = DemoSeedData.defaultRoutines()
    private val contacts = DemoSeedData.defaultContacts()

    @Test
    fun `simulation engine matches Work Hours on Monday 9 AM`() {
        val output = engine.evaluate(
            event = SimulationEvent.TimeAndDay(9, 0, DayFilter.MONDAY),
            routines = routines,
            profiles = profiles,
            context = SimulationContextState(9, 0, DayFilter.MONDAY),
            isAutomationPaused = false,
            activeProfileId = "profile_personal",
            isFocusActive = false,
            conflictPolicy = ConflictResolutionPolicy.ASK_USER
        )
        assertEquals("routine_work_hours", output.selectedRoutine?.id)
        assertEquals("profile_work", output.targetProfileId)
        assertFalse(output.trace.conflictDetected)
    }

    @Test
    fun `simulation engine detects 6 PM weekday conflict and requests confirmation under ASK_USER`() {
        val output = engine.evaluate(
            event = SimulationEvent.TimeAndDay(18, 0, DayFilter.WEDNESDAY),
            routines = routines,
            profiles = profiles,
            context = SimulationContextState(18, 0, DayFilter.WEDNESDAY),
            isAutomationPaused = false,
            activeProfileId = "profile_personal",
            isFocusActive = false,
            conflictPolicy = ConflictResolutionPolicy.ASK_USER
        )
        assertTrue(output.trace.conflictDetected)
        assertTrue(output.trace.requiresUserConfirmation)
        assertEquals(2, output.trace.conflictingRoutineIds.size)
    }

    @Test
    fun `simulation engine resolves conflict by priority when policy is HIGHEST_PRIORITY`() {
        val output = engine.evaluate(
            event = SimulationEvent.TimeAndDay(18, 0, DayFilter.WEDNESDAY),
            routines = routines,
            profiles = profiles,
            context = SimulationContextState(18, 0, DayFilter.WEDNESDAY),
            isAutomationPaused = false,
            activeProfileId = "profile_personal",
            isFocusActive = false,
            conflictPolicy = ConflictResolutionPolicy.HIGHEST_PRIORITY
        )
        assertTrue(output.trace.conflictDetected)
        assertFalse(output.trace.requiresUserConfirmation)
        assertEquals("routine_study_time", output.selectedRoutine?.id)
        assertEquals(SoundMode.SILENT, output.targetSoundMode)
    }

    @Test
    fun `global pause suspends scheduled triggers`() {
        val output = engine.evaluate(
            event = SimulationEvent.TimeAndDay(9, 0, DayFilter.MONDAY),
            routines = routines,
            profiles = profiles,
            context = SimulationContextState(9, 0, DayFilter.MONDAY),
            isAutomationPaused = true,
            activeProfileId = "profile_personal",
            isFocusActive = false,
            conflictPolicy = ConflictResolutionPolicy.ASK_USER
        )
        assertNull(output.selectedRoutine)
        assertTrue(output.trace.finalResult.contains("Global Automation Pause"))
    }

    @Test
    fun `study profile blocks normal caller and allows emergency caller`() {
        val studyProfile = profiles.first { it.id == "profile_study" }
        val normalContact = contacts.first { it.priorityLevel == ContactPriority.NORMAL }
        val emergencyContact = contacts.first { it.priorityLevel == ContactPriority.EMERGENCY }

        val normalDecision = engine.simulateIncomingInterruption(
            contact = normalContact,
            interruptionType = InterruptionType.CALL,
            isRepeatCaller = false,
            activeProfile = studyProfile
        )
        val emergencyDecision = engine.simulateIncomingInterruption(
            contact = emergencyContact,
            interruptionType = InterruptionType.CALL,
            isRepeatCaller = false,
            activeProfile = studyProfile
        )

        assertFalse(normalDecision.isAllowed)
        assertTrue(emergencyDecision.isAllowed)
    }

    @Test
    fun `assistant builds structured routine proposal and answers analytics and daily activity questions`() = runTest {
        val snapshot = AssistantStateSnapshot(
            activeProfile = profiles.first { it.id == "profile_study" },
            soundMode = SoundMode.SILENT,
            isAutomationPaused = false,
            routines = routines,
            contacts = contacts,
            notificationStats = DemoSeedData.defaultNotificationStats(),
            focusSessions = DemoSeedData.defaultFocusSessions(),
            recentEvents = DemoSeedData.defaultActivityEvents()
        )

        val proposalReply = assistant.processPrompt(
            "Keep my phone silent every day from 8 PM to 10 PM, but allow emergency contacts.",
            snapshot
        )
        assertNotNull(proposalReply.proposal)
        assertEquals("8:00 PM", proposalReply.proposal?.routine?.scheduleStartTime)
        assertEquals("10:00 PM", proposalReply.proposal?.endTimeLabel)
        assertTrue(proposalReply.proposal?.exceptionSummary?.contains("Emergency") == true)

        val sleepProposal = assistant.processPrompt("Help me organize my sleep schedule.", snapshot)
        assertNotNull(sleepProposal.proposal)
        assertEquals("profile_sleep", sleepProposal.proposal?.routine?.targetProfileId)

        val dailyActivityReply = assistant.processPrompt("Show my daily activity.", snapshot)
        assertTrue(dailyActivityReply.text.contains("Active Profile"))

        val analyticsReply = assistant.processPrompt(
            "What are my most distracting applications?",
            snapshot
        )
        assertTrue(analyticsReply.text.contains("Socialgram"))
    }

    @Test
    fun `arabic assistant parses routine intent, daily activity, focus timer, and distracting apps in Arabic`() = runTest {
        val snapshot = AssistantStateSnapshot(
            language = AppLanguage.ARABIC,
            activeProfile = profiles.first { it.id == "profile_study" },
            soundMode = SoundMode.SILENT,
            isAutomationPaused = false,
            routines = routines,
            contacts = contacts,
            notificationStats = DemoSeedData.defaultNotificationStats(),
            focusSessions = DemoSeedData.defaultFocusSessions(),
            recentEvents = DemoSeedData.defaultActivityEvents()
        )

        val proposalReply = assistant.processPrompt(
            "اكتم الإشعارات يومياً من 8 مساءً إلى 10 مساءً مع السماح بجهات الطوارئ.",
            snapshot
        )
        assertNotNull(proposalReply.proposal)
        assertTrue(proposalReply.text.contains("يمكنني إنشاء روتين"))
        assertTrue(proposalReply.proposal?.exceptionSummary?.contains("الطوارئ") == true)

        val activityReply = assistant.processPrompt("اعرض نشاطي اليومي.", snapshot)
        assertTrue(activityReply.text.contains("ملخص نشاطك"))

        val focusReply = assistant.processPrompt("ابدأ جلسة تركيز لمدة 35 دقيقة.", snapshot)
        assertEquals(35, focusReply.focusMinutesToStart)
        assertTrue(focusReply.text.contains("35"))

        val appsReply = assistant.processPrompt("ما هي أكثر التطبيقات إزعاجاً؟", snapshot)
        assertTrue(appsReply.text.contains("Socialgram"))
    }

    @Test
    fun `device action executor clearly separates simulated vs inert android os executor`() = runTest {
        val simExecutor = SimulatedDeviceActionExecutor()
        val androidExecutor = AndroidDeviceActionExecutor()

        val cmd = DeviceActionCommand.ApplySoundMode(SoundMode.SILENT, "Study Profile")
        val simRes = simExecutor.execute(cmd)
        val osRes = androidExecutor.execute(cmd)

        assertTrue(simExecutor.isSimulatedMode)
        assertTrue(simRes.executed)
        assertTrue(simRes.isSimulated)
        assertEquals(SoundMode.SILENT, simRes.appliedSoundMode)

        assertFalse(androidExecutor.isSimulatedMode)
        assertFalse(osRes.executed)
        assertFalse(osRes.isSimulated)
    }

    @Test
    fun `localization strings and feature explanations support english and arabic cleanly`() {
        val en = stringsFor(AppLanguage.ENGLISH)
        val ar = stringsFor(AppLanguage.ARABIC)

        assertFalse(en.isArabic)
        assertTrue(ar.isArabic)
        assertEquals("Smart Companion", en.appName)
        assertEquals("المرافق الذكي", ar.appName)
        assertEquals(3, en.onboardingPages.size)
        assertEquals(3, ar.onboardingPages.size)
        assertEquals(4, en.assistantSuggestedPrompts.size)
        assertEquals(4, ar.assistantSuggestedPrompts.size)
        assertEquals("الدراسة", ar.localizedProfileName(profiles.first { it.id == "profile_study" }))

        FeatureHelpTopic.entries.forEach { topic ->
            val enHelp = FeatureExplanations.get(topic, isArabic = false)
            val arHelp = FeatureExplanations.get(topic, isArabic = true)
            assertTrue(enHelp.title.isNotBlank() && enHelp.whatItIs.isNotBlank() && enHelp.whatHappens.isNotBlank())
            assertTrue(arHelp.title.isNotBlank() && arHelp.whatItIs.isNotBlank() && arHelp.whatHappens.isNotBlank())
        }
    }
}
