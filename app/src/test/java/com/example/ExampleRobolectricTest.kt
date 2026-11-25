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
    fun `onboarding skip shows 5 primary tabs and More hub routes to settings and arabic rtl`() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("onboarding_skip_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("onboarding_skip_button").performClick()

        // Verify the 5 primary bottom navigation items are visible
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("nav_home").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("nav_home").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_routines").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_assistant").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_contacts").assertIsDisplayed()
        composeRule.onNodeWithTag("nav_insights").assertIsDisplayed()

        // Navigate Home -> More -> Settings
        composeRule.onNodeWithTag("open_more_button").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("more_hub_list").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("more_hub_list").performScrollToNode(hasTestTag("more_item_settings"))
        composeRule.onNodeWithTag("more_item_settings").performClick()

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
