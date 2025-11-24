package com.github.warnastrophy.core.data.service

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import com.github.warnastrophy.core.ui.common.ErrorHandler
import com.google.android.gms.location.LocationServices

/**
 * A foreground service that provides continuous location updates using GPS.
 *
 * This service utilizes [GpsService] to manage location updates and runs in the foreground to
 * ensure it remains active even when the app is in the background.
 */
class ForegroundGpsService() : Service() {

  /** The GpsService instance used for location updates. */
  private var gpsService: GpsService? = null

  /**
   * Initializes the BackgroundService and its dependencies.
   *
   * This method is called when the service is created. It sets up the GpsService using the
   * FusedLocationProviderClient and an ErrorHandler.
   */
  override fun onCreate() {
    super.onCreate()
    if (gpsService == null) {
      val fusedClient = LocationServices.getFusedLocationProviderClient(this)
      val errorHandler = ErrorHandler()
      gpsService = GpsServiceFactory(fusedClient, errorHandler).create()
    }
  }

  /**
   * Sets a custom GpsService instance for testing purposes.
   *
   * @param gpsService The GpsService instance to be used in tests.
   */
  fun setGpsServiceForTest(gpsService: GpsService) {
    this.gpsService = gpsService
  }

  /**
   * Starts the service in the foreground to receive location updates.
   *
   * This method is called when the service is started. It initiates foreground location updates via
   * the GpsService.
   *
   * @param intent The Intent that started the service.
   * @param flags Additional data about the start request.
   * @param startId A unique integer representing this specific request to start.
   * @return An integer indicating how the system should handle the service if it's killed.
   */
  @RequiresApi(Build.VERSION_CODES.Q)
  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    // ensure runtime location permissions (ACCESS_FINE_LOCATION / BACKGROUND) are granted before
    // calling
    gpsService?.startForegroundLocationUpdates(this)
    return START_STICKY
  }

  /**
   * Cleans up resources when the service is destroyed.
   *
   * This method is called when the service is being destroyed. It clears any error messages and
   * stops location updates via the GpsService.
   */
  override fun onDestroy() {
    super.onDestroy()
    gpsService?.clearErrorMsg()
    gpsService?.close()
    Log.d("MapTrackingToggle", "BackgroundService destroyed and location updates stopped.")
  }

  /**
   * Binds the service to a client.
   *
   * This service does not support binding, so this method returns null.
   *
   * @param intent The Intent that was used to bind to the service.
   * @return Always returns null as binding is not supported.
   */
  override fun onBind(intent: Intent?): IBinder? = null
}