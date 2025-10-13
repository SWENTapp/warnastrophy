package com.github.warnastrophy.e2e

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.warnastrophy.WarnastrophyApp
import com.github.warnastrophy.core.ui.navigation.NavigationTestTags
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NavigationE2ETest {
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        composeTestRule.setContent {
            WarnastrophyApp()
        }
    }


    @Test
    fun startsOnHome_bottomNavVisible() {
        composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAV).assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
            .assertIsDisplayed()
            .assertTextContains("Home", ignoreCase = true)
    }

    @Test
    fun navigate_Home_to_Map_and_back() {
        composeTestRule.onNodeWithTag(NavigationTestTags.TAB_MAP).performClick()

        composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAV).assertIsDisplayed()
        composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
            .assertTextContains("Map", ignoreCase = true)

        composeTestRule.onNodeWithTag(NavigationTestTags.TAB_HOME).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
            .assertTextContains("Home", ignoreCase = true)
    }

    @Test
    fun navigate_to_Profile_then_back_to_Home() {
        composeTestRule.onNodeWithTag(NavigationTestTags.TAB_PROFILE).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
            .assertTextContains("Profile", ignoreCase = true)

        composeTestRule.onNodeWithTag(NavigationTestTags.TAB_HOME).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
            .assertTextContains("Home", ignoreCase = true)
    }

    @Test
    fun can_visit_all_tabs_in_sequence() {
        composeTestRule.onNodeWithTag(NavigationTestTags.TAB_MAP).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.TAB_PROFILE).performClick()
        composeTestRule.onNodeWithTag(NavigationTestTags.TAB_HOME).performClick()

        composeTestRule.onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
            .assertTextContains("Home", ignoreCase = true)
        composeTestRule.onNodeWithTag(NavigationTestTags.BOTTOM_NAV).assertIsDisplayed()
    }
}