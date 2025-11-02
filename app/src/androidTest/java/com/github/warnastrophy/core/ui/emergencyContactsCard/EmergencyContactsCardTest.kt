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

  @Test
  fun emergencyContactsCard_rendersBasicInfo() {
    composeTestRule.setContent { MaterialTheme { EmergencyContactsCard() } }

    // The card itself should be on screen
    composeTestRule
        .onNodeWithTag(EmergencyContactsTestTags.CARD, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

    // Title text exists and is displayed
    composeTestRule
        .onNodeWithTag(EmergencyContactsTestTags.TITLE, useUnmergedTree = true)
        .assertIsDisplayed()

    // Subtitle text exists and is displayed
    composeTestRule
        .onNodeWithTag(EmergencyContactsTestTags.SUBTITLE, useUnmergedTree = true)
        .assertIsDisplayed()

    // Button wrapper exists and is displayed
    composeTestRule
        .onNodeWithTag(EmergencyContactsTestTags.OPEN_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()

    // And because the button inside says "Open", we can also assert its label
    composeTestRule.onNodeWithText("Open", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun emergencyContactsCard_respectsMinHeight() {
    composeTestRule.setContent { MaterialTheme { EmergencyContactsCard() } }

    // The card should be at least 140.dp tall (the minHeight we passed to StandardDashboardCard)
    composeTestRule
        .onNodeWithTag(EmergencyContactsTestTags.CARD, useUnmergedTree = true)
        .assertHeightIsAtLeast(140.dp)
  }
}
