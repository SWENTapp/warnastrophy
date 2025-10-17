package com.github.warnastrophy.core.util

/**
 * `AppConfig` holds configuration settings for the application, such as the fetch delay in
 * milliseconds.
 */
object AppConfig {
  /** Fetch delay in milliseconds for periodic data updates. Default is 5000 ms (5 seconds). */
  var fetchDelayMs: Long = 5_000L
}
