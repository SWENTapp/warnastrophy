package com.github.warnastrophy.core.ui.map

import android.app.Activity
import com.github.warnastrophy.core.data.repository.HazardsDataSource
import com.github.warnastrophy.core.model.AppPermissions
import com.github.warnastrophy.core.model.ErrorHandler
import com.github.warnastrophy.core.model.FetcherState
import com.github.warnastrophy.core.model.GpsPositionState
import com.github.warnastrophy.core.model.Hazard
import com.github.warnastrophy.core.model.HazardsDataService
import com.github.warnastrophy.core.model.Location
import com.github.warnastrophy.core.model.PermissionManager
import com.github.warnastrophy.core.model.PermissionManagerInterface
import com.github.warnastrophy.core.model.PermissionResult
import com.github.warnastrophy.core.model.PositionService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.mockito.Mockito

val hazardList =
    listOf(
        Hazard(
            id = 1,
            type = "FL", // will map to HUE_GREEN
            description = null,
            country = null,
            date = null,
            severity = null,
            severityUnit = null,
            reportUrl = null,
            alertLevel = null,
            coordinates = listOf(Location(18.55, -72.34))),
        Hazard(
            id = 2,
            type = "EQ", // will map to HUE_RED
            description = null,
            country = null,
            date = null,
            severity = null,
            severityUnit = null,
            reportUrl = null,
            alertLevel = null,
            coordinates = listOf(Location(18.61, -72.22), Location(18.64, -72.10))))

val pos: LatLng = LatLng(18.61, -72.22)

class GpsServiceMock(initial: LatLng = pos) : PositionService {

  override val positionState = MutableStateFlow(GpsPositionState(position = initial))

  override val locationClient: FusedLocationProviderClient = Mockito.mock()

  override val errorHandler = ErrorHandler()

  var isLocationUpdated = false

  fun setPosition(position: LatLng) {
    positionState.value = GpsPositionState(position)
  }

  override fun requestCurrentLocation() {
    // No-op for mock
  }

  override fun startLocationUpdates() {
    isLocationUpdated = true
  }

  override fun stopLocationUpdates() {
    isLocationUpdated = false
  }
}

class HazardsRepositoryMock(private val hazards: List<Hazard>) : HazardsDataSource {
  override suspend fun getAreaHazards(geometry: String, days: String): List<Hazard> {
    return hazards
  }
}

class HazardServiceMock(hazards: List<Hazard> = hazardList, position: LatLng = pos) :
    HazardsDataService {
  override suspend fun fetchHazards(geometry: String, days: String) = emptyList<Hazard>()

  override val fetcherState: StateFlow<FetcherState> = MutableStateFlow(FetcherState(hazards))
  override val gpsService: PositionService = GpsServiceMock(position)
  override val errorHandler: ErrorHandler = ErrorHandler()
  override val repository = HazardsRepositoryMock(hazards)
}

/**
 * A test-friendly subclass of [PermissionManager] that allows manually controlling the permission
 * state without requiring Android APIs.
 */
class MockPermissionManager(
    private var currentResult: PermissionResult = PermissionResult.Denied(listOf("FAKE_PERMISSION"))
) : PermissionManagerInterface {

  /** Sets what result should be returned for permission checks. */
  fun setPermissionResult(result: PermissionResult) {
    currentResult = result
  }

  /** Returns the injected permission result instead of checking Android APIs. */
  override fun getPermissionResult(permissionType: AppPermissions): PermissionResult {
    return currentResult
  }

  /** Also override the Activity-based version (so tests can call either). */
  override fun getPermissionResult(
      permissionType: AppPermissions,
      activity: Activity
  ): PermissionResult {
    return currentResult
  }

  override fun markPermissionsAsAsked(permissionType: AppPermissions) {
    // no-op for tests
  }

  override fun isPermissionAskedBefore(permissionType: AppPermissions): Boolean {
    // Optional: you can simulate “has been asked” logic
    return true
  }
}
