package com.github.warnastrophy.core.ui.errorMsg

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.warnastrophy.core.ui.common.Error
import com.github.warnastrophy.core.ui.common.ErrorHandler
import com.github.warnastrophy.core.ui.common.ErrorType
import com.github.warnastrophy.core.ui.features.error.ErrorScreen
import com.github.warnastrophy.core.ui.features.error.ErrorScreenTestTags
import com.github.warnastrophy.core.ui.navigation.NavigationTestTags
import com.github.warnastrophy.core.ui.navigation.Screen
import com.github.warnastrophy.core.ui.navigation.TopBar
import com.github.warnastrophy.core.ui.util.BaseAndroidComposeTest
import org.junit.Test

class ErrorScreenTest : BaseAndroidComposeTest() {

  @Test
  fun errorScreen_displaysErrorMessage() {
    val errors = listOf(Error(ErrorType.LOCATION_ERROR, Screen.Dashboard))
    composeTestRule.setContent {
      ErrorScreen(
          message = ErrorType.LOCATION_ERROR.message, onDismiss = {}, expanded = true, errors)
    }

    composeTestRule
        .onNodeWithTag(ErrorScreenTestTags.ERROR_MESSAGE_TEXT, useUnmergedTree = true)
        .assertExists()
        .assertTextEquals(ErrorType.LOCATION_ERROR.message)
  }

  @Test
  fun errorScreen_displaysNoErrorsMessage_whenErrorListIsEmpty() {
    composeTestRule.setContent {
      ErrorScreen(
          message = "This message won't be shown",
          onDismiss = {},
          expanded = true,
          errors = emptyList())
    }

    composeTestRule.onNodeWithText("No errors").assertExists()

    composeTestRule.onNodeWithTag(ErrorScreenTestTags.ERROR_MESSAGE).assertDoesNotExist()
  }

  @Test
  fun errorScreen_correctlyDisplayErrorsFromHandler() {
    val errorHandler = ErrorHandler()
    composeTestRule.setContent {
      TopBar(currentScreen = Screen.Dashboard, errorHandler = errorHandler)
    }

    errorHandler.addError(ErrorType.LOCATION_ERROR, Screen.Dashboard)

    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_ERROR_ICON, useUnmergedTree = true)
        .assertExists()
        .performClick()

    composeTestRule
        .onNodeWithTag(ErrorScreenTestTags.ERROR_MESSAGE_TEXT, useUnmergedTree = true)
        .assertExists()
        .assertTextEquals(ErrorType.LOCATION_ERROR.message)
  }
}
