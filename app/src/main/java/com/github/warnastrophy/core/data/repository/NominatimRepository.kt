// kotlin
/**
 * This file was written by Anas Sidi Mohamed with the assistance of ChatGPT.
 *
 * Author: Anas Sidi Mohamed Assistance: ChatGPT
 *
 * Provides a concrete implementation of a simple Nominatim-based geocoding repository. The
 * implementation performs HTTP requests to the Nominatim search API and decodes the JSON response
 * into a list of [Location] domain models. Rate limiting is applied locally to avoid excessive
 * requests within a short time window.
 */
package com.github.warnastrophy.core.ui.repository

import com.github.warnastrophy.core.domain.model.Location
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

val TAG = "NominatimLocationRepository : "

/**
 * Simple abstraction for a geocoding repository.
 *
 * Implementations should provide means to resolve a textual location query into a list of
 * [Location] domain objects.
 */
interface GeocodeRepository {
  /**
   * Perform a reverse geocode or search for the given textual [location] and return a list of
   * matching [Location] objects.
   *
   * This function is suspending and may perform network IO.
   *
   * @param location The textual query to search for.
   * @return A list of matching [Location] results; empty list if none or on error.
   */
  suspend fun reverseGeocode(location: String): List<Location>
}

/**
 * Concrete repository that queries the OpenStreetMap Nominatim search API.
 *
 * This implementation:
 * - Builds a URL for the search query,
 * - Performs a blocking HTTP GET on a background dispatcher,
 * - Decodes the JSON array response into [Location] instances,
 * - Applies a simple local rate limiter controlled by [maxRateMs].
 *
 * Note: This class performs synchronous network operations inside a coroutine context.
 */
class NominatimRepository() : GeocodeRepository {

  /** HTTP Referer header value to identify the application source. */
  private val referer = "https://github.com/ssidimoh694"

  /** Base address of the Nominatim search endpoint. */
  private val baseAddress = "https://nominatim.openstreetmap.org/search"

  /**
   * Minimum time in milliseconds between consecutive requests. If a request occurs sooner than this
   * interval after the previous one, the call will be considered rate limited and `httpGet` will
   * return an empty string.
   */
  var maxRateMs: Long = 1000

  /**
   * Timestamp of the last performed query in milliseconds since epoch. `null` if no query was made
   * yet.
   */
  private var lastQueryTimestamp: Long? = null

  /**
   * Public entry point to perform a search for the provided [location] string.
   *
   * This method builds the query URL, performs the HTTP GET and decodes the JSON result into domain
   * [Location] objects.
   *
   * @param location The textual location to search for.
   * @return A list of [Location] results. Returns an empty list if the request is rate limited or
   *   if the response is empty/invalid.
   */
  override suspend fun reverseGeocode(location: String): List<Location> {
    val url = buildUrl(location)
    val jsonStr = httpGet(url)
    val locations = decodeLocation(jsonStr)
    return locations
  }

  /**
   * Build a properly encoded URL for the Nominatim search API using the provided [address].
   *
   * Spaces are replaced with `%20` and query parameters are appended to request JSON format and
   * limit results to 5 entries.
   *
   * @param address The raw address or search text.
   * @return A full URL string ready for HTTP GET.
   */
  fun buildUrl(address: String): String {
    val address = address.replace(" ", "%20")
    val url = "$baseAddress?q=$address&format=json&limit=5"
    return url
  }

  /**
   * Perform an HTTP GET request to the provided [urlStr] on the IO dispatcher.
   *
   * This method checks the local rate limiter via [isRateLimited]. If the call is rate limited, it
   * returns an empty string immediately. Otherwise it opens a connection, sets appropriate request
   * headers (language, user-agent, referer), reads the response body and returns it.
   *
   * @param urlStr The full URL to fetch.
   * @return The response body as a string, or an empty string if rate limited.
   */
  private suspend fun httpGet(urlStr: String): String =
      withContext(Dispatchers.IO) {
        val bool = isRateLimited()

        if (bool) {
          return@withContext ""
        }

        val url = URL(urlStr)
        val conn =
            (url.openConnection() as HttpURLConnection).apply {
              requestMethod = "GET"
              setRequestProperty("Accept-Language", "en")
              setRequestProperty(
                  "User-Agent", "WarnAStrophyApp/1.0 (+https://github.com/ssidimoh694)")
              setRequestProperty("Referer", referer)
            }
        val message: String
        try {
          val code = conn.responseCode
          val stream = if (code in 200..299) conn.inputStream else conn.errorStream
          message = BufferedReader(InputStreamReader(stream)).use { it.readText() }
        } finally {
          conn.disconnect()
        }
        return@withContext message
      }

  /**
   * Decode the JSON array string [jsonStr] returned by Nominatim into a list of [Location].
   *
   * If [jsonStr] is empty this method returns an empty list. Each JSON object is expected to
   * provide numeric `lat` and `lon` fields; `name` is read with `optString` falling back to
   * `"Unknown"` when missing.
   *
   * @param jsonStr The raw JSON array as a string.
   * @return A list of parsed [Location] objects.
   */
  private fun decodeLocation(jsonStr: String): List<Location> {
    if (jsonStr.isEmpty()) {
      return emptyList()
    }

    val json = JSONArray(jsonStr)
    val locations = mutableListOf<Location>()
    for (i in 0 until json.length()) {
      val obj = json.getJSONObject(i)
      val location =
          Location(
              latitude = obj.getDouble("lat"),
              longitude = obj.getDouble("lon"),
              name = obj.optString("name", "Unknown"),
          )
      locations.add(location)
    }
    return locations
  }

  /**
   * Simple local rate limiter that returns true when calls are too frequent.
   *
   * It updates [lastQueryTimestamp] on each invocation. The first call returns false and sets the
   * timestamp. Subsequent calls compute the elapsed time since the previous call and compare it to
   * [maxRateMs].
   *
   * @return `true` if the repository should consider the next request as rate limited; `false`
   *   otherwise.
   */
  fun isRateLimited(): Boolean {
    val currentTimestamp = System.currentTimeMillis()
    if (lastQueryTimestamp == null) {
      lastQueryTimestamp = currentTimestamp
      return false
    }
    val timeSinceLastQuery = currentTimestamp - lastQueryTimestamp!!

    if (timeSinceLastQuery < maxRateMs) {
      lastQueryTimestamp = currentTimestamp
      return true
    } else {
      return false
    }
  }
}
