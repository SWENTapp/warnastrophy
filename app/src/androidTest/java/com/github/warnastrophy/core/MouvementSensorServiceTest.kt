/**
 * This file was written by Anas Sidi Mohamed with the assistance of ChatGPT.
 *
 * Author: Anas Sidi Mohamed Assistance: ChatGPT
 */
package com.example.dangermode.service

import com.github.warnastrophy.core.data.repository.MotionData
import com.github.warnastrophy.core.data.repository.MouvementSensorRepository
import com.github.warnastrophy.core.ui.util.BaseAndroidComposeTest
import io.mockk.every
import io.mockk.mockk
import kotlin.compareTo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class MouvementServiceTest : BaseAndroidComposeTest() {

  private lateinit var mockRepository: MouvementSensorRepository
  private lateinit var dataFlow: MutableSharedFlow<MotionData>
  private lateinit var service: MouvementService

  @Before
  fun setup() {
    dataFlow = MutableSharedFlow(replay = 2, extraBufferCapacity = 10)
    mockRepository = mockk(relaxed = true)
    every { mockRepository.data } returns dataFlow
    service = MouvementService(mockRepository)
    service.startListening()
  }

  @Test
  fun get_recent_samples_returns_snapshot() = runTest {
    val motion = createMotionData(timestamp = System.currentTimeMillis())
    dataFlow.tryEmit(motion)

    awaitCondition { service.getRecentSamples().isNotEmpty() }

    val snapshot1 = service.getRecentSamples()
    val snapshot2 = service.getRecentSamples()

    assertEquals(snapshot1, snapshot2)
    assertNotSame(snapshot1, snapshot2)
  }

  @Test
  fun service_initializes_and_starts_listening() = runTest {
    assertTrue(service.getRecentSamples().isEmpty())
  }

  @Test
  fun service_collects_motion_data() = runTest {
    val motion1 = createMotionData(timestamp = System.currentTimeMillis())
    val motion2 = createMotionData(timestamp = System.currentTimeMillis() + 1000)

    dataFlow.emit(motion1)
    dataFlow.emit(motion2)

    awaitCondition { service.getRecentSamples().size >= 2 }

    val samples = service.getRecentSamples()
    assertEquals(2, samples.size)
    assertTrue(samples.contains(motion1))
    assertTrue(samples.contains(motion2))
  }

  @Test
  fun service_exposes_recent_data_flow() = runTest {
    val motion = createMotionData(timestamp = System.currentTimeMillis())

    dataFlow.emit(motion)

    awaitCondition { service.recentData.value.size >= 1 }

    val recentData = service.recentData.value
    assertEquals(1, recentData.size)
    assertEquals(motion, recentData.first())
  }

  @Test
  fun service_removes_samples_older_than_two_minutes() = runTest {
    val now = System.currentTimeMillis()
    val oldMotion = createMotionData(timestamp = now - 3 * 60 * 1000L)
    val recentMotion = createMotionData(timestamp = now)

    dataFlow.emit(oldMotion)
    dataFlow.emit(recentMotion)

    awaitCondition { service.getRecentSamples().size >= 1 }

    val samples = service.getRecentSamples()
    assertEquals(1, samples.size)
    assertFalse(samples.contains(oldMotion))
    assertTrue(samples.contains(recentMotion))
  }

  @Test
  fun service_keeps_samples_within_two_minute_window() = runTest {
    val now = System.currentTimeMillis()
    val motion1 = createMotionData(timestamp = now - 1 * 60 * 1000L)
    val motion2 = createMotionData(timestamp = now)

    dataFlow.emit(motion1)
    dataFlow.emit(motion2)

    awaitCondition { service.getRecentSamples().size >= 2 }

    val samples = service.getRecentSamples()
    assertEquals(2, samples.size)
  }

  @Test
  fun stop_cancels_collection_job() = runTest {
    service.stop()

    val motion = createMotionData(timestamp = System.currentTimeMillis())
    dataFlow.emit(motion)

    delay(100)
    assertTrue(service.getRecentSamples().isEmpty())
  }

  /**
   * Creates an instance of `MotionData` with the provided data.
   *
   * @param timestamp The timestamp of the motion sample, defaulting to the current time in
   *   milliseconds.
   * @param acceleration A triplet representing the acceleration on the X, Y, and Z axes, defaulting
   *   to (0f, 0f, 0f).
   * @param rotation A triplet representing the rotation on the X, Y, and Z axes, defaulting to (0f,
   *   0f, 0f).
   * @return An instance of `MotionData` containing the provided data and the calculated
   *   acceleration magnitude.
   */
  private fun createMotionData(
      timestamp: Long = System.currentTimeMillis(),
      acceleration: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
      rotation: Triple<Float, Float, Float> = Triple(0f, 0f, 0f)
  ): MotionData {
    return MotionData(
        timestamp = timestamp,
        acceleration = acceleration,
        rotation = rotation,
        accelerationMagnitude =
            kotlin.math.sqrt(
                acceleration.first * acceleration.first +
                    acceleration.second * acceleration.second +
                    acceleration.third * acceleration.third))
  }
}
