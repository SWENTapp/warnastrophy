package com.github.warnastrophy.core.ui.map

import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class MapViewModelTest {

  @get:Rule val composeTestRule = createComposeRule()

  val viewModel = MapViewModel()

  @Test
  fun testRefreshUIState_updatesLocations() {
    composeTestRule.setContent { MapScreen(viewModel) }
    viewModel.refreshUIState()
    val hazards = viewModel.uiState.value.hazards
    assertTrue(hazards.isNotEmpty())
  }

  @Test
  fun testSetAndClearErrorMsg() {
    viewModel.clearErrorMsg()
    assertNull(viewModel.uiState.value.errorMsg)
  }
}
