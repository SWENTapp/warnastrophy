package com.github.warnastrophy.core.util

import com.google.android.gms.maps.model.LatLng
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * `AppConfig` holds configuration settings for the application, such as the fetch delay in
 * milliseconds.
 */
object AppConfig {
  /** Fetch delay for periodic data updates (from GDACS API). */
  val gdacsFetchDelay = 1.minutes
  /** Throttle delay between successive API requests to avoid rate limiting. */
  val gdacsThrottleDelay = 1.seconds
  val defaultPosition: LatLng = LatLng(0.0, 0.0) // Somewhere
  val positionUpdateDelayMs: Long = 10_000L
  val rectangleHazardZone = Pair(20000.0, 20000.0)
  val priorDaysFetch = "4"
}
