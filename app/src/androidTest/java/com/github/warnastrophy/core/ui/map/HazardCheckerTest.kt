package com.github.warnastrophy.core.ui.map

import com.github.warnastrophy.core.data.service.HazardChecker
import com.github.warnastrophy.core.data.service.ServiceStateManager
import com.github.warnastrophy.core.model.Hazard
import com.github.warnastrophy.core.model.Location
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

class HazardCheckerTest {
  private val HAZARD_TIME_THRESHOLD_MS = 5000L

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun testHazardCheckerDetectsHighestPriorityHazard() = runTest {
    // --- 1. Define WKT Strings ---
    // WKT for a simple square polygon (9.9, 9.9) to (10.1, 10.1)
    // User (10.0, 10.0) is INSIDE.
    val wktA = "POLYGON((9.9 9.9, 9.9 10.1, 10.1 10.1, 10.1 9.9, 9.9 9.9))"

    // WKT for a slightly larger square polygon (9.8, 9.8) to (10.2, 10.2)
    // User (10.0, 10.0) is INSIDE.
    val wktB = "POLYGON((9.8 9.8, 9.8 10.2, 10.2 10.2, 10.2 9.8, 9.8 9.8))"

    // --- 2. Setup Test Hazard Data ---
    // Hazard A: Higher Alert Level (3), Location (10.0, 10.0) - Should win
    val testHazardA =
        Hazard(
            id = 1001, // Unique ID
            type = "TC",
            country = "Testland",
            date = "2025-01-01",
            severity = 2.0,
            severityUnit = "unit",
            reportUrl = "http://example.test/A",
            alertLevel = 3, // HIGHER priority
            coordinates = listOf(Location(latitude = 10.0, longitude = 10.0)),
            bbox = listOf(9.9, 9.9, 10.1, 10.1),
            affectedZoneWkt = wktA)

    // Hazard B: Lower Alert Level (2), Location (10.0, 10.0)
    val testHazardB =
        Hazard(
            id = 1002, // Unique ID
            type = "FL",
            country = "Testland",
            date = "2025-01-01",
            severity = 1.0,
            severityUnit = "unit",
            reportUrl = "http://example.test/B",
            alertLevel = 2, // LOWER priority
            coordinates = listOf(Location(latitude = 10.0, longitude = 10.0)),
            bbox = listOf(9.8, 9.8, 10.2, 10.2),
            affectedZoneWkt = wktB // <-- Use WKT string
            )

    val serviceStateManager = ServiceStateManager
    val testDispatcher = StandardTestDispatcher(testScheduler)

    // Reset the test state before running (Assumes TestServiceStateManager is available)
    serviceStateManager.updateActiveHazard(null)

    val hazardChecker =
        HazardChecker(listOf(testHazardA, testHazardB), testDispatcher, scope = this)

    hazardChecker.checkAndPublishAlert(10.0, 10.0)

    advanceTimeBy(HAZARD_TIME_THRESHOLD_MS)
    runCurrent()

    // --- 5. Assertion ---
    val currentHazard = ServiceStateManager.activeHazardFlow.value

    assertNotNull("The active hazard should not be null after delay.", currentHazard)
    // Assert that the highest priority hazard (A, with alertLevel 3) was selected
    assertEquals(testHazardA.id, currentHazard?.id)
  }
}
