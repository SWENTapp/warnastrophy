package com.github.warnastrophy.core.data.service

import com.github.warnastrophy.core.domain.model.Location
import com.github.warnastrophy.core.domain.model.NominatimService
import com.github.warnastrophy.core.ui.repository.GeocodeRepository
import com.github.warnastrophy.core.ui.repository.MockNominatimRepo
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
}
