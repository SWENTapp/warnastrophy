package com.github.warnastrophy.core.data.service

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
import com.github.warnastrophy.core.ui.common.ErrorType
import com.github.warnastrophy.core.ui.navigation.Screen
import com.github.warnastrophy.core.util.AppConfig
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
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
          if (pos != null) {
            updatePosition(LatLng(pos.latitude, pos.longitude))
            setSuccess("Position updated")
            clearError(ErrorType.LOCATION_ERROR)
          }
          setLoading(false)
        }

        override fun onLocationAvailability(availability: LocationAvailability) {
          if (availability.isLocationAvailable) {
            setSuccess("Location available")
            clearError(ErrorType.LOCATION_ERROR)
          }
          setLoading(false)
        }
      }

  /**
   * Requests the user's current location and updates [positionState] with the new position or the
   * corresponding error.
   *
   * @param locationClient The location provider client to use for fetching the location.
   */
  @SuppressLint("MissingPermission")
  override fun requestCurrentLocation() {
    serviceScope.launch {
      try {
        setLoading(true)
        val request =
            CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMaxUpdateAgeMillis(0)
                .build()

        val location = locationClient.getCurrentLocation(request, null).await()
        if (location != null) {
          updatePosition(LatLng(location.latitude, location.longitude))
          setSuccess("Current position obtained")
          clearError(ErrorType.LOCATION_ERROR)
          clearError(ErrorType.LOCATION_NOT_GRANTED_ERROR)
          clearError(ErrorType.LOCATION_UPDATE_ERROR)
        }
      } catch (_: SecurityException) {
        setError(ErrorType.LOCATION_NOT_GRANTED_ERROR)
      } catch (_: Exception) {
        setError(ErrorType.LOCATION_UPDATE_ERROR)
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
      clearError(ErrorType.LOCATION_NOT_GRANTED_ERROR)
      clearError(ErrorType.LOCATION_UPDATE_ERROR)
    } catch (_: SecurityException) {
      setError(ErrorType.LOCATION_NOT_GRANTED_ERROR)
    } catch (_: Exception) {
      setError(ErrorType.LOCATION_UPDATE_ERROR)
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
    try {
      // Create notification channel for foreground service
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
      clearError(ErrorType.FOREGROUND_ERROR)
    } catch (_: Exception) {
      setError(ErrorType.FOREGROUND_ERROR)
    }
  }

  /**
   * Updates the position in the state with success.
   *
   * @param position The new GPS position.
   */
  private fun updatePosition(position: LatLng) {
    _positionState.update { currentState ->
      currentState.copy(position = position, result = GpsResult.Success("Position updated"))
    }
  }

  /**
   * Sets an error message in the state.
   *
   * @param type The type of error to set.
   */
  private fun setError(type: ErrorType) {
    _positionState.update { currentState -> currentState.copy(result = GpsResult.Failed) }
    errorHandler.addErrorToScreen(type, Screen.Map)
  }

  /**
   * Clears a specific error type from the error handler.
   *
   * @param type The type of error to clear.
   */
  private fun clearError(type: ErrorType) {
    errorHandler.clearErrorFromScreen(type, Screen.Map)
  }

  /**
   * Sets a success message in the state.
   *
   * @param message The success message to set.
   */
  private fun setSuccess(message: String) {
    _positionState.update { currentState -> currentState.copy(result = GpsResult.Success(message)) }
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
    clearErrorMsg()
    serviceScope.cancel()
    locationClient.removeLocationUpdates(locationCallBack)
    setLoading(false)
  }

  /** Clears the error message in the state. */
  fun clearErrorMsg() {
    clearError(ErrorType.LOCATION_NOT_GRANTED_ERROR)
    clearError(ErrorType.LOCATION_UPDATE_ERROR)
    clearError(ErrorType.LOCATION_ERROR)
    clearError(ErrorType.FOREGROUND_ERROR)
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
