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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
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
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    dataFlow = MutableSharedFlow(replay = 0, extraBufferCapacity = 50)
    mockRepository = mockk(relaxed = true)
    every { mockRepository.data } returns dataFlow
    timeSource = TestTimeSource()
    service = MovementService(mockRepository, timeSource = timeSource, dispatcher = testDispatcher)
    service.updateConfig(service.config.copy(preDangerTimeout = 500.milliseconds))
    service.startListening()
  }

  @Test
  fun setSafe_success() =
      runTest(testDispatcher) {
        // Force into an unsafe state
        dataFlow.emit(createMotionData(acceleration = Vector3D(100.0, 0.0, 0.0)))
        testDispatcher.scheduler.advanceUntilIdle()
        timeSource += 200.milliseconds
        composeTestRule.waitUntil(timeoutMillis = 5000L) {
          service.movementState.value !is MovementState.Safe
        }
        service.setSafe()
        testDispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitUntil(timeoutMillis = 5000L) {
          service.movementState.value is MovementState.Safe
        }
      }

  @Test
  fun updateConfig_success_failure() =
      runTest(testDispatcher) {
        val newConfig =
            MovementConfig(
                preDangerThreshold = 15.0,
                dangerAverageThreshold = 10.0,
                preDangerTimeout = 5.seconds)
        assertTrue(service.updateConfig(newConfig).isSuccess)
        // Force into an unsafe state with new threshold
        dataFlow.emit(createMotionData(acceleration = Vector3D(16.0, 0.0, 0.0)))
        testDispatcher.scheduler.advanceUntilIdle()
        timeSource += 200.milliseconds
        composeTestRule.waitUntil(timeoutMillis = 5000L) {
          service.movementState.value !is MovementState.Safe
        }
        assertTrue(service.updateConfig(newConfig.copy(preDangerThreshold = 10.0)).isFailure)
        assertEquals(newConfig, service.config)
      }

  @Test
  fun danger_state_detected() =
      runTest(testDispatcher) {
        dataFlow.emit(createMotionData(acceleration = Vector3D(100.0, 0.0, 0.0)))
        testDispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitUntil(timeoutMillis = 5000L) {
          service.movementState.value !is MovementState.Safe
        }
        for (i in 1..5) {
          dataFlow.emit(createMotionData(acceleration = Vector3D(0.0, 0.0, 0.0)))
          testDispatcher.scheduler.advanceUntilIdle()
          timeSource += service.config.preDangerTimeout / 2
        }
        testDispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitUntil(timeoutMillis = 5000L) {
          service.movementState.value is MovementState.Danger
        }
      }

  @Test
  fun multiple_collisions_detect_danger() =
      runTest(testDispatcher) {
        dataFlow.emit(createMotionData(acceleration = Vector3D(100.0, 0.0, 0.0)))
        testDispatcher.scheduler.advanceUntilIdle()
        dataFlow.emit(createMotionData(acceleration = Vector3D(0.0, 0.0, 0.0)))
        testDispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitUntil(timeoutMillis = 5000L) {
          service.movementState.value is MovementState.PreDangerAcc
        }
        for (i in 1..5) {
          dataFlow.emit(createMotionData(acceleration = Vector3D(100.0, 0.0, 0.0)))
          testDispatcher.scheduler.advanceUntilIdle()
          timeSource += service.config.preDangerTimeout / 10
          composeTestRule.waitUntil(timeoutMillis = 5000L) {
            service.movementState.value is MovementState.PreDanger
          }
          dataFlow.emit(createMotionData(acceleration = Vector3D(0.0, 0.0, 0.0)))
          testDispatcher.scheduler.advanceUntilIdle()
        }

        for (i in 1..10) {
          dataFlow.emit(createMotionData(acceleration = Vector3D(0.0, 0.0, 0.0)))
          testDispatcher.scheduler.advanceUntilIdle()
          timeSource += service.config.preDangerTimeout / 5
        }
        testDispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitUntil(timeoutMillis = 5000L) {
          service.movementState.value is MovementState.Danger
        }
      }

  @Test
  fun recovers_to_safe_state() =
      runTest(testDispatcher) {
        dataFlow.emit(createMotionData(acceleration = Vector3D(100.0, 0.0, 0.0)))
        testDispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitUntil(timeoutMillis = 5000L) {
          service.movementState.value !is MovementState.Safe
        }
        for (i in 1..20) {
          dataFlow.emit(createMotionData(acceleration = Vector3D(1.0, 0.0, 0.0)))
          testDispatcher.scheduler.advanceUntilIdle()
          timeSource += service.config.preDangerTimeout / 10
        }
        testDispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitUntil(timeoutMillis = 5000L) {
          service.movementState.value is MovementState.Safe
        }
      }

  @Test
  fun stop_collection() =
      runTest(testDispatcher) {
        service.stop()
        testDispatcher.scheduler.advanceUntilIdle()
        val motion = createMotionData(acceleration = Vector3D(100.0, 0.0, 0.0))
        dataFlow.emit(motion)
        testDispatcher.scheduler.advanceUntilIdle()

        composeTestRule.waitUntil(timeoutMillis = 5000L) {
          service.movementState.value is MovementState.Safe
        }
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
