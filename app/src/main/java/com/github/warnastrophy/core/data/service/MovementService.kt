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
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource.Monotonic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
class MovementService(private val repository: MovementSensorRepository) {

  /** Root job for the service coroutine scope. Cancelling this stops the collector. */
  private val job = Job()

  /** Coroutine scope used to collect the repository flow. Runs on [Dispatchers.Default]. */
  private val scope = CoroutineScope(Dispatchers.Default + job)

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
    scope.launch {
      repository.data.collect { motion ->
        // State update logic based on acceleration magnitude
        // TODO: Allow configuring thresholds via parameters or settings
        when (_movementState.value) {
          is MovementState.Safe -> {
            if (motion.accelerationMagnitude > 50.0) {
              _movementState.value = MovementState.PreDanger(Monotonic.markNow())
              Log.d("MovementService", "State changed to PRE_DANGER")
            }
          }
          is MovementState.PreDanger -> {
            val preDangerState = _movementState.value as MovementState.PreDanger
            preDangerState.accumulatedSamples.add(motion.accelerationMagnitude)

            val elapsedTime = Monotonic.markNow().minus(preDangerState.timestamp)

            if (elapsedTime >= 10.seconds) {
              val averageAcceleration = preDangerState.accumulatedSamples.average()

              if (averageAcceleration < 2.0) {
                _movementState.value = MovementState.Danger(Monotonic.markNow())
                Log.d("MovementService", "State changed to DANGER (avg: $averageAcceleration)")
              } else {
                _movementState.value = MovementState.Safe(Monotonic.markNow())
                Log.d("MovementService", "State reverted to SAFE (avg: $averageAcceleration)")
              }
            }
          }
          is MovementState.Danger -> {
            // Remain in Danger state until externally reset
          }
        }

        val now = System.currentTimeMillis()
        samples.add(motion)
        samples.removeIf { it.timestamp < now - windowMillisMotion }
        _recentData.value = samples.toList()
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
    job.cancel()
    scope.cancel()
  }
}

/**
 * Represents the current state of the accident detection program.
 *
 * @property timestamp the time when this state was entered
 */
sealed class MovementState(val timestamp: Monotonic.ValueTimeMark) {
  class Safe(timestamp: Monotonic.ValueTimeMark) : MovementState(timestamp)

  class PreDanger(
      timestamp: Monotonic.ValueTimeMark,
      val accumulatedSamples: MutableList<Double> = mutableListOf()
  ) : MovementState(timestamp)

  class Danger(timestamp: Monotonic.ValueTimeMark) : MovementState(timestamp)
}
