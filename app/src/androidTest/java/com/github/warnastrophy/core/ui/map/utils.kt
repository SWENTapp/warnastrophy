package com.github.warnastrophy.core.ui.map

import android.app.Activity
import com.github.warnastrophy.core.data.repository.HazardsDataSource
import com.github.warnastrophy.core.domain.model.FetcherState
import com.github.warnastrophy.core.domain.model.GpsPositionState
import com.github.warnastrophy.core.domain.model.GpsResult
import com.github.warnastrophy.core.domain.model.Hazard
import com.github.warnastrophy.core.domain.model.HazardsDataService
import com.github.warnastrophy.core.domain.model.Location
import com.github.warnastrophy.core.domain.model.PositionService
import com.github.warnastrophy.core.permissions.AppPermissions
import com.github.warnastrophy.core.permissions.PermissionManager
import com.github.warnastrophy.core.permissions.PermissionManagerInterface
import com.github.warnastrophy.core.permissions.PermissionResult
import com.github.warnastrophy.core.ui.common.ErrorHandler
import com.github.warnastrophy.core.ui.repository.GeocodeRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.mockito.Mockito

val factory = GeometryFactory()
val location_a = Location(18.55, -72.34)
val location_b = Location(18.64, -72.10)
val hazardList =
    listOf(
        createHazard(
            id = 1,
            type = "FL", // will map to HUE_GREEN
            // coordinates = listOf(Location(18.55, -72.34))).,
            centroid = factory.createPoint(Coordinate(location_a.longitude, location_a.latitude))),
        createHazard(
            id = 2,
            type = "EQ", // will map to HUE_RED
            // coordinates = listOf(Location(18.61, -72.22), Location(18.64, -72.10))
            centroid = factory.createPoint(Coordinate(location_b.longitude, location_b.latitude))))

val pos: LatLng = LatLng(18.61, -72.22)

fun createHazard(
    id: Int? = null,
    type: String? = null,
    description: String? = null,
    country: String? = null,
    date: String? = null,
    severity: Double? = null,
    severityUnit: String? = null,
    articleUrl: String? = null,
    alertLevel: Double? = null,
    centroid: Geometry? = null,
    bbox: List<Double>? = null,
    affectedZone: Geometry? = null
) =
    Hazard(
        id = id,
        type = type,
        description = description,
        country = country,
        date = date,
        severity = severity,
        severityUnit = severityUnit,
        articleUrl = articleUrl,
        alertLevel = alertLevel,
        centroid = centroid,
        bbox = bbox,
        affectedZone = affectedZone)

class GpsServiceMock(initial: LatLng = pos) : PositionService {

  override val positionState = MutableStateFlow(GpsPositionState(position = initial))

  override val locationClient: FusedLocationProviderClient = Mockito.mock()

  override val errorHandler = ErrorHandler()

  /** Whether updates are "started" (just for test verification) */
  var isLocationUpdated = false
    private set

  fun setPosition(position: LatLng) {
    positionState.value =
        GpsPositionState(position, result = GpsResult.Success("Fake position emitted"))
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
  override suspend fun getPartialAreaHazards(geometry: String, days: String): List<Hazard> {
    return hazards.map { it.copy(articleUrl = null, bbox = null, affectedZone = null) }
  }

  override suspend fun completeParsingOf(hazard: Hazard): Hazard? {
    return hazards.find { it.id == hazard.id }
  }
}

class HazardServiceMock(hazards: List<Hazard> = hazardList, position: LatLng = pos) :
    HazardsDataService {
  override suspend fun fetchHazardsForLocation(geometry: String, days: String) = hazardList

  override val fetcherState = MutableStateFlow(FetcherState(hazards))
  override val gpsService: PositionService = GpsServiceMock(position)
  override val errorHandler: ErrorHandler = ErrorHandler()
  override val repository = HazardsRepositoryMock(hazards)

  fun setHazards(hazards: List<Hazard>) {
    fetcherState.value = FetcherState(hazards = hazards)
  }

  var fetchCount: Int = 0

  override fun fetchHazardsAroundUser() {
    fetchCount++
  }

  fun assertFetchCalled(expectedCount: Int) {
    assertEquals(expectedCount, fetchCount)
  }
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

class MockNominatimRepository : GeocodeRepository {

  val locations =
      listOf(
          Location(40.7128, -74.0060, "Suvy"),
          Location(40.0583, -74.4057, "Tolar"),
          Location(-40.9006, 174.8860, "New Zok"))

  override suspend fun reverseGeocode(location: String): List<Location> {
    return locations
  }
}
