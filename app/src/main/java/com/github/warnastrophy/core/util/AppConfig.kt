package com.github.warnastrophy.core.util

import com.github.warnastrophy.R
import com.google.android.gms.maps.model.LatLng

/**
 * `AppConfig` holds configuration settings for the application, such as the fetch delay in
 * milliseconds.
 */
object AppConfig {
  /** Fetch delay in milliseconds for periodic data updates. Default is 5000 ms (5 seconds). */
  var fetchDelayMs: Long = R.integer.fetch_delay_ms.toLong()
  val defaultPosition: LatLng = LatLng(0.0, 0.0) // Somewhere
  val positionUpdateDelayMs: Long = 10_000L
  val rectangleHazardZone = Pair(20000.0, 20000.0)
  val priorDaysFetch = "4"
}
