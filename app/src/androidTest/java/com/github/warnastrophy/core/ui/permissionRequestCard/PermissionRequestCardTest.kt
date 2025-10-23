package com.github.warnastrophy.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PermissionRequestCardTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val TITLE = "Location permission"
  private val MESSAGE = "We use your location to center the map and display nearby hazards."

  /**
   * Given showAllowButton is true, When the PermissionRequestCard is rendered, Then it displays the
   * title, message, both buttons, And clicking each button invokes the respective callback.
   */
  @Test
  fun renders_card_title_message_and_both_buttons_when_showAllow_true_and_invokes_callbacks() {
    var allowClicked = false
    var settingsClicked = false

    composeTestRule.setContent {
      MaterialTheme {
        PermissionRequestCard(
            title = TITLE,
            message = MESSAGE,
            showAllowButton = true,
            onAllowClick = { allowClicked = true },
            onOpenSettingsClick = { settingsClicked = true })
      }
    }

    // Card + text visible
    composeTestRule.onNodeWithTag(PermissionUiTags.CARD).assertIsDisplayed()
    composeTestRule.onNodeWithText(TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithText(MESSAGE).assertIsDisplayed()

    // Buttons visible
    composeTestRule.onNodeWithTag(PermissionUiTags.BTN_ALLOW).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PermissionUiTags.BTN_SETTINGS).assertIsDisplayed()

    // Click Allow
    composeTestRule.onNodeWithTag(PermissionUiTags.BTN_ALLOW).performClick()
    composeTestRule.runOnIdle { assertTrue("Allow callback should fire", allowClicked) }

    // Click Settings
    composeTestRule.onNodeWithTag(PermissionUiTags.BTN_SETTINGS).performClick()
    composeTestRule.runOnIdle { assertTrue("Settings callback should fire", settingsClicked) }
  }

  /**
   * Given showAllowButton is false, When the PermissionRequestCard is rendered, Then it displays
   * the title, message, only the Settings button, And clicking the Settings button invokes the
   * respective callback.
   */
  @Test
  fun hides_allow_button_when_showAllow_false_but_keeps_settings_and_texts() {
    var settingsClicked = false

    composeTestRule.setContent {
      MaterialTheme {
        PermissionRequestCard(
            title = TITLE,
            message = MESSAGE,
            showAllowButton = false,
            onAllowClick = { /* no-op */},
            onOpenSettingsClick = { settingsClicked = true })
      }
    }

    // Card + text visible
    composeTestRule.onNodeWithTag(PermissionUiTags.CARD).assertIsDisplayed()
    composeTestRule.onNodeWithText(TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithText(MESSAGE).assertIsDisplayed()

    // Allow hidden, Settings visible
    composeTestRule.onNodeWithTag(PermissionUiTags.BTN_ALLOW).assertDoesNotExist()
    composeTestRule.onNodeWithTag(PermissionUiTags.BTN_SETTINGS).assertIsDisplayed()

    // Click Settings
    composeTestRule.onNodeWithTag(PermissionUiTags.BTN_SETTINGS).performClick()
    composeTestRule.runOnIdle { assertTrue("Settings callback should fire", settingsClicked) }
  }

  /**
   * Given a long message, When the PermissionRequestCard is rendered, Then it displays the full
   * message and both buttons correctly.
   */
  @Test
  fun supports_long_message_and_still_shows_expected_controls() {
    val longMsg = buildString {
      append(MESSAGE)
      repeat(8) { append(" Additional information for the user.") }
    }

    composeTestRule.setContent {
      MaterialTheme {
        PermissionRequestCard(
            title = "Location disabled",
            message = longMsg,
            showAllowButton = true,
            onAllowClick = {},
            onOpenSettingsClick = {})
      }
    }

    composeTestRule.onNodeWithTag(PermissionUiTags.CARD).assertIsDisplayed()
    composeTestRule.onNodeWithText("Location disabled").assertIsDisplayed()
    composeTestRule.onNodeWithTag(PermissionUiTags.BTN_ALLOW).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PermissionUiTags.BTN_SETTINGS).assertIsDisplayed()
  }
}
