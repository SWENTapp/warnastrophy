package com.github.warnastrophy.core.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test

/**
 * UI test for the [Loading] composable. This test verifies that the composable correctly displays
 * its contents.
 */
class LoadingComponentTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun loadingComposable_whenDisplayed_showsCircularProgressIndicator() {
    composeTestRule.setContent { Loading() }
    composeTestRule.onNodeWithTag(LoadingTestTags.LOADING_INDICATOR).assertIsDisplayed()
  }
}
