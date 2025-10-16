package com.github.warnastrophy.core.ui.map

import androidx.compose.runtime.getValue
import com.github.warnastrophy.core.model.util.Hazard
import com.github.warnastrophy.core.model.util.Location as ModelLocation
import com.github.warnastrophy.core.service.HazardChecker
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

@ExperimentalCoroutinesApi
class HazardsCheckerTest {

  @Test
  fun testHazardCheckerDetectsHazard() = runBlocking {
    val polygon =
        listOf(
            ModelLocation(latitude = 9.9, longitude = 9.9),
            ModelLocation(latitude = 9.9, longitude = 10.1),
            ModelLocation(latitude = 10.1, longitude = 10.1),
            ModelLocation(latitude = 10.1, longitude = 9.9))
    val center = ModelLocation(latitude = 10.0, longitude = 10.0)
    val testHazard =
        Hazard(
            id = 9999,
            htmlDescription = "Nearby test hazard",
            type = "TC",
            country = "Testland",
            date = "2025-01-01",
            severity = 1.0,
            severityUnit = "unit",
            reportUrl = "http://example.test",
            alertLevel = 2,
            coordinates = center,
            bbox = listOf(9.9, 9.9, 10.1, 10.1),
            polygon = polygon)
    val polygon2 =
        listOf(
            ModelLocation(latitude = 14.9, longitude = 14.9),
            ModelLocation(latitude = 14.9, longitude = 15.1),
            ModelLocation(latitude = 15.1, longitude = 15.1),
            ModelLocation(latitude = 15.1, longitude = 14.9))
    val testHazard2 =
        Hazard(
            id = 9999,
            htmlDescription = "Nearby test hazard",
            type = "TC",
            country = "Testland",
            date = "2025-01-01",
            severity = 1.0,
            severityUnit = "unit",
            reportUrl = "http://example.test",
            alertLevel = 2,
            coordinates = center,
            bbox = listOf(13.0, 9.9, 16.0, 10.1),
            polygon = polygon2)

    val hazardChecker = HazardChecker(listOf(testHazard, testHazard2))

    hazardChecker.checkAndPublishAlert(10.0, 10.0)
    delay(10000) // Wait longer than the threshold to ensure alert is triggered
    val currentHazard = hazardChecker.getHazard()
    assertNotNull(currentHazard)
    assertEquals(testHazard, currentHazard)
  }
}
