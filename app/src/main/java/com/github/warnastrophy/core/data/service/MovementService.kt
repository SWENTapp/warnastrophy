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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Service that monitors movement data from sensors and determines the current movement state. It
 * collects motion samples from the [MovementSensorRepository] and updates the internal state based
 * on acceleration thresholds defined in [MovementConfig].
 *
 * The service exposes a [StateFlow] of [MovementState] that observers can collect to get continuous
 * updates of the current movement state.
 *
 * @param repository Repository providing motion data samples.
 * @param initialConfig Initial configuration for movement detection thresholds.
 * @param timeSource Time source used for measuring elapsed time. Defaults to monotonic time. Useful
 *   for testing.
 * @param dangerModeStateFlow Optional state flow providing danger mode state and configuration.
 */
class MovementService(
    private val repository: MovementSensorRepository,
    initialConfig: MovementConfig = MovementConfig(),
    private val timeSource: TimeSource.WithComparableMarks = TimeSource.Monotonic,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val dangerModeStateFlow: StateFlow<DangerModeService.DangerModeState>? = null,
) {

  /** Root job for the service coroutine scope. Cancelling this stops the collector. */
  private var collectionJob: Job? = null
  /** Coroutine scope used to collect the repository flow. Runs on [dispatcher]. */
  private val scope = CoroutineScope(dispatcher + Job())

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
          dangerModeStateFlow?.let { stateFlow ->
            launch {
              stateFlow.collect { dangerModeState ->
                dangerModeState.activity?.movementConfig?.let { newConfig -> config = newConfig }
              }
            }
          }

          // Collect motion data
          repository.data.collect { motion ->
            _movementState.value = processMotion(motion.accelerationMagnitude, _movementState.value)
          }
        }
  }

  /**
   * Process motion data and determine the next movement state based on current state and
   * acceleration magnitude.
   */
  private fun processMotion(
      accelerationMagnitude: Double,
      currentState: MovementState
  ): MovementState {
    return when (currentState) {
      is MovementState.Safe -> processSafeState(accelerationMagnitude, currentState)
      is MovementState.PreDanger -> processPreDangerState(accelerationMagnitude, currentState)
      is MovementState.PreDangerAcc -> processPreDangerAccState(accelerationMagnitude, currentState)
      is MovementState.Danger -> currentState // Remain in Danger state until externally reset
    }
  }

  private fun processSafeState(
      accelerationMagnitude: Double,
      currentState: MovementState.Safe
  ): MovementState {
    if (accelerationMagnitude > config.preDangerThreshold) {
      return MovementState.PreDanger(timeSource.markNow())
    }
    return currentState
  }

  private fun processPreDangerState(
      accelerationMagnitude: Double,
      currentState: MovementState.PreDanger
  ): MovementState {
    if (accelerationMagnitude <= config.dangerAverageThreshold) {
      return MovementState.PreDangerAcc(timeSource.markNow(), mutableListOf(accelerationMagnitude))
    }
    return currentState
  }

  private fun processPreDangerAccState(
      accelerationMagnitude: Double,
      currentState: MovementState.PreDangerAcc
  ): MovementState {
    if (accelerationMagnitude > config.preDangerThreshold) {
      // Reset timer and samples if a new high acceleration is detected, this avoids
      // considering multiple consecutive spikes as a safe state.
      return MovementState.PreDanger(timeSource.markNow())
    }
    return evaluatePreDangerAccTimeout(accelerationMagnitude, currentState)
  }

  private fun evaluatePreDangerAccTimeout(
      accelerationMagnitude: Double,
      currentState: MovementState.PreDangerAcc
  ): MovementState {
    currentState.accumulatedSamples.add(accelerationMagnitude)
    val elapsedTime = timeSource.markNow().minus(currentState.timestamp)

    if (elapsedTime >= config.preDangerTimeout) {
      val averageAcceleration = currentState.accumulatedSamples.average()
      return evaluateDangerCondition(averageAcceleration)
    }
    return currentState
  }

  private fun evaluateDangerCondition(averageAcceleration: Double): MovementState {
    if (averageAcceleration < config.dangerAverageThreshold) {
      return MovementState.Danger(timeSource.markNow())
    }
    return MovementState.Safe(timeSource.markNow())
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

  /**
   * Manually sets the movement state to Safe and restarts listening to sensors.
   *
   * This function is useful for resetting the state after a Danger event has been handled. It's
   * expected to be called after the UI prompts the user to confirm they are safe.
   */
  fun setSafe() {
    stop()
    _movementState.value = MovementState.Safe(timeSource.markNow())
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
