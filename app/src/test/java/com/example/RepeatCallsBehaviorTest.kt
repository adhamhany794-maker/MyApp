package com.example

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Looper
import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.automation.InMemoryEmergencyBypassBackupStore
import com.example.automation.InMemoryRepeatCallHistoryStore
import com.example.automation.InMemoryRoutineStateBackupStore
import com.example.automation.IncomingCallBypassCoordinator
import com.example.automation.NoOpRoutineAlarmScheduler
import com.example.automation.RepeatCallPolicyResolver
import com.example.automation.RepeatCallRuleSource
import com.example.automation.RepeatCallTracker
import com.example.data.PreferencesManager
import com.example.data.local.AppDatabase
import com.example.models.AppLanguage
import com.example.models.ContactPriority
import com.example.models.InterruptionType
import com.example.models.PriorityContact
import com.example.models.RepeatCallerRule
import com.example.models.SoundMode
import com.example.repositories.SmartCompanionRepository
import com.example.simulation.AndroidPhoneControlGateway
import com.example.simulation.PhoneControlGateway
import com.example.simulation.PhoneControlManager
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.SmartCompanionTheme
import com.example.utils.stringsFor
import com.example.viewmodels.SmartCompanionUiState
import com.example.viewmodels.SmartCompanionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RepeatCallsBehaviorTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class FakeRepeatPhoneGateway(
        var granted: Boolean = true,
        var ringerMode: SoundMode = SoundMode.SOUND,
        var interruptionFilter: Int = NotificationManager.INTERRUPTION_FILTER_PRIORITY,
        var starredAllowed: Boolean = true,
        var repeatCallersAllowed: Boolean = false
    ) : PhoneControlGateway {
        override fun isNotificationPolicyAccessGranted(): Boolean = granted
        override fun getCurrentRingerMode(): SoundMode = ringerMode
        override fun setRingerMode(mode: SoundMode): Boolean {
            if (!granted) return false
            ringerMode = mode
            return true
        }

        override fun isDndEnabled(): Boolean =
            interruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL &&
                interruptionFilter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN

        override fun isStarredContactsAllowedInDnd(): Boolean = starredAllowed

        override fun isRepeatCallersAllowedInDnd(): Boolean = repeatCallersAllowed

        override fun setDoNotDisturb(enabled: Boolean, allowStarredContacts: Boolean): Boolean =
            setDoNotDisturb(enabled, allowStarredContacts, allowRepeatCallers = false)

        override fun setDoNotDisturb(
            enabled: Boolean,
            allowStarredContacts: Boolean,
            allowRepeatCallers: Boolean
        ): Boolean {
            if (!granted) return false
            interruptionFilter = if (enabled) {
                NotificationManager.INTERRUPTION_FILTER_PRIORITY
            } else {
                NotificationManager.INTERRUPTION_FILTER_ALL
            }
            starredAllowed = enabled && allowStarredContacts
            repeatCallersAllowed = enabled && allowRepeatCallers
            return true
        }

        override fun getCurrentInterruptionFilter(): Int = interruptionFilter

        override fun setInterruptionFilter(filter: Int): Boolean {
            if (!granted) return false
            interruptionFilter = filter
            if (filter == NotificationManager.INTERRUPTION_FILTER_ALL) {
                starredAllowed = false
                repeatCallersAllowed = false
            }
            return true
        }

        override fun createDndAccessSettingsIntent(context: Context): Intent =
            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
    }

    private suspend fun awaitMainLooper(maxIterations: Int = 100, condition: suspend () -> Boolean) {
        var wait = 0
        while (!condition() && wait < maxIterations) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            delay(25)
            wait++
        }
        Shadows.shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `repeat call counter resets after M minutes window expires`() {
        val tracker = RepeatCallTracker(InMemoryRepeatCallHistoryStore())
        val baseTime = 1_700_000_000_000L
        val caller = "01001234567"

        // N = 3 calls within M = 15 minutes
        val call1 = tracker.recordAndEvaluateIncomingCall(
            phoneNumber = caller,
            timestampMillis = baseTime,
            emergencyContacts = emptyList(),
            perPersonRules = emptyList(),
            globalEnabled = true,
            globalRequiredCalls = 3,
            globalWindowMinutes = 15
        )
        assertEquals(1, call1.callCountInWindow)
        assertFalse(call1.shouldAllowBypass)

        val call2 = tracker.recordAndEvaluateIncomingCall(
            phoneNumber = caller,
            timestampMillis = baseTime + 5 * 60_000L,
            emergencyContacts = emptyList(),
            perPersonRules = emptyList(),
            globalEnabled = true,
            globalRequiredCalls = 3,
            globalWindowMinutes = 15
        )
        assertEquals(2, call2.callCountInWindow)
        assertFalse(call2.shouldAllowBypass)

        // 25 minutes after start (20 minutes after call 2 -> both earlier calls expired past 15m window)
        val afterExpiry = tracker.recordAndEvaluateIncomingCall(
            phoneNumber = caller,
            timestampMillis = baseTime + 25 * 60_000L,
            emergencyContacts = emptyList(),
            perPersonRules = emptyList(),
            globalEnabled = true,
            globalRequiredCalls = 3,
            globalWindowMinutes = 15
        )
        assertEquals(1, afterExpiry.callCountInWindow)
        assertFalse(afterExpiry.shouldAllowBypass)

        // Two more calls inside the new 15-minute window reach N = 3 and bypass DND
        val newWindowCall2 = tracker.recordAndEvaluateIncomingCall(
            phoneNumber = caller,
            timestampMillis = baseTime + 28 * 60_000L,
            emergencyContacts = emptyList(),
            perPersonRules = emptyList(),
            globalEnabled = true,
            globalRequiredCalls = 3,
            globalWindowMinutes = 15
        )
        assertEquals(2, newWindowCall2.callCountInWindow)
        assertFalse(newWindowCall2.shouldAllowBypass)

        val newWindowCall3 = tracker.recordAndEvaluateIncomingCall(
            phoneNumber = caller,
            timestampMillis = baseTime + 31 * 60_000L,
            emergencyContacts = emptyList(),
            perPersonRules = emptyList(),
            globalEnabled = true,
            globalRequiredCalls = 3,
            globalWindowMinutes = 15
        )
        assertEquals(3, newWindowCall3.callCountInWindow)
        assertTrue(newWindowCall3.shouldAllowBypass)
        assertEquals(RepeatCallRuleSource.GLOBAL_SETTING, newWindowCall3.source)
    }

    @Test
    fun `calls from different numbers do not add up to threshold while equivalent formats do`() {
        val tracker = RepeatCallTracker(InMemoryRepeatCallHistoryStore())
        val baseTime = 1_700_000_000_000L

        val callerAFirst = tracker.recordAndEvaluateIncomingCall(
            phoneNumber = "+20 100 111 2222",
            timestampMillis = baseTime,
            emergencyContacts = emptyList(),
            perPersonRules = emptyList(),
            globalEnabled = true,
            globalRequiredCalls = 2,
            globalWindowMinutes = 15
        )
        assertEquals(1, callerAFirst.callCountInWindow)
        assertFalse(callerAFirst.shouldAllowBypass)

        val callerBFirst = tracker.recordAndEvaluateIncomingCall(
            phoneNumber = "01001112223",
            timestampMillis = baseTime + 60_000L,
            emergencyContacts = emptyList(),
            perPersonRules = emptyList(),
            globalEnabled = true,
            globalRequiredCalls = 2,
            globalWindowMinutes = 15
        )
        assertEquals(1, callerBFirst.callCountInWindow)
        assertFalse(callerBFirst.shouldAllowBypass)

        val callerCFirst = tracker.recordAndEvaluateIncomingCall(
            phoneNumber = "01112223334",
            timestampMillis = baseTime + 120_000L,
            emergencyContacts = emptyList(),
            perPersonRules = emptyList(),
            globalEnabled = true,
            globalRequiredCalls = 2,
            globalWindowMinutes = 15
        )
        assertEquals(1, callerCFirst.callCountInWindow)
        assertFalse(callerCFirst.shouldAllowBypass)

        // Same caller A dialing with Arabic digits and local zero format increments only caller A's count to 2
        val callerASecond = tracker.recordAndEvaluateIncomingCall(
            phoneNumber = "٠١٠٠-١١١-٢٢٢٢",
            timestampMillis = baseTime + 180_000L,
            emergencyContacts = emptyList(),
            perPersonRules = emptyList(),
            globalEnabled = true,
            globalRequiredCalls = 2,
            globalWindowMinutes = 15
        )
        assertEquals(2, callerASecond.callCountInWindow)
        assertTrue(callerASecond.shouldAllowBypass)
    }

    @Test
    fun `N equals 2 within 15 min uses OS repeat callers policy while other N omit OS repeat category`() {
        assertTrue(RepeatCallPolicyResolver.shouldUseOsRepeatCallersPolicy(true, 2, 15))
        assertFalse(RepeatCallPolicyResolver.shouldUseOsRepeatCallersPolicy(false, 2, 15))
        assertFalse(RepeatCallPolicyResolver.shouldUseOsRepeatCallersPolicy(true, 3, 15))
        assertFalse(RepeatCallPolicyResolver.shouldUseOsRepeatCallersPolicy(true, 2, 10))

        val context = ApplicationProvider.getApplicationContext<Context>()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNm = Shadows.shadowOf(nm)
        shadowNm.setNotificationPolicyAccessGranted(true)

        val gateway = AndroidPhoneControlGateway(context)

        // 1. N=2, M=15 enables OS repeat callers category in DND policy
        assertTrue(gateway.setDoNotDisturb(enabled = true, allowStarredContacts = true, allowRepeatCallers = true))
        val policyWithOsRepeat = nm.notificationPolicy
        assertTrue(
            (policyWithOsRepeat.priorityCategories and NotificationManager.Policy.PRIORITY_CATEGORY_REPEAT_CALLERS) != 0
        )

        // 2. N=3 (in-app counting) omits OS repeat callers category so 2nd call does NOT slip through OS
        assertTrue(gateway.setDoNotDisturb(enabled = true, allowStarredContacts = true, allowRepeatCallers = false))
        val policyWithoutOsRepeat = nm.notificationPolicy
        assertEquals(
            0,
            policyWithoutOsRepeat.priorityCategories and NotificationManager.Policy.PRIORITY_CATEGORY_REPEAT_CALLERS
        )
    }

    @Test
    fun `custom N lifts DND when threshold is reached and restores saved state after call ends`() {
        val gateway = FakeRepeatPhoneGateway(
            granted = true,
            ringerMode = SoundMode.SOUND,
            interruptionFilter = NotificationManager.INTERRUPTION_FILTER_PRIORITY,
            starredAllowed = true,
            repeatCallersAllowed = false
        )
        val phoneControlManager = PhoneControlManager(gateway)
        val backupStore = InMemoryEmergencyBypassBackupStore()
        val tracker = RepeatCallTracker(InMemoryRepeatCallHistoryStore())
        val coordinator = IncomingCallBypassCoordinator(phoneControlManager, backupStore, tracker)

        val baseTime = 1_700_000_000_000L
        val caller = "01222333444"

        // Call 1 of 3: DND stays enabled
        val res1 = coordinator.onIncomingCallRinging(
            phoneNumber = caller,
            timestampMillis = baseTime,
            emergencyContacts = emptyList(),
            perPersonRules = emptyList(),
            globalEnabled = true,
            globalRequiredCalls = 3,
            globalWindowMinutes = 10
        )
        assertFalse(res1.evaluation.shouldAllowBypass)
        assertFalse(res1.dndLiftedByApp)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_PRIORITY, gateway.interruptionFilter)
        assertNull(backupStore.getBackup())

        // Call 2 of 3: DND stays enabled
        val res2 = coordinator.onIncomingCallRinging(
            phoneNumber = caller,
            timestampMillis = baseTime + 60_000L,
            emergencyContacts = emptyList(),
            perPersonRules = emptyList(),
            globalEnabled = true,
            globalRequiredCalls = 3,
            globalWindowMinutes = 10
        )
        assertFalse(res2.evaluation.shouldAllowBypass)
        assertFalse(res2.dndLiftedByApp)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_PRIORITY, gateway.interruptionFilter)

        // Call 3 of 3: bypass granted, backup captured, DND lifted so phone rings
        val res3 = coordinator.onIncomingCallRinging(
            phoneNumber = caller,
            timestampMillis = baseTime + 120_000L,
            emergencyContacts = emptyList(),
            perPersonRules = emptyList(),
            globalEnabled = true,
            globalRequiredCalls = 3,
            globalWindowMinutes = 10
        )
        assertTrue(res3.evaluation.shouldAllowBypass)
        assertFalse(res3.usedOsPolicy)
        assertTrue(res3.dndLiftedByApp)
        assertEquals(SoundMode.SOUND, gateway.ringerMode)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_ALL, gateway.interruptionFilter)
        assertNotNull(backupStore.getBackup())

        // Call ends -> restores previous DND PRIORITY filter and clears backup
        assertTrue(coordinator.onCallEnded())
        assertEquals(SoundMode.SOUND, gateway.ringerMode)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_PRIORITY, gateway.interruptionFilter)
        assertNull(backupStore.getBackup())
    }

    @Test
    fun `per-person repeat rule overrides global setting while others follow global setting`() {
        val tracker = RepeatCallTracker(InMemoryRepeatCallHistoryStore())
        val baseTime = 1_700_000_000_000L
        val emergencyContact = PriorityContact(
            id = "em_1",
            name = "أمي",
            initials = "أم",
            relationship = "طوارئ",
            priorityLevel = ContactPriority.EMERGENCY,
            allowedInterruptions = listOf(InterruptionType.CALL, InterruptionType.MESSAGE),
            isEnabled = true,
            allowRepeatCallers = true,
            phoneNumber = "01000000001"
        )
        val vipRule = RepeatCallerRule(
            id = "rule_vip",
            name = "المدير",
            phoneNumber = "01099988877",
            requiredCalls = 3,
            windowMinutes = 5,
            isEnabled = true
        )
        val otherNumber = "01233344455"

        // Case 1: Global OFF, per-person VIP requires 3 calls in 5m
        val otherWhileGlobalOff1 = tracker.recordAndEvaluateIncomingCall(
            phoneNumber = otherNumber,
            timestampMillis = baseTime,
            emergencyContacts = listOf(emergencyContact),
            perPersonRules = listOf(vipRule),
            globalEnabled = false,
            globalRequiredCalls = 2,
            globalWindowMinutes = 15
        )
        val otherWhileGlobalOff2 = tracker.recordAndEvaluateIncomingCall(
            phoneNumber = otherNumber,
            timestampMillis = baseTime + 30_000L,
            emergencyContacts = listOf(emergencyContact),
            perPersonRules = listOf(vipRule),
            globalEnabled = false,
            globalRequiredCalls = 2,
            globalWindowMinutes = 15
        )
        assertFalse(otherWhileGlobalOff1.shouldAllowBypass)
        assertFalse(otherWhileGlobalOff2.shouldAllowBypass)
        assertEquals(RepeatCallRuleSource.DISABLED, otherWhileGlobalOff2.source)

        // Emergency contact still bypasses on the 1st call
        val emergencyFirst = tracker.recordAndEvaluateIncomingCall(
            phoneNumber = "+20 100 000 0001",
            timestampMillis = baseTime + 40_000L,
            emergencyContacts = listOf(emergencyContact),
            perPersonRules = listOf(vipRule),
            globalEnabled = false,
            globalRequiredCalls = 2,
            globalWindowMinutes = 15
        )
        assertTrue(emergencyFirst.shouldAllowBypass)
        assertEquals(RepeatCallRuleSource.EMERGENCY_CONTACT, emergencyFirst.source)

        // VIP reaches 2 calls while global is ON at (2, 15): still blocked because per-person rule requires 3!
        val vip1 = tracker.recordAndEvaluateIncomingCall(
            phoneNumber = vipRule.phoneNumber,
            timestampMillis = baseTime + 50_000L,
            emergencyContacts = listOf(emergencyContact),
            perPersonRules = listOf(vipRule),
            globalEnabled = true,
            globalRequiredCalls = 2,
            globalWindowMinutes = 15
        )
        val vip2 = tracker.recordAndEvaluateIncomingCall(
            phoneNumber = vipRule.phoneNumber,
            timestampMillis = baseTime + 80_000L,
            emergencyContacts = listOf(emergencyContact),
            perPersonRules = listOf(vipRule),
            globalEnabled = true,
            globalRequiredCalls = 2,
            globalWindowMinutes = 15
        )
        assertFalse(vip1.shouldAllowBypass)
        assertFalse(vip2.shouldAllowBypass)
        assertEquals(3, vip2.requiredCalls)
        assertEquals(RepeatCallRuleSource.PER_PERSON_RULE, vip2.source)

        val vip3 = tracker.recordAndEvaluateIncomingCall(
            phoneNumber = "+20 109 998 8877",
            timestampMillis = baseTime + 110_000L,
            emergencyContacts = listOf(emergencyContact),
            perPersonRules = listOf(vipRule),
            globalEnabled = true,
            globalRequiredCalls = 2,
            globalWindowMinutes = 15
        )
        assertTrue(vip3.shouldAllowBypass)
        assertEquals(RepeatCallRuleSource.PER_PERSON_RULE, vip3.source)
    }

    @Test
    fun `ViewModel persists repeat calls config, per-person rules, and SettingsScreen renders Arabic controls`(): Unit = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val prefs = PreferencesManager(context)
        val repository = SmartCompanionRepository(db, prefs)
        val gateway = FakeRepeatPhoneGateway()
        val store = androidx.lifecycle.ViewModelStore()
        val viewModel = SmartCompanionViewModel(
            repository = repository,
            phoneControlManager = PhoneControlManager(gateway),
            routineBackupStore = InMemoryRoutineStateBackupStore(),
            alarmScheduler = NoOpRoutineAlarmScheduler(),
            repeatCallHistoryStore = InMemoryRepeatCallHistoryStore()
        )
        val job = launch(Dispatchers.Unconfined) { viewModel.uiState.collect {} }

        try {
            prefs.setLanguage(AppLanguage.ARABIC)
            awaitMainLooper { viewModel.uiState.value.isReady && viewModel.uiState.value.preferences.language == AppLanguage.ARABIC }

            // Configure global N=3, M=20 -> omits OS repeat callers
            viewModel.setRepeatCallsConfig(enabled = true, requiredCalls = 3, windowMinutes = 20)
            awaitMainLooper {
                viewModel.uiState.value.preferences.repeatCallsEnabled &&
                    viewModel.uiState.value.preferences.repeatCallCount == 3 &&
                    viewModel.uiState.value.preferences.repeatCallWindowMinutes == 20
            }
            assertFalse(gateway.repeatCallersAllowed)

            // Configure global N=2, M=15 -> enables OS repeat callers
            viewModel.setRepeatCallsConfig(enabled = true, requiredCalls = 2, windowMinutes = 15)
            awaitMainLooper {
                viewModel.uiState.value.preferences.repeatCallCount == 2 &&
                    viewModel.uiState.value.preferences.repeatCallWindowMinutes == 15
            }
            assertTrue(gateway.repeatCallersAllowed)

            // Save a per-person rule and verify duplicate number is rejected cleanly
            assertTrue(
                viewModel.saveRepeatCallerRuleChecked(
                    RepeatCallerRule(
                        id = "rp_test_1",
                        name = "سارة",
                        phoneNumber = "01077788899",
                        requiredCalls = 4,
                        windowMinutes = 10
                    )
                )
            )
            assertFalse(
                viewModel.saveRepeatCallerRuleChecked(
                    RepeatCallerRule(
                        id = "rp_test_dup",
                        name = "سارة ٢",
                        phoneNumber = "+20 107 778 8899",
                        requiredCalls = 2,
                        windowMinutes = 15
                    )
                )
            )
            awaitMainLooper { viewModel.uiState.value.repeatCallerRules.size == 1 }

            val arStrings = stringsFor(AppLanguage.ARABIC)
            val uiState = SmartCompanionUiState(
                isReady = true,
                preferences = viewModel.uiState.value.preferences,
                repeatCallerRules = viewModel.uiState.value.repeatCallerRules
            )
            composeRule.setContent {
                SmartCompanionTheme(language = AppLanguage.ARABIC) {
                    SettingsScreen(
                        state = uiState,
                        strings = arStrings,
                        viewModel = viewModel,
                        onNavigate = {}
                    )
                }
            }

            composeRule.onNodeWithTag("settings_list")
                .performScrollToNode(hasTestTag("settings_repeat_calls_card"))
            composeRule.onNodeWithText("المكالمات المتكررة").assertIsDisplayed()
            composeRule.onNodeWithTag("repeat_calls_switch").assertIsDisplayed()
            composeRule.onNodeWithTag("repeat_calls_mode_note").assertIsDisplayed()
            composeRule.onNodeWithTag("add_repeat_person_pick_btn").assertIsDisplayed()
            composeRule.onNodeWithTag("add_repeat_person_manual_btn").assertIsDisplayed()
            composeRule.onNodeWithText("سارة").assertIsDisplayed()

            // Clicking Grant Call Log permission button displays clear Arabic explanation dialog first
            composeRule.onNodeWithTag("settings_list")
                .performScrollToNode(hasTestTag("grant_call_log_perm_btn"))
            composeRule.onNodeWithTag("grant_call_log_perm_btn").performClick()
            composeRule.onNodeWithTag("call_log_rationale_dialog").assertIsDisplayed()
            composeRule.onNodeWithTag("confirm_call_log_perm_btn").assertIsDisplayed()
        } finally {
            job.cancel()
            store.clear()
            prefs.resetDefaults(preserveOnboarding = false)
            db.close()
        }
    }
}
