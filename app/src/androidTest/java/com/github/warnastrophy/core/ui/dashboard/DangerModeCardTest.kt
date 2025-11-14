package com.github.warnastrophy.core.ui.dashboard

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.warnastrophy.core.ui.features.dashboard.DangerModeCapability
import com.github.warnastrophy.core.ui.features.dashboard.DangerModeCard
import com.github.warnastrophy.core.ui.features.dashboard.DangerModeCardViewModel
import com.github.warnastrophy.core.ui.features.dashboard.DangerModePreset
import com.github.warnastrophy.core.ui.features.dashboard.DangerModeTestTags
import com.github.warnastrophy.core.ui.util.BaseAndroidComposeTest
import org.junit.Test

class DangerModeCardTest : BaseAndroidComposeTest() {

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
    composeTestRule.setContent { MaterialTheme { DangerModeCard() } }

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
    composeTestRule.setContent { MaterialTheme { DangerModeCard() } }

    val switchNode =
        composeTestRule.onNodeWithTag(DangerModeTestTags.SWITCH, useUnmergedTree = true)

    switchNode.assertIsOff()

    switchNode.performClick()

    switchNode.assertIsOn()
  }

  /* Verify that the DangerModeCard shows exactly one toggle buttons for each capability */
  @Test
  fun dangerModeCard_showsActionButtons() {
    composeTestRule.setContent { MaterialTheme { DangerModeCard() } }

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
    composeTestRule.setContent { MaterialTheme { DangerModeCard() } }

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
      viewModel = viewModel()
      MaterialTheme { DangerModeCard(viewModel = viewModel) }
    }

    val switchNode =
        composeTestRule.onNodeWithTag(DangerModeTestTags.SWITCH, useUnmergedTree = true)
    switchNode.performClick()
    assert(viewModel.isDangerModeEnabled)

    val modeLabelNode =
        composeTestRule.onNodeWithTag(DangerModeTestTags.MODE_LABEL, useUnmergedTree = true)
    modeLabelNode.performClick()
    val selectedMode = DangerModePreset.entries[1]
    composeTestRule
        .onNodeWithTag(DangerModeTestTags.modeTag(selectedMode), useUnmergedTree = true)
        .performClick()
    assert(viewModel.currentMode == selectedMode)

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
      viewModel = viewModel()
      MaterialTheme { DangerModeCard(viewModel = viewModel) }
    }

    assert(viewModel.dangerLevel == 0)

    composeTestRule
        .onNodeWithTag(DangerModeTestTags.dangerLevelTag(1), useUnmergedTree = true)
        .performClick()

    assert(viewModel.dangerLevel > 0)
  }
}
