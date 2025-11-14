package com.github.warnastrophy.core.ui.latestNewsCard

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.github.warnastrophy.core.ui.features.dashboard.LatestNewsCard
import com.github.warnastrophy.core.ui.features.dashboard.LatestNewsTestTags
import com.github.warnastrophy.core.ui.map.HazardServiceMock
import com.github.warnastrophy.core.ui.util.BaseAndroidComposeTest
import com.github.warnastrophy.core.ui.util.hazards
import com.github.warnastrophy.core.util.formatDate
import com.google.android.gms.maps.MapsInitializer
import org.junit.Before
import org.junit.Test

class LatestNewsCardTest : BaseAndroidComposeTest() {

  /* Verify that the LatestNewsCard renders its Card and Header components:
   * - Card
   * - Header Row
   * - Header Title
   * - Header Timestamp
   */
  private lateinit var hazardService: HazardServiceMock

  @Before
  override fun setUp() {
    super.setUp()
    hazardService = HazardServiceMock()
    hazardService.setHazards(hazards)
    val context = ApplicationProvider.getApplicationContext<Context>()
    MapsInitializer.initialize(context)
  }

  @Test
  fun latestNewsCard_rendersCardAndHeader() {
    composeTestRule.setContent { MaterialTheme { LatestNewsCard(hazardService) } }

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

    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.RIGHT_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.LEFT_BUTTON, useUnmergedTree = true)
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.LINK, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  /* Verify that the LatestNewsCard shows the headline, body, and image */
  @Test
  fun latestNewsCard_showsHeadlineBodyAndImage() {
    composeTestRule.setContent { MaterialTheme { LatestNewsCard(hazardService) } }

    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.HEADLINE, useUnmergedTree = true)
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.BODY, useUnmergedTree = true)
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.IMAGE_BOX, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun latestNewsCard_rightButton_updatesHeadline() {
    composeTestRule.setContent { MaterialTheme { LatestNewsCard(hazardService) } }

    // Click the right button to go to the next hazard
    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.RIGHT_BUTTON, useUnmergedTree = true)
        .performClick()

    // Headline should update to the second hazard's description
    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.HEADLINE, useUnmergedTree = true)
        .assertIsDisplayed()
        .assert(hasText(hazards[1].description!!))

    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.BODY, useUnmergedTree = true)
        .assertIsDisplayed()
        .assert(hasText(hazards[1].severityText!!))

    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.HEADER_TIMESTAMP, useUnmergedTree = true)
        .assertIsDisplayed()
        .assert(hasText(formatDate(hazards[1].date!!)))
    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.LINK, useUnmergedTree = true)
        .assertIsDisplayed()
        .assert(hasText("read"))

    // Click the right button to go to the next hazard
    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.RIGHT_BUTTON, useUnmergedTree = true)
        .performClick()

    // Headline should update to the second hazard's description
    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.HEADLINE, useUnmergedTree = true)
        .assertIsDisplayed()
        .assert(hasText(hazards[2].description!!))

    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.BODY, useUnmergedTree = true)
        .assertIsDisplayed()
        .assert(hasText(hazards[2].severityText!!))

    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.HEADER_TIMESTAMP, useUnmergedTree = true)
        .assertIsDisplayed()
        .assert(hasText(formatDate(hazards[2].date!!)))
    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.LINK, useUnmergedTree = true)
        .assertIsDisplayed()
        .assert(hasText("read"))
  }

  @Test
  fun latestNewsCard_leftButton_updatesHeadline() {
    composeTestRule.setContent { MaterialTheme { LatestNewsCard(hazardService) } }

    // Click the right button to go to the next hazard
    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.LEFT_BUTTON, useUnmergedTree = true)
        .performClick()

    // Headline should update to the second hazard's description
    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.HEADLINE, useUnmergedTree = true)
        .assertIsDisplayed()
        .assert(hasText(hazards[2].description!!))

    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.BODY, useUnmergedTree = true)
        .assertIsDisplayed()
        .assert(hasText(hazards[2].severityText!!))

    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.HEADER_TIMESTAMP, useUnmergedTree = true)
        .assertIsDisplayed()
        .assert(hasText(formatDate(hazards[2].date!!)))
    composeTestRule
        .onNodeWithTag(LatestNewsTestTags.LINK, useUnmergedTree = true)
        .assertIsDisplayed()
        .assert(hasText("read"))
  }
}
