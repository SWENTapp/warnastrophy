package com.github.warnastrophy.core.data.service

import com.github.warnastrophy.core.data.repository.usecase.RefreshHazardsIfMovedUseCase
import com.github.warnastrophy.core.model.Location
import com.github.warnastrophy.core.model.PositionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
    if (isTracking) return // Prevent multiple starts

    isTracking = true
    println("HazardTrackingManager started tracking.")

    serviceScope.launch {
      // Collects the latest position and updates hazards
      gpsService?.positionState?.collectLatest { positionState ->
        positionState.position.let { currentLocation ->
          // Assuming you have a way to map your position object to your Location object
          val hazardLocation = Location(currentLocation.latitude, currentLocation.longitude)

          refreshHazardsIfMovedUseCase?.execute(hazardLocation)
          println("Executed RefreshHazardsIfMovedUseCase at: $hazardLocation")
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
