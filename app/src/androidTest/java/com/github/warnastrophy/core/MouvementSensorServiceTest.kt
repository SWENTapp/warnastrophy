// kotlin
package com.example.dangermode.service

import com.github.warnastrophy.core.data.repository.MotionData
import com.github.warnastrophy.core.data.repository.MouvementSensorRepository
import io.mockk.every
import io.mockk.mockk
import kotlin.compareTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MouvementServiceTest {

  private lateinit var mockRepository: MouvementSensorRepository
  private lateinit var dataFlow: MutableSharedFlow<MotionData>
  private lateinit var service: MouvementService

  @Before
  fun setup() {
    // Augmenter extraBufferCapacity pour éviter la perte d'émissions multiples
    dataFlow = MutableSharedFlow(replay = 2, extraBufferCapacity = 10)
    mockRepository = mockk(relaxed = true)
    every { mockRepository.data } returns dataFlow
  }

  // Helper : attend qu'une condition soit vraie ou échoue au bout du timeout,
  // en utilisant le temps réel pour permettre l'exécution sur Dispatchers.Default.
  private suspend fun awaitCondition(timeoutMillis: Long = 2000L, condition: () -> Boolean) {
    withContext(Dispatchers.Default.limitedParallelism(1)) {
      withTimeout(timeoutMillis) {
        while (!condition()) {
          delay(10)
        }
      }
    }
  }

  @Test
  fun get_recent_samples_returns_snapshot() = runTest {
    service = MouvementService(mockRepository)

    val motion = createMotionData(timestamp = System.currentTimeMillis())
    // tryEmit non suspendant + replay=1 garantit livraison
    dataFlow.tryEmit(motion)

    awaitCondition { service.getRecentSamples().isNotEmpty() }

    val snapshot1 = service.getRecentSamples()
    val snapshot2 = service.getRecentSamples()

    assertEquals(snapshot1, snapshot2)
    assertNotSame(snapshot1, snapshot2)
  }

  @Test
  fun service_initializes_and_starts_listening() = runTest {
    service = MouvementService(mockRepository)
    assertTrue(service.getRecentSamples().isEmpty())
  }

  @Test
  fun service_collects_motion_data() = runTest {
    service = MouvementService(mockRepository)

    val motion1 = createMotionData(timestamp = System.currentTimeMillis())
    val motion2 = createMotionData(timestamp = System.currentTimeMillis() + 1000)

    // utiliser emit suspendant pour s'assurer que chaque élément est livré au collecteur
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
    service = MouvementService(mockRepository)

    val motion = createMotionData(timestamp = System.currentTimeMillis())

    dataFlow.emit(motion)

    awaitCondition { service.recentData.value.size >= 1 }

    val recentData = service.recentData.value
    assertEquals(1, recentData.size)
    assertEquals(motion, recentData.first())
  }

  @Test
  fun service_removes_samples_older_than_two_minutes() = runTest {
    service = MouvementService(mockRepository)

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
    service = MouvementService(mockRepository)

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
    service = MouvementService(mockRepository)

    service.stop()

    val motion = createMotionData(timestamp = System.currentTimeMillis())
    dataFlow.emit(motion)

    delay(100)
    assertTrue(service.getRecentSamples().isEmpty())
  }

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
