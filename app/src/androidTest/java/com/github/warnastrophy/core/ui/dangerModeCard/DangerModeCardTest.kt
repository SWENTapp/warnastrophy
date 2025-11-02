package com.github.warnastrophy.core.ui.dangerModeCard

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.warnastrophy.core.ui.util.BaseAndroidComposeTest
import org.junit.Test

class DangerModeCardTest : BaseAndroidComposeTest() {

  @Test
  fun dangerModeCard_rendersRootElements() {
    composeTestRule.setContent { MaterialTheme { DangerModeCard() } }

    // Card container exists
    composeTestRule
        .onNodeWithTag(DangerModeTestTags.CARD, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

    // Title text is visible
    composeTestRule
        .onNodeWithTag(DangerModeTestTags.TITLE, useUnmergedTree = true)
        .assertIsDisplayed()

    // Mode label shows "Climbing mode"
    composeTestRule
        .onNodeWithTag(DangerModeTestTags.MODE_LABEL, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertExists()

    // Sends row exists
    composeTestRule
        .onNodeWithTag(DangerModeTestTags.SENDS_ROW, useUnmergedTree = true)
        .assertIsDisplayed()

    // Color row exists
    composeTestRule
        .onNodeWithTag(DangerModeTestTags.COLOR_ROW, useUnmergedTree = true)
        .assertIsDisplayed()

    // "Open" CTA exists
    composeTestRule
        .onNodeWithTag(DangerModeTestTags.OPEN_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun dangerModeCard_switch_togglesState() {
    composeTestRule.setContent { MaterialTheme { DangerModeCard() } }

    val switchNode =
        composeTestRule.onNodeWithTag(DangerModeTestTags.SWITCH, useUnmergedTree = true)

    // Initially off
    switchNode.assertIsOff()

    // Tap it
    switchNode.performClick()

    // Now on
    switchNode.assertIsOn()
  }

  @Test
  fun dangerModeCard_showsActionButtons() {
    composeTestRule.setContent { MaterialTheme { DangerModeCard() } }

    // We can assert by text because those are visible labels
    composeTestRule.onNodeWithText("Call").assertIsDisplayed()
    composeTestRule.onNodeWithText("SMS").assertIsDisplayed()
    composeTestRule.onNodeWithText("Location").assertIsDisplayed()
  }
}
