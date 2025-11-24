package com.github.warnastrophy.core.ui.dashboard

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.github.warnastrophy.core.data.permissions.PermissionResult
import com.github.warnastrophy.core.data.service.DangerLevel
import com.github.warnastrophy.core.data.service.DangerModeService
import com.github.warnastrophy.core.data.service.StateManagerService
import com.github.warnastrophy.core.ui.features.dashboard.DangerModeCapability
import com.github.warnastrophy.core.ui.features.dashboard.DangerModeCard
import com.github.warnastrophy.core.ui.features.dashboard.DangerModeCardViewModel
import com.github.warnastrophy.core.ui.features.dashboard.DangerModePreset
import com.github.warnastrophy.core.ui.features.dashboard.DangerModeTestTags
import com.github.warnastrophy.core.ui.map.MockPermissionManager
import com.github.warnastrophy.core.util.BaseAndroidComposeTest
import org.junit.Before
import org.junit.Test

class DangerModeCardTest : BaseAndroidComposeTest() {
  @Before
  fun setup() {
    StateManagerService.init(composeTestRule.activity.applicationContext)
    StateManagerService.permissionManager =
        MockPermissionManager(currentResult = PermissionResult.Granted)
    StateManagerService.dangerModeService =
        DangerModeService(permissionManager = StateManagerService.permissionManager)
  }

  private val testViewModel by lazy {
    // provide no-op start/stop so tests do not attempt to start a real foreground service
    DangerModeCardViewModel(
        startService = { _: Context -> /* no-op in tests */ },
        stopService = { _: Context -> /* no-op in tests */ })
  }

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
    composeTestRule.setContent { MaterialTheme { DangerModeCard(viewModel = testViewModel) } }

    val modeLabelNode =
        composeTestRule.onNodeWithTag(DangerModeTestTags.MODE_LABEL, useUnmergedTree = true)

    modeLabelNode.performClick()

    // Verify that the dropdown menu is displayed by checking for one of the menu items
    DangerModePreset.entries.forEach {
      composeTestRule
          .onNodeWithTag(DangerModeTestTags.modeTag(it), useUnmergedTree = true)
          .assertIsDisplayed()
    }
  }

  /* Verify that toggles and dropdown selections update the DangerModeCard state in the view model */
  @Test
  fun dangerModeCard_interactions_updateViewModelState() {

    lateinit var viewModel: DangerModeCardViewModel
    composeTestRule.setContent {
      // Use a surface to get the background color
      viewModel = testViewModel
      MaterialTheme { DangerModeCard(viewModel = viewModel) }
    }

    val switchNode =
        composeTestRule.onNodeWithTag(DangerModeTestTags.SWITCH, useUnmergedTree = true)
    switchNode.performClick()
    assert(viewModel.isDangerModeEnabled.value)

    val modeLabelNode =
        composeTestRule.onNodeWithTag(DangerModeTestTags.MODE_LABEL, useUnmergedTree = true)
    modeLabelNode.performClick()
    val selectedMode = DangerModePreset.entries[1]
    composeTestRule
        .onNodeWithTag(DangerModeTestTags.modeTag(selectedMode), useUnmergedTree = true)
        .performClick()
    assert(viewModel.currentMode.value == selectedMode)

    val capability = DangerModeCapability.entries[1]
    val capabilityNode =
        composeTestRule.onNodeWithTag(
            DangerModeTestTags.capabilityTag(capability), useUnmergedTree = true)
    capabilityNode.performClick()

    capabilityNode.assertIsSelected()

    assert(viewModel.capabilities.value.contains(capability))
    assert(viewModel.capabilities.value.size == 1)
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
}
