package com.github.warnastrophy.core.model.util

import com.google.android.gms.maps.model.LatLng
import kotlin.div
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.text.toInt
import kotlin.times
import org.checkerframework.checker.units.qual.km

data class Location(val latitude: Double, val longitude: Double, val name: String? = null) {

  companion object {
    const val earthRadiusKm = 6371.009

    val kmBetweeenPoints = 500

    fun toLatLng(location: Location): LatLng {
      return LatLng(location.latitude, location.longitude)
    }

    fun kmToLatVariation(km: Double): Double {
      return Math.toDegrees((km / earthRadiusKm))
    }

    fun kmToLonVariation(km: Double, latitude: Double): Double {
      val latRad = Math.toRadians(latitude)
      val localRadius = 2 * Math.PI * earthRadiusKm * cos(latRad)
      if (km > localRadius) {
        return 360.0 // entire longitude range
      }
      return Math.toDegrees(km / earthRadiusKm)
    }

    fun getPolygon(center: Location, kmLon: Double, kmLat: Double): List<Location> {
      val nbrLatPoints = kotlin.math.ceil((kmLat / 2.0) / kmBetweeenPoints).toInt()

      val latVar = kmToLatVariation(kmLat)
      val latVarFrag = (latVar / 2.0) / nbrLatPoints

      val listOfLocs = mutableListOf<Location>()

      for (i in 0..nbrLatPoints * 2) {
        val currentLat = normalizeLat(center.latitude - latVar / 2.0 + i * latVarFrag)
        val currentLonVar = kmToLonVariation(kmLon, currentLat) / 2.0
        val lonPair =
            listOf(
                normalizeLon(center.longitude - currentLonVar),
                normalizeLon(center.longitude + currentLonVar))

        if (!listOfLocs.any { it.latitude == currentLat && it.longitude == lonPair.min() }) {
          listOfLocs.add(Location(currentLat, lonPair.min()))
        }
        if (!listOfLocs.any { it.latitude == currentLat && it.longitude == lonPair.max() }) {
          listOfLocs.add(Location(currentLat, lonPair.max()))
        }
      }
      return listOfLocs
    } // check radius and pole edge case

    fun normalizeLat(lat: Double): Double {
      return when {
        lat > 90.0 -> 90.0
        lat < -90.0 -> -90.0
        else -> lat
      }
    }

    fun normalizeLon(lon: Double): Double {
      return when {
        lon > 180 -> lon - 360
        lon < -180 -> lon + 360
        else -> lon
      }
    }
  }
}
