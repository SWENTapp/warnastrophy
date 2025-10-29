package com.github.warnastrophy.core.util

import com.google.android.gms.maps.model.LatLng

/**
 * `AppConfig` holds configuration settings for the application, such as the fetch delay in
 * milliseconds.
 */
object AppConfig {
  /** Fetch delay in milliseconds for periodic data updates. Default is 5000 ms (5 seconds). */
  var fetchDelayMs: Long = 5_000L
  val defaultPosition: LatLng = LatLng(18.5944, -72.3074) // Port au prince
  val positionUpdateDelayMs: Long = 10_000L
  val rectangleHazardZone = Pair(100.0, 100.0)
  val priorDaysFetch = "4"
}
