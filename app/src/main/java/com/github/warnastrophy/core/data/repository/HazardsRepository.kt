package com.github.warnastrophy.core.data.repository

import android.util.Log
import com.github.warnastrophy.core.model.Hazard
import com.github.warnastrophy.core.model.Location
import com.github.warnastrophy.core.util.WktParser
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import org.json.JSONArray
import org.json.JSONObject
import org.locationtech.jts.geom.Geometry

val TAGrep = "HasardsRepository"

interface HazardsDataSource {
  suspend fun getAreaHazards(geometry: String, days: String): List<Hazard>
}

class HazardsRepository() : HazardsDataSource {

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
        try {
          val code = conn.responseCode
          val stream = if (code in 200..299) conn.inputStream else conn.errorStream
          BufferedReader(InputStreamReader(stream)).use { it.readText() }
        } finally {
          conn.disconnect()
        }
      }

  override suspend fun getAreaHazards(geometry: String, days: String): List<Hazard> {
    val url = buildUrlAreaHazards(geometry, days)
    val response = httpGet(url)
    if (response.isBlank()) {
      Log.d("GetAreaHazards", "Received empty or null response from server.")
      return emptyList()
    }
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
    try {
      val properties = root.getJSONObject("properties")
      val isCurrent = properties.getBoolean("iscurrent")
      // if(!isCurrent) return null

      val geometryOfCentroid = root.getJSONObject("geometry")
      val coordinates = mutableListOf<Location>()
      when (geometryOfCentroid.getString("type")) {
        "Point" -> {
          val arr = geometryOfCentroid.getJSONArray("coordinates")
          coordinates.add(Location(latitude = arr.getDouble(1), longitude = arr.getDouble(0)))
        }
        "Polygon" -> { // we take first point as centroid
          val polygons =
              geometryOfCentroid.getJSONArray("coordinates").getJSONArray(0).getJSONArray(0)
          val polygon = polygons.getJSONArray(0)
          coordinates.add(
              Location(latitude = polygon.getDouble(1), longitude = polygon.getDouble(0)))
        }
      }

      val urlsOfHazard = properties.getJSONObject("url")
      val detailedGeometryUrl = urlsOfHazard.getString("geometry")
      val geometryRes = httpGet(detailedGeometryUrl)
      val hazardDetailUrl = urlsOfHazard.getString("details")
      val articleUrl =
          getHazardArticleUrl(hazardDetailUrl)
              ?: run {
                Log.d(TAGrep, "Skipping hazard due to missing article URL.")
                return null
              }
      val bbox =
          getBbox(geometryRes)
              ?: run {
                Log.d(TAGrep, "Skipping hazard due to missing bbox.")
                return null
              }
      val affectedZone =
          getAffectedZone(geometryRes)
              ?: run {
                Log.d(TAGrep, "Skipping hazard due to missing affected zone.")
                return null
              }
      val hazard =
          Hazard(
              id = properties.getInt("eventid"),
              type = properties.getString("eventtype"),
              description = properties.optString("description"),
              country = properties.getString("country"),
              date = properties.getString("fromdate"),
              severity = properties.getJSONObject("severitydata").getDouble("severity"),
              severityUnit = properties.getJSONObject("severitydata").getString("severityunit"),
              articleUrl = articleUrl,
              alertLevel = properties.getDouble("alertscore"),
              centroid = coordinates,
              affectedZone = affectedZone,
              bbox = bbox)
      return hazard
    } catch (e: Exception) {
      Log.d(TAGrep, "Failed to construct Hazard object: ${e.message}")
      return null
    }
  }

  private fun getBbox(geometryRes: String): List<Double>? {
    return try {
      getRawGeoJsonGeometry(geometryRes).getJSONArray("bbox").let { bboxArray ->
        List(bboxArray.length()) { i -> bboxArray.getDouble(i) }
      }
    } catch (e: Exception) {
      Log.e(TAGrep, "Failed to get bbox: ${e.message}")
      null
    }
  }

  private fun getRawGeoJsonGeometry(geometryRes: String): JSONObject {
    return try {
      val geometry = JSONObject(geometryRes)
      val rawGeoJsonGeometry = geometry.getJSONArray("features").getJSONObject(1)
      rawGeoJsonGeometry
    } catch (e: Exception) {
      Log.e(TAGrep, "Failed to get raw geometry JSON: ${e.message}")
      JSONObject() // return empty json object in case of exception
    }
  }

  private fun getAffectedZone(geometryRes: String): Geometry? {
    return try {
      val rawGeoJsonAffectedZone = getRawGeoJsonGeometry(geometryRes).getString("geometry")
      WktParser.convertRawGeoJsonGeometryToJTS(rawGeoJsonAffectedZone)
    } catch (e: Exception) {
      Log.e(TAGrep, "Failed to get affected zone: ${e.message}")
      null
    }
  }

  private fun getHazardArticleUrl(hazardDetailUrl: String): String? {
    val detailRes = httpGet(hazardDetailUrl)
    return try {
      val json = JSONObject(detailRes)
      val mediaUrl = json.getJSONObject("properties").getJSONObject("url").getString("media")
      val mediaRes = httpGet(mediaUrl)
      val mediaJson = JSONArray(mediaRes)
      mediaJson.getJSONObject(0).getString("link")
    } catch (e: Exception) {
      Log.e(TAGrep, "Failed to parse media url: ${e.message}")
      return null
    }
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
