package com.github.warnastrophy.core.ui.map

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.warnastrophy.core.model.AppPermissions
import com.github.warnastrophy.core.model.FetcherState
import com.github.warnastrophy.core.model.GpsPositionState
import com.github.warnastrophy.core.model.Hazard
import com.github.warnastrophy.core.model.HazardsDataService
import com.github.warnastrophy.core.model.PermissionManagerInterface
import com.github.warnastrophy.core.model.PermissionResult
import com.github.warnastrophy.core.model.PositionService
import kotlin.collections.filter
import kotlin.collections.groupBy
import kotlin.collections.map
import kotlin.collections.maxOf
import kotlin.collections.minOf
import kotlin.collections.toMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Represents the UI state for the map screen.
 *
 * @property permissionResult The current state of the location permission. See [PermissionResult].
 * @property isTrackingLocation True if the app is actively tracking the user's location, false
 *   otherwise.
 * @property isOsRequestInFlight True if a system permission dialog is currently being shown to the
 *   user.
 * @property severitiesByType A map of hazard types to their corresponding severity ranges.
 * @property positionState The state of the GPS position data fetching. See [GpsPositionState].
 * @property hazardState The state of the hazard data fetching. See [FetcherState].
 */
data class MapUIState(
    val permissionResult: PermissionResult,
    val isTrackingLocation: Boolean = false,
    val isOsRequestInFlight: Boolean = false,
    val severitiesByType: Map<String, Pair<Double, Double>> = emptyMap(),
    val positionState: GpsPositionState = GpsPositionState(isLoading = true),
    val hazardState: FetcherState = FetcherState(isLoading = true)
) {
  /** A computed property that is true if the permission is granted. */
  val isGranted: Boolean
    get() = permissionResult is PermissionResult.Granted

  /** A computed property that is true if any of the sub-states are loading. */
  val isLoading: Boolean
    get() = positionState.isLoading || hazardState.isLoading
}

class MapViewModel(
    private val gpsService: PositionService,
    private val hazardsService: HazardsDataService,
    private val permissionManager: PermissionManagerInterface,
) : ViewModel() {
  val locationPermissions = AppPermissions.LocationFine

  private val _uiState =
      MutableStateFlow(
          MapUIState(permissionResult = permissionManager.getPermissionResult(locationPermissions)))
  val uiState: StateFlow<MapUIState> = _uiState.asStateFlow()

  init {
    observeDataSources()
  }

  /**
   * Observes the data sources for position and hazard information.
   *
   * This function uses `combine` to listen to the latest emissions from both the
   * `gpsService.positionState` and `hazardsService.fetcherState` flows. Whenever either of these
   * data sources emits a new state, the lambda is executed. Inside the lambda, it updates the
   * `_uiState` with the new position and hazard states, ensuring the UI is always in sync with the
   * underlying data. The resulting combined flow is launched in the `viewModelScope` to manage its
   * lifecycle automatically.
   */
  private fun observeDataSources() {
    combine(gpsService.positionState, hazardsService.fetcherState) {
            newPositionState,
            newHazardState ->
          val severities = computeSeverities(newHazardState.hazards)

          _uiState.update {
            it.copy(
                positionState = newPositionState,
                hazardState = newHazardState,
                severitiesByType = severities)
          }
        }
        .launchIn(viewModelScope)
  }

  /**
   * Called when the UI is about to ask the user for permissions. This is used to prevent multiple
   * permission requests from being launched at the same time.
   */
  fun onPermissionsRequestStart() {
    _uiState.update { it.copy(isOsRequestInFlight = true) }
  }

  /**
   * Updates the UI state with the latest permission status. This function should be called after a
   * permission request has been completed (e.g., in onResume or after the system permission dialog
   * is dismissed). It checks the current permission status, marks that the permission has been
   * requested at least once, and updates the UI state accordingly.
   *
   * @param activity The current activity, used as context to check for rationales.
   */
  fun applyPermissionsResult(activity: Activity) {
    val result = permissionManager.getPermissionResult(locationPermissions, activity)
    permissionManager.markPermissionsAsAsked(locationPermissions)
    _uiState.update { it.copy(permissionResult = result, isOsRequestInFlight = false) }
  }

  /** Request a single location update and start location updates. */
  fun startLocationUpdate() {
    viewModelScope.launch {
      gpsService.requestCurrentLocation()
      gpsService.startLocationUpdates()
    }
  }

  /** Stop location updates. */
  fun stopLocationUpdate() {
    viewModelScope.launch { gpsService.stopLocationUpdates() }
  }

  /**
   * Sets the location tracking state for the UI.
   *
   * @param enabled True to enable UI tracking, false to disable it.
   */
  fun setTracking(enabled: Boolean) {
    _uiState.update { it.copy(isTrackingLocation = enabled) }
  }

  /**
   * Computes a map of severities from a list of hazards.
   *
   * This function processes a list of `Hazard` objects and groups them by their `severity` level.
   * It returns a map where each key is a `Severity` enum and the corresponding value is a list of
   * all hazards that have that severity.
   *
   * Example: If the input list contains two "High" severity hazards and one "Medium" severity
   * hazard, the output map will be: ` { HIGH: [hazard1, hazard2], MEDIUM: [\hazard3] } `
   *
   * @param hazards A list of `Hazard` objects to be processed.
   * @return A `Map<Severity, List<Hazard>>` grouping hazards by their severity.
   */
  private fun computeSeverities(hazards: List<Hazard>): Map<String, Pair<Double, Double>> {
    return hazards
        .filter { it.type != null && it.severity != null }
        .groupBy { it.type }
        .map { group ->
          val minSev = group.value.minOf { hazard -> hazard.severity ?: 0.0 }
          val maxSev = group.value.maxOf { hazard -> hazard.severity ?: 0.0 }
          (group.key ?: "Unknown") to Pair(minSev, maxSev)
        }
        .toMap()
  }
}
