package com.github.warnastrophy.core.model.util

import com.github.warnastrophy.core.model.util.Location.Companion.earthRadiusKm
import kotlin.compareTo
import kotlin.math.abs
import kotlin.rem
import kotlin.text.get
import org.junit.Assert.*
import org.junit.Test

class LocationTest {
  fun varLatToKm(latVar: Double): Double {
    return Math.toRadians(latVar) * Location.earthRadiusKm
  }

  fun varLonToKm(lonVar: Double, latitude: Double): Double {
    val latRad = Math.toRadians(latitude)
    return Math.toRadians(lonVar) * (Location.earthRadiusKm * Math.cos(latRad))
  }

  fun formatBoundingBoxKeeneSite(polygon: List<Location>): String {
    val pairs = polygon.filterIndexed { index, _ -> index % 2 == 0 }
    val impairs = polygon.filterIndexed { index, _ -> index % 2 != 0 }.reversed()
    val orderedpoly = (pairs + impairs)
    return buildString {
      orderedpoly.forEachIndexed { i, loc ->
        append("${loc.longitude}, ${loc.latitude}")
        append("\n")
      }
    }
  }

  @Test
  fun testGetBoundingBox() {
    val location = Location(85.0, 0.0, "")
    val polygon = Location.getPolygon(location, 5000.0, 1000.0)
    debugPrint(formatBoundingBoxKeeneSite(polygon))
    assertNotNull(polygon)
  }

  @Test
  fun `equator 1000km x 1000km`() {
    val location = Location(0.0, 0.0, "Equator")
    val polygon = Location.getPolygon(location, 1000.0, 1000.0)
    assertNotNull(polygon)
    val pairs = polygon.filterIndexed { index, _ -> index % 2 == 0 }
    val impairs = polygon.filterIndexed { index, _ -> index % 2 != 0 }.reversed()

    for (i in pairs.indices) {
      val lonVar = abs(impairs[i].longitude - pairs[i].longitude)
      val lat = pairs[i].latitude
      val lonKm = varLonToKm(lonVar, lat)
      assertTrue(abs(lonKm - 1000.0) < 10.0)
    }

    val maxLat = pairs.maxOf { it.latitude }
    val minLat = pairs.minOf { it.latitude }
    val latVar = maxLat - minLat
    val latKm = varLatToKm(latVar)
    debugPrint("LatVar: $latVar, LatKm: $latKm")
    assertTrue(abs(latKm - 1000.0) < 1.0)
  }

  @Test
  fun `pole nord 1000km x 1000km`() {
    val location = Location(85.0, 0.0, "Pole Nord")
    val polygon = Location.getPolygon(location, 5000.0, 100.0)
    assertNotNull(polygon)
    val pairs = polygon.filterIndexed { index, _ -> index % 2 == 0 }
    val impairs = polygon.filterIndexed { index, _ -> index % 2 != 0 }.reversed()

    for (i in pairs.indices) {
      val lonVar = abs(impairs[i].longitude - pairs[i].longitude)
      assertEquals(360.0, lonVar, 1.0)
    }
  }

  @Test
  fun testToLatLng() {
    val location = Location(46.8182, 8.2275, "Switzerland")
    val latLng = Location.toLatLng(location)
    assertEquals(46.8182, latLng.latitude, 0.0001)
    assertEquals(8.2275, latLng.longitude, 0.0001)
  }

  @Test
  fun testLargeLatLonVariationFromSwitzerland() {
    val switzerland = Location(46.8182, 8.2275, "Switzerland")
    val latVariation = 60_000.0 // 60 000 km, très grande variation
    val lonVariation = 2_000.0 // 2 000 km
    val polygon = Location.getPolygon(switzerland, lonVariation, latVariation)
    assertNotNull(polygon)
    // Vérifie que la latitude max et min sont bien espacées
    val maxLat = polygon.maxOf { it.latitude }
    val minLat = polygon.minOf { it.latitude }
    val latVar = maxLat - minLat
    val latKm = Math.toRadians(latVar) * Location.earthRadiusKm
    assertTrue(latKm >= 59_000) // tolérance
  }
}

// https://www.keene.edu/campus/maps/tool/ keene website
// test rectangles on map of website
