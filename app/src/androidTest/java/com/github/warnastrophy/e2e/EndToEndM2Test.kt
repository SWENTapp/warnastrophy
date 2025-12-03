package com.github.warnastrophy.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import com.github.warnastrophy.core.data.provider.ActivityRepositoryProvider
import com.github.warnastrophy.core.data.provider.ContactRepositoryProvider
import com.github.warnastrophy.core.data.provider.HealthCardRepositoryProvider
import com.github.warnastrophy.core.data.service.StateManagerService
import com.github.warnastrophy.core.ui.features.dashboard.DashboardScreenTestTags
import com.github.warnastrophy.core.ui.navigation.NavigationTestTags
import org.junit.Before
import org.junit.Test

class EndToEndM2Test : EndToEndUtils() {

  @Before
  override fun setUp() {
    super.setUp()

    val context = composeTestRule.activity.applicationContext
    ContactRepositoryProvider.initLocal(context)
    ActivityRepositoryProvider.init(context)
    contactRepository = ContactRepositoryProvider.repository
    activityRepository = ActivityRepositoryProvider.repository
    StateManagerService.init(context)
    HealthCardRepositoryProvider.useLocalEncrypted(context)
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

  @Test
  fun create_edit_and_delete_contact() {
    setContent()
    addNewContact()
    editContact(saveChanges = false)
    editContact(saveChanges = true)
    deleteContact()
  }
}
