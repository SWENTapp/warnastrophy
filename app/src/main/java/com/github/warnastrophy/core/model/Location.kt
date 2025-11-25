package com.github.warnastrophy.core.model

import com.github.warnastrophy.core.model.Location.Companion.kmToLonVariation
import com.github.warnastrophy.core.model.Location.Companion.normalizeLat
import com.github.warnastrophy.core.model.Location.Companion.normalizeLon
import com.google.android.gms.maps.model.LatLng
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Represents a geographic position in decimal degrees.
 *
 * @property latitude Latitude in degrees \[-90.0, 90.0\].
 * @property longitude Longitude in degrees \[-180.0, 180.0\].
 * @property name Optional name associated with the position.
 */
data class Location(val latitude: Double, val longitude: Double, val name: String? = null) {

  companion object {
    /** Mean Earth radius in kilometers (WGS84). */
    const val earthRadiusKm = 6371.009

    /** Nominal step in kilometers between successive generated points. */
    val kmBetweeenPoints = 500
    /**
     * Converts a [Location] to a Google Maps LatLng].
     *
     * @param location Location to convert.
     * @return Equivalent [com.google.android.gms.maps.model.LatLng] representation.
     */
    fun toLatLng(location: Location): LatLng {
      return LatLng(location.latitude, location.longitude)
    }

    /**
     * Converts a distance in kilometers to a latitude angular variation in degrees.
     *
     * Simple spherical approximation: angle = km / R, then converted to degrees.
     *
     * @param km Distance in kilometers.
     * @return Latitude variation in degrees.
     */
    private fun kmToLatVariation(km: Double): Double {
      return Math.toDegrees((km / earthRadiusKm))
    }

    /**
     * Converts a distance in kilometers to a longitude angular variation in degrees at a given
     * latitude.
     *
     * If the distance exceeds the local circumference (function of cos(latitude)), 360° is
     * returned.
     *
     * @param km Distance in kilometers.
     * @param latitude Current latitude in degrees.
     * @return Longitude variation in degrees.
     */
    private fun kmToLonVariation(km: Double, latitude: Double): Double {
      val latRad = Math.toRadians(latitude)
      val localRadius = 2 * Math.PI * earthRadiusKm * cos(latRad)
      if (km > localRadius) {
        return 360.0 // full longitude range
      }
      return Math.toDegrees(km / (earthRadiusKm * cos(latRad)))
    }

    /**
     * Builds a set of points distributed around a center by sweeping latitudes and generating, for
     * each, a pair of symmetric longitudes.
     * - The number of latitudinal steps depends on `kmLat`.
     * - For each latitude, the longitudinal variation is computed via [kmToLonVariation].
     * - Coordinates are normalized with [normalizeLat] and [normalizeLon].
     *
     * @param center Geographic center of the shape.
     * @param kmLon Target longitudinal span in kilometers.
     * @param kmLat Target latitudinal span in kilometers.
     * @return List of normalized \[latitude, longitude\] points.
     */
    fun getPolygon(center: Location, kmLon: Double, kmLat: Double): List<Location> {
      val halfSquareNbrLatPoints = ceil((kmLat / 2.0) / kmBetweeenPoints).toInt()

      val latVar = kmToLatVariation(kmLat)
      val latVarFrag = (latVar / 2.0) / halfSquareNbrLatPoints

      val listOfLocs = mutableListOf<Location>()

      for (i in 0..halfSquareNbrLatPoints * 2) {
        val currentLat = normalizeLat(center.latitude - latVar / 2.0 + i * latVarFrag)
        val currentLonVar = kmToLonVariation(kmLon, currentLat) / 2.0
        val lonPair =
            listOf(
                normalizeLon(center.longitude - currentLonVar),
                normalizeLon(center.longitude + currentLonVar))

        // Avoid inserting latitude/longitude duplicates
        if (!listOfLocs.any { it.latitude == currentLat && it.longitude == lonPair.min() }) {
          listOfLocs.add(Location(currentLat, lonPair.min()))
        }
        if (!listOfLocs.any { it.latitude == currentLat && it.longitude == lonPair.max() }) {
          listOfLocs.add(Location(currentLat, lonPair.max()))
        }
      }
      // close polygon
      return listOfLocs
    }

    /**
     * Normalizes a latitude into the range \[-90.0, 90.0\] by clamping.
     *
     * @param lat Latitude in degrees.
     * @return Latitude clamped to \[-90.0, 90.0\].
     */
    private fun normalizeLat(lat: Double): Double {
      return when {
        lat > 90.0 -> 90.0
        lat < -90.0 -> -90.0
        else -> lat
      }
    }

    /**
     * Normalizes a longitude into the range \[-180.0, 180.0\] by wrapping.
     *
     * @param lon Longitude in degrees.
     * @return Longitude wrapped to \[-180.0, 180.0\].
     */
    private fun normalizeLon(lon: Double): Double {
      return when {
        lon > 180 -> lon - 360
        lon < -180 -> lon + 360
        else -> lon
      }
    }

    fun locationsToWktPolygon(points: List<Location>): String {
      if (points.isEmpty()) return ""
      val pairs = points.filterIndexed { index, _ -> index % 2 == 0 }
      val impairs = points.filterIndexed { index, _ -> index % 2 == 1 }
      val coords =
          (pairs + impairs.reversed() + pairs[0]).joinToString(",") {
            Locale.US.run { String.format(this, "%.2f%%20%.2f", it.longitude, it.latitude) }
          }
      return "POLYGON(($coords))"
    }

    /**
     * Calculates the distance between this location and another location using the Haversine
     * formula, which accounts for the Earth's curvature.
     *
     * @param target The target Location.
     * @return The distance in kilometers (Double).
     */
    fun distanceBetween(from: Location, target: Location): Double {

      // 1. Convert Lat/Lon from Degrees to Radians
      val lat1Rad = Math.toRadians(from.latitude)
      val lon1Rad = Math.toRadians(from.longitude)
      val lat2Rad = Math.toRadians(target.latitude)
      val lon2Rad = Math.toRadians(target.longitude)

      // 2. Calculate the difference in coordinates
      val dLat = lat2Rad - lat1Rad
      val dLon = lon2Rad - lon1Rad

      // 3. Apply the Haversine formula (Part 1: The central angle 'a')
      val a = sin(dLat / 2.0).pow(2) + cos(lat1Rad) * cos(lat2Rad) * sin(dLon / 2.0).pow(2)

      // 4. Apply the Haversine formula (Part 2: The central angle 'c')
      val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))

      // 5. Calculate the final distance (Distance = Radius * c)
      return earthRadiusKm * c
    }
  }
}
