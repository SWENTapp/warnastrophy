// kotlin
package com.github.warnastrophy.core.ui.repository

import android.util.Log
import com.github.warnastrophy.core.domain.model.Location
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

val TAG = "NominatimLocationRepository : "

class NominatimRepository() {

  private val referer = "https://github.com/ssidimoh694"
  private val baseAddress = "https://nominatim.openstreetmap.org/search"
  var maxRateMs: Long = 1000
  private var lastQueryTimestamp: Long? = null

  suspend fun reverseGeocode(location: String): List<Location> {
    val url = buildUrl(location)
    val jsonStr = httpGet(url)
    val locations = decodeLocation(jsonStr)
    return locations
  }

  fun buildUrl(address: String): String {
    val address = address.replace(" ", "%20")
    val url = "$baseAddress?q=$address&format=json&limit=5"
    return url
  }

  private suspend fun httpGet(urlStr: String): String =
      withContext(Dispatchers.IO) {
        val bool = isRateLimited()
        Log.d(TAG, "httpGet: isRateLimited = $bool")

        if (bool) {
          return@withContext ""
        }

        val url = URL(urlStr)
        val conn =
            (url.openConnection() as HttpURLConnection).apply {
              requestMethod = "GET"
              setRequestProperty("Accept-Language", "en")
              // Identifie explicitement l'application (éviter les User-Agents génériques)
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

  private fun decodeLocation(jsonStr: String): List<Location> {
    Log.d(TAG, "decodeLocation: jsonStr = $jsonStr")
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

  fun isRateLimited(): Boolean {
    val currentTimestamp = System.currentTimeMillis()
    if (lastQueryTimestamp == null) {
      lastQueryTimestamp = currentTimestamp
      return false
    }
    val timeSinceLastQuery = currentTimestamp - lastQueryTimestamp!!
    lastQueryTimestamp = currentTimestamp

    Log.d(TAG, "isRateLimited: timeSinceLastQuery = $timeSinceLastQuery ms")
    if (timeSinceLastQuery < maxRateMs) {
      return true
    } else {
      return false
    }
  }
}
