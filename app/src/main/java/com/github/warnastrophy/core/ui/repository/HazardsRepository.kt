package com.github.warnastrophy.core.ui.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

val tag = "HasardsRepository :"
sealed class HazardType(val eventType: String) {
    object Earthquakes : HazardType("EQ")
    object Floods : HazardType("FL")
    object Volcanoes : HazardType("VO")
    object TropicalCyclones : HazardType("TC")

    companion object {
        private val map = listOf(Earthquakes, Floods, Volcanoes, TropicalCyclones)
            .associateBy { it.eventType }

        fun fromEventType(eventType: String): HazardType? = map[eventType]
    }
}

data class Coordinate(
    val latitude: Double,
    val longitude: Double
)

data class Hazard(
    val id: String?,
    val type: HazardType?,
    val country: String?,
    val date : String?,
    val severity: String?,
    val severityUnit: String?,
    val reportUrl: String?,
    val alertLevel : Int?,
    val coordinates: List<Coordinate>?
)

class HazardsRepository {

    private fun buildUrlWorldHazards(hazardType: HazardType): String {
        val base = "https://www.gdacs.org/gdacsapi/api/events/geteventlist/MAP"
        return "$base?eventtypes=${hazardType.eventType}"
    }

    private fun buildUrlCountryHazards(
        eventTypes: List<String>,
        country: String,
        fromDate: String,
        toDate: String,
        alertLevels: List<String>
    ): String {
        val eventList = eventTypes.joinToString(";")
        val alertLevelList = alertLevels.joinToString(";")
        val encodedCountry = country.replace(" ", "%20")
        val base = "https://www.gdacs.org/gdacsapi/api/events/geteventlist/SEARCH"
        val queryparams = "?fromDate=$fromDate&toDate=$toDate&alertlevel=$alertLevelList&eventlist=$eventList&country=$encodedCountry"
        return "$base$queryparams"
    }

    private suspend fun httpGet(urlStr: String): String {
        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            connectTimeout = 15000
            readTimeout = 15000
        }
        val message : String
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            message = BufferedReader(InputStreamReader(stream)).use { it.readText() }
        } finally {
            conn.disconnect()
        }
        return message
    }

    private fun parseHazard(root: JSONObject): Hazard? {

        val properties = root.getJSONObject("properties")

        val isCurrent = properties.getBoolean("iscurrent")
        if(!isCurrent) return null

        val geometry = root.getJSONObject("geometry")
        val coordinates = mutableListOf<Coordinate>()
        when (geometry.getString("type")) {
            "Point" -> {
                val arr = geometry.getJSONArray("coordinates")
                coordinates.add(
                    Coordinate(
                        latitude = arr.getDouble(1),
                        longitude = arr.getDouble(0)
                    )
                )
            }
            "MultiPolygon" -> {
                val polygons = geometry.getJSONArray("coordinates")
                for (i in 0 until polygons.length()) {
                    val polygon = polygons.getJSONArray(i)
                    coordinates.add(
                        Coordinate(
                            latitude = polygon.getDouble(1),
                            longitude = polygon.getDouble(0)
                        )
                    )
                }
            }
        }

        return Hazard(
            id = root.getString("id"),
            type = HazardType.fromEventType(properties.getString("eventtype")),
            country = properties.getString("country"),
            date = properties.getString("fromdate"),
            severity = properties.getJSONObject("severitydata").getString("severity"),
            severityUnit = properties.getJSONObject("severitydata").getString("severityunit"),
            reportUrl = properties.getJSONObject("url").getString("report"),
            alertLevel = properties.getInt("alertscore"),
            coordinates = coordinates
        )
    }

    suspend fun getCountryHazards(
        eventTypes: List<String>,
        country: String,
        fromDate: String,
        toDate: String,
        alertLevels: List<String>
    ) : List<Hazard> {
        val url = buildUrlCountryHazards(eventTypes, country, fromDate, toDate, alertLevels)
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

    suspend fun getAllWorldHazards(): List<Hazard> {
        val allHazards = mutableListOf<Hazard>()
        val hazardTypes = listOf(
            HazardType.Earthquakes,
            HazardType.Floods,
            HazardType.Volcanoes,
            HazardType.TropicalCyclones
        )
        for (hazardType in hazardTypes) {
            val url = buildUrlWorldHazards(hazardType)
            val response = httpGet(url)
            val jsonObject = JSONObject(response)
            val jsonHazards = jsonObject.getJSONArray("features")
            for (i in 0 until jsonHazards.length()) {
                val hazardJson = jsonHazards.getJSONObject(i)
                val hazard = parseHazard(hazardJson)
                if (hazard != null) {
                    allHazards.add(hazard)
                }
            }
        }
        return allHazards
    }
}
