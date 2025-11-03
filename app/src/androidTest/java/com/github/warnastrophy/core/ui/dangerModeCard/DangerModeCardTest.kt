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

  /* Verify that the DangerModeCard renders its root elements:
   * - Card
   * - Title
   * - Mode Label
   * - Sends Row
   * - Color Row
   * - Open Button
   */
  @Test
  fun dangerModeCard_rendersRootElements() {
    composeTestRule.setContent { MaterialTheme { DangerModeCard() } }

    composeTestRule
        .onNodeWithTag(DangerModeTestTags.CARD, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(DangerModeTestTags.TITLE, useUnmergedTree = true)
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(DangerModeTestTags.MODE_LABEL, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertExists()

    composeTestRule
        .onNodeWithTag(DangerModeTestTags.SENDS_ROW, useUnmergedTree = true)
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(DangerModeTestTags.COLOR_ROW, useUnmergedTree = true)
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(DangerModeTestTags.OPEN_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  /* Verify that the DangerModeCard's switch toggles its state when clicked */
  @Test
  fun dangerModeCard_switch_togglesState() {
    composeTestRule.setContent { MaterialTheme { DangerModeCard() } }

    val switchNode =
        composeTestRule.onNodeWithTag(DangerModeTestTags.SWITCH, useUnmergedTree = true)

    switchNode.assertIsOff()

    switchNode.performClick()

    switchNode.assertIsOn()
  }

  /* Verify that the DangerModeCard shows the action buttons: Call, SMS, Location */
  @Test
  fun dangerModeCard_showsActionButtons() {
    composeTestRule.setContent { MaterialTheme { DangerModeCard() } }

    composeTestRule.onNodeWithText("Call").assertIsDisplayed()
    composeTestRule.onNodeWithText("SMS").assertIsDisplayed()
    composeTestRule.onNodeWithText("Location").assertIsDisplayed()
  }
}
