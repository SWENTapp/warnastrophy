package com.github.warnastrophy.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.warnastrophy.core.data.repository.ContactRepositoryProvider
import com.github.warnastrophy.core.data.repository.HealthCardRepositoryProvider
import com.github.warnastrophy.core.ui.features.dashboard.DashboardHealthCardTestTags
import com.github.warnastrophy.core.ui.features.dashboard.DashboardScreenTestTags
import com.github.warnastrophy.core.ui.navigation.NavigationTestTags
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class EndToEndM2Test : EndToEndUtils() {

  @Before
  override fun setUp() {
    super.setUp()

    val context = composeTestRule.activity.applicationContext
    ContactRepositoryProvider.init(context)
    repository = ContactRepositoryProvider.repository
    HealthCardRepositoryProvider.useLocalEncrypted(context)
    runBlocking { runCatching { HealthCardRepositoryProvider.repository.deleteMyHealthCard() } }
  }

  @Test
  fun testTagsAreCorrectlySet() {
    setContent()
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAV).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_DASHBOARD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_MAP).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_PROFILE).assertIsDisplayed()

    // Check Dashboard tags
    composeTestRule.onNodeWithTag(DashboardScreenTestTags.ROOT_SCROLL).assertIsDisplayed()
    composeTestRule.onNodeWithTag(DashboardScreenTestTags.LATEST_NEWS_SECTION).assertIsDisplayed()
    composeTestRule.onNodeWithTag(DashboardScreenTestTags.MAP_PREVIEW_SECTION).assertIsDisplayed()
    composeTestRule.onNodeWithTag(DashboardScreenTestTags.ROW_TWO_SMALL_CARDS).assertIsDisplayed()
  }

  /**
   * E2E test to verify the health card flow.
   * - Starts on the Dashboard screen.
   * - Clicks on the Health Card widget.
   * - Fills and saves a new Health Card.
   * - Navigates to the Profile screen.
   * - Opens the Health Card from the profile.
   * - Updates the Health Card content and saves it.
   * - Deletes the Health Card.
   */
  @Test
  fun create_and_update_health_card_flow() {
    setContent()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains("Dashboard", ignoreCase = true)

    composeTestRule.onNodeWithTag(DashboardHealthCardTestTags.CARD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(DashboardHealthCardTestTags.CARD).performClick()

    createHealthCard(fullName = "John Doe", allergies = "Peanuts")

    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_PROFILE).performClick()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains("Profile", ignoreCase = true)

    composeTestRule.onNodeWithTag(NavigationTestTags.HEALTH_CARD).performClick()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains("Emergency Card", ignoreCase = true)

    editHealthCard(fullName = "Johnathan Smith", allergies = "Peanuts, Shellfish")

    composeTestRule.waitForIdle()

    deleteHealthCard()
  }
}
