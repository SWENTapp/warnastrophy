package com.github.warnastrophy.core.ui.map

import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MapUIState(
    // TODO : Set to user's location
    val target: LatLng = LatLng(18.5446778, -72.3395897),
    // TODO : change this to a list of DangerZone objects
    val locations: List<LatLng> = emptyList(),
    val errorMsg: String? = null
)

class MapViewModel() : ViewModel() {
  private val _uiState = MutableStateFlow(MapUIState())
  val uiState: StateFlow<MapUIState> = _uiState.asStateFlow()

  init {
    refreshUIState()
  }

  private fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  fun refreshUIState() {
    // TODO : Fetch locations from repository
    // For now, we use a static list of locations in Haiti
    val sampleLocations =
        listOf(
            LatLng(18.5944, -72.3074), // Port-au-Prince
            LatLng(19.7595, -72.2040), // Cap-Haïtien
            LatLng(18.9112, -72.7822), // Gonaïves
            LatLng(19.3000, -72.6167), // Saint-Marc
            LatLng(18.5392, -72.3299) // Carrefour
            )
    _uiState.value = _uiState.value.copy(locations = sampleLocations)
  }
}
