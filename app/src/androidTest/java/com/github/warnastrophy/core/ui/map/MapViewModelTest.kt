package com.github.warnastrophy.core.ui.map

import androidx.compose.ui.test.junit4.createComposeRule
import com.github.warnastrophy.core.ui.viewModel.MapViewModel
import com.github.warnastrophy.core.model.util.AppConfig
import com.github.warnastrophy.core.model.util.HazardRepositoryProvider
import com.github.warnastrophy.core.model.util.HazardsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class MapViewModelTest {

  @get:Rule val composeTestRule = createComposeRule()

  val viewModel = MapViewModel()

  @Test
  fun testSetAndClearErrorMsg() {
    viewModel.clearErrorMsg()
    assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun testHazardsLoadedIntoViewModel() = runBlocking {
    val viewModel = MapViewModel()
    // Wait for hazards to load
    delay(2000)
    val hazards = viewModel.uiState.value.hazards
    assertNotNull(hazards)
    assertTrue(hazards != null)
  }

  @Test
  fun testHazardsPeriodicallyUpdate() = runBlocking {
    val reference = HazardsRepository().getAreaHazards(HazardRepositoryProvider.WORLD_POLYGON)
    delay(2000)

    if (reference.isEmpty()) {
      // If no hazards in the world polygon, skip the test
      return@runBlocking
    }
    HazardRepositoryProvider.locationPolygon = HazardRepositoryProvider.WORLD_POLYGON

    val viewModel = MapViewModel()

    AppConfig.fetchDelayMs = 5000L // Set fetch delay to 5 seconds for testing
    val fetchDelay = AppConfig.fetchDelayMs + 2000 // Add buffer to ensure fetch completes

    delay(fetchDelay)
    val hazardsAfterFirstFetch = viewModel.uiState.value.hazards
    assertTrue(hazardsAfterFirstFetch != null && hazardsAfterFirstFetch.isNotEmpty())

    viewModel.resetHazards()

    delay(fetchDelay)
    val hazardsAfterSecondFetch = viewModel.uiState.value.hazards
    assertTrue(hazardsAfterSecondFetch != null && hazardsAfterSecondFetch.isNotEmpty())
  }
}
