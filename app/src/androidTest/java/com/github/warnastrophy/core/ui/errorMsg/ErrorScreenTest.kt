package com.github.warnastrophy.core.ui.errorMsg

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.warnastrophy.core.model.Error
import com.github.warnastrophy.core.model.ErrorHandler
import com.github.warnastrophy.core.ui.error.ErrorScreen
import com.github.warnastrophy.core.ui.error.ErrorScreenTestTags
import com.github.warnastrophy.core.ui.navigation.NavigationTestTags
import com.github.warnastrophy.core.ui.navigation.Screen
import com.github.warnastrophy.core.ui.navigation.TopBar
import com.github.warnastrophy.core.ui.util.BaseAndroidComposeTest
import org.junit.Test

class ErrorScreenTest : BaseAndroidComposeTest() {

  @Test
  fun errorScreen_displaysErrorMessage() {
    val testErrorMessage = "Test error message"
    val errors = listOf(Error(testErrorMessage, Screen.HOME))
    composeTestRule.setContent {
      ErrorScreen(message = testErrorMessage, onDismiss = {}, expanded = true, errors)
    }

    composeTestRule
        .onNodeWithTag(ErrorScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertExists()
        .assertTextEquals(testErrorMessage)
  }

  @Test
  fun errorScreen_correctlyDisplayErrorsFromHandler() {
    val errorHandler = ErrorHandler()
    composeTestRule.setContent { TopBar(currentScreen = Screen.HOME, errorHandler = errorHandler) }

    val testErrorMessage = "Handler error message"
    errorHandler.addError(testErrorMessage, Screen.HOME)

    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_ERROR_ICON, useUnmergedTree = true)
        .assertExists()
        .performClick()

    composeTestRule
        .onNodeWithTag(ErrorScreenTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertExists()
        .assertTextEquals(testErrorMessage)
  }
}
