package com.github.warnastrophy.core.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import com.github.warnastrophy.core.ui.util.BaseSimpleComposeTest
import org.junit.Before
import org.junit.Test

/**
 * UI test for the [Loading] composable. This test verifies that the composable correctly displays
 * its contents.
 */
class LoadingComponentTest : BaseSimpleComposeTest() {
  @Before
  override fun setUp() {
    super.setUp()
    composeTestRule.setContent { Loading() }
  }

  @Test
  fun loadingComposable_whenDisplayed_showsCircularProgressIndicator() {
    composeTestRule.onNodeWithTag(LoadingTestTags.LOADING_INDICATOR).assertIsDisplayed()
  }
}
