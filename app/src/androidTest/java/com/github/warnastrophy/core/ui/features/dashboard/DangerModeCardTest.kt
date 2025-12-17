package com.github.warnastrophy.core.ui.features.dashboard

import android.Manifest
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.github.warnastrophy.core.data.provider.ActivityRepositoryProvider
import com.github.warnastrophy.core.data.provider.UserPreferencesRepositoryProvider
import com.github.warnastrophy.core.data.repository.MockActivityRepository
import com.github.warnastrophy.core.data.service.DangerLevel
import com.github.warnastrophy.core.data.service.DangerModeService
import com.github.warnastrophy.core.data.service.StateManagerService
import com.github.warnastrophy.core.model.Activity
import com.github.warnastrophy.core.permissions.PermissionResult
import com.github.warnastrophy.core.ui.map.MockPermissionManager
import com.github.warnastrophy.core.util.AppConfig
import com.github.warnastrophy.core.util.BaseAndroidComposeTest
import com.github.warnastrophy.userPrefsDataStore
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class DangerModeCardTest : BaseAndroidComposeTest() {
  private lateinit var mockActivityRepository: MockActivityRepository

  @Before
  fun setup() {
    val context = composeTestRule.activity.applicationContext
    UserPreferencesRepositoryProvider.initLocal(context.userPrefsDataStore)
    // Initialize the ActivityRepositoryProvider with mock for testing
    mockActivityRepository = MockActivityRepository()
    ActivityRepositoryProvider.useMock()
    // Make sure user preferences are reset to defaults so tests are deterministic
    runBlocking {
      val repo = UserPreferencesRepositoryProvider.repository
      repo.setAutoActionsEnabled(false)
      repo.setAutomaticCalls(false)
      repo.setAutomaticSms(false)
      repo.setVoiceConfirmationEnabled(false)
      repo.setTouchConfirmationRequired(false)
      repo.setInactivityDetection(false)
      repo.setAlertMode(false)
    }
    // Initialize services after prefs have been reset
    InstrumentationRegistry.getInstrumentation().runOnMainSync { StateManagerService.init(context) }
    StateManagerService.permissionManager =
        MockPermissionManager(currentResult = PermissionResult.Granted)
    StateManagerService.dangerModeService =
        DangerModeService(permissionManager = StateManagerService.permissionManager)
  }

  private fun createTestViewModel(repository: MockActivityRepository = mockActivityRepository) =
      DangerModeCardViewModel(repository = repository, userId = AppConfig.defaultUserId)

  private val testViewModel by lazy { createTestViewModel() }

  @Before
  fun grantPermissions() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val pkg = instrumentation.targetContext.packageName
    val uiAutomation = instrumentation.uiAutomation

    // Grant runtime location permissions
    uiAutomation.grantRuntimePermission(pkg, Manifest.permission.ACCESS_FINE_LOCATION)
    uiAutomation.grantRuntimePermission(pkg, Manifest.permission.ACCESS_COARSE_LOCATION)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      uiAutomation.grantRuntimePermission(pkg, Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    // Notifications on Android 13+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      uiAutomation.grantRuntimePermission(pkg, Manifest.permission.POST_NOTIFICATIONS)
    }
  }

  /* Verify that the DangerModeCard renders its root elements:
   * - Card
   * - Title
   * - Mode Label
   * - Sends Row
   * - Color Row
   * - Open Button
   */
  @Test
  fun dangerModeCard_rendersRootElements() {
    composeTestRule.setContent { MaterialTheme { DangerModeCard(viewModel = testViewModel) } }

    composeTestRule
        .onNodeWithTag(DangerModeTestTags.CARD, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(DangerModeTestTags.TITLE, useUnmergedTree = true)
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(DangerModeTestTags.MODE_LABEL, useUnmergedTree = true)
        .assertIsDisplayed()
        .assertExists()

    composeTestRule
        .onNodeWithTag(DangerModeTestTags.SENDS_ROW, useUnmergedTree = true)
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(DangerModeTestTags.COLOR_ROW, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  /* Verify that the DangerModeCard's switch toggles its state when clicked */
  @Test
  fun dangerModeCard_switch_togglesState() {
    composeTestRule.setContent { MaterialTheme { DangerModeCard(viewModel = testViewModel) } }

    val switchNode =
        composeTestRule.onNodeWithTag(DangerModeTestTags.SWITCH, useUnmergedTree = true)

    switchNode.assertIsOff()

    switchNode.performClick()

    switchNode.assertIsOn()
  }

  /* Verify that the DangerModeCard shows exactly one toggle buttons for each capability */
  @Test
  fun dangerModeCard_showsActionButtons() {
    composeTestRule.setContent { MaterialTheme { DangerModeCard(viewModel = testViewModel) } }

    DangerModeCapability.entries.forEach {
      composeTestRule
          .onAllNodesWithTag(DangerModeTestTags.capabilityTag(it), useUnmergedTree = true)
          .assertCountEquals(1)
          .onFirst()
          .assertIsDisplayed()
    }
  }

  /* Verify that the DangerModeCard's dropdown menu opens when the mode label is clicked */
  @Test
  fun dangerModeCard_dropdownMenu_opensOnClick() {
    val testActivities =
        listOf(
            Activity(id = "1", activityName = "Hiking"),
            Activity(id = "2", activityName = "Climbing"))

    // Add activities to the mock repository
    val mockRepo = MockActivityRepository()
    runBlocking { testActivities.forEach { mockRepo.addActivity(activity = it) } }
    val viewModel = createTestViewModel(repository = mockRepo)

    composeTestRule.setContent { MaterialTheme { DangerModeCard(viewModel = viewModel) } }

    // Manually trigger refresh after content is set to ensure coroutine runs in proper context
    viewModel.refreshActivities()

    composeTestRule.waitUntilWithTimeout { viewModel.activities.value.isNotEmpty() }

    val modeLabelNode =
        composeTestRule.onNodeWithTag(DangerModeTestTags.MODE_LABEL, useUnmergedTree = true)

    modeLabelNode.performClick()

    // Verify that the dropdown menu is displayed by checking for one of the menu items
    testActivities.forEach {
      composeTestRule
          .onNodeWithTag(DangerModeTestTags.activityTag(it.activityName), useUnmergedTree = true)
          .assertIsDisplayed()
    }
  }

  /* Verify that toggles and dropdown selections update the DangerModeCard state in the view model */
  @Test
  fun dangerModeCard_interactions_updateViewModelState() {
    val testActivities =
        listOf(
            Activity(id = "1", activityName = "Hiking"),
            Activity(id = "2", activityName = "Climbing"))

    // Add activities to the mock repository
    val mockRepo = MockActivityRepository()
    runBlocking { testActivities.forEach { mockRepo.addActivity(activity = it) } }
    val viewModel = createTestViewModel(repository = mockRepo)

    composeTestRule.setContent { MaterialTheme { DangerModeCard(viewModel = viewModel) } }

    val switchNode =
        composeTestRule.onNodeWithTag(DangerModeTestTags.SWITCH, useUnmergedTree = true)
    switchNode.performClick()
    assert(viewModel.isDangerModeEnabled.value)

    val modeLabelNode =
        composeTestRule.onNodeWithTag(DangerModeTestTags.MODE_LABEL, useUnmergedTree = true)
    modeLabelNode.performClick()
    val selectedActivity = testActivities[1]
    composeTestRule
        .onNodeWithTag(
            DangerModeTestTags.activityTag(selectedActivity.activityName), useUnmergedTree = true)
        .performClick()
    assert(viewModel.currentActivity.value == selectedActivity)

    val capability = DangerModeCapability.entries[1]
    val capabilityNode =
        composeTestRule.onNodeWithTag(
            DangerModeTestTags.capabilityTag(capability), useUnmergedTree = true)
    capabilityNode.performClick()
    // Check the UI-facing optimistic capabilities
    assert(viewModel.capabilitiesInternal.value.contains(capability))
    assert(viewModel.capabilitiesInternal.value.size == 1)
  }

  /* Verify that the DangerModeCard danger level changes as capabilities are toggled */
  @Test
  fun dangerModeCard_dangerLevel_changes() {
    lateinit var viewModel: DangerModeCardViewModel
    composeTestRule.setContent {
      viewModel = testViewModel
      MaterialTheme { DangerModeCard(viewModel = viewModel) }
    }

    assert(viewModel.dangerLevel.value == DangerLevel.LOW)

    composeTestRule
        .onNodeWithTag(DangerModeTestTags.dangerLevelTag(1), useUnmergedTree = true)
        .performClick()

    assert(viewModel.dangerLevel.value.ordinal > DangerLevel.LOW.ordinal)
  }

  @Test
  fun dangerModeCard_advancedSection_shownWhenCapabilitySelected() {
    lateinit var viewModel: DangerModeCardViewModel
    composeTestRule.setContent {
      viewModel = testViewModel
      MaterialTheme { DangerModeCard(viewModel = viewModel) }
    }

    // Ensure the ViewModel starts with no capabilities enabled to make the test deterministic
    testViewModel.onCapabilitiesChanged(emptySet())
    assert(testViewModel.capabilitiesInternal.value.isEmpty())

    // Advanced section should not be visible initially
    composeTestRule
        .onNodeWithTag(DangerModeTestTags.ADVANCED_SECTION, useUnmergedTree = true)
        .assertDoesNotExist()

    // Enable CALL capability
    composeTestRule.waitForIdleWithTimeout()
    val callCapabilityNode =
        composeTestRule.onNodeWithTag(
            DangerModeTestTags.capabilityTag(DangerModeCapability.CALL), useUnmergedTree = true)
    callCapabilityNode.assertExists().assertIsDisplayed()
    callCapabilityNode.performClick()

    // Wait for ViewModel to reflect the capability change
    composeTestRule.waitUntilWithTimeout {
      viewModel.capabilitiesInternal.value.contains(DangerModeCapability.CALL)
    }

    // Advanced section should now be visible in the UI
    composeTestRule
        .onNodeWithTag(DangerModeTestTags.ADVANCED_SECTION, useUnmergedTree = true)
        .assertIsDisplayed()

    // Auto actions and confirmation switches should be off by default in ViewModel
    assert(!viewModel.autoActionsEnabled.value)
    assert(!viewModel.confirmTouchRequired.value)
    assert(!viewModel.confirmVoiceRequired.value)
  }

  /* Advanced switches update their respective state in the ViewModel independently */
  @Test
  fun dangerModeCard_advancedSwitches_updateViewModelState_independently() {
    lateinit var viewModel: DangerModeCardViewModel
    composeTestRule.setContent {
      viewModel = testViewModel
      MaterialTheme { DangerModeCard(viewModel = viewModel) }
    }

    // Enable CALL capability to show advanced section
    composeTestRule.waitForIdleWithTimeout()
    val callCapabilityNode =
        composeTestRule.onNodeWithTag(
            DangerModeTestTags.capabilityTag(DangerModeCapability.CALL), useUnmergedTree = true)
    callCapabilityNode.assertExists().assertIsDisplayed()
    callCapabilityNode.performClick()

    val autoActionsSwitch =
        composeTestRule.onNodeWithTag(DangerModeTestTags.AUTO_CALL_SWITCH, useUnmergedTree = true)
    val confirmTouchSwitch =
        composeTestRule.onNodeWithTag(
            DangerModeTestTags.CONFIRM_TOUCH_SWITCH, useUnmergedTree = true)
    val confirmVoiceSwitch =
        composeTestRule.onNodeWithTag(
            DangerModeTestTags.CONFIRM_VOICE_SWITCH, useUnmergedTree = true)

    // Wait for ViewModel/UI to settle and assert initial ViewModel state (deterministic)
    composeTestRule.waitUntilWithTimeout { viewModel.autoActionsEnabled.value == false }
    assert(!viewModel.autoActionsEnabled.value)
    assert(!viewModel.confirmTouchRequired.value)
    assert(!viewModel.confirmVoiceRequired.value)

    // Toggle auto actions
    autoActionsSwitch.performClick()
    // Assert via ViewModel to avoid flakiness with semantics merging
    composeTestRule.waitUntilWithTimeout { viewModel.autoActionsEnabled.value }
    assert(viewModel.autoActionsEnabled.value)

    // Toggle touch confirmation and assert via ViewModel
    confirmTouchSwitch.performClick()
    composeTestRule.waitUntilWithTimeout { viewModel.confirmTouchRequired.value }
    assert(viewModel.confirmTouchRequired.value)

    // Toggle voice confirmation and assert via ViewModel
    confirmVoiceSwitch.performClick()
    composeTestRule.waitUntilWithTimeout { viewModel.confirmVoiceRequired.value }
    assert(viewModel.confirmVoiceRequired.value)
  }

  /** Verify that toggling one capability disables the other (mutually exclusive) in the UI */
  @Test
  fun dangerModeCard_capabilities_are_mutually_exclusive_ui() {
    lateinit var viewModel: DangerModeCardViewModel
    composeTestRule.setContent {
      viewModel = testViewModel
      MaterialTheme { DangerModeCard(viewModel = viewModel) }
    }

    // Toggle CALL capability via the ViewModel (optimistic update is synchronous)
    testViewModel.onCapabilityToggled(DangerModeCapability.CALL)
    assert(testViewModel.capabilitiesInternal.value.contains(DangerModeCapability.CALL))

    // Toggle SMS capability via the ViewModel - should replace CALL (mutually exclusive)
    testViewModel.onCapabilityToggled(DangerModeCapability.SMS)
    assert(testViewModel.capabilitiesInternal.value.contains(DangerModeCapability.SMS))
    assert(!testViewModel.capabilitiesInternal.value.contains(DangerModeCapability.CALL))
  }

  @Test
  fun dangerModeCard_confirmSwitches_are_mutually_exclusive_ui() {
    lateinit var viewModel: DangerModeCardViewModel
    composeTestRule.setContent {
      viewModel = testViewModel
      MaterialTheme { DangerModeCard(viewModel = viewModel) }
    }

    // Enable CALL capability to show advanced section
    val callNode =
        composeTestRule.onNodeWithTag(
            DangerModeTestTags.capabilityTag(DangerModeCapability.CALL), useUnmergedTree = true)
    callNode.performClick()

    val confirmTouchSwitch =
        composeTestRule.onNodeWithTag(
            DangerModeTestTags.CONFIRM_TOUCH_SWITCH, useUnmergedTree = true)
    val confirmVoiceSwitch =
        composeTestRule.onNodeWithTag(
            DangerModeTestTags.CONFIRM_VOICE_SWITCH, useUnmergedTree = true)

    // Toggle voice -> voice on, touch off
    confirmVoiceSwitch.performClick()
    confirmVoiceSwitch.assertIsOn()
    confirmTouchSwitch.assertIsOff()

    // Toggle touch -> touch on, voice off
    confirmTouchSwitch.performClick()
    confirmTouchSwitch.assertIsOn()
    confirmVoiceSwitch.assertIsOff()
  }
}
