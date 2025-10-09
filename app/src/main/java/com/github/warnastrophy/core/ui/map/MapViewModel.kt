package com.github.warnastrophy.core.ui.map

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    println("Current pos: ${_uiState.value.target}")
  }

  @SuppressLint("MissingPermission")
  fun updateUserLocation(locationClient: FusedLocationProviderClient) {
    viewModelScope.launch {
      locationClient.lastLocation
          .addOnSuccessListener { location ->
            if (location != null) {
              val currentLatLng = LatLng(location.latitude, location.longitude)
              _uiState.value = _uiState.value.copy(target = currentLatLng)
              println("User location: $currentLatLng")
            } else {
              _uiState.value = _uiState.value.copy(errorMsg = "Unable to get location")
              println("Location is null")
            }
          }
          .addOnFailureListener {
            _uiState.value = _uiState.value.copy(errorMsg = it.message)
            println("Error getting location: ${it.message}")
          }
    }
  }
}
