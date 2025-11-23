// kotlin
package com.github.warnastrophy.core.domain.model

import com.github.warnastrophy.core.ui.repository.NominatimRepository
import com.github.warnastrophy.core.domain.model.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A service class that manages geocoding queries using a Nominatim repository.
 *
 * This class provides functionality to perform location searches while respecting rate limits.
 * It maintains a state of the latest search results and ensures that only one search query is
 * executed at a time. The results are exposed as a `StateFlow` for reactive collection.
 *
 * @property repository The Nominatim repository used to perform geocoding queries.
 */
class NominatimService(
    private val repository: NominatimRepository
) {

    private val rootJob = Job()
    private val scope = CoroutineScope(Dispatchers.IO + rootJob)

    private val _locations = MutableStateFlow<List<Location>>(emptyList())
    val locations: StateFlow<List<Location>> = _locations.asStateFlow()

    private var currentSearchJob: Job? = null

    /**
     * Initiates a search query for the given location string.
     *
     * If a previous search is still running, it will be canceled. The method respects the rate
     * limit by delaying the query if necessary. The results of the query are stored in the
     * `_locations` state and can be collected via the `locations` property.
     *
     * @param query The location string to search for.
     */
    fun searchQuery(query: String) {
        currentSearchJob?.cancel()

        currentSearchJob = scope.launch {
            val waitMs = repository.DelayForNextQuery()
            delay(waitMs)

            val result = repository.reverseGeocode(query)

            _locations.value = result
        }
    }
}
