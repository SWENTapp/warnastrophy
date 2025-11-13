package com.github.warnastrophy.core.data.service

import com.github.warnastrophy.core.model.Location
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Monitor the user's location and triggering hazard data updates based on movement.
 *
 * This class uses Kotlin Coroutines to subscribe to a continuous stream of GPS position updates
 * from the [PositionService] and delegates the movement check and network fetch logic to the
 * [RefreshHazardsIfMovedUseCase].
 *
 * @property gpsService The service providing the continuous stream of user position data via Flow.
 * @property refreshHazardsIfMovedUseCase The use case responsible for checking distance moved and
 *   executing the hazard data network fetch.
 * @property serviceScope The [CoroutineScope] used to launch and manage the tracking coroutine. It
 *   should typically be tied to the application's process lifecycle (e.g., [Application] scope).
 */
class HazardTrackingService(
    private val gpsService: PositionService? = null,
    private val refreshHazardsIfMovedUseCase: RefreshHazardsIfMovedUseCase? = null,
    private val serviceScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
  private var isTracking = false

  /**
   * Starts the hazard tracking process by launching a coroutine that listens to the positionState
   * flow and executes the use case.
   */
  fun startTracking() {
    if (isTracking) return

    isTracking = true

    serviceScope.launch {
      gpsService?.positionState?.collectLatest { positionState ->
        positionState.position.let { currentLocation ->
          val hazardLocation = Location(currentLocation.latitude, currentLocation.longitude)
          refreshHazardsIfMovedUseCase?.execute(hazardLocation)
        }
      }
    }
  }

  /** Cancels the coroutine scope and stops tracking. */
  fun stopTracking() {
    if (!isTracking) return
    serviceScope.cancel()
    isTracking = false
  }
}
