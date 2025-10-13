package com.github.warnastrophy.core.ui.map

import android.annotation.SuppressLint
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.warnastrophy.core.model.util.HazardRepositoryProvider
import com.github.warnastrophy.core.ui.repository.Hazard
import com.github.warnastrophy.core.ui.repository.HazardsRepository
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
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
    val target: LatLng = LatLng(18.5944, -72.3074), // Default to Port-au-Prince
    val hazards: List<Hazard> = emptyList(),
    val errorMsg: String? = null,
    val isLoading: Boolean = false
)

/**
 * ViewModel for [MapScreen].
 *
 * Currently uses static data. Will be updated to use data from a repository in the future.
 */
class MapViewModel(
    private val repository: HazardsRepository = HazardRepositoryProvider.repository
) : ViewModel() {
  val LOCATION_UPDATE_INTERVAL = 2000L // 2 seconds
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

  @SuppressLint("MissingPermission")
  fun startLocationUpdates(locationClient: FusedLocationProviderClient) {
    try {
      _uiState.value = _uiState.value.copy(isLoading = true)
      val request =
          LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL).build()

      locationClient.requestLocationUpdates(
          request,
          object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
              val location = result.lastLocation
              if (location != null) {
                val latLng = LatLng(location.latitude, location.longitude)
                _uiState.value =
                    _uiState.value.copy(
                        target = latLng,
                        errorMsg = null, // clear previous errors
                        isLoading = false)
              } else {
                _uiState.value =
                    _uiState.value.copy(errorMsg = "No location fix available", isLoading = false)
              }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
              if (!availability.isLocationAvailable) {
                _uiState.value =
                    _uiState.value.copy(
                        errorMsg = "Location temporarily unavailable", isLoading = false)
              }
            }
          },
          Looper.getMainLooper())
    } catch (e: SecurityException) {
      _uiState.value =
          _uiState.value.copy(errorMsg = "Location permission not granted !", isLoading = false)
    } catch (e: Exception) {
      _uiState.value =
          _uiState.value.copy(errorMsg = "Location update failed: ${e.message}", isLoading = false)
    }
  }
}
