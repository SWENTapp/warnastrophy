// kotlin
/**
 * This file was written by Anas Sidi Mohamed with the assistance of ChatGPT.
 *
 * Author: Anas Sidi Mohamed Assistance: ChatGPT
 */
package com.github.warnastrophy.core.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
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

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun `isRateLimited returns false when requests respect the delay`() = runTest {
    val repo = NominatimRepository()
    advanceTimeBy(2000) // simulate waiting
    val delay = repo.delayForNextQuery()
    Assert.assertTrue(delay <= 0)
  }
}
