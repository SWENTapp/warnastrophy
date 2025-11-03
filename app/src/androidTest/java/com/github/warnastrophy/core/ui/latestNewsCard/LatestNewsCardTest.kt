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

    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.CARD, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.HEADER_ROW, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.HEADER_TITLE, useUnmergedTree = true)
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.HEADER_TIMESTAMP, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun latestNewsCard_showsHeadlineBodyAndImage() {
    composeTestRule.setContent { MaterialTheme { LatestNewsCard() } }

    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.HEADLINE, useUnmergedTree = true)
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.BODY, useUnmergedTree = true)
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.IMAGE_BOX, useUnmergedTree = true)
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithText("Tropical cyclone just hit Jamaica", useUnmergedTree = true)
        .assertIsDisplayed()

    composeTestRule.onNodeWithText("Image", useUnmergedTree = true).assertIsDisplayed()
  }
}
