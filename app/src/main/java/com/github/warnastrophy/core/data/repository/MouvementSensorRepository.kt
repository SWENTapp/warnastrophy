/**
 * This file was written by Anas Sidi Mohamed with the assistance of ChatGPT.
 *
 * Author: Anas Sidi Mohamed Assistance: ChatGPT
 */
package com.github.warnastrophy.core.data.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Represents a single motion sample produced by sensors.
 *
 * @property timestamp Epoch milliseconds when the sample was created.
 * @property acceleration Linear acceleration on X, Y, Z axes (m/s²).
 * @property rotation Rotation rates on X, Y, Z axes (rad/s).
 * @property accelerationMagnitude Magnitude of the linear acceleration vector.
 */
data class MotionData(
    val timestamp: Long,
    val acceleration: Triple<Float, Float, Float>,
    val rotation: Triple<Float, Float, Float>,
    val accelerationMagnitude: Float
)

/**
 * Repository that exposes a cold [Flow] of [MotionData] samples by listening to the accelerometer
 * and gyroscope sensors.
 *
 * The flow uses a low-pass filter to estimate gravity and subtract it from raw accelerometer values
 * to produce linear acceleration values. The sensor listeners are registered when the flow is
 * collected and unregistered when the collector is cancelled.
 *
 * Note: Consumers should handle lifecycle and permissions externally.
 *
 * @param context Application context used to obtain the SensorManager.
 */
class MouvementSensorRepository(context: Context) {

  private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

  private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

  private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

  // Gravity estimator for low-pass filter (initialized to zeros).
  private val gravity = FloatArray(3)

  /**
   * Flow that emits [MotionData] whenever sensor events arrive.
   *
   * The implementation uses `callbackFlow` and registers a [SensorEventListener] that merges
   * accelerometer and gyroscope values into a single [MotionData].
   */
  val data: Flow<MotionData> = callbackFlow {
    val listener =
        object : SensorEventListener {
          private var accValues = Triple(0f, 0f, 0f)
          private var gyroValues = Triple(0f, 0f, 0f)

          override fun onSensorChanged(event: SensorEvent?) {
            event ?: return

            when (event.sensor.type) {
              Sensor.TYPE_ACCELEROMETER -> {
                // Low-pass filter to estimate gravity.
                val alpha = 0.8f
                gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
                gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
                gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]

                val linearAccX = event.values[0] - gravity[0]
                val linearAccY = event.values[1] - gravity[1]
                val linearAccZ = event.values[2] - gravity[2]

                accValues = Triple(linearAccX, linearAccY, linearAccZ)
              }
              Sensor.TYPE_GYROSCOPE -> {
                gyroValues = Triple(event.values[0], event.values[1], event.values[2])
              }
            }

            val magnitude =
                sqrt(
                    accValues.first * accValues.first +
                        accValues.second * accValues.second +
                        accValues.third * accValues.third)

            trySend(
                    MotionData(
                        timestamp = event.timestamp,
                        acceleration = accValues,
                        rotation = gyroValues,
                        accelerationMagnitude = magnitude))
                .isSuccess // result ignored here; callbackFlow will keep working
          }

          override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

    accelerometer?.let {
      sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME)
    }
    gyroscope?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }

    awaitClose { sensorManager.unregisterListener(listener) }
  }
}
