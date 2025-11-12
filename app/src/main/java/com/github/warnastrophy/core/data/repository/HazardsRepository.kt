package com.github.warnastrophy.core.data.repository

import android.util.Log
import com.github.warnastrophy.core.model.ErrorHandler
import com.github.warnastrophy.core.model.Hazard
import com.github.warnastrophy.core.util.AppConfig
import com.github.warnastrophy.core.util.AppConfig.Endpoints
import com.github.warnastrophy.core.util.AppConfig.HTTP_TIMEOUT
import com.github.warnastrophy.core.util.GeometryParser
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.time.TimeSource
import kotlinx.coroutines.delay
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
  /**
   * Fetches a list of current hazards that are inside the defined geographic area.
   *
   * This method performs a partial fetch, only retrieving basic hazard information and centroid
   * geometry, skipping detailed geometry, bounding box (bbox), and article URL retrieval.
   *
   * @param geometry A WKT string defining the area to search within.
   * @param days The number of days back to search for events.
   * @return A List of successfully parsed [Hazard] objects with partial data.
   */
  suspend fun getPartialAreaHazards(geometry: String, days: String): List<Hazard>

  /**
   * Completes the parsing of a partially constructed Hazard by fetching detailed geometry, bounding
   * box (bbox), and article URL.
   *
   * @param hazard The partially constructed [Hazard] object.
   * @return A complete [Hazard] object with additional fields populated, or null on failure.
   */
  suspend fun completeParsingOf(hazard: Hazard): Hazard?
}

/**
 * Repository class responsible for fetching hazard data from the GDACS API (Global Disaster Alert
 * and Coordination System).
 *
 * This repository handles the multi-step process of network communication, GeoJSON parsing, and
 * conversion to JTS Geometry objects.
 */
class HazardsRepository(
    val errorHandler: ErrorHandler = ErrorHandler(),
) : HazardsDataSource {

  private var lastApiCall = TimeSource.Monotonic.markNow() - AppConfig.gdacsThrottleDelay

  /**
   * Constructs the full URL for fetching hazard events within a specific geographic area.
   *
   * @param geometry A WKT (Well-Known Text) string defining the search area.
   * @param days The number of days back to search for events.
   * @return The complete, URL-encoded string for the GDACS API endpoint.
   */
  private fun buildUrlAreaHazards(geometry: String, days: String): String {
    val geom = geometry.replace(" ", "%20")
    return "${Endpoints.EVENTS_BY_AREA}?geometryArea=$geom&days=$days"
  }

  /**
   * Executes a synchronous HTTP GET request on an IO dispatcher.
   *
   * This function performs a blocking network call and should always be called from within a
   * coroutine running on Dispatchers.IO.
   *
   * @param urlStr The URL to fetch.
   * @return The response body as a String, or null when the status code is not 2xx or on error.
   */
  private suspend fun httpGet(urlStr: String): String? {
    // Throttle API calls to avoid rate limiting
    delay(AppConfig.gdacsThrottleDelay - lastApiCall.elapsedNow())
    lastApiCall = TimeSource.Monotonic.markNow()

    val url = URL(urlStr)
    val conn =
        (url.openConnection() as HttpURLConnection).apply {
          requestMethod = "GET"
          setRequestProperty("Accept", "application/json")
          connectTimeout = HTTP_TIMEOUT
          readTimeout = HTTP_TIMEOUT
        }
    return try {
      if (conn.responseCode in 200..299) {
        BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
      } else {
        null
      }
    } catch (e: Exception) {
      Log.e("HazardsRepository", "HTTP GET error for $urlStr", e)
      null
    } finally {
      conn.disconnect()
    }
  }

  override suspend fun getPartialAreaHazards(geometry: String, days: String): List<Hazard> {
    val url = buildUrlAreaHazards(geometry, days)
    val response = httpGet(url)
    if (response == null || response.isBlank()) {
      return emptyList()
    }

    val jsonObject = JSONObject(response)
    val jsonHazards = jsonObject.getJSONArray("features")
    return (0 until jsonHazards.length()).mapNotNull { i ->
      val hazardJson = jsonHazards.getJSONObject(i)
      parsePartialHazard(hazardJson)
    }
  }

  /**
   * Parses a single GeoJSON Feature object into a partial Hazard data class.
   *
   * This function only extracts basic information and the centroid geometry, skipping detailed
   * geometry, bounding box (bbox), and article URL retrieval. It filters out non-current hazards
   * and skips those with missing fields.
   *
   * @param root The JSONObject representing a single GeoJSON Feature (a hazard event).
   * @return A partially constructed [Hazard] object, or null if parsing fails or fields are
   *   missing.
   */
  private fun parsePartialHazard(root: JSONObject): Hazard? {
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

      return Hazard(
          id = properties.getInt("eventid"),
          type = properties.getString("eventtype"),
          description = properties.optString("description"),
          severityText = properties.getJSONObject("severitydata").getString("severitytext"),
          country = properties.getString("country"),
          date = properties.getString("fromdate"),
          severity = properties.getJSONObject("severitydata").getDouble("severity"),
          severityUnit = properties.getJSONObject("severitydata").getString("severityunit"),
          articleUrl = null,
          alertLevel = properties.getDouble("alertscore"),
          centroid = centroid,
          affectedZone = null,
          bbox = null)
    } catch (e: Exception) {
      Log.e("HazardsRepository", "Error parsing partial hazard", e)
      return null
    }
  }

  override suspend fun completeParsingOf(hazard: Hazard): Hazard? {
    return try {
      val detailedGeometryUrl =
          "${Endpoints.GET_GEOMETRY}?eventtype=${hazard.type}&eventid=${hazard.id}"
      val geometryRes = httpGet(detailedGeometryUrl)
      val articleUrl = getHazardArticleUrl(hazard)
      val bbox = geometryRes?.let { getBbox(it) }
      val affectedZone = geometryRes?.let { getAffectedZone(it) }
      return hazard.copy(articleUrl = articleUrl, affectedZone = affectedZone, bbox = bbox)
    } catch (e: Exception) {
      Log.e("HazardsRepository", "Error completing hazard parsing", e)
      null
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
    } catch (e: Exception) {
      Log.e("HazardsRepository", "Error parsing bbox", e)
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
    } catch (e: Exception) {
      Log.e("HazardsRepository", "Error extracting raw GeoJSON geometry", e)
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
    } catch (e: Exception) {
      Log.e("HazardsRepository", "Error parsing affected zone", e)
      null
    }
  }

  /**
   * Fetches the final article URL for the hazard.
   *
   * @param hazard the [Hazard] object for which to fetch the article URL.
   * @return The direct article link (String), or null on parsing/network failure.
   */
  private suspend fun getHazardArticleUrl(hazard: Hazard): String? {
    return try {
      // We limit to 1 result as we only need the first article link
      val res =
          httpGet(
              "${Endpoints.EMM_NEWS_BY_KEY}?eventtype=${hazard.type}&eventid=${hazard.id}&limit=1")
      res?.let { JSONArray(it).getJSONObject(0).getString("link") }
    } catch (e: Exception) {
      Log.e("HazardsRepository", "Error fetching article URL", e)
      null
    }
  }
}
