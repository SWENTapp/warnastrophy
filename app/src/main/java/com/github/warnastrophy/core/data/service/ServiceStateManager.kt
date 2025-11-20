package com.github.warnastrophy.core.data.service

import android.content.Context
import com.github.warnastrophy.core.data.repository.HazardRepositoryProvider
import com.github.warnastrophy.core.domain.model.GpsService
import com.github.warnastrophy.core.domain.model.Hazard
import com.github.warnastrophy.core.domain.model.HazardsDataService
import com.github.warnastrophy.core.domain.model.HazardsService
import com.github.warnastrophy.core.domain.usecase.HazardChecker
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

object ServiceStateManager {
  val serviceScope = CoroutineScope(Dispatchers.IO)
  lateinit var gpsService: GpsService
  lateinit var hazardsService: HazardsDataService
  lateinit var dangerModeService: DangerModeService
  private val _activeHazardFlow = MutableStateFlow<Hazard?>(null)

  val activeHazardFlow: StateFlow<Hazard?> = _activeHazardFlow.asStateFlow()

  /**
   * Called by the HazardGeofencingService to update the alert status. This is thread-safe and
   * updates all collectors instantly.
   *
   * @param hazard The new Hazard object the user is currently inside, or null if safe.
   */
  fun updateActiveHazard(hazard: Hazard?) {
    // Only update if the state has genuinely changed to avoid unnecessary UI redraws
    if (_activeHazardFlow.value != hazard) {
      _activeHazardFlow.value = hazard
    }
  }

  /**
   * Optional: Helper function for the ViewModel to acknowledge and clear an alert if the user
   * dismisses the notification/modal manually.
   */
  fun clearActiveAlert() {
    if (_activeHazardFlow.value != null) {
      _activeHazardFlow.value = null
    }
  }

  fun init(context: Context) {
    val locationClient = LocationServices.getFusedLocationProviderClient(context)
    gpsService = GpsService(locationClient)

    hazardsService =
        HazardsService(
            HazardRepositoryProvider.repository,
            gpsService,
        )

    // Subscribe to hazard updates to keep the active hazard flow current
    serviceScope.launch {
      hazardsService.fetcherState.collectLatest {
        HazardChecker(it.hazards, Dispatchers.IO, serviceScope)
            .checkAndPublishAlert(
                gpsService.positionState.value.position.longitude,
                gpsService.positionState.value.position.latitude)
      }
    }

    dangerModeService = DangerModeService()
  }
}
