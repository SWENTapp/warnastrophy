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

  @Test
  fun healthCard_respectsMinHeight() {
    composeTestRule.setContent { MaterialTheme { HealthCardPreview() } }

    composeTestRule
        .onNodeWithTag(HealthCardPreviewTestTags.CARD, useUnmergedTree = true)
        .assertHeightIsAtLeast(140.dp)
  }
}
