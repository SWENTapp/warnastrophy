package com.github.warnastrophy.core.service

import android.util.Log
import com.github.warnastrophy.core.common.ServiceStateManager
import com.github.warnastrophy.core.model.util.Hazard
import com.github.warnastrophy.core.util.WktParserUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point

class HazardChecker(private val allHazards: List<Hazard>) {
  private val checkerScope = CoroutineScope(Dispatchers.Default + Job())
  private val geometryFactory = GeometryFactory()

  // State to track entry time for each hazard (Hazard ID -> Entry Time in Millis)
  private val hazardEntryTimes = mutableMapOf<Int, Long>()

  // State to hold a pending job for a notification (Hazard ID -> Job)
  private val pendingAlertJobs = mutableMapOf<Int, Job>()

  private val HAZARD_TIME_THRESHOLD_MS = 5000L

  /**
   * Executes the two-step geofencing check and publishes the result. This function is called
   * frequently by the ViewModel's LocationCallback.
   *
   * @param userLat The user's current latitude.
   * @param userLng The user's current longitude.
   */
  fun checkAndPublishAlert(userLng: Double, userLat: Double) {
    checkerScope.launch {
      val activeHazard: Hazard? = findHighestPriorityActiveHazard(userLng, userLat)

      // 1. Handle Entry: Start tracking time and schedule a delayed job
      if (activeHazard != null) {
        handleHazardEntry(activeHazard)
      }
      // 2. Handle Exit: Cancel tracking and pending jobs for hazards no longer active
      else {
        // Determine which hazards are no longer active and clean them up
        cleanUpInactiveHazards(activeHazard)
      }
    }
  }

  /** Performs the initial check to find the hazard the user is currently inside. */
  private fun findHighestPriorityActiveHazard(userLng: Double, userLat: Double): Hazard? {
    var highestPriorityHazard: Hazard? = null

    for (hazard in allHazards) {
      Log.d("HazardChecker", "Evaluating hazard ID=${hazard.id}...")
      // Assumes isInsideBBox is already implemented
      if (hazard.bbox != null && isInsideBBox(userLng, userLat, hazard.bbox)) {
        val polygonWKT = WktParserUtil.polygonWktFromLocations(hazard.polygon) ?: continue
        Log.d("HazardChecker", "Checking hazard ID=${hazard.id} with WKT=$polygonWKT")
        if (hazard.polygon != null && isInsideMultiPolygon(userLat, userLng, polygonWKT)) {
          Log.d("HazardChecker", "User is inside hazard ID=${hazard.id}!!!!!!")

          if (highestPriorityHazard == null ||
              (hazard.alertLevel ?: 0) > (highestPriorityHazard.alertLevel ?: 0)) {
            highestPriorityHazard = hazard
          }
        }
      }
    }
    return highestPriorityHazard
  }

  private fun handleHazardEntry(hazard: Hazard) {
    val hazardId = hazard.id ?: return
    val currentTime = System.currentTimeMillis()

    // CASE 1: New Entry (or first time detected since last exit)
    if (!hazardEntryTimes.containsKey(hazardId)) {
      hazardEntryTimes[hazardId] = currentTime

      // Schedule the job to check containment after the threshold period
      scheduleAlertCheck(hazard)
    }

    // CASE 2: Already inside (no action needed, timer is running/job is scheduled)
    // We don't need to reset the time, as the original entry time holds.
  }

  private fun scheduleAlertCheck(hazard: Hazard) {
    // Cancel any existing job for this hazard to avoid duplicates (though rare here)
    val hazardId = hazard.id ?: return
    pendingAlertJobs[hazardId]?.cancel()

    // Schedule a job to run after the time threshold
    val job =
        checkerScope.launch {
          delay(HAZARD_TIME_THRESHOLD_MS)

          // Check if the user is *still* inside the hazard zone
          // This final check confirms GPS stability (removes drift)
          val currentEntryTime = hazardEntryTimes[hazardId]

          if (currentEntryTime != null) {
            // If we reach here, the time has passed AND we haven't been canceled,
            // so the user has been stable inside the zone.
            ServiceStateManager.updateActiveHazard(hazard)
          }

          // Clean up the job entry after execution
          pendingAlertJobs.remove(hazard.id)
        }
    pendingAlertJobs[hazardId] = job
  }

  /**
   * Helper function for the efficient Bounding Box (BBox) check. NOTE: Replace with your final
   * implementation.
   */
  private fun isInsideBBox(lng: Double, lat: Double, bbox: List<Double>): Boolean {
    // BBox order is typically [min_lng, min_lat, max_lng, max_lat]
    if (bbox.size != 4) return false
    return lng >= bbox[0] && lat >= bbox[1] && lng <= bbox[2] && lat <= bbox[3]
  }

  /**
   * Performs the robust Point-in-Polygon (PIP) check using the JTS Topology Suite.
   *
   * @param lat The user's current latitude (Y coordinate).
   * @param lng The user's current longitude (X coordinate).
   * @param multiPolygonWKT The WKT string representing the hazard area.
   * @return True if the point is contained within the geometry, false otherwise.
   */
  private fun isInsideMultiPolygon(lat: Double, lng: Double, multiPolygonWKT: String): Boolean {

    // 1. Get the JTS Geometry object for the hazard.
    val hazardGeometry = WktParserUtil.parseWktToJtsGeometry(multiPolygonWKT)

    // Safety check: if parsing failed or the geometry is empty, return false.
    if (hazardGeometry == null || hazardGeometry.isEmpty) {
      return false
    }

    // 2. Create the user's JTS Point object.
    // CRITICAL: JTS uses (x, y) coordinates, which corresponds to (Longitude, Latitude) in WGS84.
    val userCoordinate = Coordinate(lng, lat)
    val userPoint: Point = geometryFactory.createPoint(userCoordinate)

    // 3. Perform the containment check using the JTS spatial predicate.
    // The .contains() method handles the full complexity of MultiPolygons/holes automatically.
    return hazardGeometry.contains(userPoint)

    // NOTE: If you need to include the boundary, use hazardGeometry.covers(userPoint) instead.
  }

  private fun cleanUpInactiveHazards(currentActiveHazard: Hazard?) {
    val currentHazardId = currentActiveHazard?.id

    // Iterate over all currently tracked entries
    val iter = hazardEntryTimes.keys.iterator()
    while (iter.hasNext()) {
      val hazardId = iter.next()

      // If the user is NOT in this zone now (null means user is safe, or different ID)
      if (hazardId != currentHazardId) {
        // 1. Cancel the pending delayed alert job for this hazard
        pendingAlertJobs[hazardId]?.cancel()
        pendingAlertJobs.remove(hazardId)

        // 2. Remove the entry time tracking
        iter.remove()

        // 3. Clear the alert if this was the active one
        if (ServiceStateManager.activeHazardFlow.value?.id == hazardId) {
          ServiceStateManager.updateActiveHazard(null)
        }
      }
    }
  }

  // Optional: Add a function to cancel the checker's scope if needed
  fun cancelChecks() {
    checkerScope.cancel()
  }
}
