package com.github.warnastrophy.core.ui.repository

import com.github.warnastrophy.core.model.util.Location
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

val TAG = "NominatimLocationRepository : "

class NominatimLocationRepository() {

  private val referer = "https://github.com/ssidimoh694"
  private val baseAddress = "https://nominatim.openstreetmap.org/search"
  private var lastQueryTimestamp: Long = System.currentTimeMillis()
  private val maxRateMs: Long = 500

  fun buildUrl(address: String): String {
    val address = address.replace(" ", "%20")
    val url = "$baseAddress?q=$address&format=json&limit=5"
    return url
  }

  private suspend fun httpGet(urlStr: String): String =
      withContext(Dispatchers.IO) {
        if (isRateLimited()) return@withContext ""

        val url = URL(urlStr)
        val conn = (url.openConnection() as HttpURLConnection).apply { requestMethod = "GET" }
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

  private fun decodeUrl(jsonStr: String): List<Location> {
    if (jsonStr == "") return emptyList()

    val json = JSONArray(jsonStr)
    val locations = mutableListOf<Location>()
    for (i in 0 until json.length()) {
      val obj = json.getJSONObject(i)
      val location = Location(latitude = obj.getDouble("lat"), longitude = obj.getDouble("lon"))
      locations.add(location)
    }
    return locations
  }

  private fun isRateLimited(): Boolean {
    val currentTimestamp = System.currentTimeMillis()
    val timeSinceLastQuery = currentTimestamp - lastQueryTimestamp

    if (timeSinceLastQuery < maxRateMs) {
      lastQueryTimestamp = currentTimestamp
      return true
    } else return false
  }
}
