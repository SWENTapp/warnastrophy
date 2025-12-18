package com.github.warnastrophy.core.ui.components

import android.Manifest
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.github.warnastrophy.WarnastrophyComposable
import com.github.warnastrophy.core.data.provider.ContactRepositoryProvider
import com.github.warnastrophy.core.data.provider.UserPreferencesRepositoryProvider
import com.github.warnastrophy.core.data.service.StateManagerService
import com.github.warnastrophy.core.ui.features.dashboard.ConfirmationPopup
import com.github.warnastrophy.core.ui.features.dashboard.ConfirmationPopupTestTags
import com.github.warnastrophy.core.util.BaseAndroidComposeTest
import com.github.warnastrophy.userPrefsDataStore
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ConfirmationPopupTest : BaseAndroidComposeTest() {

  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          Manifest.permission.ACCESS_FINE_LOCATION,
          Manifest.permission.ACCESS_COARSE_LOCATION,
          Manifest.permission.POST_NOTIFICATIONS)

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

  @Test
  fun confirmationPopup_shows_and_confirm_calls_orchestrator() {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    UserPreferencesRepositoryProvider.initLocal(ctx.userPrefsDataStore)
    ContactRepositoryProvider.initLocal(ctx)
    InstrumentationRegistry.getInstrumentation().runOnMainSync { StateManagerService.init(ctx) }
    // Launch the app composable
    composeTestRule.setContent { WarnastrophyComposable() }

    // Trigger the touch confirmation via the orchestrator debug helper
    val orchestrator = StateManagerService.dangerModeOrchestrator

    // Ensure touch confirmation is required
    orchestrator.setTouchConfirmationRequired(true)

    // Trigger touch confirmation
    composeTestRule.runOnUiThread { orchestrator.debugTriggerTouchConfirmation() }

    // Wait for popup and assert it's visible
    composeTestRule.waitUntilWithTimeout {
      try {
        composeTestRule.onNodeWithTag(ConfirmationPopupTestTags.DIALOG).assertIsDisplayed()
        true
      } catch (_: Throwable) {
        false
      }
    }

    composeTestRule.onNodeWithTag(ConfirmationPopupTestTags.DIALOG).assertIsDisplayed()

    // Click confirm button
    composeTestRule.onNodeWithTag(ConfirmationPopupTestTags.CONFIRM_BUTTON).performClick()

    // After confirm, popup should no longer be displayed
    composeTestRule.waitUntilWithTimeout {
      try {
        composeTestRule.onNodeWithTag(ConfirmationPopupTestTags.DIALOG).assertIsNotDisplayed()
        true
      } catch (_: Throwable) {
        false
      }
    }
  }
}
