package com.github.warnastrophy.core.data.service

import android.util.Log
import com.github.warnastrophy.core.model.Hazard
import com.github.warnastrophy.core.util.WktParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point

private const val TAG = "HazardChecker"

class HazardChecker(
    private val allHazards: List<Hazard>,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    private val scope: CoroutineScope
) {

  private val checkerScope = CoroutineScope(dispatcher + Job())
  private val geometryFactory = GeometryFactory()
  private val hazardLock = Mutex()

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
    scope.launch(dispatcher) {
      val activeHazard: Hazard? = findHighestPriorityActiveHazard(userLng, userLat)
      Log.d(TAG, "active hazard is: ${activeHazard?.id}")

      // 1. Clean up ALL inactive/lower-priority hazards first
      cleanUpInactiveHazards(activeHazard)

      // 2. Handle Entry for the currently active (highest priority) hazard
      if (activeHazard != null) {
        handleHazardEntry(activeHazard)
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
        if (hazard.affectedZoneWkt == null) continue
        Log.d("HazardChecker", "Checking hazard ID=${hazard.id} with WKT=${hazard.affectedZoneWkt}")
        if (isInsideMultiPolygon(userLat, userLng, hazard.affectedZoneWkt)) {
          Log.d("HazardChecker", "User is inside hazard ID=${hazard.id}!!!!!!")

          if (highestPriorityHazard == null ||
              (hazard.alertLevel ?: 0) > (highestPriorityHazard.alertLevel ?: 0)) {
            highestPriorityHazard = hazard
          }
        }
      }
    }
    Log.d("HazardChecker", "Highest hazard is ID=${highestPriorityHazard?.id}!!!!!!")
    return highestPriorityHazard
  }

  private suspend fun handleHazardEntry(hazard: Hazard) =
      hazardLock.withLock {
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
    Log.d(TAG, "Schedule hazard: ${hazard.id}")
    pendingAlertJobs[hazardId]?.cancel()

    // Schedule a job to run after the time threshold
    val job =
        scope.launch(dispatcher) {
          delay(HAZARD_TIME_THRESHOLD_MS)

          // Check if the user is *still* inside the hazard zone
          // This final check confirms GPS stability (removes drift)
          val currentEntryTime = hazardEntryTimes[hazardId]
          Log.d(TAG, "Current entry time: ${currentEntryTime}")
          if (currentEntryTime != null) {
            // If we reach here, the time has passed AND we haven't been canceled,
            // so the user has been stable inside the zone.
            Log.d(TAG, "Update active hazard in service state manager: ${hazard.id}")
            ServiceStateManager.updateActiveHazard(hazard)
          }

          // Clean up the job entry after execution
          pendingAlertJobs.remove(hazard.id)
        }
    Log.d(TAG, "Job add: ${hazard.id}")
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
    val hazardGeometry = WktParser.parseWktToJtsGeometry(multiPolygonWKT)

    // Safety check: if parsing failed or the geometry is empty, return false.
    if (hazardGeometry == null || hazardGeometry.isEmpty) {
      return false
    }

    val userCoordinate = Coordinate(lng, lat)
    val userPoint: Point = geometryFactory.createPoint(userCoordinate)

    return hazardGeometry.contains(userPoint)
  }

  private suspend fun cleanUpInactiveHazards(currentActiveHazard: Hazard?) {
    val currentHazardId = currentActiveHazard?.id

    hazardLock.withLock {
      val hazardsToClean = hazardEntryTimes.keys - setOfNotNull(currentHazardId)
      hazardsToClean.forEach { hazardId ->
        Log.d(TAG, "Cleaning up inactive hazard ID=$hazardId")
        pendingAlertJobs[hazardId]?.cancel()
        pendingAlertJobs.remove(hazardId)
        hazardEntryTimes.remove(hazardId)
        if (ServiceStateManager.activeHazardFlow.value?.id == hazardId) {
          Log.d(TAG, "Clearing global active hazard ID=$hazardId (user exited)")
          ServiceStateManager.clearActiveAlert()
        }
      }
      Log.d("HazardChecker", "There is no hazard to clear: ${hazardsToClean.isEmpty()}")
    }
  }
}
