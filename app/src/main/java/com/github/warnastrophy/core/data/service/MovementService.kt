/**
 * This file was written by Anas Sidi Mohamed with the assistance of ChatGPT.
 *
 * Author: Anas Sidi Mohamed Assistance: ChatGPT
 */
package com.example.dangermode.service

import android.util.Log
import com.github.warnastrophy.core.data.repository.MotionData
import com.github.warnastrophy.core.data.repository.MovementSensorRepository
import com.github.warnastrophy.core.util.AppConfig.windowMillisMotion
import kotlin.compareTo
import kotlin.text.compareTo
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource.Monotonic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Service that collects motion samples from a [MovementSensorRepository] and exposes recent samples
 * as a snapshot or as a [StateFlow].
 *
 * The service starts listening to the repository's `data` flow immediately after construction. It
 * keeps an in-memory sliding window of samples and regularly prunes samples older than
 * [windowMillis].
 *
 * Note: `getRecentSamples()` returns a snapshot (`List`) built with `toList()` to avoid exposing
 * internal mutable state.
 *
 * @param repository Source of motion data samples.
 */
class MovementService(
    private val repository: MovementSensorRepository,
    initialConfig: MovementConfig = MovementConfig()
) {

  /** Root job for the service coroutine scope. Cancelling this stops the collector. */
  private var collectionJob: Job? = null
  /** Coroutine scope used to collect the repository flow. Runs on [Dispatchers.Default]. */
  private val scope = CoroutineScope(Dispatchers.Default + Job())
  /**
   * Internal mutable list that stores received samples.
   *
   * Access to this list is confined to the coroutine started in [startListening]. Consumers should
   * use [getRecentSamples] or [recentData] to obtain a safe snapshot.
   */
  private val samples = mutableListOf<MotionData>()

  /** Backing _StateFlow_ that emits a snapshot of recent samples when updated. */
  private val _recentData = MutableStateFlow<List<MotionData>>(emptyList())

  /**
   * Public read-only [StateFlow] that observers can collect to get continuous updates of recent
   * samples. Each emission is a new immutable list snapshot.
   */
  val recentData: StateFlow<List<MotionData>> = _recentData

  /** Backing _StateFlow_ that emits the current movement state. */
  private val _movementState =
      MutableStateFlow<MovementState>(MovementState.Safe(Monotonic.markNow()))

  /**
   * Public read-only [StateFlow] that observers can collect to get continuous updates of the
   * current movement state.
   */
  val movementState: StateFlow<MovementState> = _movementState

  /** Current movement detection configuration. */
  private var config = initialConfig

  /** Window duration in milliseconds used to keep only recent samples (default 2 minutes). */
  /**
   * Start collecting motion samples from the repository.
   *
   * This function launches a coroutine that collects `repository.data`, appends each incoming
   * sample to the internal buffer, removes samples older than the configured sliding window, and
   * emits a new list snapshot to [_recentData].
   *
   * The collector runs until [stop] is called which cancels the internal job.
   */
  fun startListening() {
    // If already listening, do nothing
    if (collectionJob?.isActive == true) return

    collectionJob =
        scope.launch {
          Log.d("MovementService", "Started listening to motion data")
          repository.data
              .distinctUntilChanged { old, new ->
                // Consider samples equal if their timestamps are the same to avoid redundant
                // updates
                old.timestamp.compareTo(new.timestamp) == 0
              }
              .collectLatest { motion ->
                // State update logic based on acceleration magnitude
                _movementState.value =
                    when (val currentState = _movementState.value) {
                      is MovementState.Safe -> {
                        if (motion.accelerationMagnitude > config.preDangerThreshold) {
                          Log.d("MovementService", "State changed to PRE_DANGER")
                          MovementState.PreDanger(Monotonic.markNow())
                        } else {
                          currentState
                        }
                      }
                      is MovementState.PreDanger -> {
                        if (motion.accelerationMagnitude <= config.dangerAverageThreshold) {
                          Log.d("MovementService", "State changed to PRE_DANGER_ACC")
                          MovementState.PreDangerAcc(
                              Monotonic.markNow(), mutableListOf(motion.accelerationMagnitude))
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
                          MovementState.PreDanger(Monotonic.markNow())
                        }

                        currentState.accumulatedSamples.add(motion.accelerationMagnitude)
                        val elapsedTime = Monotonic.markNow().minus(currentState.timestamp)

                        if (elapsedTime >= config.preDangerTimeout) {
                          val averageAcceleration = currentState.accumulatedSamples.average()

                          if (averageAcceleration < config.dangerAverageThreshold) {
                            Log.d(
                                "MovementService",
                                "State changed to DANGER (avg: $averageAcceleration)")
                            MovementState.Danger(Monotonic.markNow())
                          } else {
                            Log.d(
                                "MovementService",
                                "State reverted to SAFE (avg: $averageAcceleration)")
                            MovementState.Safe(Monotonic.markNow())
                          }
                        } else {
                          currentState
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
   * Returns a snapshot of the motion samples recorded during the last window.
   *
   * The returned list is a copy of the internal buffer (created with `toList()`), so callers can
   * safely iterate or modify the returned list without affecting the service's internal state.
   *
   * @return an immutable snapshot [List] of recent [MotionData].
   */
  fun getRecentSamples(): List<MotionData> {
    val data = _recentData.value.toMutableList()
    data.removeIf { it.timestamp < System.currentTimeMillis() - windowMillisMotion }
    return data.toList()
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
    _movementState.value = MovementState.Safe(Monotonic.markNow())
    Log.d("MovementService", "State manually set to SAFE")
    startListening()
  }

  /**
   * Updates the movement detection configuration. Only applies when in Safe state to prevent
   * unwanted behavior during PreDanger state.
   *
   * @param newConfig The new configuration to apply
   * @return true if configuration was updated, false if update was rejected due to current state
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
sealed class MovementState(val timestamp: Monotonic.ValueTimeMark) {
  /** User is considered safe, no significant movement detected. */
  class Safe(timestamp: Monotonic.ValueTimeMark) : MovementState(timestamp)

  /** Potential danger detected, waiting for the acceleration values to go to acceptable range */
  class PreDanger(timestamp: Monotonic.ValueTimeMark) : MovementState(timestamp)

  /** Potential danger detected, accumulate values to confirm danger */
  class PreDangerAcc(
      timestamp: Monotonic.ValueTimeMark,
      val accumulatedSamples: MutableList<Double> = mutableListOf()
  ) : MovementState(timestamp)

  /** Danger detected, big acceleration followed by stillness */
  class Danger(timestamp: Monotonic.ValueTimeMark) : MovementState(timestamp)
}
