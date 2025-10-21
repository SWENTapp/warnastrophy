package com.github.warnastrophy.core.model

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.util.Log
import com.github.warnastrophy.core.util.AppConfig
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Service managing the user's GPS position. Maintains an observable state representing the current
 * position and possible errors.
 *
 * @param applicationContext Application context to access location services.
 */
interface PositionService {
  /** Observable state representing the current GPS position and metadata. */
  val positionState: StateFlow<GpsPositionState>

  fun requestCurrentLocation(locationClient: FusedLocationProviderClient): Unit

  fun startLocationUpdates(locationClient: FusedLocationProviderClient): Unit
}

val TAG = "GpsService"

/**
 * Implementation of the PositionService interface, handling GPS position updates and state.
 *
 * @property applicationContext Application context for accessing location services.
 */
class GpsService(
    private val applicationContext: Context,
) : PositionService {

  /** Coroutine scope for background operations. */
  private val serviceScope = CoroutineScope(Dispatchers.IO)

  /** Internal state flow holding the current GPS position state. */
  private val _positionState = MutableStateFlow(GpsPositionState())

  /** Public state flow exposing the current GPS position state. */
  override val positionState: StateFlow<GpsPositionState> = _positionState.asStateFlow()

  /** Location client for accessing fused location services. */
  private val locationClient: FusedLocationProviderClient =
      LocationServices.getFusedLocationProviderClient(applicationContext)

  /**
   * Requests the user's current location and updates [positionState] with the new position or the
   * corresponding error.
   *
   * @param locationClient The location provider client to use for fetching the location.
   */
  @SuppressLint("MissingPermission")
  override fun requestCurrentLocation(locationClient: FusedLocationProviderClient) {
    Log.e("GpsService", "Requesting current location")
    serviceScope.launch {
      setLoading(true)

      try {
        val request =
            CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMaxUpdateAgeMillis(0)
                .build()

        val location = locationClient.getCurrentLocation(request, null).await()
        Log.e("GpsService", "Location obtained: $location")
        if (location != null) {
          updatePosition(LatLng(location.latitude, location.longitude))
          setSuccess("Current position obtained")
        } else {
          setError("Position not available")
        }
      } catch (e: SecurityException) {
        setError("Location permission not granted")
      } catch (e: Exception) {
        setError("Location error: ${e.message}")
      } finally {
        setLoading(false)
      }
    }
  }

  /**
   * Starts automatic location updates. Updates are received via a callback that updates
   * [positionState].
   *
   * @param locationClient The location provider client to use for location updates.
   */
  @SuppressLint("MissingPermission")
  override fun startLocationUpdates(locationClient: FusedLocationProviderClient) {
    Log.e(TAG, "launching startLocationUpdates")
    setLoading(true)

    try {
      val request =
          LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, AppConfig.positionUpdateDelayMs)
              .build()

      locationClient.requestLocationUpdates(
          request,
          object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {

              val pos = result.lastLocation
              Log.e(TAG, "requestPosupd : $pos")
              if (pos == null) {
                setError("No location fix available")
                setLoading(false)
              } else {
                updatePosition(LatLng(pos.latitude, pos.longitude))
                setSuccess("Position updated")
                setLoading(false)
              }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
              if (!availability.isLocationAvailable) {
                setError("Location temporarily unavailable")
                setLoading(false)
              } else {
                setSuccess("Location available")
                setLoading(false)
              }
            }
          },
          Looper.getMainLooper())
    } catch (e: SecurityException) {
      setError("Location permission not granted!")
      setLoading(false)
    } catch (e: Exception) {
      setError("Location update failed: ${e.message}")
      setLoading(false)
    }
  }

  /**
   * Updates the position in the state with success.
   *
   * @param position The new GPS position.
   */
  private fun updatePosition(position: LatLng) {
    _positionState.update { currentState ->
      currentState.copy(
          position = position, result = GpsResult.Success("Position updated"), errorMessage = null)
    }
  }

  /**
   * Sets an error message in the state.
   *
   * @param message The error message to set.
   */
  private fun setError(message: String) {
    _positionState.update { currentState ->
      currentState.copy(result = GpsResult.Failed, errorMessage = message)
    }
  }

  /**
   * Sets a success message in the state.
   *
   * @param message The success message to set.
   */
  private fun setSuccess(message: String) {
    _positionState.update { currentState ->
      currentState.copy(result = GpsResult.Success(message), errorMessage = null)
    }
  }

  /**
   * Updates the loading state.
   *
   * @param isLoading Whether the service is currently loading.
   */
  private fun setLoading(isLoading: Boolean) {
    _positionState.update { currentState -> currentState.copy(isLoading = isLoading) }
  }

  /** Releases resources used by this service. Call when the service is no longer needed. */
  fun close() {
    serviceScope.cancel()
  }

  /** Clears the error message in the state. */
  fun clearErrorMsg() {
    _positionState.value = positionState.value.copy(errorMessage = null)
  }
}

/**
 * State representing the current GPS position and associated metadata.
 *
 * @property position The current GPS position.
 * @property isLoading Whether the service is currently loading.
 * @property errorMessage The current error message, if any.
 * @property result The result of the last GPS operation.
 */
data class GpsPositionState(
    val position: LatLng = AppConfig.defaultPosition,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val result: GpsResult = GpsResult.Failed
)

/** Result of a GPS operation. */
sealed class GpsResult {
  /** The operation failed. */
  object Failed : GpsResult()

  /**
   * The operation succeeded with the specified message.
   *
   * @property message Success message.
   */
  data class Success(val message: String = "Success") : GpsResult()
}
