package com.github.warnastrophy.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.warnastrophy.core.util.BaseSimpleComposeTest
import junit.framework.TestCase
import org.junit.Test

class PermissionRequestCardTest : BaseSimpleComposeTest() {
  private val title = "Location permission"
  private val message = "We use your location to center the map and display nearby hazards."

  /**
   * Given showAllowButton is true, When the PermissionRequestCard is rendered, Then it displays the
   * title, message, both buttons (and their labels), And clicking each button invokes the
   * respective callback.
   *
   * Covers: default modifier, Surface(CARD), "Allow location", "Open settings".
   */
  @Test
  fun renders_card_title_message_and_both_buttons_whenAllow_true_and_invoke_callbacks() {
    var allowClicked = false
    var settingsClicked = false

    composeTestRule.setContent {
      MaterialTheme {
        PermissionRequestCard(
            title = title,
            message = message,
            showAllowButton = true,
            onAllowClick = { allowClicked = true },
            onOpenSettingsClick = { settingsClicked = true })
      }
    }

    // Card (Surface) + text visible
    composeTestRule.onNodeWithTag(PermissionUiTags.CARD, useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText(title, useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText(message, useUnmergedTree = true).assertIsDisplayed()

    // Buttons visible (by tag) and their labels visible (by text)
    composeTestRule
        .onNodeWithTag(PermissionUiTags.BTN_ALLOW, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Allow location", useUnmergedTree = true).assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(PermissionUiTags.BTN_SETTINGS, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Open settings", useUnmergedTree = true).assertIsDisplayed()

    // Click Allow
    composeTestRule.onNodeWithTag(PermissionUiTags.BTN_ALLOW, useUnmergedTree = true).performClick()
    composeTestRule.runOnIdle { TestCase.assertTrue("Allow callback should fire", allowClicked) }

    // Click Settings
    composeTestRule
        .onNodeWithTag(PermissionUiTags.BTN_SETTINGS, useUnmergedTree = true)
        .performClick()
    composeTestRule.runOnIdle {
      TestCase.assertTrue("Settings callback should fire", settingsClicked)
    }
  }
}
