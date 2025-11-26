package com.github.warnastrophy.core.model

import com.github.warnastrophy.core.util.debugPrint
import kotlin.math.PI
import kotlin.math.abs
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationTest {
  private val EPSILON = 0.01

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
    val orderedPoly = (pairs + impairs)
    return buildString {
      orderedPoly.forEachIndexed { i, loc ->
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
    Assert.assertNotNull(polygon)
  }

  @Test
  fun `equator 1000km x 1000km`() {
    val location = Location(0.0, 0.0, "Equator")
    val polygon = Location.getPolygon(location, 1000.0, 1000.0)
    Assert.assertNotNull(polygon)
    val pairs = polygon.filterIndexed { index, _ -> index % 2 == 0 }
    val impairs = polygon.filterIndexed { index, _ -> index % 2 != 0 }

    for (i in pairs.indices) {
      val lonVar = abs(impairs[i].longitude - pairs[i].longitude)
      val lat = pairs[i].latitude
      val lonKm = varLonToKm(lonVar, lat)
      Assert.assertTrue(abs(lonKm - 1000.0) < 1.0)
    }

    val maxLat = pairs.maxOf { it.latitude }
    val minLat = pairs.minOf { it.latitude }
    val latVar = maxLat - minLat
    val latKm = varLatToKm(latVar)
    Assert.assertTrue(abs(latKm - 1000.0) < 1.0)
  }

  @Test
  fun `pole nord 1000km x 1000km`() {
    val location = Location(85.0, 0.0, "Pole Nord")
    val polygon = Location.getPolygon(location, 5000.0, 100.0)
    Assert.assertNotNull(polygon)
    val pairs = polygon.filterIndexed { index, _ -> index % 2 == 0 }
    val impairs = polygon.filterIndexed { index, _ -> index % 2 != 0 }

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
  fun `pole nord 5000km x 5000km unique points`() {
    val location = Location(85.0, 0.0, "Pole Nord")
    val polygon = Location.getPolygon(location, 5000.0, 5000.0)
    Assert.assertNotNull(polygon)
    // Vérifie l'unicité des points. Au moins un point similaire car polygone fermé
    val uniquePoints = polygon.toSet()
    assertEquals("Il y a des doublons dans le polygone", polygon.size, uniquePoints.size)
  }

  @Test
  fun `wktPolygonFormat`() {
    val locations =
        listOf(
            Location(10.3850934834, 20.3850934834, ""),
            Location(10.3850934834, 30.3850934834, ""),
            Location(20.3850934834, 30.3850934834, ""),
            Location(10.3850934834, 20.3850934834, ""),
        )
    val polygon = Location.locationsToWktPolygon(locations)
    debugPrint(polygon)
  }

  @Test
  fun `distanceBetween correctly calculates distance between two points on the Equator`() {
    // Known Distance: 90 degrees longitude apart on the Equator
    val equator1 = Location(latitude = 0.0, longitude = 0.0)
    val equator2 = Location(latitude = 0.0, longitude = 90.0)

    // Expected distance = 1/4 of the circumference (2 * pi * R / 4)
    val expectedDistance = (2 * PI * Location.earthRadiusKm) / 4.0
    val distance = Location.distanceBetween(equator1, equator2)

    assertEquals(expectedDistance, distance, EPSILON)
  }

  @Test
  fun `distanceBetween correctly calculates a known real-world distance (New York to London)`() {
    // Coordinates for New York City (approx)
    val nyc = Location(latitude = 40.7128, longitude = -74.0060)
    // Coordinates for London (approx)
    val london = Location(latitude = 51.5074, longitude = 0.1278)

    // Reference distance (Source: Google Maps/Haversine calculator using R=6371.009km): ~5585.0226
    // km
    val expectedDistance = 5587.022646618559

    val distance = Location.distanceBetween(nyc, london)

    // We use a slightly larger tolerance here (0.5 km) as the exact radius used can vary slightly.
    assertEquals(expectedDistance, distance, EPSILON)
  }
}

// https://www.keene.edu/campus/maps/tool/ keene website
// test rectangles on map of website
