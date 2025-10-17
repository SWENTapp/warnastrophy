package com.github.warnastrophy.core.model.util

import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import org.json.JSONObject

val TAGrep = "HazardsRepository"

data class Hazard(
    val id: Int?,
    val htmlDescription: String?,
    val type: String?,
    val country: String?,
    val date: String?,
    val severity: Double?,
    val severityUnit: String?,
    val reportUrl: String?,
    val alertLevel: Int?,
    val coordinates: Location?,
    val bbox: List<Double>?,
    val polygon: List<Location>?
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

  fun getAreaHazards(geometryWKT: String, days: String = "1"): List<Hazard> {
    val url = buildUrlAreaHazards(geometryWKT, days)
    Log.d(TAGrep, "Fetching hazards from URL: $url")
    val response = httpGet(url)
    Log.d(TAGrep, "Response: $response")
    val hazards = mutableListOf<Hazard>()
    val jsonObject = JSONObject(response)
    try {
      val jsonHazards = jsonObject.getJSONArray("features")
      Log.d(TAGrep, "Number of hazards found: ${jsonHazards.length()}")
      for (i in 0 until jsonHazards.length()) {
        val hazardJson = jsonHazards.getJSONObject(i)
        val geometryUrl =
            hazardJson.getJSONObject("properties").getJSONObject("url").getString("geometry")
        val geometry = getHazardGeometry(geometryUrl)
        if (geometry == null) {
          Log.e(TAGrep, "Skipping hazard due to missing geometry from $geometryUrl")
          continue
        }
        val hazard = parseHazard(hazardJson, geometry)
        Log.d(TAGrep, "Parsed hazard: $hazard")
        if (hazard != null) hazards.add(hazard)
      }
    } catch (e: Exception) {
      Log.e(TAGrep, "No hazards found: $e")
      return emptyList()
    }
    Log.d(TAGrep, "Total hazards parsed: ${hazards.size}")
    return hazards
  }

  private fun parseHazard(root: JSONObject, geometryJson: JSONObject): Hazard? {

    val properties = root.getJSONObject("properties")
    val geometry = root.getJSONObject("geometry")
    val isCurrent = properties.getBoolean("iscurrent")
    // if(!isCurrent) return null

    Log.d(TAGrep, "features length : ${geometryJson.getJSONArray("features").length()}")
    val feature = geometryJson.getJSONArray("features").getJSONObject(1)
    Log.d(TAGrep, "feature: $feature")

    // --- 3. Extract Bounding Box ---
    val bbox =
        feature.getJSONArray("bbox").let { bboxArray ->
          List(bboxArray.length()) { i -> bboxArray.getDouble(i) }
        }
    val featureGeometry = feature.getJSONObject("geometry")
    val polygon =
        when (featureGeometry.getString("type")) {
          "Polygon" -> featureGeometry.getJSONArray("coordinates").getJSONArray(0)
          "MultiPolygon" ->
              featureGeometry
                  .getJSONArray("coordinates")
                  .getJSONArray(0)
                  .getJSONArray(0) // Always fetch outer ring
          else -> throw IllegalArgumentException("Unsupported geometry type")
        }

    val hazard =
        Hazard(
            id = properties.getInt("eventid"),
            htmlDescription = properties.getString("htmldescription"),
            type = properties.getString("eventtype"),
            country = properties.getString("country"),
            date = properties.getString("fromdate"),
            severity = properties.getJSONObject("severitydata").getDouble("severity"),
            severityUnit = properties.getJSONObject("severitydata").getString("severityunit"),
            reportUrl = properties.getJSONObject("url").getString("report"),
            alertLevel = properties.getInt("alertscore"),
            coordinates =
                Location(
                    geometry.getJSONArray("coordinates").getDouble(1),
                    geometry.getJSONArray("coordinates").getDouble(0)),
            bbox = bbox,
            polygon =
                List(polygon.length()) { i ->
                  val point = polygon.getJSONArray(i)
                  Location(point.getDouble(1), point.getDouble(0))
                })

    return hazard
  }

  private fun getHazardGeometry(geometryUrl: String): JSONObject? {
    val geometryResponse = httpGet(geometryUrl)
    return try {
      JSONObject(geometryResponse)
    } catch (e: Exception) {
      Log.e(TAGrep, "Failed to parse geometry JSON from $geometryUrl: $e")
      null
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
