package com.github.warnastrophy.core.ui.profile.preferences

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.warnastrophy.core.data.repository.UserPreferencesRepository
import com.github.warnastrophy.core.permissions.PermissionResult
import com.github.warnastrophy.core.ui.components.FALLBACK_ACTIVITY_ERROR
import com.github.warnastrophy.core.ui.features.profile.preferences.DangerModePreferencesScreen
import com.github.warnastrophy.core.ui.features.profile.preferences.DangerModePreferencesScreenTestTags
import com.github.warnastrophy.core.ui.features.profile.preferences.DangerModePreferencesViewModel
import com.github.warnastrophy.core.ui.features.profile.preferences.PendingAction
import com.github.warnastrophy.core.ui.map.MockPermissionManager
import com.github.warnastrophy.core.ui.util.MockUserPreferencesRepository
import com.github.warnastrophy.core.util.BaseAndroidComposeTest
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DangerModePreferencesScreenTest : BaseAndroidComposeTest() {
  private lateinit var mockPermissionManager: MockPermissionManager
  private lateinit var userPreferencesRepository: UserPreferencesRepository
  private lateinit var viewModel: DangerModePreferencesViewModel

  @Before
  override fun setUp() {
    super.setUp()
    mockPermissionManager = MockPermissionManager()
    userPreferencesRepository = MockUserPreferencesRepository()
  }

  /** Helper function to set the content of the test rule with a configured ViewModel. */
  private fun setContent() {
    viewModel = DangerModePreferencesViewModel(mockPermissionManager, userPreferencesRepository)
    composeTestRule.setContent {
      CompositionLocalProvider(LocalContext provides composeTestRule.activity) {
        DangerModePreferencesScreen(viewModel = viewModel)
      }
    }

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
    composeTestRule
        .onNodeWithTag(DangerModePreferencesScreenTestTags.AUTOMATIC_CALLS_ITEM)
        .assertIsDisplayed()
  }

  /** Applies a given [PermissionResult] to the view model. */
  private fun applyPerm(permissionResult: PermissionResult) {
    mockPermissionManager.setPermissionResult(permissionResult)
    viewModel.onPermissionsResult(composeTestRule.activity)
  }

  private fun toggleSwitch(tag: String) {
    composeTestRule.onNodeWithTag(tag).assertIsEnabled().performClick()

    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(tag).assertIsEnabled().assertIsOn()
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
    composeTestRule
        .onNodeWithTag(DangerModePreferencesScreenTestTags.AUTOMATIC_CALLS_SWITCH)
        .assertIsOff()
  }

  /** Verifies that a fallback error message is shown when the context is not an Activity. */
  @Test
  fun showsFallbackError_whenNoActivityContextAvailable() {
    viewModel = DangerModePreferencesViewModel(mockPermissionManager, userPreferencesRepository)
    // Arrange: use non-activity context to verify fallback UI is displayed
    val applicationContext =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    composeTestRule.setContent {
      // Temporarily override LocalContext
      CompositionLocalProvider(LocalContext provides applicationContext) {
        DangerModePreferencesScreen(viewModel = viewModel)
      }
    }
    composeTestRule.onNodeWithTag(FALLBACK_ACTIVITY_ERROR).assertIsDisplayed()
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

  /**
   * Checks if the `isOsRequestInFlight` flag is correctly set when a permission request is
   * initiated.
   */
  @Test
  fun onPermissionsRequestStart_works_asExpected() = runTest {
    mockPermissionManager.setPermissionResult(PermissionResult.Denied(listOf("FAKE_PERMISSION")))
    setContent()

    PendingAction.entries.forEach { action ->
      assertNull(viewModel.uiState.value.pendingPermissionAction)
      viewModel.onPermissionsRequestStart(action = action)
      assertEquals(action, viewModel.uiState.value.pendingPermissionAction)
      viewModel.onPermissionsResult(composeTestRule.activity)
      composeTestRule.waitForIdle()
    }
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

  @Test
  fun alertModeToggle_requestsPermission_whenNotGranted() {
    mockPermissionManager.setPermissionResult(PermissionResult.Denied(listOf("FAKE_PERMISSION")))
    setContent()

    // Simulate toggle the switch
    composeTestRule
        .onNodeWithTag(DangerModePreferencesScreenTestTags.ALERT_MODE_SWITCH)
        .assertIsEnabled()
        .assertIsOff()

    applyPerm(PermissionResult.Granted)

    toggleSwitch(DangerModePreferencesScreenTestTags.ALERT_MODE_SWITCH)
  }

  @Test
  fun inactivityDetectionToggle_requestsPermission_whenNotGranted() {
    mockPermissionManager.setPermissionResult(PermissionResult.Granted)
    setContent()

    // Toggle Alert Mode to enable inactivity detection switch
    toggleSwitch(DangerModePreferencesScreenTestTags.ALERT_MODE_SWITCH)

    mockPermissionManager.setPermissionResult(PermissionResult.Denied(listOf("FAKE_PERMISSION")))

    // Simulate toggle the switch
    composeTestRule
        .onNodeWithTag(DangerModePreferencesScreenTestTags.INACTIVITY_DETECTION_SWITCH)
        .assertIsEnabled()
        .assertIsOff()

    applyPerm(PermissionResult.Granted)

    toggleSwitch(DangerModePreferencesScreenTestTags.INACTIVITY_DETECTION_SWITCH)
  }

  @Test
  fun automaticSmsToggle_requestsPermission_whenNotGranted() {
    mockPermissionManager.setPermissionResult(PermissionResult.Granted)
    setContent()

    // Toggle Alert Mode & Inactivity Detection to enable automatic sms switch
    toggleSwitch(DangerModePreferencesScreenTestTags.ALERT_MODE_SWITCH)
    toggleSwitch(DangerModePreferencesScreenTestTags.INACTIVITY_DETECTION_SWITCH)

    mockPermissionManager.setPermissionResult(PermissionResult.Denied(listOf("FAKE_PERMISSION")))

    // Simulate toggle the switch
    composeTestRule
        .onNodeWithTag(DangerModePreferencesScreenTestTags.AUTOMATIC_SMS_SWITCH)
        .assertIsEnabled()
        .assertIsOff()

    applyPerm(PermissionResult.Granted)

    toggleSwitch(DangerModePreferencesScreenTestTags.AUTOMATIC_SMS_SWITCH)
  }

  @Test
  fun automaticCallsDescription_isDisplayed() {
    mockPermissionManager.setPermissionResult(PermissionResult.Granted)
    setContent()

    val title =
        composeTestRule.activity.getString(
            com.github.warnastrophy.R.string.danger_mode_automatic_calls_title)

    // The title text should be visible in the UI (more reliable than matching large description)
    composeTestRule.onNodeWithText(title).assertIsDisplayed()
  }

  @Test
  fun automaticCallsToggle_requestsPermission_whenNotGranted() {
    mockPermissionManager.setPermissionResult(PermissionResult.Granted)
    setContent()

    // Toggle Alert Mode & Inactivity Detection to enable automatic calls switch
    toggleSwitch(DangerModePreferencesScreenTestTags.ALERT_MODE_SWITCH)
    toggleSwitch(DangerModePreferencesScreenTestTags.INACTIVITY_DETECTION_SWITCH)

    mockPermissionManager.setPermissionResult(PermissionResult.Denied(listOf("FAKE_PERMISSION")))

    // Simulate toggle the switch
    composeTestRule
        .onNodeWithTag(DangerModePreferencesScreenTestTags.AUTOMATIC_CALLS_SWITCH)
        .assertIsEnabled()
        .assertIsOff()

    applyPerm(PermissionResult.Granted)

    toggleSwitch(DangerModePreferencesScreenTestTags.AUTOMATIC_CALLS_SWITCH)
  }
}
