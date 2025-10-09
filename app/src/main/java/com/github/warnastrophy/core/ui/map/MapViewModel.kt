package com.github.warnastrophy.core.ui.map

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * UI state for [MapScreen].
 *
 * @param target The target location where the map camera should be centered.
 * @param locations List of locations to be marked on the map.
 * @param errorMsg An optional error message to be displayed in the UI.
 */
data class MapUIState(
    // TODO : Set to user's location
    val target: LatLng = LatLng(18.5944, -72.3074), // Default to Port-au-Prince
    // TODO : change this to a list of DangerZone objects
    val locations: List<LatLng> = emptyList(),
    val errorMsg: String? = null,
    val isLoading: Boolean = false
)

/**
 * ViewModel for [MapScreen].
 *
 * Currently uses static data. Will be updated to use data from a repository in the future.
 */
class MapViewModel() : ViewModel() {
  private val _uiState = MutableStateFlow(MapUIState())

  /** The UI state as a read-only [StateFlow]. */
  val uiState: StateFlow<MapUIState> = _uiState.asStateFlow()

  init {
    refreshUIState()
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
  fun requestCurrentLocation(locationClient: FusedLocationProviderClient) {
    _uiState.value = _uiState.value.copy(isLoading = true)
    val request =
        CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(0)
            .build()

    locationClient
        .getCurrentLocation(request, null)
        .addOnSuccessListener { location ->
          location?.let {
            val latLng = LatLng(it.latitude, it.longitude)
            _uiState.value = _uiState.value.copy(target = latLng, isLoading = false)
          }
        }
        .addOnFailureListener { exception ->
          setErrorMsg("Error getting location: ${exception.message}")
          _uiState.value = _uiState.value.copy(isLoading = false)
        }
  }
}
