package com.github.warnastrophy.core.data.repository.usecase

import com.github.warnastrophy.core.model.HazardsDataService
import com.github.warnastrophy.core.model.Location

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
