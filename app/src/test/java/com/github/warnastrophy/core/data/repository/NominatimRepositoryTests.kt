// kotlin
/**
 * This file was written by Anas Sidi Mohamed with the assistance of ChatGPT.
 *
 * Author: Anas Sidi Mohamed Assistance: ChatGPT
 */
package com.github.warnastrophy.core.data.repository

import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test

class NominatimRepositoryTests {

  @Test
  fun `reverseGeocode returns an empty list for an invalid location`() = runBlocking {
    val repo = NominatimRepository()
    val result = repo.reverseGeocode("InvalidLocation123")
    Assert.assertTrue(result.isEmpty())
  }

  @Test
  fun `reverseGeocode returns data in the expected format`() = runBlocking {
    val repo = NominatimRepository()
    val result = repo.reverseGeocode("Tokyo")
    val location = result.first()
    Assert.assertTrue(location.latitude != 0.0)
    Assert.assertTrue(location.longitude != 0.0)
    Assert.assertTrue(location.name!!.isNotEmpty())
  }

  @Test
  fun `buildUrl generates the correct URL for a given address`() {
    val repo = NominatimRepository()
    val url = repo.buildUrl("Tokyo")
    Assert.assertEquals(
        "https://nominatim.openstreetmap.org/search?q=Tokyo&format=json&limit=5", url)
  }

  @Test
  fun `isRateLimited returns true when requests are too frequent`() = runBlocking {
    val repo = NominatimRepository()
    repo.maxRateMs = 2000
    repo.reverseGeocode("Tokyo") // simulate a request
    val delay = repo.delayForNextQuery()
    Assert.assertTrue(delay > 0)
  }

  @Test
  fun `isRateLimited returns false when requests respect the delay`() = runBlocking {
    val repo = NominatimRepository()
    Thread.sleep(600) // wait longer than maxRateMs
    val delay = repo.delayForNextQuery()
    Assert.assertTrue(delay <= 0)
  }
}
