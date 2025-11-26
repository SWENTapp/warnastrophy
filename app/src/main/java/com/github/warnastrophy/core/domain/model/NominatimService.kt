/**
 * This file was written by Anas Sidi Mohamed with the assistance of ChatGPT.
 *
 * Author: Anas Sidi Mohamed Assistance: ChatGPT
 */
package com.github.warnastrophy.core.domain.model

import com.github.warnastrophy.core.data.repository.GeocodeRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface GeocodeService {
  val nominatimState: StateFlow<List<Location>>

  fun searchQuery(query: String)
}

/**
 * A service class that manages geocoding queries using a Nominatim repository.
 *
 * This class provides functionality to perform location searches while respecting rate limits. It
 * maintains a state of the latest search results and ensures that only one search query is executed
 * at a time. The results are exposed as a `StateFlow` for reactive collection.
 *
 * @property repository The Nominatim repository used to perform geocoding queries.
 */
data class NominatimState(
    val locations: List<Location> = emptyList(),
    val selectedLocation: Location? = null
)

class NominatimService(
    private val repository: GeocodeRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : GeocodeService {

  private val rootJob = Job()
  private val scope = CoroutineScope(dispatcher + rootJob)

  private val _locations = MutableStateFlow<List<Location>>(emptyList())
  override val nominatimState: StateFlow<List<Location>> = _locations.asStateFlow()

  private var currentSearchJob: Job? = null

  /**
   * Initiates a search query for the given location string.
   *
   * If a previous search is still running, it will be canceled. The method respects the rate limit
   * by delaying the query if necessary. The results of the query are stored in the `_locations`
   * state and can be collected via the `locations` property.
   *
   * @param query The location string to search for.
   */
  override fun searchQuery(query: String) {
    if (currentSearchJob?.isActive == true) {
      currentSearchJob?.cancel()
    }
    currentSearchJob =
        scope.launch {
          val waitMs = repository.delayForNextQuery()
          delay(waitMs)

          val result = repository.reverseGeocode(query)

          _locations.value = result
        }
  }
}

class MockNominatimService : GeocodeService {

  private val _locations = MutableStateFlow<List<Location>>(emptyList())
  override val nominatimState: StateFlow<List<Location>> = _locations.asStateFlow()

  override fun searchQuery(query: String) {}

  fun setLocations(locations: List<Location>) {
    _locations.value = locations
  }
}
