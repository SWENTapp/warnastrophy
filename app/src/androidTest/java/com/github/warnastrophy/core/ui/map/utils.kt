package com.github.warnastrophy.core.ui.map

import com.github.warnastrophy.core.data.repository.HazardsDataSource
import com.github.warnastrophy.core.model.GpsPositionState
import com.github.warnastrophy.core.model.Hazard
import com.github.warnastrophy.core.model.HazardsDataService
import com.github.warnastrophy.core.model.PositionService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class GpsServiceMock(initial: LatLng) : PositionService {
  override val positionState = MutableStateFlow(GpsPositionState(position = initial))

  override fun requestCurrentLocation(locationClient: FusedLocationProviderClient) {
    // No-op for mock
  }

  override fun startLocationUpdates(locationClient: FusedLocationProviderClient) {
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

  override val currentHazardsState: StateFlow<List<Hazard>> = MutableStateFlow(hazards)
  override val gpsService: PositionService = GpsServiceMock(position)
  override val repository = HazardsRepositoryMock(hazards)
}
