package com.github.warnastrophy

import kotlin.math.*
import com.github.warnastrophy.core.ui.repository.Location

object GeoUtils {
    private const val earthRadiusKm = 6371.009

    fun kmToLatVariation(km: Double): Double {
        return (km / earthRadiusKm) * (180.0 / Math.PI)
    }

    fun kmToLonVariation(km: Double, latitude: Double): Double {
        val latRad = Math.toRadians(latitude)
        return (km / (earthRadiusKm * cos(latRad))) * (180.0 / Math.PI)
    }

    fun getBoundingBox(center: Location, km: Double): List<Double> {

        val halfKm = km / 2.0
        val latVar = kmToLatVariation(halfKm)
        val lonVar = kmToLonVariation(halfKm, center.latitude)

        val lat1 = normalizeLat(center.latitude - latVar)
        val lat2 = normalizeLat(center.latitude + latVar)
        val lon1 = normalizeLon(center.longitude - lonVar)
        val lon2 = normalizeLon(center.longitude + lonVar)

        val minLat = min(lat1, lat2)
        val maxLat = max(lat1, lat2)
        val minLon = min(lon1, lon2)
        val maxLon = max(lon1, lon2)

        val minLatRadius = earthRadiusKm * cos(minLat)
        //if(minLatRadius){}
        return listOf(minLon, minLat, maxLon, maxLat)
    }

    fun normalizeLat(lat: Double): Double {
        return when {
            lat > 90.0 -> lat - 180.0
            lat < -90.0 -> lat + 180.0
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
