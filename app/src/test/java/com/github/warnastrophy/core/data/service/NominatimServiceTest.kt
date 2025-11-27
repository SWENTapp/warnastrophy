package com.github.warnastrophy.core.data.service

import com.github.warnastrophy.core.data.repository.GeocodeRepository
import com.github.warnastrophy.core.data.repository.MockNominatimRepository
import com.github.warnastrophy.core.model.Location
import kotlinx.coroutines.Dispatchers
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

  private lateinit var mockLocations: List<Location>

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    repository = MockNominatimRepository()
    nominatimService = NominatimService(repository, testDispatcher)
    val repo = repository as MockNominatimRepository
    mockLocations = repo.locations
  }

  @Test
  fun serviceCorrectlyInitialised() = runTest {
    assertTrue(nominatimService.nominatimState.value.isEmpty())
  }

  @Test
  fun searchQueryUpdatesWithLocResult() = runTest {
    nominatimService.searchQuery("query")
    testDispatcher.scheduler.advanceUntilIdle()

    val result = nominatimService.nominatimState.first()
    assertEquals(mockLocations, result)
  }

  @Test
  fun searchQueryCancelSearchIfStillRunning() = runTest {
    val calls = mutableListOf<String>()
    var delayCall = 0
    val fakeRepo =
        object : GeocodeRepository {
          override fun delayForNextQuery(): Long = if (delayCall++ == 0) 10_000L else 0L

          override suspend fun reverseGeocode(location: String): List<Location> {
            calls.add(location)
            return if (location == "q2") mockLocations else listOf(Location(0.0, 0.0, "first"))
          }
        }

    nominatimService = NominatimService(fakeRepo, dispatcher = testDispatcher)

    nominatimService.searchQuery("q1")
    testDispatcher.scheduler.runCurrent()
    nominatimService.searchQuery("q2")
    testDispatcher.scheduler.advanceUntilIdle()

    assertEquals(1, calls.size)
    assertEquals("q2", calls.first())
    assertEquals(mockLocations, nominatimService.nominatimState.first())
  }
}
