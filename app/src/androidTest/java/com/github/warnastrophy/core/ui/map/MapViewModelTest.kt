package com.github.warnastrophy.core.ui.map

import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
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

    composeTestRule.waitUntil(timeoutMillis = 3000) { hazards.isNotEmpty() }
  }

  @Test
  fun testSetAndClearErrorMsg() {
    viewModel.clearErrorMsg()
    assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun testHazardsLoadedIntoViewModel() = runBlocking {
    val viewModel = MapViewModel()
    // Wait for hazards to load
    kotlinx.coroutines.delay(1000)
    val hazards = viewModel.uiState.value.hazards
    assertNotNull(hazards)
    assertTrue(hazards.isNotEmpty())
  }

  @Test
  fun testHazardsPeriodicallyUpdate() = runBlocking {
    val viewModel = MapViewModel()
    val fetchDelay = viewModel.fetchDelayMs + 1000 // Add buffer to ensure fetch completes

    delay(fetchDelay)
    val hazardsAfterFirstFetch = viewModel.uiState.value.hazards
    assertNotNull(hazardsAfterFirstFetch)

    viewModel.resetHazards()

    delay(fetchDelay)
    val hazardsAfterSecondFetch = viewModel.uiState.value.hazards
    assertNotNull(hazardsAfterSecondFetch)
  }
}
