package com.github.warnastrophy.core.ui.dashboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.github.warnastrophy.core.ui.util.BaseAndroidComposeTest
import org.junit.Test

class HealthCardPreviewTest : BaseAndroidComposeTest() {

  @Test
  fun healthCard_rendersBasicInfo() {
    composeTestRule.setContent { MaterialTheme { HealthCardPreview() } }

    // Card container exists and is visible
    composeTestRule
        .onNodeWithTag(HealthCardPreviewTestTags.CARD, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

    // Title "Health" is shown
    composeTestRule
        .onNodeWithTag(HealthCardPreviewTestTags.TITLE, useUnmergedTree = true)
        .assertIsDisplayed()

    // Subtitle / description is shown
    composeTestRule
        .onNodeWithTag(HealthCardPreviewTestTags.SUBTITLE, useUnmergedTree = true)
        .assertIsDisplayed()

    // The Open button container is present
    composeTestRule
        .onNodeWithTag(HealthCardPreviewTestTags.OPEN_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()

    // And we also assert the "Open" text itself for safety
    composeTestRule.onNodeWithText("Open", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun healthCard_respectsMinHeight() {
    composeTestRule.setContent { MaterialTheme { HealthCardPreview() } }

    // The card should be at least 140.dp tall because we pass minHeight = 140.dp
    composeTestRule
        .onNodeWithTag(HealthCardPreviewTestTags.CARD, useUnmergedTree = true)
        .assertHeightIsAtLeast(140.dp)
  }
}
