package com.github.warnastrophy.core.domain.model

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat.startForeground
import com.github.warnastrophy.core.ui.common.ErrorHandler
import com.github.warnastrophy.core.ui.navigation.Screen
import com.github.warnastrophy.core.util.AppConfig
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import kotlin.text.compareTo
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
 * @property positionState Observable state of the current GPS position and metadata.
 * @property locationClient Location provider client for accessing fused location services.
 * @property errorHandler Handler for managing errors related to GPS operations.
 */
interface PositionService {
  /** Observable state representing the current GPS position and metadata. */
  val positionState: StateFlow<GpsPositionState>

  val locationClient: FusedLocationProviderClient

  val errorHandler: ErrorHandler

  /**
   * Requests the user's current location and updates [positionState] with the new position or the
   * corresponding error.
   */
  fun requestCurrentLocation()

  /**
   * Starts automatic location updates. Updates are received via a callback that updates
   * [positionState].
   */
  fun startLocationUpdates()

  /** Releases resources used by this service. Call when the service is no longer needed. */
  fun stopLocationUpdates()

  fun startForegroundLocationUpdates(
      service: Service,
      channelId: String = DEFAULT_CHANNEL_ID,
      channelName: String = DEFAULT_CHANNEL_NAME,
      notificationId: Int = DEFAULT_NOTIFICATION_ID
  )
}

const val TAG = "GpsService"
private const val DEFAULT_CHANNEL_ID = "gps_foreground_channel"
private const val DEFAULT_CHANNEL_NAME = "GPS tracking"
private const val DEFAULT_NOTIFICATION_ID = 101

/**
 * Implementation of the PositionService interface, handling GPS position updates and state.
 *
 * @property locationClient Location provider client for accessing fused location services.
 * @property errorHandler Handler for managing errors related to GPS operations.
 */
class GpsService(
    override val locationClient: FusedLocationProviderClient,
    override val errorHandler: ErrorHandler = ErrorHandler(),
) : PositionService {

  /** Coroutine scope for background operations. */
  private val serviceScope = CoroutineScope(Dispatchers.IO)

  /** Internal state flow holding the current GPS position state. */
  private val _positionState = MutableStateFlow(GpsPositionState())

  /** Public state flow exposing the current GPS position state. */
  override val positionState: StateFlow<GpsPositionState> = _positionState.asStateFlow()

  private val locationCallBack =
      object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {

          val pos = result.lastLocation
          Log.e(TAG, "requestPosupd : $pos")
          if (pos == null) {
            setError("No location fix available")
          } else {
            updatePosition(LatLng(pos.latitude, pos.longitude))
            setSuccess("Position updated")
          }
          setLoading(false)
        }

        override fun onLocationAvailability(availability: LocationAvailability) {
          if (!availability.isLocationAvailable) {
            setError("Location temporarily unavailable")
          } else {
            setSuccess("Location available")
          }
          setLoading(false)
        }
      }

  /** Location client for accessing fused location services. */

  /**
   * Requests the user's current location and updates [positionState] with the new position or the
   * corresponding error.
   *
   * @param locationClient The location provider client to use for fetching the location.
   */
  @SuppressLint("MissingPermission")
  override fun requestCurrentLocation() {
    Log.e("GpsService", "Requesting current location")
    serviceScope.launch {
      try {
        setLoading(true)
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
      } catch (_: SecurityException) {
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
  override fun startLocationUpdates() {
    Log.e(TAG, "launching startLocationUpdates")
    try {
      setLoading(true)
      val request =
          LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, AppConfig.positionUpdateDelayMs)
              .build()

      locationClient.requestLocationUpdates(request, locationCallBack, Looper.getMainLooper())
    } catch (_: SecurityException) {
      setError("Location permission not granted!")
    } catch (e: Exception) {
      setError("Location update failed: ${e.message}")
    } finally {
      setLoading(false)
    }
  }

  override fun stopLocationUpdates() {
    close()
  }

  @RequiresApi(Build.VERSION_CODES.Q)
  @SuppressLint("MissingPermission")
  override fun startForegroundLocationUpdates(
      service: Service,
      channelId: String,
      channelName: String,
      notificationId: Int
  ) {
    // create channel for O+
    val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
    val nm = service.getSystemService(NotificationManager::class.java)
    nm?.createNotificationChannel(channel)
    Log.d("MapTrackingToggle", "Notification channel created for foreground GPS service.")

    // Pending intent to open app when the user taps the notification
    val launchIntent = service.packageManager.getLaunchIntentForPackage(service.packageName)
    val pendingIntent =
        PendingIntent.getActivity(
            service,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    val notification =
        NotificationCompat.Builder(service, channelId)
            .setContentTitle("Location tracking active")
            .setContentText("Your location is being monitored")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .build()

    // Promote the provided Service to foreground
    startForeground(
        service, notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
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
    errorHandler.addError("GPS Error: $message", Screen.Map)
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
    locationClient.removeLocationUpdates(locationCallBack)
    setLoading(false)
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

class GpsServiceFactory(
    private val locationClient: FusedLocationProviderClient,
    private val errorHandler: ErrorHandler
) {
  fun create(): GpsService {
    return GpsService(locationClient, errorHandler)
  }
}
