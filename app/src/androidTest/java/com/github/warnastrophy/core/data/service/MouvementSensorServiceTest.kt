/**
 * This file was written by Anas Sidi Mohamed with the assistance of ChatGPT.
 *
 * Author: Anas Sidi Mohamed Assistance: ChatGPT
 */
package com.example.dangermode.service

import com.github.warnastrophy.core.data.repository.MotionData
import com.github.warnastrophy.core.data.repository.MovementSensorRepository
import com.github.warnastrophy.core.data.service.MovementConfig
import com.github.warnastrophy.core.data.service.MovementService
import com.github.warnastrophy.core.data.service.MovementState
import com.github.warnastrophy.core.util.BaseAndroidComposeTest
import io.mockk.every
import io.mockk.mockk
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.locationtech.jts.math.Vector3D

class MouvementServiceTest : BaseAndroidComposeTest() {

  private lateinit var mockRepository: MovementSensorRepository
  private lateinit var dataFlow: MutableSharedFlow<MotionData>
  private lateinit var timeSource: TestTimeSource
  private lateinit var service: MovementService

  @Before
  fun setup() {
    dataFlow = MutableSharedFlow(replay = 2, extraBufferCapacity = 10)
    mockRepository = mockk(relaxed = true)
    every { mockRepository.data } returns dataFlow
    timeSource = TestTimeSource()
    service = MovementService(mockRepository, timeSource = timeSource)
    service.updateConfig(service.config.copy(preDangerTimeout = 500.milliseconds))
    service.startListening()
  }

  @Test
  fun setSafe_success() = runTest {
    // Force into an unsafe state
    dataFlow.emit(createMotionData(acceleration = Vector3D(100.0, 0.0, 0.0)))
    awaitCondition { service.movementState.value !is MovementState.Safe }
    service.setSafe()
    assertTrue(service.movementState.value is MovementState.Safe)
  }

  @Test
  fun updateConfig_success_failure() = runTest {
    val newConfig =
        MovementConfig(
            preDangerThreshold = 15.0, dangerAverageThreshold = 10.0, preDangerTimeout = 5.seconds)
    assertTrue(service.updateConfig(newConfig).isSuccess)
    // Force into an unsafe state with new threshold
    dataFlow.emit(createMotionData(acceleration = Vector3D(16.0, 0.0, 0.0)))
    awaitCondition { service.movementState.value !is MovementState.Safe }
    assertTrue(service.updateConfig(newConfig.copy(preDangerThreshold = 10.0)).isFailure)
    assertEquals(newConfig, service.config)
  }

  @Test
  fun danger_state_detected() = runTest {
    dataFlow.emit(createMotionData(acceleration = Vector3D(100.0, 0.0, 0.0)))
    awaitCondition { service.movementState.value !is MovementState.Safe }
    for (i in 1..100) {
      dataFlow.emit(createMotionData(acceleration = Vector3D(0.0, 0.0, 0.0)))
      timeSource += service.config.preDangerTimeout / 10
      delay(service.config.preDangerTimeout.inWholeMilliseconds / 10)
    }
    awaitCondition { service.movementState.value is MovementState.Danger }
    assertTrue(service.movementState.value is MovementState.Danger)
  }

  @Test
  fun multiple_collisions_detect_danger() = runTest {
    dataFlow.emit(createMotionData(acceleration = Vector3D(100.0, 0.0, 0.0)))
    dataFlow.emit(createMotionData(acceleration = Vector3D(0.0, 0.0, 0.0)))
    awaitCondition { service.movementState.value is MovementState.PreDangerAcc }
    for (i in 1..5) {
      dataFlow.emit(createMotionData(acceleration = Vector3D(100.0, 0.0, 0.0)))
      timeSource += service.config.preDangerTimeout / 10
      delay(service.config.preDangerTimeout.inWholeMilliseconds / 10)
      awaitCondition { service.movementState.value is MovementState.PreDanger }
      dataFlow.emit(createMotionData(acceleration = Vector3D(0.0, 0.0, 0.0)))
    }

    for (i in 1..20) {
      dataFlow.emit(createMotionData(acceleration = Vector3D(0.0, 0.0, 0.0)))
      timeSource += service.config.preDangerTimeout / 10
      delay(service.config.preDangerTimeout.inWholeMilliseconds / 10)
    }
    awaitCondition { service.movementState.value is MovementState.Danger }
  }

  @Test
  fun recovers_to_safe_state() = runTest {
    dataFlow.emit(createMotionData(acceleration = Vector3D(100.0, 0.0, 0.0)))
    awaitCondition { service.movementState.value !is MovementState.Safe }
    for (i in 1..20) {
      dataFlow.emit(createMotionData(acceleration = Vector3D(1.0, 0.0, 0.0)))
      timeSource += service.config.preDangerTimeout / 10
      delay(service.config.preDangerTimeout.inWholeMilliseconds / 10)
    }
    awaitCondition { service.movementState.value is MovementState.Safe }
  }

  @Test
  fun stop_stops_collection() = runTest {
    service.stop()
    val motion = createMotionData(acceleration = Vector3D(100.0, 0.0, 0.0))
    dataFlow.emit(motion)
    delay(
        service.config.preDangerTimeout /
            2) // Wait less than timeout so it should not return to safe
    assertTrue(service.movementState.value is MovementState.Safe)
  }

  //  @Test
  //  fun service_collects_motion_data() = runTest {
  //    val motion1 = createMotionData(timestamp = System.currentTimeMillis())
  //    val motion2 = createMotionData(timestamp = System.currentTimeMillis() + 1000)
  //
  //    dataFlow.emit(motion1)
  //    dataFlow.emit(motion2)
  //
  //    awaitCondition { service.getRecentSamples().size >= 2 }
  //
  //    val samples = service.getRecentSamples()
  //    assertEquals(2, samples.size)
  //    assertTrue(samples.contains(motion1))
  //    assertTrue(samples.contains(motion2))
  //  }

  //  @Test
  //  fun service_removes_samples_older_than_two_minutes() = runTest {
  //    val now = System.currentTimeMillis()
  //    val oldMotion = createMotionData(timestamp = now - 3 * 60 * 1000L)
  //    val recentMotion = createMotionData(timestamp = now)
  //
  //    dataFlow.emit(oldMotion)
  //    dataFlow.emit(recentMotion)
  //
  //    awaitCondition { service.getRecentSamples().size >= 1 }
  //
  //    val samples = service.getRecentSamples()
  //    assertEquals(1, samples.size)
  //    assertFalse(samples.contains(oldMotion))
  //    assertTrue(samples.contains(recentMotion))
  //  }

  //  @Test
  //  fun service_keeps_samples_within_two_minute_window() = runTest {
  //    val now = System.currentTimeMillis()
  //    val motion1 = createMotionData(timestamp = now - 1 * 60 * 1000L)
  //    val motion2 = createMotionData(timestamp = now)
  //
  //    dataFlow.emit(motion1)
  //    dataFlow.emit(motion2)
  //
  //    awaitCondition { service.getRecentSamples().size >= 2 }
  //
  //    val samples = service.getRecentSamples()
  //    assertEquals(2, samples.size)
  //  }

  //  @Test
  //  fun stop_cancels_collection_job() = runTest {
  //    service.stop()
  //
  //    val motion = createMotionData(timestamp = System.currentTimeMillis())
  //    dataFlow.emit(motion)
  //
  //    delay(100)
  //    assertTrue(service.getRecentSamples().isEmpty())
  //  }

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
      acceleration: Vector3D = Vector3D(0.0, 0.0, 0.0),
      rotation: Vector3D = Vector3D(0.0, 0.0, 0.0)
  ): MotionData {
    return MotionData(
        timestamp = timestamp,
        acceleration = acceleration,
        rotation = rotation,
        accelerationMagnitude = acceleration.length())
  }
}
