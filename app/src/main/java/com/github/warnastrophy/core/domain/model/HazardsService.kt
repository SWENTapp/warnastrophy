package com.github.warnastrophy.core.domain.model

import android.util.Log
import com.github.warnastrophy.core.data.repository.HazardRepositoryProvider
import com.github.warnastrophy.core.data.repository.HazardsDataSource
import com.github.warnastrophy.core.ui.common.ErrorHandler
import com.github.warnastrophy.core.ui.navigation.Screen
import com.github.warnastrophy.core.util.AppConfig
import javax.inject.Inject
import kotlin.time.TimeSource
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Singleton

/**
 * Service responsible for fetching and maintaining the current list of hazards based on the user's
 * position.
 *
 * @property repository Data source used to retrieve hazards.
 * @property gpsService Service providing the current GPS position.
 * @property errorHandler Handler for managing errors that occur during hazard fetching.
 */
interface HazardsDataService {
  val repository: HazardsDataSource
  val gpsService: PositionService

  val errorHandler: ErrorHandler

  val fetcherState: StateFlow<FetcherState>

  suspend fun fetchHazardsForLocation(
      geometry: String,
      days: String = AppConfig.priorDaysFetch
  ): List<Hazard>

  fun fetchHazardsAroundUser()
}

@Singleton
class HazardsService
@Inject
constructor(
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
          val lastFetch = TimeSource.Monotonic.markNow()
          _fetcherState.value =
              _fetcherState.value.copy(
                  hazards = fetchHazardsForLocation(HazardRepositoryProvider.locationPolygon),
                  isLoading = false)

          // Fetch complete data sequentially for each hazard to avoid making the UI wait
          val currentHazards = _fetcherState.value.hazards.toMutableList()
          currentHazards.forEachIndexed { index, hazard ->
            repository.completeParsingOf(hazard)?.let {
              // Mutate the list in place
              currentHazards[index] = it // Assign the completed hazard back to the list
              _fetcherState.value =
                  _fetcherState.value.copy(hazards = currentHazards.toList(), isLoading = false)
            }
          }
          Log.i("HazardsService", "Fetched ${_fetcherState.value.hazards.size} hazards")
          delay(AppConfig.gdacsFetchDelay - lastFetch.elapsedNow())
        } catch (e: Exception) {
          Log.e("HazardsService", "Error fetching hazards", e)
          errorHandler.addError(
              "Error fetching hazards: ${e.message ?: "Unknown error"}", Screen.Map)
          _fetcherState.value =
              _fetcherState.value.copy(errorMsg = "Error fetching hazards", isLoading = false)
        }
      }
    }
  }

  /**
   * Fetches hazards for the given polygon and number of days.
   *
   * @param geometry The polygon in WKT format representing the area to search for hazards.
   * @param days The number of days to look back for hazards (default: [AppConfig.priorDaysFetch]).
   * @return A list of hazards found in the specified area and time frame.
   */
  override suspend fun fetchHazardsForLocation(geometry: String, days: String): List<Hazard> {
    return repository.getPartialAreaHazards(geometry, days)
  }

  override fun fetchHazardsAroundUser() {
    TODO("Not yet implemented")
  }

  /** Cancels the background hazard fetching and releases resources. */
  fun close() {
    serviceScope.cancel()
  }
}

/**
 * Data class representing the state of the hazard fetcher, including the list of hazards, loading
 * status, and any error messages.
 *
 * @property hazards List of currently fetched hazards.
 * @property isLoading Indicates whether a fetch operation is in progress.
 * @property errorMsg Optional error message if an error occurred during fetching.
 */
data class FetcherState(
    val hazards: List<Hazard> = emptyList(),
    val isLoading: Boolean = false,
    val errorMsg: String? = null
)

class HazardsServiceFactory(
    private val repository: HazardsDataSource,
    private val gpsService: PositionService,
    private val errorHandler: ErrorHandler
) {
  fun create(): HazardsService {
    return HazardsService(repository, gpsService, errorHandler)
  }
}
