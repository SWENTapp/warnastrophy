package com.github.warnastrophy.core.data.service

import android.content.Context
import android.util.Log
import com.github.warnastrophy.core.data.Provider.HazardRepositoryProvider
import com.github.warnastrophy.core.data.permissions.PermissionManager
import com.github.warnastrophy.core.data.permissions.PermissionManagerInterface
import com.github.warnastrophy.core.domain.usecase.HazardCheckerService
import com.github.warnastrophy.core.model.Hazard
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object StateManagerService {
  private val serviceScope = CoroutineScope(Dispatchers.IO)
  private val hazardCheckerScope = CoroutineScope(Dispatchers.Main)

  lateinit var gpsService: PositionService
  lateinit var hazardsService: HazardsDataService
  lateinit var permissionManager: PermissionManagerInterface
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

    permissionManager = PermissionManager(context)

    dangerModeService = DangerModeService(permissionManager = permissionManager)

    startHazardSubscription()
  }

  /** Overload for tests or DI where services are provided directly. */
  fun init(
      gpsService: PositionService,
      hazardsService: HazardsDataService,
      dangerModeService: DangerModeService
  ) {
    this.gpsService = gpsService
    this.hazardsService = hazardsService
    this.dangerModeService = dangerModeService

    startHazardSubscription()
  }

  /**
   * Starts a coroutine to monitor hazard and GPS position updates, checking for hazard alerts
   * whenever either changes.
   *
   * This function combines the hazard fetcher state and GPS position state flows, and on each
   * update, it cancels any ongoing hazard checks and initiates a new check using the
   * [HazardCheckerService].
   */
  private fun startHazardSubscription() {
    serviceScope.launch {
      kotlinx.coroutines.flow
          .combine(hazardsService.fetcherState, gpsService.positionState) {
              fetcherState,
              positionState ->
            fetcherState to positionState
          }
          .collect { (fetcherState, positionState) ->
            Log.d("ServiceStateManager", "${fetcherState.hazards}")
            Log.d("ServiceStateManager", "$positionState")
            Log.d("ServiceStateManager", "Checking hazards for position update")
            hazardCheckerScope.cancel()
            HazardCheckerService(fetcherState.hazards, Dispatchers.Main, hazardCheckerScope)
                .checkAndPublishAlert(
                    positionState.position.longitude, positionState.position.latitude)
          }
    }
  }
}
