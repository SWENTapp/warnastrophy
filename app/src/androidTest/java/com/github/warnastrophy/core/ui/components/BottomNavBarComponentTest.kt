package com.github.warnastrophy.core.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.compose.rememberNavController
import com.github.warnastrophy.core.ui.navigation.BottomNavigationBar
import com.github.warnastrophy.core.ui.navigation.BottomNavigationBarPreview
import com.github.warnastrophy.core.ui.navigation.NavigationTestTags
import com.github.warnastrophy.core.ui.navigation.Screen
import org.junit.Rule
import org.junit.Test

class BottomNavBarComponentTest {

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun testBottomNavPreview() {
    composeTestRule.setContent { BottomNavigationBarPreview() }
    composeTestRule
        .onNodeWithTag(NavigationTestTags.BOTTOM_NAV_PREVIEW, useUnmergedTree = true)
        .assertExists()
  }

  @Test
  fun bottomBar_showsTitle_whenHasBottomBarTrue() {
    composeTestRule.setContent {
      MaterialTheme { BottomNavigationBar(Screen.Home, navController = rememberNavController()) }
    }

    val expected = composeTestRule.activity.getString(Screen.Home.title)
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TAB_HOME)
        .assertIsDisplayed()
        .assertTextEquals(expected)
  }
}
