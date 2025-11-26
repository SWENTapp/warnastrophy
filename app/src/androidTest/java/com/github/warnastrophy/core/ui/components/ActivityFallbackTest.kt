package com.github.warnastrophy.core.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.warnastrophy.R
import com.github.warnastrophy.core.ui.util.BaseAndroidComposeTest
import org.junit.Test
import org.junit.runner.RunWith

/** Instrumented tests for the [ActivityFallback] composable */
@RunWith(AndroidJUnit4::class)
class ActivityFallbackTest : BaseAndroidComposeTest() {

  @Test
  fun activityFallback_displaysDefaultMessage() {
    composeTestRule.setContent { ActivityFallback() }

    val expectedDefaultMessage =
        composeTestRule.activity.getString(R.string.no_activity_fallback_message)
    composeTestRule.onNodeWithTag(FALLBACK_ACTIVITY_ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithText(expectedDefaultMessage).assertIsDisplayed()
  }

  @Test
  fun activityFallback_displaysCustomMessage() {
    val customMessage = "This is a custom error message."

    composeTestRule.setContent { ActivityFallback(message = customMessage) }

    composeTestRule.onNodeWithTag(FALLBACK_ACTIVITY_ERROR).assertIsDisplayed()
    composeTestRule.onNodeWithText(customMessage).assertIsDisplayed()
  }
}
