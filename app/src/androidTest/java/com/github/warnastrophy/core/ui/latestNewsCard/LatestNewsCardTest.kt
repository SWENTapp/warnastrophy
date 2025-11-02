package com.github.warnastrophy.core.ui.dashboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.github.warnastrophy.core.ui.util.BaseAndroidComposeTest
import org.junit.Test

class LatestNewsCardTest : BaseAndroidComposeTest() {

  @Test
  fun latestNewsCard_rendersCardAndHeader() {
    composeTestRule.setContent { MaterialTheme { LatestNewsCard() } }

    // Card container exists and is visible
    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.CARD, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

    // Header row (pink area) is present
    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.HEADER_ROW, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

    // Title "Latest news" is shown
    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.HEADER_TITLE, useUnmergedTree = true)
        .assertIsDisplayed()

    // Timestamp / category is shown
    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.HEADER_TIMESTAMP, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun latestNewsCard_showsHeadlineBodyAndImage() {
    composeTestRule.setContent { MaterialTheme { LatestNewsCard() } }

    // Headline text (main news line)
    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.HEADLINE, useUnmergedTree = true)
        .assertIsDisplayed()

    // Body/summary text
    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.BODY, useUnmergedTree = true)
        .assertIsDisplayed()

    // The "Image" placeholder box exists and is visible
    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.IMAGE_BOX, useUnmergedTree = true)
        .assertIsDisplayed()

    // Bonus: verify user-facing strings, in case tags change later
    composeTestRule
        .onNodeWithText("Tropical cyclone just hit Jamaica", useUnmergedTree = true)
        .assertIsDisplayed()

    composeTestRule.onNodeWithText("Image", useUnmergedTree = true).assertIsDisplayed()
  }
}
