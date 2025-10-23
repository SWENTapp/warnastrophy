package com.github.warnastrophy.core.data.repository

import android.util.Log
import com.github.warnastrophy.core.model.Hazard
import com.github.warnastrophy.core.model.Location
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject

val TAGrep = "HasardsRepository"

interface HazardsDataSource {
  suspend fun getAreaHazards(geometry: String, days: String): List<Hazard>
}

class HazardsRepository : HazardsDataSource {

  private fun buildUrlAreaHazards(geometry: String, days: String): String {
    val base = "https://www.gdacs.org/gdacsapi/api/Events/geteventlist/eventsbyarea"
    val geom = geometry.replace(" ", "%20")
    return "$base?geometryArea=$geom&days=$days"
  }

  private suspend fun httpGet(urlStr: String): String =
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
        Log.d(TAGrep, "HTTP GET $urlStr \nResponse: $message")
        return message
      }

  override suspend fun getAreaHazards(geometry: String, days: String): List<Hazard> {
    val url = buildUrlAreaHazards(geometry, days)
    val response = httpGet(url)
    val hazards = mutableListOf<Hazard>()
    val jsonObject = JSONObject(response)
    try {
      val jsonHazards = jsonObject.getJSONArray("features")
      for (i in 0 until jsonHazards.length()) {
        val hazardJson = jsonHazards.getJSONObject(i)
        val hazard = parseHazard(hazardJson)
        if (hazard != null) hazards.add(hazard)
      }
    } catch (e: Exception) {
      Log.d(TAGrep, "No hazards found: $e")
      return emptyList()
    }
    return hazards
  }

  private fun parseHazard(root: JSONObject): Hazard? {

    val properties = root.getJSONObject("properties")
    val isCurrent = properties.getBoolean("iscurrent")
    // if(!isCurrent) return null

    val geometry = root.getJSONObject("geometry")
    val coordinates = mutableListOf<Location>()
    when (geometry.getString("type")) {
      "Point" -> {
        val arr = geometry.getJSONArray("coordinates")
        coordinates.add(Location(latitude = arr.getDouble(1), longitude = arr.getDouble(0)))
      }
      "Polygon" -> {
        val polygons = geometry.getJSONArray("coordinates").getJSONArray(0).getJSONArray(0)
        for (i in 0 until polygons.length()) {
          val polygon = polygons.getJSONArray(i)
          coordinates.add(
              Location(latitude = polygon.getDouble(1), longitude = polygon.getDouble(0)))
        }
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
            coordinates = coordinates)
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
