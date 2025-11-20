package com.github.warnastrophy.core.domain.model

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import com.github.warnastrophy.core.ui.common.ErrorHandler
import com.google.android.gms.location.LocationServices

class BackgroundService() : Service() {

  private lateinit var gpsService: GpsService

  override fun onCreate() {
    super.onCreate()
    val fusedClient = LocationServices.getFusedLocationProviderClient(this)
    val errorHandler = ErrorHandler()
    // instantiate via factory or directly
    gpsService = GpsServiceFactory(fusedClient, errorHandler).create()
  }

  @RequiresApi(Build.VERSION_CODES.Q)
  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    // ensure runtime location permissions (ACCESS_FINE_LOCATION / BACKGROUND) are granted before
    // calling
    gpsService.startForegroundLocationUpdates(this)
    return START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()
    gpsService.clearErrorMsg()
    // call close() if you want to cancel coroutine scope and cleanup
    gpsService.close()
    Log.d("MapTrackingToggle", "BackgroundService destroyed and location updates stopped.")
  }

  override fun onBind(intent: Intent?): IBinder? = null
}
