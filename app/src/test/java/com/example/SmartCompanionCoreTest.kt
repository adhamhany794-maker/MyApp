package com.example

import com.example.assistant.AssistantStateSnapshot
import com.example.assistant.LocalDemoAssistantEngine
import com.example.data.DemoSeedData
import com.example.models.ActionType
import com.example.models.AppLanguage
import com.example.models.ConflictResolutionPolicy
import com.example.models.ContactPriority
import com.example.models.DayFilter
import com.example.models.InterruptionType
import com.example.models.PriorityContact
import com.example.models.RuleAction
import com.example.models.RuleTrigger
import com.example.models.SimulationContextState
import com.example.models.SmartRoutine
import com.example.models.SoundMode
import com.example.models.TriggerType
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
        val emergencyContact = contacts.first { it.priorityLevel == ContactPriority.EMERGENCY }

        val normalDecision = engine.simulateIncomingInterruption(
            contact = null,
            interruptionType = InterruptionType.CALL,
            isRepeatCaller = false,
            activeProfile = studyProfile,
            callerName = "Omar Tariq"
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

    @Test
    fun `emergency contacts pass unconditionally in every profile even when toggle off or channel restricted`() {
        val restrictedEmergencyContact = PriorityContact(
            id = "contact_em_restricted",
            name = "Emergency Only Calls",
            initials = "EC",
            relationship = "Family",
            priorityLevel = ContactPriority.EMERGENCY,
            allowedInterruptions = listOf(InterruptionType.CALL),
            isEnabled = true,
            allowRepeatCallers = false
        )
        val noChannelsEmergencyContact = restrictedEmergencyContact.copy(
            id = "contact_em_none",
            allowedInterruptions = emptyList(),
            isEnabled = false
        )
        val allTestContacts = contacts.filter { it.priorityLevel == ContactPriority.EMERGENCY } +
            listOf(restrictedEmergencyContact, noChannelsEmergencyContact)

        val lockedSilentProfile = profiles.first { it.id == "profile_sleep" }.copy(
            id = "profile_locked_silent",
            name = "Strict Silent",
            soundMode = SoundMode.SILENT,
            allowHighPriorityContacts = false,
            allowedCategories = emptyList()
        )
        val allProfiles = profiles + lockedSilentProfile

        for (profile in allProfiles) {
            for (emergency in allTestContacts) {
                for (channel in listOf(InterruptionType.CALL, InterruptionType.MESSAGE, InterruptionType.REPEAT_CALLER)) {
                    val decision = engine.simulateIncomingInterruption(
                        contact = emergency,
                        interruptionType = channel,
                        isRepeatCaller = false,
                        activeProfile = profile
                    )
                    assertTrue(
                        "Expected ${emergency.name} ($channel) to pass in ${profile.name} (${profile.soundMode})",
                        decision.isAllowed
                    )
                }
            }
        }
    }

    @Test
    fun `emergency contacts pass in every routine and conflict resolution policy`() {
        val emergencyContacts = contacts.filter { it.priorityLevel == ContactPriority.EMERGENCY }
        val strictNoExceptionRoutine = SmartRoutine(
            id = "routine_strict_silent",
            name = "Strict Silent No Exceptions",
            description = "Silent mode without explicit emergency exceptions",
            isEnabled = true,
            trigger = RuleTrigger(type = TriggerType.MANUAL, summary = "Manual"),
            conditions = emptyList(),
            actions = listOf(
                RuleAction(ActionType.ACTIVATE_PROFILE, "profile_sleep", "Activate Sleep"),
                RuleAction(ActionType.SET_SOUND_MODE, SoundMode.SILENT.name, "Set Silent")
            ),
            exceptions = emptyList(),
            priority = 10,
            scheduleStartTime = "10:00 PM",
            scheduleEndTime = "6:00 AM",
            targetProfileId = "profile_sleep",
            lastExecutionResult = "Ready",
            lastExecutedAtMillis = null,
            executionCount = 0,
            skippedCount = 0
        )
        val allRoutines = routines + strictNoExceptionRoutine

        // Every routine individually
        for (routine in allRoutines) {
            val output = engine.evaluate(
                event = SimulationEvent.ManualRoutine(routine.id),
                routines = allRoutines,
                profiles = profiles,
                context = SimulationContextState(18, 0, DayFilter.WEDNESDAY),
                isAutomationPaused = false,
                activeProfileId = "profile_personal",
                isFocusActive = false,
                conflictPolicy = ConflictResolutionPolicy.HIGHEST_PRIORITY
            )
            val baseProfile = profiles.firstOrNull { it.id == output.targetProfileId }
                ?: profiles.first { it.id == "profile_personal" }
            val resultingProfile = baseProfile.copy(
                soundMode = output.targetSoundMode ?: baseProfile.soundMode
            )
            for (emergency in emergencyContacts) {
                for (channel in listOf(InterruptionType.CALL, InterruptionType.MESSAGE)) {
                    val decision = engine.simulateIncomingInterruption(
                        contact = emergency,
                        interruptionType = channel,
                        isRepeatCaller = false,
                        activeProfile = resultingProfile
                    )
                    assertTrue(
                        "Routine ${routine.name} must not block ${emergency.name} ($channel)",
                        decision.isAllowed
                    )
                }
            }
        }

        // Conflict resolution under every policy (ASK_USER, HIGHEST_PRIORITY, KEEP_CURRENT)
        for (policy in ConflictResolutionPolicy.entries) {
            val conflictOutput = engine.evaluate(
                event = SimulationEvent.TimeAndDay(18, 0, DayFilter.WEDNESDAY),
                routines = routines,
                profiles = profiles,
                context = SimulationContextState(18, 0, DayFilter.WEDNESDAY),
                isAutomationPaused = false,
                activeProfileId = "profile_study",
                isFocusActive = false,
                conflictPolicy = policy
            )
            assertTrue(conflictOutput.trace.conflictDetected)

            val selected = conflictOutput.selectedRoutine
            val resolvedOutput = if (conflictOutput.trace.requiresUserConfirmation && selected != null) {
                engine.executeConfirmedRoutine(
                    winner = selected,
                    conflictingNames = conflictOutput.trace.matchingRoutineNames,
                    triggerLabel = conflictOutput.trace.triggerReceived,
                    conflictExplanation = conflictOutput.trace.conflictExplanation
                )
            } else {
                conflictOutput
            }

            val resultingProfile = (profiles.firstOrNull { it.id == resolvedOutput.targetProfileId }
                ?: profiles.first { it.id == "profile_study" }).copy(
                soundMode = resolvedOutput.targetSoundMode ?: SoundMode.SILENT
            )
            for (emergency in emergencyContacts) {
                assertTrue(
                    engine.simulateIncomingInterruption(emergency, InterruptionType.CALL, false, resultingProfile).isAllowed
                )
                assertTrue(
                    engine.simulateIncomingInterruption(emergency, InterruptionType.MESSAGE, false, resultingProfile).isAllowed
                )
            }
        }
    }

    @Test
    fun `emergency contacts pass during active focus session and global pause state`() {
        val emergencyContacts = contacts.filter { it.priorityLevel == ContactPriority.EMERGENCY }
        val customFocusProfile = profiles.first { it.id == "profile_custom" }.copy(
            soundMode = SoundMode.SILENT,
            allowHighPriorityContacts = false
        )

        // Focus session active (isFocusActive = true)
        val focusOutput = engine.evaluate(
            event = SimulationEvent.FocusLifecycle(started = true),
            routines = routines,
            profiles = profiles,
            context = SimulationContextState(14, 0, DayFilter.TUESDAY),
            isAutomationPaused = false,
            activeProfileId = customFocusProfile.id,
            isFocusActive = true,
            conflictPolicy = ConflictResolutionPolicy.HIGHEST_PRIORITY
        )
        val focusProfile = (profiles.firstOrNull { it.id == focusOutput.targetProfileId } ?: customFocusProfile).copy(
            soundMode = focusOutput.targetSoundMode ?: SoundMode.SILENT
        )
        for (emergency in emergencyContacts) {
            assertTrue(engine.simulateIncomingInterruption(emergency, InterruptionType.CALL, false, focusProfile).isAllowed)
            assertTrue(engine.simulateIncomingInterruption(emergency, InterruptionType.MESSAGE, false, focusProfile).isAllowed)
        }

        // Global automation pause active (isAutomationPaused = true)
        val pausedOutput = engine.evaluate(
            event = SimulationEvent.TimeAndDay(9, 0, DayFilter.MONDAY),
            routines = routines,
            profiles = profiles,
            context = SimulationContextState(9, 0, DayFilter.MONDAY),
            isAutomationPaused = true,
            activeProfileId = customFocusProfile.id,
            isFocusActive = false,
            conflictPolicy = ConflictResolutionPolicy.ASK_USER
        )
        assertNull(pausedOutput.selectedRoutine)
        for (profile in profiles) {
            for (emergency in emergencyContacts) {
                assertTrue(engine.simulateIncomingInterruption(emergency, InterruptionType.CALL, false, profile).isAllowed)
                assertTrue(engine.simulateIncomingInterruption(emergency, InterruptionType.MESSAGE, false, profile).isAllowed)
            }
        }
    }
}
