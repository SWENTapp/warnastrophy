package com.github.warnastrophy.core.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.github.warnastrophy.R
import com.github.warnastrophy.core.data.repository.usecase.RefreshHazardsIfMovedUseCase
import com.github.warnastrophy.core.di.AppDependencies
import com.github.warnastrophy.core.model.Location
import com.github.warnastrophy.core.model.PositionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HazardTrackingService(
    private val testGpsService: PositionService? = null,
    private val testUseCase: RefreshHazardsIfMovedUseCase? = null,
    private val serviceScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val enableForegroud: Boolean = true
) : Service() {
  private lateinit var gpsService: PositionService
  private lateinit var refreshHazardsIfMovedUseCase: RefreshHazardsIfMovedUseCase

  override fun onCreate() {
    super.onCreate()
    println("Service onCreate called - Running in JVM Test")

    // Initialize dependencies (from your AppDependencies)
    gpsService = testGpsService ?: AppDependencies.gpsService
    refreshHazardsIfMovedUseCase = testUseCase ?: AppDependencies.refreshHazardsIfMovedUseCase

    if (enableForegroud) startForegroundService()
    startTracking()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    return START_STICKY
  }

  private fun startForegroundService() {
    Log.d("start", "ok")
    val channelId = "hazard_tracking_channel"
    val channelName = "Hazard Tracking"

    val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
    val manager = getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(channel)

    val notification: Notification =
        NotificationCompat.Builder(this, channelId)
            .setContentTitle("Warnastrophy")
            .setContentText("Tracking your location for hazard updates...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()

    startForeground(1, notification)
  }

  private fun startTracking() {
    serviceScope.launch {
      gpsService.positionState.collectLatest { positionState ->
        positionState.position.let { currentLocation ->
          refreshHazardsIfMovedUseCase.execute(
              Location(currentLocation.latitude, currentLocation.longitude))
        }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    serviceScope.cancel()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  fun stopTrackingForTest() = serviceScope.cancel()
}
