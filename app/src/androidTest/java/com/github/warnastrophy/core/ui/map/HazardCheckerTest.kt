package com.github.warnastrophy.core.ui.map

import com.github.warnastrophy.core.data.service.ServiceStateManager
import com.github.warnastrophy.core.domain.model.Hazard
import com.github.warnastrophy.core.domain.usecase.HazardChecker
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LinearRing

@OptIn(ExperimentalCoroutinesApi::class)
class HazardCheckerTest {
  private val HAZARD_TIME_THRESHOLD_MS = 5000L
  val factory = GeometryFactory()

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun testHazardCheckerDetectsHighestPriorityHazard() = runTest {
    val coordsA =
        arrayOf(
            Coordinate(9.9, 9.9),
            Coordinate(9.9, 10.1),
            Coordinate(10.1, 10.1),
            Coordinate(10.1, 9.9),
            Coordinate(9.9, 9.9) // Must close the ring!
            )
    // A LinearRing is the boundary of a Polygon
    val shellA: LinearRing = factory.createLinearRing(coordsA)
    val geometryA: Geometry = factory.createPolygon(shellA)

    val coordsB =
        arrayOf(
            Coordinate(9.8, 9.8),
            Coordinate(9.8, 10.2),
            Coordinate(10.2, 10.2),
            Coordinate(10.2, 9.8),
            Coordinate(9.8, 9.8) // Must close the ring!
            )
    val shellB: LinearRing = factory.createLinearRing(coordsB)
    val geometryB: Geometry = factory.createPolygon(shellB)

    val testHazardA =
        Hazard(
            id = 1001, // Unique ID
            type = "TC",
            description = "Test Hazard A",
            country = "Testland",
            date = "2025-01-01",
            severity = 2.0,
            severityUnit = "unit",
            articleUrl = "http://example.test/A",
            alertLevel = 3.0, // HIGHER priority
            centroid = null,
            bbox = listOf(9.9, 9.9, 10.1, 10.1),
            affectedZone = geometryA)

    val testHazardB =
        Hazard(
            id = 1002, // Unique ID
            type = "FL",
            description = "Test Hazard B",
            country = "Testland",
            date = "2025-01-01",
            severity = 1.0,
            severityUnit = "unit",
            articleUrl = "http://example.test/B",
            alertLevel = 2.0, // LOWER priority
            centroid = null,
            bbox = listOf(9.8, 9.8, 10.2, 10.2),
            affectedZone = geometryB // <-- Use WKT string
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
    val coordsA =
        arrayOf(
            Coordinate(9.9, 9.9),
            Coordinate(9.9, 10.1),
            Coordinate(10.1, 10.1),
            Coordinate(10.1, 9.9),
            Coordinate(9.9, 9.9) // Must close the ring!
            )
    val geometryA: Geometry = factory.createPolygon(factory.createLinearRing(coordsA))

    val testHazardA =
        Hazard(
            id = 1001,
            type = "TC",
            description = "Test Hazard A",
            country = "Testland",
            date = "2025-01-01",
            severity = 2.0,
            severityUnit = "unit",
            articleUrl = "http://example.test/A",
            alertLevel = 3.0,
            centroid = null,
            bbox = listOf(9.9, 9.9, 10.1, 10.1),
            affectedZone = geometryA)
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
    val coordsA =
        arrayOf(
            Coordinate(9.9, 9.9),
            Coordinate(9.9, 10.1),
            Coordinate(10.1, 10.1),
            Coordinate(10.1, 9.9),
            Coordinate(9.9, 9.9) // Must close the ring!
            )
    val geometryA: Geometry = factory.createPolygon(factory.createLinearRing(coordsA))

    val hazardA =
        Hazard(
            id = 1001,
            alertLevel = 3.0,
            type = "TC",
            description = "Test Hazard A",
            country = "Testland",
            date = "2025-01-01",
            severity = 2.0,
            severityUnit = "unit",
            articleUrl = "http://example.test/A",
            centroid = null,
            bbox = listOf(9.9, 9.9, 10.1, 10.1),
            affectedZone = geometryA)

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
    val coordsA =
        arrayOf(
            Coordinate(9.9, 9.9),
            Coordinate(9.9, 10.1),
            Coordinate(10.1, 10.1),
            Coordinate(10.1, 9.9),
            Coordinate(9.9, 9.9) // Must close the ring!
            )
    val geometryA: Geometry = factory.createPolygon(factory.createLinearRing(coordsA))

    val hazardA =
        Hazard(
            id = 1001,
            alertLevel = 3.0,
            type = "TC",
            description = "Test Hazard A",
            country = "Testland",
            date = "2025-01-01",
            severity = 2.0,
            severityUnit = "unit",
            articleUrl = "http://example.test/A",
            centroid = null,
            bbox = listOf(9.9, 9.9, 10.1, 10.1),
            affectedZone = geometryA)

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
