package com.github.warnastrophy.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.warnastrophy.MainActivity
import com.github.warnastrophy.core.ui.navigation.NavigationTestTags
import org.junit.Rule
import org.junit.Test

class NavigationE2ETest {
  @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun testTagsAreCorrectlySet() {
    composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAV).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_HOME).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_MAP).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_PROFILE).assertIsDisplayed()
  }

  @Test
  fun startsOnHome_bottomNavVisible() {
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAV).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertIsDisplayed()
        .assertTextContains("Home", ignoreCase = true)
  }

  @Test
  fun navigate_Home_to_Map_and_back() {
    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_MAP).performClick()

    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAV).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains("Map", ignoreCase = true)

    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_HOME).performClick()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains("Home", ignoreCase = true)
  }

  @Test
  fun can_visit_all_tabs_in_sequence() {
    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_MAP).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_PROFILE).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_HOME).performClick()

    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains("Home", ignoreCase = true)
    composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAV).assertIsDisplayed()
  }

  @Test
  fun navigate_to_Profile_then_back_to_Home() {
    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_PROFILE).performClick()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains("Profile", ignoreCase = true)

    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_HOME).performClick()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains("Home", ignoreCase = true)
  }

  @Test
  fun navigate_to_contact_list_then_back_to_Home() {
    // composeTestRule.setContent { WarnastrophyApp() }

    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_PROFILE).performClick()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains("Profile", ignoreCase = true)

    composeTestRule.onNodeWithTag(NavigationTestTags.CONTACT_LIST).performClick()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains("Contact List", ignoreCase = true)

    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_HOME).performClick()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains("Home", ignoreCase = true)
  }

  @Test
  fun navigate_to_health_card_then_back_to_Home() {
    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_PROFILE).performClick()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains("Profile", ignoreCase = true)

    composeTestRule.onNodeWithTag(NavigationTestTags.HEALTH_CARD).performClick()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains("Emergency Card", ignoreCase = true)

    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_HOME).performClick()
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains("Home", ignoreCase = true)
  }
}
