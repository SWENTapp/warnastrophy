package com.github.warnastrophy.core.ui.repository

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NominatimRepositoryTests {

  @Test
  fun `reverseGeocode retourne une liste vide pour une localisation invalide`() = runBlocking {
    val repo = NominatimRepository()
    val result = repo.reverseGeocode("InvalidLocation123")
    assertTrue(result.isEmpty())
  }

  @Test
  fun `reverseGeocode respecte le format des donnees retournees`() = runBlocking {
    val repo = NominatimRepository()
    val result = repo.reverseGeocode("Tokyo")
    val location = result.first()
    assertTrue(location.latitude != 0.0)
    assertTrue(location.longitude != 0.0)
    assertTrue(location.name!!.isNotEmpty())
  }

  @Test
  fun `buildUrl genere une URL correcte pour une adresse donnee`() {
    val repo = NominatimRepository()
    val url = repo.buildUrl("Tokyo")
    assertEquals("https://nominatim.openstreetmap.org/search?q=Tokyo&format=json&limit=5", url)
  }

  @Test
  fun `isRateLimited retourne true si les requetes sont trop frequentes`() = runBlocking {
    val repo = NominatimRepository()
    repo.maxRateMs = 2000
    repo.reverseGeocode("Tokyo") // Simule une requête
    val isRateLimited = repo.isRateLimited()
    assertTrue(isRateLimited)
  }

  @Test
  fun `isRateLimited retourne false si les requetes respectent le delai`() = runBlocking {
    val repo = NominatimRepository()
    Thread.sleep(600) // Attendre plus que maxRateMs
    val isRateLimited = repo.isRateLimited()
    assertTrue(!isRateLimited)
  }
}
