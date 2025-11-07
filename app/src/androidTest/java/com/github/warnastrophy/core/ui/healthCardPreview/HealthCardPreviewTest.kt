package com.github.warnastrophy.core.ui.dashboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.github.warnastrophy.core.ui.healthCardPreview.HealthCardPreview
import com.github.warnastrophy.core.ui.healthCardPreview.HealthCardPreviewTestTags
import com.github.warnastrophy.core.ui.healthcard.HealthCardTestTags
import com.github.warnastrophy.core.ui.util.BaseAndroidComposeTest
import org.junit.Test

class HealthCardPreviewTest : BaseAndroidComposeTest() {

  /* Verify that the HealthCardPreview renders its basic information:
   * - Card
   * - Title
   * - Subtitle
   * - Open Button
   */
  @Test
  fun healthCard_rendersBasicInfo() {
    composeTestRule.setContent { MaterialTheme { HealthCardPreview() } }

    composeTestRule
        .onNodeWithTag(HealthCardPreviewTestTags.CARD, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(HealthCardPreviewTestTags.TITLE, useUnmergedTree = true)
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(HealthCardPreviewTestTags.SUBTITLE, useUnmergedTree = true)
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(HealthCardPreviewTestTags.OPEN_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()

    composeTestRule.onNodeWithText("Open", useUnmergedTree = true).assertIsDisplayed()
  }

  /* Verify that the HealthCardPreview respects the minimum height of 140.dp */
  @Test
  fun healthCard_respectsMinHeight() {
    composeTestRule.setContent { MaterialTheme { HealthCardPreview() } }

    composeTestRule
        .onNodeWithTag(HealthCardPreviewTestTags.CARD, useUnmergedTree = true)
        .assertHeightIsAtLeast(140.dp)
  }

  /* Verify that clicking the "Open" button on the HealthCardPreview
   * navigates to the HealthCard screen by checking for the presence
   * of a known element on that screen (e.g., Allergies field).
   */
  @Test
  fun healthCard_openButtonWorks() {
    composeTestRule.setContent { MaterialTheme { HealthCardPreview() } }

    composeTestRule
        .onNodeWithTag(HealthCardPreviewTestTags.OPEN_BUTTON, useUnmergedTree = true)
        .performClick()

    composeTestRule.onNodeWithTag(HealthCardTestTags.ALLERGIES_FIELD).isDisplayed()
  }
}
