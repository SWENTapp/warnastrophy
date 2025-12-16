package com.github.warnastrophy.core.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.warnastrophy.core.ui.features.dashboard.ConfirmationPopup
import com.github.warnastrophy.core.ui.features.dashboard.ConfirmationPopupTestTags
import com.github.warnastrophy.core.util.BaseAndroidComposeTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ConfirmationPopupTest : BaseAndroidComposeTest() {

  private fun setPopupContent(onConfirm: () -> Unit = {}, onCancel: () -> Unit = {}) {
    composeTestRule.setContent {
      ConfirmationPopup(
          title = "Test Title",
          message = "Test description",
          confirmLabel = "Confirm",
          cancelLabel = "Cancel",
          onConfirm = onConfirm,
          onCancel = onCancel)
    }
  }

  @Test
  fun confirmationPopup_displaysAllTexts() {
    setPopupContent()

    composeTestRule.onNodeWithTag(ConfirmationPopupTestTags.DIALOG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ConfirmationPopupTestTags.TITLE_TEXT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ConfirmationPopupTestTags.DESCRIPTION_TEXT).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ConfirmationPopupTestTags.CONFIRM_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ConfirmationPopupTestTags.CANCEL_BUTTON).assertIsDisplayed()
  }

  @Test
  fun confirmationPopup_confirmInvokesCallback() {
    var confirmCount = 0
    setPopupContent(onConfirm = { confirmCount++ })

    composeTestRule.onNodeWithTag(ConfirmationPopupTestTags.CONFIRM_BUTTON).performClick()

    assertEquals(1, confirmCount)
  }

  @Test
  fun confirmationPopup_cancelInvokesCallback() {
    var cancelCount = 0
    setPopupContent(onCancel = { cancelCount++ })

    composeTestRule.onNodeWithTag(ConfirmationPopupTestTags.CANCEL_BUTTON).performClick()

    assertEquals(1, cancelCount)
  }
}
