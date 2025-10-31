package com.github.warnastrophy.core.ui.map

import com.github.warnastrophy.core.data.service.HazardChecker
import com.github.warnastrophy.core.data.service.ServiceStateManager
import com.github.warnastrophy.core.model.Hazard
import com.github.warnastrophy.core.model.Location
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
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
    val wktB = "POLYGON((9.8 9.8, 9.8 10.2, 10.2 10.2, 10.2 9.8, 9.8 9.8))"
    val testHazardA =
        Hazard(
            id = 1001, // Unique ID
            type = "TC",
            description = "Test Hazard A",
            country = "Testland",
            date = "2025-01-01",
            severity = 2.0,
            severityUnit = "unit",
            reportUrl = "http://example.test/A",
            alertLevel = 3, // HIGHER priority
            coordinates = listOf(Location(latitude = 10.0, longitude = 10.0)),
            bbox = listOf(9.9, 9.9, 10.1, 10.1),
            affectedZoneWkt = wktA)

    val testHazardB =
        Hazard(
            id = 1002, // Unique ID
            type = "FL",
            description = "Test Hazard B",
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

    serviceStateManager.updateActiveHazard(null)

    val hazardChecker =
        HazardChecker(listOf(testHazardA, testHazardB), testDispatcher, scope = this)

    hazardChecker.checkAndPublishAlert(10.0, 10.0)

    advanceTimeBy(HAZARD_TIME_THRESHOLD_MS)
    runCurrent()

    val currentHazard = ServiceStateManager.activeHazardFlow.value

    assertNotNull("The active hazard should not be null after delay.", currentHazard)
    assertEquals(testHazardA.id, currentHazard?.id)
  }

  @Test
  fun testHazardCheckerDetectsNoHazardWhenOutside() = runTest {
    val wktA = "POLYGON((9.9 9.9, 9.9 10.1, 10.1 10.1, 10.1 9.9, 9.9 9.9))"

    val testHazardA =
        Hazard(
            id = 1001,
            type = "TC",
            description = "Test Hazard A",
            country = "Testland",
            date = "2025-01-01",
            severity = 2.0,
            severityUnit = "unit",
            reportUrl = "http://example.test/A",
            alertLevel = 3,
            coordinates = listOf(Location(latitude = 10.0, longitude = 10.0)),
            bbox = listOf(9.9, 9.9, 10.1, 10.1),
            affectedZoneWkt = wktA)
    val serviceStateManager = ServiceStateManager
    serviceStateManager.clearActiveAlert()

    val hazardChecker =
        HazardChecker(listOf(testHazardA), StandardTestDispatcher(testScheduler), scope = this)

    hazardChecker.checkAndPublishAlert(20.0, 20.0) // User outside hazard

    advanceTimeBy(HAZARD_TIME_THRESHOLD_MS)
    runCurrent()

    val currentHazard = serviceStateManager.activeHazardFlow.value
    assertNull("The active hazard should be null when user is outside.", currentHazard)
  }

  @Test
  fun testHazardCheckerClearsHazardOnExit() = runTest {
    val wktA = "POLYGON((9.9 9.9, 9.9 10.1, 10.1 10.1, 10.1 9.9, 9.9 9.9))"

    val hazardA =
        Hazard(
            id = 1001,
            alertLevel = 3,
            type = "TC",
            description = "Test Hazard A",
            country = "Testland",
            date = "2025-01-01",
            severity = 2.0,
            severityUnit = "unit",
            reportUrl = "http://example.test/A",
            coordinates = listOf(Location(10.0, 10.0)),
            bbox = listOf(9.9, 9.9, 10.1, 10.1),
            affectedZoneWkt = wktA)

    val hazardChecker =
        HazardChecker(listOf(hazardA), StandardTestDispatcher(testScheduler), scope = this)

    // User enters hazard
    hazardChecker.checkAndPublishAlert(10.0, 10.0)
    advanceTimeBy(HAZARD_TIME_THRESHOLD_MS)
    runCurrent()
    assertEquals(hazardA.id, ServiceStateManager.activeHazardFlow.value?.id)

    // User exits hazard
    hazardChecker.checkAndPublishAlert(20.0, 20.0) // outside
    advanceTimeBy(HAZARD_TIME_THRESHOLD_MS)
    runCurrent()
    assertNull(ServiceStateManager.activeHazardFlow.value)
  }

  @Test
  fun testHazardCheckerDoesNotTriggerAlertIfUserLeavesEarly() = runTest {
    val wktA = "POLYGON((9.9 9.9, 9.9 10.1, 10.1 10.1, 10.1 9.9, 9.9 9.9))"

    val hazardA =
        Hazard(
            id = 1001,
            alertLevel = 3,
            type = "TC",
            description = "Test Hazard A",
            country = "Testland",
            date = "2025-01-01",
            severity = 2.0,
            severityUnit = "unit",
            reportUrl = "http://example.test/A",
            coordinates = listOf(Location(10.0, 10.0)),
            bbox = listOf(9.9, 9.9, 10.1, 10.1),
            affectedZoneWkt = wktA)

    val hazardChecker =
        HazardChecker(listOf(hazardA), StandardTestDispatcher(testScheduler), scope = this)

    // --- Step 1: User enters the hazard ---
    hazardChecker.checkAndPublishAlert(10.0, 10.0)

    // Advance time LESS than the threshold
    advanceTimeBy(HAZARD_TIME_THRESHOLD_MS / 2)
    runCurrent()

    // --- Step 2: User leaves hazard before threshold ---
    hazardChecker.checkAndPublishAlert(20.0, 20.0) // outside hazard
    advanceTimeBy(HAZARD_TIME_THRESHOLD_MS)
    runCurrent() // process the cleanUpInactiveHazards()

    val currentHazard = ServiceStateManager.activeHazardFlow.value
    assertNull(
        "Active hazard should not be set because the user left before threshold.", currentHazard)
  }
}
