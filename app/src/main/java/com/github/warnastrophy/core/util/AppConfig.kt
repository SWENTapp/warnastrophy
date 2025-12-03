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
  val gdacsFetchDelay = 10.minutes
  /** Throttle delay between successive API requests to avoid rate limiting. */
  val gdacsThrottleDelay = 1.seconds
  val defaultPosition: LatLng = LatLng(0.0, 0.0) // Somewhere
  val positionUpdateDelayMs: Long = 10_000L
  val rectangleHazardZone = Pair(20000.0, 20000.0)
  val priorDaysFetch = "4"

  const val HTTP_TIMEOUT = 15000

  val GOOGLE_MAP_LINK = "https://www.google.com/maps"

  object Endpoints {
    const val EVENTS_BY_AREA = "https://www.gdacs.org/gdacsapi/api/Events/geteventlist/eventsbyarea"
    const val EMM_NEWS_BY_KEY = "https://www.gdacs.org/gdacsapi/api/Emm/getemmnewsbykey"
    const val GET_GEOMETRY = "https://www.gdacs.org/gdacsapi/api/polygons/getgeometry"
  }

  const val PREF_FILE_NAME = "warnastrophy_app_prefs"

  const val defaultUserId = "default_user"
}
