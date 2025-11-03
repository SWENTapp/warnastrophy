package com.github.warnastrophy.core.ui.dashboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.github.warnastrophy.core.ui.util.BaseAndroidComposeTest
import org.junit.Test

class EmergencyContactsCardTest : BaseAndroidComposeTest() {

  /* Verify that the EmergencyContactsCard renders its basic information:
   * - Card
   * - Title
   * - Subtitle
   * - Open Button
   */
  @Test
  fun emergencyContactsCard_rendersBasicInfo() {
    composeTestRule.setContent { MaterialTheme { EmergencyContactsCard() } }

    composeTestRule
        .onNodeWithTag(EmergencyContactsTestTags.CARD, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(EmergencyContactsTestTags.TITLE, useUnmergedTree = true)
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(EmergencyContactsTestTags.SUBTITLE, useUnmergedTree = true)
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(EmergencyContactsTestTags.OPEN_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()

    composeTestRule.onNodeWithText("Open", useUnmergedTree = true).assertIsDisplayed()
  }

  /* Verify that the EmergencyContactsCard respects the minimum height of 140.dp */
  @Test
  fun emergencyContactsCard_respectsMinHeight() {
    composeTestRule.setContent { MaterialTheme { EmergencyContactsCard() } }

    composeTestRule
        .onNodeWithTag(EmergencyContactsTestTags.CARD, useUnmergedTree = true)
        .assertHeightIsAtLeast(140.dp)
  }
}
