/**
 * This file was written by Anas Sidi Mohamed with the assistance of ChatGPT.
 *
 * Author: Anas Sidi Mohamed Assistance: ChatGPT
 */
package com.github.warnastrophy.core.data.service

import android.util.Log
import com.github.warnastrophy.core.data.repository.MovementSensorRepository
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** TODO: Document MovementService */
class MovementService(
    private val repository: MovementSensorRepository,
    initialConfig: MovementConfig = MovementConfig(),
    private val timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic
) {

  /** Root job for the service coroutine scope. Cancelling this stops the collector. */
  private var collectionJob: Job? = null
  /** Coroutine scope used to collect the repository flow. Runs on [Dispatchers.Default]. */
  private val scope = CoroutineScope(Dispatchers.Default + Job())

  /** Backing _StateFlow_ that emits the current movement state. */
  private val _movementState =
      MutableStateFlow<MovementState>(MovementState.Safe(timeSource.markNow()))

  /**
   * Public read-only [StateFlow] that observers can collect to get continuous updates of the
   * current movement state.
   */
  val movementState: StateFlow<MovementState> = _movementState

  /** Current movement detection configuration. */
  var config = initialConfig
    private set

  /**
   * Start collecting motion samples from the repository.
   *
   * This function launches a coroutine that collects `repository.data` and updates the internal
   * movement state based on acceleration magnitude thresholds defined in [config]. The collector
   * runs until [stop] is called which cancels the internal job.
   */
  fun startListening() {
    // If already listening, do nothing
    if (collectionJob?.isActive == true) return

    collectionJob =
        scope.launch {
          Log.d("MovementService", "Started listening to motion data")
          repository.data.collect { motion ->
            Log.d("MovementService", "Received motion data: accMag=${motion.accelerationMagnitude}")
            // State update logic based on acceleration magnitude
            _movementState.value =
                when (val currentState = _movementState.value) {
                  is MovementState.Safe -> {
                    if (motion.accelerationMagnitude > config.preDangerThreshold) {
                      Log.d("MovementService", "State changed to PRE_DANGER")
                      MovementState.PreDanger(timeSource.markNow())
                    } else {
                      currentState
                    }
                  }
                  is MovementState.PreDanger -> {
                    if (motion.accelerationMagnitude <= config.dangerAverageThreshold) {
                      Log.d("MovementService", "State changed to PRE_DANGER_ACC")
                      MovementState.PreDangerAcc(
                          timeSource.markNow(), mutableListOf(motion.accelerationMagnitude))
                    } else {
                      currentState
                    }
                  }
                  is MovementState.PreDangerAcc -> {
                    if (motion.accelerationMagnitude > config.preDangerThreshold) {
                      // Reset timer and samples if a new high acceleration is detected, this
                      // avoids
                      // considering multiple consecutive spikes as a safe state.
                      Log.d("MovementService", "PRE_DANGER reset due to new high acceleration")
                      MovementState.PreDanger(timeSource.markNow())
                    } else {
                      currentState.accumulatedSamples.add(motion.accelerationMagnitude)
                      val elapsedTime = timeSource.markNow().minus(currentState.timestamp)

                      Log.d(
                          "MovementService",
                          "PRE_DANGER_ACC elapsedTime=$elapsedTime, samples=${currentState.accumulatedSamples.size}")

                      if (elapsedTime >= config.preDangerTimeout) {
                        val averageAcceleration = currentState.accumulatedSamples.average()

                        if (averageAcceleration < config.dangerAverageThreshold) {
                          Log.d(
                              "MovementService",
                              "State changed to DANGER (avg: $averageAcceleration)")
                          MovementState.Danger(timeSource.markNow())
                        } else {
                          Log.d(
                              "MovementService",
                              "State reverted to SAFE (avg: $averageAcceleration)")
                          MovementState.Safe(timeSource.markNow())
                        }
                      } else {
                        currentState
                      }
                    }
                  }
                  is MovementState.Danger -> {
                    currentState
                    // Remain in Danger state until externally reset
                  }
                }
          }
        }
  }

  /**
   * Stop listening to sensors and cancel internal coroutines.
   *
   * After calling this function the service will no longer collect new samples.
   */
  fun stop() {
    collectionJob?.cancel()
    collectionJob = null
  }

  fun setSafe() {
    stop()
    _movementState.value = MovementState.Safe(timeSource.markNow())
    Log.d("MovementService", "State manually set to SAFE")
    startListening()
  }

  /**
   * Updates the movement detection configuration. Only applies when in Safe state to prevent
   * unwanted behavior during PreDanger state.
   *
   * @param newConfig The new configuration to apply
   * @return Result indicating success or failure of the update
   */
  fun updateConfig(newConfig: MovementConfig): Result<Unit> {
    return if (_movementState.value is MovementState.Safe) {
      config = newConfig
      Log.d("MovementService", "Configuration updated: $newConfig")
      Result.success(Unit)
    } else {
      Log.w("MovementService", "Configuration update rejected - not in Safe state")
      Result.failure(IllegalStateException("Not in Safe state"))
    }
  }
}

/**
 * Configuration for movement detection thresholds.
 *
 * @param preDangerThreshold Acceleration magnitude threshold to trigger PreDanger state
 * @param dangerAverageThreshold Average acceleration threshold below which Danger state is
 *   triggered
 * @param preDangerTimeout Duration to wait in PreDanger before evaluating for Danger state
 */
data class MovementConfig(
    val preDangerThreshold: Double = 50.0,
    val dangerAverageThreshold: Double = 1.0,
    val preDangerTimeout: Duration = 10.seconds
)

/** Represents the current state of the accident detection program. */
sealed class MovementState(val timestamp: ComparableTimeMark) {
  /** User is considered safe, no significant movement detected. */
  class Safe(timestamp: ComparableTimeMark) : MovementState(timestamp)

  /** Potential danger detected, waiting for the acceleration values to go to acceptable range */
  class PreDanger(timestamp: ComparableTimeMark) : MovementState(timestamp)

  /** Potential danger detected, accumulate values to confirm danger */
  class PreDangerAcc(
      timestamp: ComparableTimeMark,
      val accumulatedSamples: MutableList<Double> = mutableListOf()
  ) : MovementState(timestamp)

  /** Danger detected, big acceleration followed by stillness */
  class Danger(timestamp: ComparableTimeMark) : MovementState(timestamp)
}
