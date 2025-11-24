package com.github.warnastrophy.core.ui.features.profile.preferences

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.warnastrophy.core.permissions.PermissionResult
import com.github.warnastrophy.core.ui.map.MockPermissionManager
import com.github.warnastrophy.core.ui.util.BaseAndroidComposeTest
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DangerModePreferencesScreenTest : BaseAndroidComposeTest() {
  private lateinit var mockPermissionManager: MockPermissionManager
  private lateinit var viewModel: DangerModePreferencesViewModel

  @Before
  override fun setUp() {
    super.setUp()
    mockPermissionManager = MockPermissionManager()
  }

  /** Helper function to set the content of the test rule with a configured ViewModel. */
  private fun setContent() {
    viewModel = DangerModePreferencesViewModel(mockPermissionManager)
    composeTestRule.setContent { DangerModePreferencesScreen(viewModel = viewModel) }

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(DangerModePreferencesScreenTestTags.ALERT_MODE_ITEM)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(DangerModePreferencesScreenTestTags.INACTIVITY_DETECTION_ITEM)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(DangerModePreferencesScreenTestTags.AUTOMATIC_SMS_ITEM)
        .assertIsDisplayed()
  }

  private fun assertSwitchesOff() {
    composeTestRule
        .onNodeWithTag(DangerModePreferencesScreenTestTags.ALERT_MODE_SWITCH)
        .assertIsOff()
    composeTestRule
        .onNodeWithTag(DangerModePreferencesScreenTestTags.INACTIVITY_DETECTION_SWITCH)
        .assertIsOff()
    composeTestRule
        .onNodeWithTag(DangerModePreferencesScreenTestTags.AUTOMATIC_SMS_SWITCH)
        .assertIsOff()
  }

  // TODO Duplication from MapScreenTest -> need to generalize fallback
  /** Verifies that a fallback error message is shown when the context is not an Activity. */
  @Test
  fun showsFallbackError_whenNoActivityContextAvailable() {
    viewModel = DangerModePreferencesViewModel(mockPermissionManager)
    // Arrange: use non-activity context to verify fallback UI is displayed
    val applicationContext =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    composeTestRule.setContent {
      // Temporarily override LocalContext
      CompositionLocalProvider(LocalContext provides applicationContext) {
        DangerModePreferencesScreen(viewModel = viewModel)
      }
    }
    composeTestRule
        .onNodeWithTag(DangerModePreferencesScreenTestTags.FALLBACK_ACTIVITY_ERROR)
        .assertIsDisplayed()
  }

  @Test
  fun allItems_areInitiallyDisabled_whenPermissionsAreDenied() {
    mockPermissionManager.setPermissionResult(PermissionResult.Denied(listOf("FAKE_PERMISSION")))

    setContent()

    assertSwitchesOff()

    composeTestRule
        .onNodeWithTag(DangerModePreferencesScreenTestTags.ALERT_MODE_SWITCH)
        .assertIsEnabled()
    composeTestRule
        .onNodeWithTag(DangerModePreferencesScreenTestTags.INACTIVITY_DETECTION_SWITCH)
        .assertIsNotEnabled()
    composeTestRule
        .onNodeWithTag(DangerModePreferencesScreenTestTags.AUTOMATIC_SMS_SWITCH)
        .assertIsNotEnabled()
  }

  @Test
  fun alertModeToggle_enablesFeature_whenPermissionsAreAlreadyGranted() {
    mockPermissionManager.setPermissionResult(PermissionResult.Granted)

    setContent()

    composeTestRule
        .onNodeWithTag(DangerModePreferencesScreenTestTags.ALERT_MODE_SWITCH)
        .performClick()

    composeTestRule
        .onNodeWithTag(DangerModePreferencesScreenTestTags.ALERT_MODE_SWITCH)
        .assertIsOn()
    composeTestRule
        .onNodeWithTag(DangerModePreferencesScreenTestTags.INACTIVITY_DETECTION_SWITCH)
        .assertIsEnabled()
    composeTestRule
        .onNodeWithTag(DangerModePreferencesScreenTestTags.AUTOMATIC_SMS_SWITCH)
        .assertIsNotEnabled()
  }

  @Test
  fun isOsRequestInFlight_whenPermissionRequestIsPending_AlertMode() {
    mockPermissionManager.setPermissionResult(PermissionResult.Denied(listOf("FAKE_PERMISSION")))

    setContent()

    composeTestRule
        .onNodeWithTag(DangerModePreferencesScreenTestTags.ALERT_MODE_SWITCH)
        .assertIsOff()

    viewModel.onPermissionsRequestStart(PendingAction.TOGGLE_ALERT_MODE)
    composeTestRule.waitForIdle()

    assertTrue(viewModel.uiState.value.isOsRequestInFlight)

    mockPermissionManager.setPermissionResult(PermissionResult.Granted)
    viewModel.onPermissionsResult(composeTestRule.activity)
    composeTestRule.waitForIdle()

    assertFalse(viewModel.uiState.value.isOsRequestInFlight)
    composeTestRule
        .onNodeWithTag(DangerModePreferencesScreenTestTags.ALERT_MODE_SWITCH)
        .assertIsOn()
  }

  @Test
  fun turningOffParentToggle_disablesChildToggles() {
    mockPermissionManager.setPermissionResult(PermissionResult.Granted)

    setContent()

    composeTestRule
        .onNodeWithTag(DangerModePreferencesScreenTestTags.ALERT_MODE_SWITCH)
        .performClick()
    composeTestRule
        .onNodeWithTag(DangerModePreferencesScreenTestTags.INACTIVITY_DETECTION_SWITCH)
        .performClick()
    composeTestRule
        .onNodeWithTag(DangerModePreferencesScreenTestTags.AUTOMATIC_SMS_SWITCH)
        .performClick()

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(DangerModePreferencesScreenTestTags.ALERT_MODE_SWITCH)
        .assertIsOn()
    composeTestRule
        .onNodeWithTag(DangerModePreferencesScreenTestTags.INACTIVITY_DETECTION_SWITCH)
        .assertIsOn()
    composeTestRule
        .onNodeWithTag(DangerModePreferencesScreenTestTags.AUTOMATIC_SMS_SWITCH)
        .assertIsOn()

    composeTestRule
        .onNodeWithTag(DangerModePreferencesScreenTestTags.ALERT_MODE_SWITCH)
        .performClick()

    composeTestRule.waitForIdle()

    assertSwitchesOff()

    composeTestRule
        .onNodeWithTag(DangerModePreferencesScreenTestTags.ALERT_MODE_SWITCH)
        .assertIsEnabled()
    composeTestRule
        .onNodeWithTag(DangerModePreferencesScreenTestTags.INACTIVITY_DETECTION_SWITCH)
        .assertIsNotEnabled()
    composeTestRule
        .onNodeWithTag(DangerModePreferencesScreenTestTags.AUTOMATIC_SMS_SWITCH)
        .assertIsNotEnabled()
  }
}
