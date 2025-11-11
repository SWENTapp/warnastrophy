package com.github.warnastrophy.core.data.repository.usecase

import com.github.warnastrophy.core.model.HazardsDataService
import com.github.warnastrophy.core.model.Location

/**
 * A use case that controls when to refresh hazard data from the server.
 *
 * This acts as a rate limiter/throttle for hazard fetching, ensuring the service is only called if
 * the user has moved beyond a specific distance threshold (e.g., 5 km) from the location where the
 * last successful fetch occurred.
 *
 * @property hazardsService The service responsible for fetching hazard data.
 * @property distanceThresholdKm The minimum distance (in kilometers) the user must move before a
 *   new fetch is allowed. Defaults to 5.0 km.
 * @property distanceCalculator A lambda function to calculate the distance between two [Location]
 *   objects.
 */
class RefreshHazardsIfMovedUseCase(
    private val hazardsService: HazardsDataService,
    private val distanceThresholdKm: Double = 5.0,
    private val distanceCalculator: (Location, Location) -> Double = { start, end ->
      Location.distanceBetween(start, end)
    }
) {
  private var lastFetchLocation: Location? = null

  /**
   * Determines if a new hazard data fetch should be triggered based on the user's movement.
   *
   * Fetch is triggered if the user is moving for the first time, OR if the current position is
   * beyond the predefined distance threshold from the last successfully fetched location.
   *
   * The function safely handles the mutable 'lastFetchLocation' property by creating an immutable
   * local snapshot to avoid concurrency issues (the smart cast error).
   *
   * @param currentLocation The user's latest geographic position.
   * @return True if a new fetch should be executed; False if the user has not moved far enough.
   */
  fun shouldFetchHazards(currentLocation: Location): Boolean {
    val lastLocation = lastFetchLocation
    if (lastLocation == null) {
      return true
    }
    return distanceCalculator(lastLocation, currentLocation) > distanceThresholdKm
  }

  /**
   * Checks if the distance criteria is met and triggers a fetch if necessary.
   *
   * @param currentLocation The latest position from the GPS service.
   */
  fun execute(currentLocation: Location) {
    if (shouldFetchHazards(currentLocation)) {
      hazardsService.fetchHazardsAroundUser()
      lastFetchLocation = currentLocation
    }
  }
}
