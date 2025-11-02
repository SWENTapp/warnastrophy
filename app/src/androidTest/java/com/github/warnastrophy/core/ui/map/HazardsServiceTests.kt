package com.github.warnastrophy.core.ui.map

import com.github.warnastrophy.core.data.repository.HazardsDataSource
import com.github.warnastrophy.core.model.Hazard
import com.github.warnastrophy.core.model.HazardsService
import com.github.warnastrophy.core.model.PositionService
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class HazardsServiceTests {

  private lateinit var gps: PositionService
  private lateinit var hazardProvider: HazardsDataSource

  private val hazards =
      listOf(
          Hazard(
              id = 1,
              type = null,
              description = null,
              country = null,
              date = null,
              severity = null,
              severityUnit = null,
              reportUrl = null,
              alertLevel = null,
              coordinates = null),
          Hazard(
              id = 2,
              type = null,
              description = null,
              country = null,
              date = null,
              severity = null,
              severityUnit = null,
              reportUrl = null,
              alertLevel = null,
              coordinates = null))

  @Before
  fun setUp() {
    gps = GpsServiceMock(LatLng(48.8146, 2.3486))
    hazardProvider = HazardsRepositoryMock(hazards)
  }

  @After
  fun tearDown() {
    // rien à faire ici
  }

  @Test
  fun testHazardsLoadedIntoService() = runBlocking {
    val service = HazardsService(hazardProvider, gps)
    delay(500)
    val hazards = service.fetcherState.value.hazards
    Assert.assertNotNull(hazards)
    Assert.assertTrue(hazards.isNotEmpty())
    Assert.assertEquals(service.fetcherState.value.hazards, hazards)
    service.close()
  }
}
