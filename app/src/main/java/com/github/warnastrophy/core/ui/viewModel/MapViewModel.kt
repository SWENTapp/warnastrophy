package com.github.warnastrophy.core.ui.viewModel

import android.annotation.SuppressLint
import android.os.Looper
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.warnastrophy.core.model.util.AppConfig
import com.github.warnastrophy.core.model.util.Hazard
import com.github.warnastrophy.core.model.util.HazardRepositoryProvider
import com.github.warnastrophy.core.model.util.HazardsRepository
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * UI state for [com.github.warnastrophy.core.ui.map.MapScreen].
 *
 * @param target The target location where the map camera should be centered.
 * @param hazards List of locations to be marked on the map.
 * @param errorMsg An optional error message to be displayed in the UI.
 */
data class MapUIState(
    // TODO : Set to user's location
    val target: LatLng = LatLng(18.5944, -72.3074), // Default to Port-au-Prince
    val hazards: List<Hazard>? = null,
    val errorMsg: String? = null,
    val isLoading: Boolean = false
)

/**
 * ViewModel for [com.github.warnastrophy.core.ui.map.MapScreen].
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
    viewModelScope.launch(Dispatchers.IO) {
      while (true) {
        refreshUIState()
        delay(AppConfig.fetchDelayMs)
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
        setErrorMsg("Failed to load hazards: ${e.message}")
        // We keep the existing hazards in case of an error
      }
    }
  }

  @SuppressLint("MissingPermission")
  fun requestCurrentLocation(locationClient: FusedLocationProviderClient) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true) }
      try {
        val request =
            CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMaxUpdateAgeMillis(0)
                .build()

        val location = locationClient.getCurrentLocation(request, null).await()
        location?.let {
          val latLng = LatLng(it.latitude, it.longitude)
          _uiState.update { it.copy(target = latLng) }
        } ?: setErrorMsg("Location unavailable")
      } catch (e: Exception) {
        setErrorMsg("Error getting location: ${e.message}")
      } finally {
        _uiState.update { it.copy(isLoading = false) }
      }
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
          object : LocationCallback() { // This callback is now assigned to the property
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
          Looper.getMainLooper(),
      )
    } catch (_: SecurityException) {
      _uiState.value =
          _uiState.value.copy(errorMsg = "Location permission not granted !", isLoading = false)
    } catch (e: Exception) {
      _uiState.value =
          _uiState.value.copy(errorMsg = "Location update failed: ${e.message}", isLoading = false)
    }
  }

  /** Resets the hazards list in the UI state to be empty. */
  fun resetHazards() {
    _uiState.value = _uiState.value.copy(hazards = emptyList())
  }
}
