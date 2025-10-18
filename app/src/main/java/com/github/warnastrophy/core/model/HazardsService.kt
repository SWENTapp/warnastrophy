package com.github.warnastrophy.core.model

import com.github.warnastrophy.core.data.repository.HazardsDataSource
import com.github.warnastrophy.core.util.AppConfig
import com.github.warnastrophy.core.util.AppConfig.fetchDelayMs
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service responsible for fetching and maintaining the current list of hazards based on the user's
 * position.
 *
 * @property repository Data source used to retrieve hazards.
 * @property gpsService Service providing the current GPS position.
 */
class HazardsService(
    private val repository: HazardsDataSource,
    private val gpsService: PositionService
) {
  /** Coroutine scope used for background hazard fetching. */
  private val serviceScope = CoroutineScope(Dispatchers.IO)

  /** Internal state flow holding the current list of hazards. */
  private val _currentHazardsState = MutableStateFlow<List<Hazard>>(emptyList())

  /** Public state flow exposing the current list of hazards. */
  val currentHazardsState = _currentHazardsState.asStateFlow()

  /** Initializes the service and starts periodic hazard fetching based on the user's position. */
  init {
    serviceScope.launch {
      while (isActive) {
        val currPosition =
            Location(
                latitude = gpsService.positionState.value.position.latitude,
                longitude = gpsService.positionState.value.position.longitude)
        val polygon =
            Location.getPolygon(
                currPosition,
                AppConfig.rectangleHazardZone.first,
                AppConfig.rectangleHazardZone.second)

        val wktPolygon = Location.locationsToWktPolygon(polygon)
        val hazards = fetchHazards(wktPolygon)
        _currentHazardsState.value = hazards
        delay(fetchDelayMs)
      }
    }
  }

  /**
   * Fetches hazards for the given polygon and number of days.
   *
   * @param polygon The polygon in WKT format representing the area to search for hazards.
   * @param days The number of days to look back for hazards (default: [AppConfig.priorDaysFetch]).
   * @return A list of hazards found in the specified area and time frame.
   */
  suspend fun fetchHazards(polygon: String, days: String = AppConfig.priorDaysFetch): List<Hazard> {
    return repository.getAreaHazards(polygon, days)
  }

  /** Cancels the background hazard fetching and releases resources. */
  fun close() {
    serviceScope.cancel()
  }
}
