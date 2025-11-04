package com.github.warnastrophy.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import com.github.warnastrophy.core.ui.navigation.NavigationTestTags
import com.github.warnastrophy.core.ui.navigation.Screen
import com.github.warnastrophy.core.ui.navigation.TopBar
import com.github.warnastrophy.core.ui.navigation.TopBarPreview
import com.github.warnastrophy.core.ui.util.BaseAndroidComposeTest
import org.junit.Test

class TopBarComponentTest : BaseAndroidComposeTest() {

  @Test
  fun testTopBarPreview() {
    composeTestRule.setContent { TopBarPreview() }
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_PREVIEW, useUnmergedTree = true)
        .assertExists()
  }

  @Test
  fun topBar_showsTitle_whenHasTopBarTrue() {
    composeTestRule.setContent { MaterialTheme { TopBar(Screen.Dashboard) } }

    val expected = composeTestRule.activity.getString(Screen.Dashboard.title)
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertIsDisplayed()
        .assertTextEquals(expected)
  }
}
