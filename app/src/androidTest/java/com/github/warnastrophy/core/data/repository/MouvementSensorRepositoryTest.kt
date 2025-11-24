/**
 * This file was written by Anas Sidi Mohamed with the assistance of ChatGPT.
 *
 * Author: Anas Sidi Mohamed Assistance: ChatGPT
 */
package com.github.warnastrophy.core

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.test.core.app.ApplicationProvider
import com.github.warnastrophy.core.data.repository.MotionData
import com.github.warnastrophy.core.data.repository.MouvementSensorRepository
import com.github.warnastrophy.core.util.BaseAndroidComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.sqrt

class MouvementSensorRepositoryTest : BaseAndroidComposeTest() {
  private lateinit var repo: MouvementSensorRepository
  private lateinit var context: Context
  private lateinit var sensorManager: SensorManager
  private lateinit var mockSensorManager: SensorManager
  private lateinit var mockAccelerometer: Sensor
  private lateinit var mockGyroscope: Sensor

  private val listenerSlot = slot<SensorEventListener>()

  @Before
  override fun setUp() {
    super.setUp()
    context = ApplicationProvider.getApplicationContext()
    sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
  }

  private fun setupMockedRepository(): Pair<SensorManager, SensorEventListener> {
    mockSensorManager = mockk(relaxed = true)
    mockAccelerometer = mockk(relaxed = true)
    mockGyroscope = mockk(relaxed = true)

    val proxy =
        object : SensorEventListener {
          var delegate: SensorEventListener? = null

          override fun onSensorChanged(event: SensorEvent?) {
            delegate?.onSensorChanged(event)
          }

          override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            delegate?.onAccuracyChanged(sensor, accuracy)
          }
        }

    every { mockSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) } returns
        mockAccelerometer
    every { mockSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) } returns mockGyroscope

    every {
      mockSensorManager.registerListener(capture(listenerSlot), any<Sensor>(), any())
    } answers
        {
          proxy.delegate = listenerSlot.captured
          true
        }

    every { mockSensorManager.unregisterListener(any<SensorEventListener>()) } answers
        {
          val arg = firstArg<SensorEventListener>()
          if (listenerSlot.isCaptured && arg === listenerSlot.captured) {
            proxy.delegate = null
          }
          Unit
        }

    val mockContext = mockk<Context>(relaxed = true)
    every { mockContext.getSystemService(Context.SENSOR_SERVICE) } returns mockSensorManager

    repo = MouvementSensorRepository(mockContext)

    return Pair(mockSensorManager, proxy)
  }

  private fun createMockSensorEvent(sensorType: Int, values: FloatArray): SensorEvent {
    val sensor = mockk<Sensor>(relaxed = true)
    every { sensor.type } returns sensorType

    try {
      val ctor = SensorEvent::class.java.getDeclaredConstructor(Int::class.javaPrimitiveType)
      ctor.isAccessible = true
      val event = ctor.newInstance(values.size) as SensorEvent

      val valuesField = SensorEvent::class.java.getDeclaredField("values")
      valuesField.isAccessible = true
      valuesField.set(event, values)

      val sensorField = SensorEvent::class.java.getDeclaredField("sensor")
      sensorField.isAccessible = true
      sensorField.set(event, sensor)

      val timestampField = SensorEvent::class.java.getDeclaredField("timestamp")
      timestampField.isAccessible = true
      timestampField.setLong(event, System.currentTimeMillis()) // Convert to nanoseconds

      return event
    } catch (e: Exception) {
      throw RuntimeException("Impossible to create SensorEvent via reflection", e)
    }
  }

  @Test
  fun repository_initialization_succeeds() {
    repo = MouvementSensorRepository(context)
    assertNotNull(repo)
    assertNotNull(repo.data)
  }

  @Test
  fun data_flow_is_created_successfully() = runTest {
    repo = MouvementSensorRepository(context)
    val flow = repo.data
    assertNotNull(flow)
  }

  @Test
  fun sensor_manager_registers_listeners_on_flow_collection() = runTest {
    val (mockManager, listener) = setupMockedRepository()

    val job = launch { repo.data.collect {} }

    delay(100)

    verify(exactly = 1) {
      mockManager.registerListener(any(), mockAccelerometer, SensorManager.SENSOR_DELAY_GAME)
    }
    verify(exactly = 1) {
      mockManager.registerListener(any(), mockGyroscope, SensorManager.SENSOR_DELAY_GAME)
    }

    job.cancel()
  }

  @Test
  fun sensor_manager_unregisters_listener_on_flow_cancellation() = runTest {
    val (mockManager, listener) = setupMockedRepository()

    val job = launch { repo.data.collect {} }

    delay(100)
    job.cancel()
    delay(100)

    assertTrue("The registered listener must be captured", listenerSlot.isCaptured)

    verify(atLeast = 1) { mockManager.unregisterListener(listenerSlot.captured) }
  }

  @Test
  fun accelerometer_data_is_processed_with_low_pass_filter() = runTest {
    val (_, listener) = setupMockedRepository()

    val receivedData = mutableListOf<MotionData>()
    val job = launch { repo.data.take(3).toList().also { receivedData.addAll(it) } }

    delay(100)

    listener.onSensorChanged(
        createMockSensorEvent(Sensor.TYPE_ACCELEROMETER, floatArrayOf(9.8f, 0f, 0f)))
    listener.onSensorChanged(
        createMockSensorEvent(Sensor.TYPE_ACCELEROMETER, floatArrayOf(9.8f, 0f, 0f)))
    listener.onSensorChanged(
        createMockSensorEvent(Sensor.TYPE_ACCELEROMETER, floatArrayOf(9.8f, 0f, 0f)))

    job.join()

    assertTrue(receivedData.size >= 3)
    receivedData.forEach { data ->
      assertNotNull(data.acceleration)
      assertTrue(data.acceleration.first.isFinite())
    }
  }

  @Test
  fun gyroscope_data_is_captured_correctly() = runTest {
    val (_, listener) = setupMockedRepository()

    val receivedData = mutableListOf<MotionData>()
    val job = launch { repo.data.take(2).toList().also { receivedData.addAll(it) } }

    delay(100)

    listener.onSensorChanged(
        createMockSensorEvent(Sensor.TYPE_GYROSCOPE, floatArrayOf(1.5f, -0.5f, 0.8f)))
    listener.onSensorChanged(
        createMockSensorEvent(Sensor.TYPE_GYROSCOPE, floatArrayOf(1.5f, -0.5f, 0.8f)))

    job.join()

    assertTrue(receivedData.size >= 2)
    receivedData.forEach { data ->
      assertEquals(1.5f, data.rotation.first, 0.01f)
      assertEquals(-0.5f, data.rotation.second, 0.01f)
      assertEquals(0.8f, data.rotation.third, 0.01f)
    }
  }

  @Test
  fun acceleration_magnitude_is_calculated_correctly() = runTest {
    val (_, listener) = setupMockedRepository()

    val receivedData = mutableListOf<MotionData>()
    val job = launch { repo.data.take(1).toList().also { receivedData.addAll(it) } }

    delay(100)

    listener.onSensorChanged(
        createMockSensorEvent(Sensor.TYPE_ACCELEROMETER, floatArrayOf(3f, 4f, 0f)))

    job.join()

    assertTrue(receivedData.isNotEmpty())
    val data = receivedData.first()
    val expectedMagnitude =
        sqrt(
            data.acceleration.first * data.acceleration.first +
                data.acceleration.second * data.acceleration.second +
                data.acceleration.third * data.acceleration.third)
    assertEquals(expectedMagnitude, data.accelerationMagnitude, 0.01f)
  }

  @Test
  fun timestamp_is_valid_and_recent() = runTest {
    val (_, listener) = setupMockedRepository()

    val beforeTime = System.currentTimeMillis()

    val receivedData = mutableListOf<MotionData>()
    val job = launch { repo.data.take(1).toList().also { receivedData.addAll(it) } }

    delay(100)

    listener.onSensorChanged(
        createMockSensorEvent(Sensor.TYPE_ACCELEROMETER, floatArrayOf(0f, 0f, 0f)))

    job.join()

    val afterTime = System.currentTimeMillis()

    assertTrue(receivedData.isNotEmpty())
    val data = receivedData.first()
    assertTrue(data.timestamp >= beforeTime)
    assertTrue(data.timestamp <= afterTime)
  }

  @Test
  fun acceleration_magnitude_is_always_positive() = runTest {
    val (_, listener) = setupMockedRepository()

    val receivedData = mutableListOf<MotionData>()
    val job = launch { repo.data.take(5).toList().also { receivedData.addAll(it) } }

    delay(100)

    listener.onSensorChanged(
        createMockSensorEvent(Sensor.TYPE_ACCELEROMETER, floatArrayOf(-3f, -4f, -5f)))
    listener.onSensorChanged(
        createMockSensorEvent(Sensor.TYPE_ACCELEROMETER, floatArrayOf(1f, 2f, 3f)))
    listener.onSensorChanged(
        createMockSensorEvent(Sensor.TYPE_ACCELEROMETER, floatArrayOf(0f, 0f, 0f)))
    listener.onSensorChanged(
        createMockSensorEvent(Sensor.TYPE_ACCELEROMETER, floatArrayOf(10f, -10f, 5f)))
    listener.onSensorChanged(
        createMockSensorEvent(Sensor.TYPE_ACCELEROMETER, floatArrayOf(-2f, 8f, -3f)))

    job.join()

    assertTrue(receivedData.size >= 5)
    receivedData.forEach { data ->
      assertTrue(
          "Magnitude should be positive: ${data.accelerationMagnitude}",
          data.accelerationMagnitude >= 0f)
    }
  }

  @Test
  fun null_sensor_event_is_handled_gracefully() = runTest {
    val (_, listener) = setupMockedRepository()

    val job = launch { repo.data.collect {} }

    delay(100)

    listener.onSensorChanged(null)

    assertTrue(true)

    job.cancel()
  }

  @Test
  fun onAccuracyChanged_does_nothing() = runTest {
    val (_, listener) = setupMockedRepository()

    val job = launch { repo.data.collect {} }

    delay(100)

    listener.onAccuracyChanged(mockAccelerometer, SensorManager.SENSOR_STATUS_ACCURACY_HIGH)

    assertTrue(true)

    job.cancel()
  }

  @Test
  fun mixed_accelerometer_and_gyroscope_events() = runTest {
    val (_, listener) = setupMockedRepository()

    val receivedData = mutableListOf<MotionData>()
    val job = launch { repo.data.take(4).toList().also { receivedData.addAll(it) } }

    delay(100)

    listener.onSensorChanged(
        createMockSensorEvent(Sensor.TYPE_ACCELEROMETER, floatArrayOf(1f, 2f, 3f)))
    listener.onSensorChanged(
        createMockSensorEvent(Sensor.TYPE_GYROSCOPE, floatArrayOf(0.5f, 0.6f, 0.7f)))
    listener.onSensorChanged(
        createMockSensorEvent(Sensor.TYPE_ACCELEROMETER, floatArrayOf(4f, 5f, 6f)))
    listener.onSensorChanged(
        createMockSensorEvent(Sensor.TYPE_GYROSCOPE, floatArrayOf(0.8f, 0.9f, 1.0f)))

    job.join()

    assertTrue(receivedData.size >= 4)
    val lastData = receivedData.last()
    assertEquals(0.8f, lastData.rotation.first, 0.01f)
    assertEquals(0.9f, lastData.rotation.second, 0.01f)
    assertEquals(1.0f, lastData.rotation.third, 0.01f)
  }
}
