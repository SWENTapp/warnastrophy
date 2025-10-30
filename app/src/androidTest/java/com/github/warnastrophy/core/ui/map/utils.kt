package com.github.warnastrophy.core.ui.map

import com.github.warnastrophy.core.data.repository.HazardsDataSource
import com.github.warnastrophy.core.model.FetcherState
import com.github.warnastrophy.core.model.GpsPositionState
import com.github.warnastrophy.core.model.Hazard
import com.github.warnastrophy.core.model.HazardsDataService
import com.github.warnastrophy.core.model.PositionService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.mockito.Mockito
import org.mockito.kotlin.mock

class GpsServiceMock(initial: LatLng) : PositionService {

  override val positionState = MutableStateFlow(GpsPositionState(position = initial))

  override val locationClient: FusedLocationProviderClient =
      Mockito.mock<FusedLocationProviderClient>()

  override fun requestCurrentLocation() {
    // No-op for mock
  }

  override fun startLocationUpdates() {
    // No-op for mock
  }
}

class HazardsRepositoryMock(private val hazards: List<Hazard>) : HazardsDataSource {
  override suspend fun getAreaHazards(geometry: String, days: String): List<Hazard> {
    return hazards
  }
}

class HazardServiceMock(val hazards: List<Hazard>, val position: LatLng) : HazardsDataService {
  override suspend fun fetchHazards(geometry: String, days: String) = emptyList<Hazard>()

  override val fetcherState: StateFlow<FetcherState> = MutableStateFlow(FetcherState(hazards))
  override val gpsService: PositionService = GpsServiceMock(position)
  override val repository = HazardsRepositoryMock(hazards)
}
