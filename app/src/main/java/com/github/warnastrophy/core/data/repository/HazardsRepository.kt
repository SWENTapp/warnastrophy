package com.github.warnastrophy.core.data.repository

import com.github.warnastrophy.core.model.Hazard
import com.github.warnastrophy.core.util.GeometryParser
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import org.json.JSONArray
import org.json.JSONObject
import org.locationtech.jts.geom.Geometry

/**
 * Defines the contract for any class responsible for fetching hazard data based on a geographic
 * area.
 *
 * This interface abstracts the underlying data source
 */
interface HazardsDataSource {
    suspend fun getAreaHazards(geometry: String, days: String): List<Hazard>
}

/**
 * Repository class responsible for fetching hazard data from the GDACS API (Global Disaster Alert
 * and Coordination System).
 *
 * This repository handles the multi-step process of network communication, GeoJSON parsing, and
 * conversion to JTS Geometry objects.
 */
class HazardsRepository() : HazardsDataSource {

    /**
     * Constructs the full URL for fetching hazard events within a specific geographic area.
     *
     * @param geometry A WKT (Well-Known Text) string defining the search area.
     * @param days The number of days back to search for events.
     * @return The complete, URL-encoded string for the GDACS API endpoint.
     */
    private fun buildUrlAreaHazards(geometry: String, days: String): String {
        val base = "https://www.gdacs.org/gdacsapi/api/Events/geteventlist/eventsbyarea"
        val geom = geometry.replace(" ", "%20")
        return "$base?geometryArea=$geom&days=$days"
    }

    /**
     * Executes a synchronous HTTP GET request on an IO dispatcher.
     *
     * This function performs a blocking network call and should always be called from within a
     * coroutine running on Dispatchers.IO.
     *
     * @param urlStr The URL to fetch.
     * @return The response body as a String, or an empty string if the connection fails.
     */
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

    /**
     * Fetches a list of current hazards that are inside the defined geographic area.
     *
     * @param geometry A WKT string defining the area to search within.
     * @param days The number of days back to search for events.
     * @return A List of successfully parsed [Hazard] objects.
     */
    override suspend fun getAreaHazards(geometry: String, days: String): List<Hazard> {
        val url = buildUrlAreaHazards(geometry, days)
        val response = httpGet(url)
        if (response.isBlank()) {
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

    /**
     * Parses a single GeoJSON Feature object into a full Hazard data class.
     *
     * This function performs multiple subsequent network calls to retrieve detailed geometry,
     * bounding box (bbox), and the article URL for the hazard. It filters out non-current hazards and
     * skips those with missing fields.
     *
     * @param root The JSONObject representing a single GeoJSON Feature (a hazard event).
     * @return A fully constructed [Hazard] object, or null if parsing fails or fields are missing.
     */
    private fun parseHazard(root: JSONObject): Hazard? {
        try {
            val properties = root.getJSONObject("properties")
            val isCurrent = properties.getBoolean("iscurrent")
            if (!isCurrent) {
                return null
            }

            val centroid =
                GeometryParser.convertRawGeoJsonGeometryToJTS(root.getJSONObject("geometry").toString())
                    ?: run {
                        return null
                    }

            val urlsOfHazard = properties.getJSONObject("url")
            val detailedGeometryUrl = urlsOfHazard.getString("geometry")
            val geometryRes = httpGet(detailedGeometryUrl)
            val hazardDetailUrl = urlsOfHazard.getString("details")

            val articleUrl =
                getHazardArticleUrl(hazardDetailUrl)
                    ?: run {
                        return null
                    }
            val bbox =
                getBbox(geometryRes)
                    ?: run {
                        return null
                    }
            val affectedZone =
                getAffectedZone(geometryRes)
                    ?: run {
                        return null
                    }
            val hazard =
                Hazard(
                    id = properties.getInt("eventid"),
                    type = properties.getString("eventtype"),
                    description = properties.optString("description"),
                    severityText = properties.getJSONObject("severitydata").getString("severitytext"),
                    country = properties.getString("country"),
                    date = properties.getString("fromdate"),
                    severity = properties.getJSONObject("severitydata").getDouble("severity"),
                    severityUnit = properties.getJSONObject("severitydata").getString("severityunit"),
                    articleUrl = articleUrl,
                    alertLevel = properties.getDouble("alertscore"),
                    centroid = centroid,
                    affectedZone = affectedZone,
                    bbox = bbox)
            return hazard
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * Extracts the bounding box (bbox) array from the detailed geometry response.
     *
     * @param geometryRes The raw JSON response string from the detailed geometry URL.
     * @return A List of four Doubles representing [minLon, minLat, maxLon, maxLat], or null on
     *   failure.
     */
    private fun getBbox(geometryRes: String): List<Double>? {
        return try {
            getRawGeoJsonGeometry(geometryRes).getJSONArray("bbox").let { bboxArray ->
                List(bboxArray.length()) { i -> bboxArray.getDouble(i) }
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Navigates the detailed geometry response to find the core GeoJSON geometry Feature.
     *
     * The GDACS geometry endpoint returns a FeatureCollection, and this function (based on what we
     * have observed in API) assumes the desired geometry is located at index [1] of the "features"
     * array.
     *
     * @param geometryRes The raw JSON response string from the detailed geometry URL.
     * @return The JSONObject representing the raw GeoJSON Feature needed for parsing.
     */
    private fun getRawGeoJsonGeometry(geometryRes: String): JSONObject {
        return try {
            val geometry = JSONObject(geometryRes)
            val rawGeoJsonGeometry = geometry.getJSONArray("features").getJSONObject(1)
            rawGeoJsonGeometry
        } catch (_: Exception) {
            JSONObject() // return empty json object in case of exception
        }
    }

    /**
     * Extracts the affected zone geometry from the detailed geometry response and converts it to JTS.
     *
     * @param geometryRes The raw JSON response string from the detailed geometry URL.
     * @return A JTS [Geometry] object representing the affected zone (e.g., a Polygon or
     *   MultiPolygon), or null on failure.
     */
    private fun getAffectedZone(geometryRes: String): Geometry? {
        return try {
            val rawGeoJsonAffectedZone = getRawGeoJsonGeometry(geometryRes).getString("geometry")
            GeometryParser.convertRawGeoJsonGeometryToJTS(rawGeoJsonAffectedZone)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Fetches the final article URL for the hazard, requiring two subsequent network calls.
     * 1. Fetches the initial hazard detail JSON.
     * 2. Fetches the media URL specified in the detail JSON.
     * 3. Parses the media response to find the actual article link.
     *
     * @param hazardDetailUrl The URL to fetch detailed information about the hazard.
     * @return The direct article link (String), or null on parsing/network failure.
     */
    private fun getHazardArticleUrl(hazardDetailUrl: String): String? {
        val detailRes = httpGet(hazardDetailUrl)
        return try {
            val json = JSONObject(detailRes)
            val mediaUrl = json.getJSONObject("properties").getJSONObject("url").getString("media")
            val mediaRes = httpGet(mediaUrl)
            val mediaJson = JSONArray(mediaRes)
            mediaJson.getJSONObject(0).getString("link")
        } catch (_: Exception) {
            return null
        }
    }
}