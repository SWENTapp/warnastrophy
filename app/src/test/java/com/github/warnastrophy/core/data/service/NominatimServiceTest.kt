package com.github.warnastrophy.core.data.service

import com.github.warnastrophy.core.data.repository.GeocodeRepository
import com.github.warnastrophy.core.data.repository.MockNominatimRepo
import com.github.warnastrophy.core.domain.model.Location
import com.github.warnastrophy.core.domain.model.NominatimService
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

  private val mockLocations = listOf(Location(1.0, 2.0, "Mock Location"))

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    repository = MockNominatimRepo(mockLocations)
    nominatimService = NominatimService(repository)
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
          override fun delayForNextQuery(): Long = if (delayCall++ == 0) Long.MAX_VALUE else 0L

          override suspend fun reverseGeocode(location: String): List<Location> {
            calls.add(location)
            return if (location == "q2") mockLocations else listOf(Location(0.0, 0.0, "first"))
          }
        }

    nominatimService = NominatimService(fakeRepo, dispatcher = Dispatchers.IO)

    nominatimService.searchQuery("q1")
    nominatimService.searchQuery("q2")

    assertEquals(1, calls.size)
    assertEquals("q2", calls.first())
    assertEquals(mockLocations, nominatimService.nominatimState.first())
  }
}
