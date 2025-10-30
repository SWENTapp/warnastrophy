package com.github.warnastrophy.core.model

import android.util.Log
import com.github.warnastrophy.core.data.repository.HazardRepositoryProvider
import com.github.warnastrophy.core.data.repository.HazardsDataSource
import com.github.warnastrophy.core.ui.navigation.Screen
import com.github.warnastrophy.core.util.AppConfig
import com.github.warnastrophy.core.util.AppConfig.fetchDelayMs
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service responsible for fetching and maintaining the current list of hazards based on the user's
 * position.
 *
 * @property repository Data source used to retrieve hazards.
 * @property gpsService Service providing the current GPS position.
 */
interface HazardsDataService {
  val repository: HazardsDataSource
  val gpsService: PositionService

  val errorHandler: ErrorHandler

  val fetcherState: StateFlow<FetcherState>

  suspend fun fetchHazards(polygon: String, days: String = AppConfig.priorDaysFetch): List<Hazard>
}

class HazardsService(
    override val repository: HazardsDataSource,
    override val gpsService: PositionService,
    override val errorHandler: ErrorHandler = ErrorHandler(),
) : HazardsDataService {
  /** Coroutine scope used for background hazard fetching. */
  private val serviceScope = CoroutineScope(Dispatchers.IO)

  /** Internal state flow holding the current list of hazards. */
  private val _fetcherState = MutableStateFlow(FetcherState())

  /** Public state flow exposing the current list of hazards. */
  override val fetcherState = _fetcherState.asStateFlow()

  /** Initializes the service and starts periodic hazard fetching based on the user's position. */
  init {
    serviceScope.launch {
      while (isActive) {
        // for now we only use a fixed polygon from the repository provider
        /*
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
           */
        try {
          val hazards = fetchHazards(HazardRepositoryProvider.locationPolygon)
          _fetcherState.value = _fetcherState.value.copy(hazards = hazards)
        } catch (e: Exception) {
          Log.e("HazardsService", "Error fetching hazards", e)
          errorHandler.addError(
              "Error fetching hazards: ${e.message ?: "Unknown error"}", Screen.MAP)
        }
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
  override suspend fun fetchHazards(polygon: String, days: String): List<Hazard> {
    return repository.getAreaHazards(polygon, days)
  }

  /** Cancels the background hazard fetching and releases resources. */
  fun close() {
    serviceScope.cancel()
  }
}

data class FetcherState(
    val hazards: List<Hazard> = emptyList(),
    val isLoading: Boolean = false,
    val errorMsg: String? = null
)
