/**
 * This file was written by Anas Sidi Mohamed with the assistance of ChatGPT.
 *
 * Author: Anas Sidi Mohamed Assistance: ChatGPT
 */
package com.github.warnastrophy.core.data.service

import com.github.warnastrophy.core.data.repository.MotionData
import com.github.warnastrophy.core.data.repository.MovementSensorRepository
import com.github.warnastrophy.core.model.Activity
import com.github.warnastrophy.core.util.BaseAndroidComposeTest
import io.mockk.every
import io.mockk.mockk
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.locationtech.jts.math.Vector3D

class MovementServiceTest : BaseAndroidComposeTest() {
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

  @Test
  fun dangerModeStateFlow_updates_movementConfig() =
      runTest(testDispatcher) {
        // Create a MutableStateFlow to simulate DangerModeService.DangerModeState changes
        val dangerModeStateFlow = MutableStateFlow(DangerModeService.DangerModeState())

        // Create a new MovementService instance with the dangerModeStateFlow
        val serviceWithDangerMode =
            MovementService(
                repository = mockRepository,
                initialConfig =
                    MovementConfig(
                        preDangerThreshold = 50.0,
                        dangerAverageThreshold = 1.0,
                        preDangerTimeout = 500.milliseconds),
                timeSource = timeSource,
                dispatcher = testDispatcher,
                dangerModeStateFlow = dangerModeStateFlow)
        serviceWithDangerMode.startListening()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify initial config - acceleration of 40 should NOT trigger PreDanger (threshold is 50)
        dataFlow.emit(createMotionData(acceleration = Vector3D(40.0, 0.0, 0.0)))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(
            "Should remain Safe with acceleration 40 (threshold 50)",
            serviceWithDangerMode.movementState.value is MovementState.Safe)

        // Update DangerModeState with a new Activity that has a lower preDangerThreshold
        val newActivity =
            Activity(
                id = "test-activity",
                activityName = "Test Activity",
                movementConfig =
                    MovementConfig(
                        preDangerThreshold = 30.0, // Lower threshold
                        dangerAverageThreshold = 1.0,
                        preDangerTimeout = 500.milliseconds))
        dangerModeStateFlow.value =
            DangerModeService.DangerModeState(isActive = true, activity = newActivity)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify config was updated
        assertEquals(
            "Config should be updated from DangerModeState",
            30.0,
            serviceWithDangerMode.config.preDangerThreshold,
            0.001)

        // Now the same acceleration of 40 SHOULD trigger PreDanger (new threshold is 30)
        dataFlow.emit(createMotionData(acceleration = Vector3D(40.0, 0.0, 0.0)))
        testDispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitUntil(timeoutMillis = 5000L) {
          serviceWithDangerMode.movementState.value is MovementState.PreDanger
        }

        // Clean up
        serviceWithDangerMode.stop()
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
