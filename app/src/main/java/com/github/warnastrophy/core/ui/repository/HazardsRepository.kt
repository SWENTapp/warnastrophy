package com.github.warnastrophy.core.ui.repository

import android.util.Log
import com.github.warnastrophy.core.model.util.Location
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import org.json.JSONArray
import org.json.JSONObject

val TAGrep = "HasardsRepository"

data class Hazard(
    val id: Int?,
    val type: String?,
    val country: String?,
    val date: String?,
    val severity: Double?,
    val severityUnit: String?,
    val reportUrl: String?,
    val alertLevel: Int?,
    val coordinates: List<Location>?,
    val bbox: List<Double>?,
    val multiPolygonWKT: String?
)

class HazardsRepository {

  private fun buildUrlAreaHazards(geometry: String, days: String): String {
    val base = "https://www.gdacs.org/gdacsapi/api/Events/geteventlist/eventsbyarea"
    val geom = geometry.replace(" ", "%20")
    return "$base?geometryArea=$geom&days=$days"
  }

  private fun httpGet(urlStr: String): String =
      with(Dispatchers.IO) {
        val url = URL(urlStr)
        val conn =
            (url.openConnection() as HttpURLConnection).apply {
              requestMethod = "GET"
              setRequestProperty("Accept", "application/json")
              connectTimeout = 15000
              readTimeout = 15000
            }
        var message: String = ""
        try {
          val code = conn.responseCode
          val stream = if (code in 200..299) conn.inputStream else conn.errorStream
          message = BufferedReader(InputStreamReader(stream)).use { it.readText() }
        } catch (e: Exception) {
          Log.d("TAGrep", "$e")
        } finally {
          conn.disconnect()
        }
        return message
      }

  suspend fun getAreaHazards(geometryWKT: String, days: String = "1"): List<Hazard> {
    val url = buildUrlAreaHazards(geometryWKT, days)
    val response = httpGet(url)
    val hazards = mutableListOf<Hazard>()
    val jsonObject = JSONObject(response)
    val jsonHazards = jsonObject.getJSONArray("features")
    for (i in 0 until jsonHazards.length()) {
      val hazardJson = jsonHazards.getJSONObject(i)
      val hazard = parseHazard(hazardJson)
      if (hazard != null) hazards.add(hazard)
    }
    return hazards
  }

  private fun parseHazard(root: JSONObject): Hazard? {

    val properties = root.getJSONObject("properties")
    val isCurrent = properties.getBoolean("iscurrent")
    // if(!isCurrent) return null

    val bbox: List<Double>? =
        try {
          val bboxArray = root.getJSONArray("bbox")
          (0 until bboxArray.length()).map { bboxArray.getDouble(it) }
        } catch (e: Exception) {
          // Handle case where bbox might be missing or null (as in the example's root 'bbox')
          Log.d("Parsing Hazard", "bbox is missing")
          null
        }
    // --- 3. Extract MultiPolygon WKT/GeoJSON URL (for lazy/eager fetch) ---
    // We store the URL for the geometry, as the actual multi-polygon is fetched separately.
    val multiPolygonUrl: String? =
        try {
          properties.getJSONObject("url").getString("geometry")
        } catch (e: Exception) {
            Log.d("Parsing Hazard", "multipolygon is missing")
          null
        }

    // --- 4. Extract Coordinates (Centroid/Simple Points) ---
    val geometry = root.getJSONObject("geometry")
    val coordinates = mutableListOf<Location>()

    when (geometry.getString("type")) {
      "Point" -> {
        val arr = geometry.getJSONArray("coordinates")
        // GeoJSON order is [longitude, latitude]
        coordinates.add(Location(latitude = arr.getDouble(1), longitude = arr.getDouble(0)))
      }
      "Polygon" -> {
        // NOTE: This parsing logic assumes a simple Polygon with NO HOLES
        // and extracts only the main outer ring.
        try {
          // Polygon structure: [[[lng, lat], [lng, lat], ...]]
          val outerRing = geometry.getJSONArray("coordinates").getJSONArray(0)
          for (i in 0 until outerRing.length()) {
            val pointArr: JSONArray = outerRing.getJSONArray(i)
            coordinates.add(
                Location(latitude = pointArr.getDouble(1), longitude = pointArr.getDouble(0)))
          }
        } catch (e: Exception) {
          println("Error parsing Polygon coordinates: $e")
          // Fallback: Use the first coordinate as a central point if parsing fails
          if (coordinates.isEmpty()) {
            val arr = geometry.getJSONArray("coordinates").getJSONArray(0).getJSONArray(0)
            coordinates.add(Location(latitude = arr.getDouble(1), longitude = arr.getDouble(0)))
          }
        }
      }
      // "MultiPolygon" is typically not fully parsed here; we rely on the URL.
      else -> {
        // For other types (like MultiPolygon), we rely on the URL/BBox
        // or simply use the coordinates list as empty.
      }
    }

    val hazard =
        Hazard(
            id = properties.getInt("eventid"),
            type = properties.getString("eventtype"),
            country = properties.getString("country"),
            date = properties.getString("fromdate"),
            severity = properties.getJSONObject("severitydata").getDouble("severity"),
            severityUnit = properties.getJSONObject("severitydata").getString("severityunit"),
            reportUrl = properties.getJSONObject("url").getString("report"),
            alertLevel = properties.getInt("alertscore"),
            coordinates = coordinates,
            // New Spatial Fields
            bbox = bbox,
            multiPolygonWKT = multiPolygonUrl // Storing the URL as a String
            )

    return hazard
  }
}

// private fun buildUrlWorldHazards(hazardType: HazardType): String {
//    val base = "https://www.gdacs.org/gdacsapi/api/events/geteventlist/MAP"
//    return "$base?eventtypes=${hazardType.eventType}"
// }

//    suspend fun getCountryHazards(
//        eventTypes: List<String>,
//        country: String,
//        fromDate: String,
//        toDate: String,
//        alertLevels: List<String>
//    ) : List<Hazard> {
//        val url = buildUrlCountryHazards(eventTypes, country, fromDate, toDate, alertLevels)
//        val response = httpGet(url)
//        val jsonObject = JSONObject(response)
//        val jsonHazards = jsonObject.getJSONArray("features")
//        val hazards = mutableListOf<Hazard>()
//        for (i in 0 until jsonHazards.length()) {
//            val hazardJson = jsonHazards.getJSONObject(i)
//            val hazard = parseHazard(hazardJson)
//            if (hazard != null) {
//                hazards.add(hazard)
//            }
//        }
//        return hazards
//    }

//    suspend fun getAllWorldHazards(): List<Hazard> {
//        val allHazards = mutableListOf<Hazard>()
//        val hazardTypes = listOf(
//            HazardType.Earthquakes,
//            HazardType.Floods,
//            HazardType.Volcanoes,
//            HazardType.TropicalCyclones
//        )
//        for (hazardType in hazardTypes) {
//            val url = buildUrlWorldHazards(hazardType)
//            val response = httpGet(url)
//            val jsonObject = JSONObject(response)
//            val jsonHazards = jsonObject.getJSONArray("features")
//            for (i in 0 until jsonHazards.length()) {
//                val hazardJson = jsonHazards.getJSONObject(i)
//                val hazard = parseHazard(hazardJson)
//                if (hazard != null) {
//                    allHazards.add(hazard)
//                }
//            }
//        }
//        return allHazards
//    }
// }

// private fun buildUrlCountryHazards(
//    eventTypes: List<String>,
//    country: String,
//    fromDate: String,
//    toDate: String,
//    alertLevels: List<String>
// ): String {
//    val eventList = eventTypes.joinToString(";")
//    val alertLevelList = alertLevels.joinToString(";")
//    val encodedCountry = country.replace(" ", "%20")
//    val base = "https://www.gdacs.org/gdacsapi/api/events/geteventlist/SEARCH"
//    return "$base$queryparams"
// }
