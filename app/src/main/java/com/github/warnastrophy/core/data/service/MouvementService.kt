/**
 * This file was written by Anas Sidi Mohamed with the assistance of ChatGPT.
 *
 * Author: Anas Sidi Mohamed Assistance: ChatGPT
 */
package com.example.dangermode.service

import com.github.warnastrophy.core.data.repository.MotionData
import com.github.warnastrophy.core.data.repository.MouvementSensorRepository
import com.github.warnastrophy.core.util.AppConfig.windowMillisMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Service that collects motion samples from a [MouvementSensorRepository] and exposes recent
 * samples as a snapshot or as a [StateFlow].
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
class MouvementService(private val repository: MouvementSensorRepository) {

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
    samples.removeIf { it.timestamp < System.currentTimeMillis() - windowMillisMotion }
    return samples.toList()
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
