package com.github.warnastrophy.core.ui.map

import org.junit.Assert.*
import org.junit.Test

class MapViewModelTest {

  @Test
  fun testRefreshUIState_updatesLocations() {
    val viewModel = MapViewModel()
    val locations = viewModel.uiState.value.locations
    assertTrue(locations.isNotEmpty())
    assertEquals(5, locations.size)
  }

  @Test
  fun testSetAndClearErrorMsg() {
    val viewModel = MapViewModel()
    viewModel.clearErrorMsg()
    assertNull(viewModel.uiState.value.errorMsg)
  }
}
