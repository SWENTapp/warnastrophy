package com.github.warnastrophy.core.ui.repository

import android.util.Log
import com.github.warnastrophy.core.model.util.Location
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject

val TAGrep = "HasardsRepository"

data class Hazard(
    val id: String?,
    val type: String?,
    val country: String?,
    val date: String?,
    val severity: String?,
    val severityUnit: String?,
    val reportUrl: String?,
    val alertLevel: Int?,
    val coordinates: List<Location>?
)

class HazardsRepository {
  private fun buildUrlAreaHazards(geometry: String): String =
      with(Dispatchers.IO) {
        val base = "https://www.gdacs.org/gdacsapi/api/Events/geteventlist/eventsbyarea"
        val geom = geometry.replace(" ", "%20")
        return "$base?geometryArea=$geom&days=360"
      }

  private suspend fun httpGet(urlStr: String): String {
    println(TAGrep + "HTTP GET : $urlStr")
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
    Log.d("TAGrep", "resp message: $message")
    return message
  }

  suspend fun getAreaHazards(geometry: String): List<Hazard> {
    val url = buildUrlAreaHazards(geometry)
    val response = httpGet(url)
    val jsonObject = JSONObject(response)
    val jsonHazards = jsonObject.getJSONArray("features")
    val hazards = mutableListOf<Hazard>()
    for (i in 0 until jsonHazards.length()) {
      val hazardJson = jsonHazards.getJSONObject(i)
      val hazard = parseHazard(hazardJson)
      if (hazard != null) {
        hazards.add(hazard)
      }
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
      "MultiPolygon" -> {
        val polygons = geometry.getJSONArray("coordinates")
        for (i in 0 until polygons.length()) {
          val polygon = polygons.getJSONArray(i)
          coordinates.add(
              Location(latitude = polygon.getDouble(1), longitude = polygon.getDouble(0)))
        }
      }
    }

    val hazard =
        Hazard(
            id = properties.getString("eventid"),
            type = properties.getString("eventtype"),
            country = properties.getString("country"),
            date = properties.getString("fromdate"),
            severity = properties.getJSONObject("severitydata").getString("severity"),
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
//    val queryparams =
// "?fromDate=$fromDate&toDate=$toDate&alertlevel=$alertLevelList&eventlist=$eventList&country=$encodedCountry"
//    return "$base$queryparams"
// }
