package com.github.warnastrophy.core.data.service

import android.content.Context
import com.github.warnastrophy.core.data.provider.ActivityRepositoryProvider
import com.github.warnastrophy.core.data.provider.HazardRepositoryProvider
import com.github.warnastrophy.core.data.provider.UserPreferencesRepositoryProvider
import com.github.warnastrophy.core.data.repository.ActivityRepository
import com.github.warnastrophy.core.data.repository.MovementSensorRepository
import com.github.warnastrophy.core.data.repository.UserPreferencesRepository
import com.github.warnastrophy.core.domain.usecase.HazardCheckerService
import com.github.warnastrophy.core.model.Hazard
import com.github.warnastrophy.core.permissions.PermissionManager
import com.github.warnastrophy.core.permissions.PermissionManagerInterface
import com.github.warnastrophy.core.ui.common.ErrorHandler
import com.github.warnastrophy.core.util.startForegroundGpsService
import com.github.warnastrophy.core.util.stopForegroundGpsService
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Global singleton object serving as the central Service Locator and orchestrator for core
 * application services.
 *
 * This service is responsible for:
 * 1. **Initialization:** Creating and wiring up all dependencies (GPS, Hazards, Permission Manager,
 *    etc.) at application startup.
 * 2. **Lifecycle Management:** Providing a unified `shutdown()` method to release resources.
 * 3. **Data Coordination:** Combining the location stream (`gpsService.positionState`) with the
 *    fetched hazard data (`hazardsService.fetcherState`) to trigger the hazard checking logic.
 * 4. **State Management:** Exposing a central StateFlow (`activeHazardFlow`) to inform UI/other
 *    services when the user enters a dangerous zone.
 */
object StateManagerService {
  private lateinit var appContext: Context
  private var initialized = false
  private val serviceScope = CoroutineScope(Dispatchers.IO)
  private val hazardCheckerScope = CoroutineScope(Dispatchers.Main)
  private var hazardCheckerJob: Job? = null

  lateinit var gpsService: PositionService
  lateinit var hazardsService: HazardsDataService
  lateinit var permissionManager: PermissionManagerInterface
  lateinit var errorHandler: ErrorHandler
  lateinit var dangerModeService: DangerModeService
  lateinit var movementService: MovementService
  lateinit var dangerModeOrchestrator: DangerModeOrchestrator
  private val _activeHazardFlow = MutableStateFlow<Hazard?>(null)

  private val dangerModeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  val userPreferencesRepository: UserPreferencesRepository
    get() = UserPreferencesRepositoryProvider.repository

  val activityRepository: ActivityRepository
    get() = ActivityRepositoryProvider.repository

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
    if (initialized) return
    this.appContext = context.applicationContext

    val locationClient = LocationServices.getFusedLocationProviderClient(context)

    errorHandler = ErrorHandler()

    ActivityRepositoryProvider.init(context, errorHandler)

    gpsService = GpsService(locationClient, errorHandler)

    hazardsService = HazardsService(HazardRepositoryProvider.repository, gpsService, errorHandler)

    permissionManager = PermissionManager(context)

    dangerModeService =
        DangerModeService(
            activeHazardFlow = activeHazardFlow,
            serviceScope = dangerModeScope,
            permissionManager = permissionManager)

    movementService =
        MovementService(
            MovementSensorRepository(context), dangerModeStateFlow = dangerModeService.state)
    movementService.startListening()
    startForegroundGpsService(appContext)

    dangerModeOrchestrator =
        DangerModeOrchestrator(
            dangerModeService = dangerModeService,
            movementService = movementService,
            userPreferencesRepository = userPreferencesRepository,
            gpsService = gpsService)
    dangerModeOrchestrator.initialize(context)
    dangerModeOrchestrator.startMonitoring()

    startHazardSubscription()

    initialized = true
  }

  /** Overload for tests or DI where services are provided directly. */
  fun init(
      gpsService: PositionService,
      hazardsService: HazardsDataService,
      dangerModeService: DangerModeService,
      movementService: MovementService? = null,
      errorHandler: ErrorHandler = ErrorHandler()
  ) {
    this.gpsService = gpsService
    this.hazardsService = hazardsService
    this.dangerModeService = dangerModeService
    this.errorHandler = errorHandler
    if (movementService != null) {
      this.movementService = movementService
    }
    startHazardSubscription()
  }

  fun initForTests(
      gpsService: PositionService,
      hazardsService: HazardsDataService,
      permissionManager: PermissionManagerInterface,
      dangerModeService: DangerModeService
  ) {
    this.gpsService = gpsService
    this.hazardsService = hazardsService
    this.permissionManager = permissionManager
    this.dangerModeService = dangerModeService

    startHazardSubscription()
  }

  /** This method cancel all services to release ressources */
  fun shutdown() {
    serviceScope.cancel()
    dangerModeScope.cancel()
    // Close services
    movementService.stop()
    gpsService.stopLocationUpdates()
    hazardsService.close()
    dangerModeService.close()
    stopForegroundGpsService(appContext)
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
            hazardCheckerJob?.cancel()
            hazardCheckerJob =
                hazardCheckerScope.launch {
                  HazardCheckerService(fetcherState.hazards, Dispatchers.Main, hazardCheckerScope)
                      .checkAndPublishAlert(
                          positionState.position.longitude, positionState.position.latitude)
                }
          }
    }
  }
}
