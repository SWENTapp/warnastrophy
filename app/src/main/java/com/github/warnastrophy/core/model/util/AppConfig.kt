package com.github.warnastrophy.core.model.util

import com.google.android.gms.maps.model.LatLng

/**
 * `AppConfig` holds configuration settings for the application, such as the fetch delay in
 * milliseconds.
 */
object AppConfig {
  /** Fetch delay in milliseconds for periodic data updates. Default is 5000 ms (5 seconds). */
  var fetchDelayMs: Long = 5_000L

  var positionUpdateDelayMs: Long = 2_000L

  var defaultPosition = LatLng(18.5944, -72.3074) // Default to Port-au-Prince
}
