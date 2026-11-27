package com.example

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.automation.InMemoryRoutineStateBackupStore
import com.example.automation.NoOpRoutineAlarmScheduler
import com.example.data.PreferencesManager
import com.example.data.local.AppDatabase
import com.example.models.AppLanguage
import com.example.models.ContactPriority
import com.example.models.InterruptionType
import com.example.models.PriorityContact
import com.example.models.SoundMode
import com.example.repositories.SmartCompanionRepository
import com.example.simulation.PhoneControlGateway
import com.example.simulation.PhoneControlManager
import com.example.ui.screens.ContactsScreen
import com.example.ui.theme.SmartCompanionTheme
import com.example.utils.stringsFor
import com.example.viewmodels.SmartCompanionUiState
import com.example.viewmodels.SmartCompanionViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EmergencyContactsUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private class InertGateway : PhoneControlGateway {
        override fun isNotificationPolicyAccessGranted(): Boolean = true
        override fun getCurrentRingerMode(): SoundMode = SoundMode.SOUND
        override fun setRingerMode(mode: SoundMode): Boolean = true
        override fun isDndEnabled(): Boolean = false
        override fun isStarredContactsAllowedInDnd(): Boolean = true
        override fun setDoNotDisturb(enabled: Boolean, allowStarredContacts: Boolean): Boolean = true
        override fun createDndAccessSettingsIntent(context: Context): Intent =
            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
    }

    @Test
    fun `ContactsScreen shows Choose from phone contacts button, Arabic rationale dialog, manual fallback, and هيوصلك دايما badge`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val prefs = PreferencesManager(context)
        val repository = SmartCompanionRepository(db, prefs)
        val store = androidx.lifecycle.ViewModelStore()
        val viewModel = androidx.lifecycle.ViewModelProvider(
            store,
            SmartCompanionViewModel.Factory(
                repository = repository,
                phoneControlManager = PhoneControlManager(InertGateway()),
                routineBackupStore = InMemoryRoutineStateBackupStore(),
                alarmScheduler = NoOpRoutineAlarmScheduler()
            )
        )[SmartCompanionViewModel::class.java]
        val emergencyContact = PriorityContact(
            id = "c_em_ui_1",
            name = "أمي",
            initials = "أ",
            relationship = "العائلة",
            priorityLevel = ContactPriority.EMERGENCY,
            allowedInterruptions = listOf(InterruptionType.CALL, InterruptionType.MESSAGE),
            isEnabled = true,
            allowRepeatCallers = true,
            phoneNumber = "01001234567"
        )
        val uiState = SmartCompanionUiState(
            isReady = true,
            contacts = listOf(emergencyContact)
        )
        val arStrings = stringsFor(AppLanguage.ARABIC)

        try {
            composeRule.setContent {
                SmartCompanionTheme(language = AppLanguage.ARABIC) {
                    ContactsScreen(
                        state = uiState,
                        strings = arStrings,
                        viewModel = viewModel
                    )
                }
            }

            composeRule.onNodeWithTag("choose_phone_contact_btn").assertIsDisplayed()
            composeRule.onNodeWithTag("add_contact_manual_btn").assertIsDisplayed()

            // Scroll to the emergency contact card and verify phone number and "هيوصلك دايما" badge
            composeRule.onNodeWithTag("contacts_list")
                .performScrollToNode(hasTestTag("contact_card_c_em_ui_1"))
            composeRule.onNodeWithTag("emergency_always_badge_c_em_ui_1").assertIsDisplayed()
            composeRule.onNodeWithText("هيوصلك دايما").assertIsDisplayed()
            composeRule.onNodeWithText("01001234567").assertIsDisplayed()

            // Tapping "Choose from phone contacts" without READ_CONTACTS shows the Arabic explanation dialog first
            composeRule.onNodeWithTag("contacts_list")
                .performScrollToNode(hasTestTag("choose_phone_contact_btn"))
            composeRule.onNodeWithTag("choose_phone_contact_btn").performClick()
            composeRule.onNodeWithTag("read_contacts_rationale_dialog").assertIsDisplayed()
            composeRule.onNodeWithTag("confirm_read_contacts_permission_btn").assertIsDisplayed()

            // Dismiss to manual entry keeps manual entry working
            composeRule.onNodeWithTag("fallback_manual_contact_btn").performClick()
            composeRule.onNodeWithTag("contact_name_input").performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithTag("contact_phone_input").performScrollTo().assertIsDisplayed()
        } finally {
            store.clear()
            runBlocking { prefs.resetDefaults(preserveOnboarding = false) }
            db.close()
        }
    }
}
