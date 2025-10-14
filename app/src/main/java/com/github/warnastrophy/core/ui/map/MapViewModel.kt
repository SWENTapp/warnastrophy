package com.github.warnastrophy.core.ui.map

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.warnastrophy.core.model.util.AppConfig
import com.github.warnastrophy.core.model.util.Hazard
import com.github.warnastrophy.core.model.util.HazardRepositoryProvider
import com.github.warnastrophy.core.model.util.HazardsRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI state for [MapScreen].
 *
 * @param target The target location where the map camera should be centered.
 * @param hazards List of locations to be marked on the map.
 * @param errorMsg An optional error message to be displayed in the UI.
 */
data class MapUIState(
    // TODO : Set to user's location
    val target: LatLng = LatLng(18.5446778, -72.3395897),
    val hazards: List<Hazard> = emptyList(),
    val errorMsg: String? = null
)

/**
 * ViewModel for [MapScreen].
 *
 * Currently uses static data. Will be updated to use data from a repository in the future.
 */
class MapViewModel(
    private val repository: HazardsRepository = HazardRepositoryProvider.repository
) : ViewModel() {
  private val _uiState = MutableStateFlow(MapUIState())

  /** The UI state as a read-only [StateFlow]. */
  val uiState: StateFlow<MapUIState> = _uiState.asStateFlow()

  init {
    refreshUIState()
    viewModelScope.launch(Dispatchers.IO) {
      while (true) {
        delay(AppConfig.fetchDelayMs)
        refreshUIState()
      }
    }
  }

  /**
   * Sets an error message in the UI state.
   *
   * @param errorMsg The error message to be set.
   */
  private fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  /** Clears any existing error message from the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /** Refreshes the UI state by fetching the latest locations. */
  fun refreshUIState() {
    viewModelScope.launch(Dispatchers.IO) {
      try {
        Log.e("viewModel", "Fetching hazards from repository")
        val sampleHazards = repository.getAreaHazards(HazardRepositoryProvider.locationPolygon)
        Log.e("viewModel", "Fetched hazards: $sampleHazards")
        _uiState.value = _uiState.value.copy(hazards = sampleHazards)
      } catch (e: Exception) {
        Log.e("Error", "Failed to load hazards: ${e.message}")
      }
    }
  }

  /** Resets the hazards list in the UI state to be empty. */
  fun resetHazards() {
    _uiState.value = _uiState.value.copy(hazards = emptyList())
  }
}
