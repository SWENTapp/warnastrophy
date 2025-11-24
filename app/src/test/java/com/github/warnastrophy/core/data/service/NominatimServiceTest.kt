package com.github.warnastrophy.core.data.service

import com.github.warnastrophy.core.domain.model.Location
import com.github.warnastrophy.core.domain.model.NominatimService
import com.github.warnastrophy.core.ui.repository.GeocodeRepository
import com.github.warnastrophy.core.ui.repository.MockNominatimRepo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * This file was written by Anas Sidi Mohamed with the assistance of ChatGPT.
 *
 * Author: Anas Sidi Mohamed Assistance: ChatGPT
 */
class NominatimServiceTest {

  private lateinit var repository: GeocodeRepository
  private lateinit var nominatimService: NominatimService

  private val testDispatcher = StandardTestDispatcher()

  private val mockLocations = listOf(Location(1.0, 2.0, "Mock Location"))

  @Before
  fun setUp() {
    repository = MockNominatimRepo(mockLocations)
    nominatimService = NominatimService(repository)
  }

  @Test
  fun serviceCorrectlyInitialised() = runTest {
    assertTrue(nominatimService.locations.value.isEmpty())
  }

  @Test
  fun searchQueryUpdatesWithLocResult() = runTest {
    nominatimService.searchQuery("query")
    advanceUntilIdle()

    val result = nominatimService.locations.first()
    assertEquals(mockLocations, result)
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun searchQueryCancelSearchIfStillRunning() = runTest {
    val calls = mutableListOf<String>()
    var delayCall = 0
    val fakeRepo =
        object : GeocodeRepository {
          override fun delayForNextQuery(): Long = if (delayCall++ == 0) Long.MAX_VALUE else 0L

          override suspend fun reverseGeocode(location: String): List<Location> {
            calls.add(location)
            return if (location == "q2") mockLocations else listOf(Location(0.0, 0.0, "first"))
          }
        }

    val testDispatcher = StandardTestDispatcher(testScheduler)
    nominatimService = NominatimService(fakeRepo, dispatcher = testDispatcher)

    nominatimService.searchQuery("q1")
    nominatimService.searchQuery("q2")
    advanceUntilIdle()

    assertEquals(1, calls.size)
    assertEquals("q2", calls.first())
    assertEquals(mockLocations, nominatimService.locations.first())
  }
}
