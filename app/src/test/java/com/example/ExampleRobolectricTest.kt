package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Smart Companion", appName)
    }

    @Test
    fun `4 bottom tabs, simplified home cards, 2-tap merged routes, and arabic rtl replay onboarding`() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("onboarding_skip_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("onboarding_skip_button").performClick()

        // Verify strictly 4 bottom navigation tabs: Home, Routines, Assistant, Settings
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("nav_home").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("nav_home").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_routines").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_assistant").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_settings").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithTag("nav_contacts").fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithTag("nav_insights").fetchSemanticsNodes().isEmpty())

        // Verify Home screen displays the 3 primary cards
        composeRule.onNodeWithTag("home_card_assistant").assertIsDisplayed()
        composeRule.onNodeWithTag("home_card_routines").assertIsDisplayed()
        composeRule.onNodeWithTag("home_screen_list").performScrollToNode(hasTestTag("home_card_contacts"))
        composeRule.onNodeWithTag("home_card_contacts").assertIsDisplayed()

        // Navigate Home -> Emergency contacts (1 tap)
        composeRule.onNodeWithTag("home_card_contacts").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("top_back_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("top_back_button").performClick()

        // Navigate Settings -> More -> Activity (2 taps total)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("nav_settings").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_list").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("settings_activity_row"))
        composeRule.onNodeWithTag("settings_activity_row").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("activity_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("activity_tab_HISTORY").assertIsDisplayed()
        composeRule.onNodeWithTag("activity_tab_NOTIFICATIONS").assertIsDisplayed()
        composeRule.onNodeWithTag("top_back_button").performClick()

        // Navigate Settings -> Advanced (2 taps total)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_list").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("settings_advanced_row"))
        composeRule.onNodeWithTag("settings_advanced_row").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("advanced_screen").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("advanced_tab_CONTEXT").assertIsDisplayed()
        composeRule.onNodeWithTag("advanced_tab_PLAYGROUND").assertIsDisplayed()
        composeRule.onNodeWithTag("top_back_button").performClick()

        // Switch to Arabic RTL and replay onboarding
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("settings_list").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("lang_ARABIC"))
        composeRule.onNodeWithTag("lang_ARABIC").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("settings_list").performScrollToNode(hasTestTag("settings_replay_onboarding_btn"))
        composeRule.onNodeWithText("عرض دليل التطبيق التعريفي").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_replay_onboarding_btn").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("onboarding_skip_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("onboarding_skip_button").assertIsDisplayed()
    }
}
