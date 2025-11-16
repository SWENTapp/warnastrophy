package com.github.warnastrophy.core.ui.map

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.github.warnastrophy.HiltTestActivity
import com.github.warnastrophy.core.data.repository.HazardsDataSource
import com.github.warnastrophy.core.domain.error.ErrorDisplayManager
import com.github.warnastrophy.core.domain.model.Hazard
import com.github.warnastrophy.core.domain.model.HazardsDataService
import com.github.warnastrophy.core.domain.model.HazardsService
import com.github.warnastrophy.core.domain.model.PositionService
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class HazardsServiceTests {
  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @get:Rule(order = 1) val composeTestRule = createAndroidComposeRule<HiltTestActivity>()

  @Inject lateinit var gpsMock: PositionService

  @Inject lateinit var hazardsRepositoryMock: HazardsDataSource

  lateinit var hazardsService: HazardsDataService

  @Inject lateinit var errorDisplayManager: ErrorDisplayManager

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
              articleUrl = null,
              alertLevel = null,
              centroid = null,
              bbox = null,
              affectedZone = null),
          Hazard(
              id = 2,
              type = null,
              description = null,
              country = null,
              date = null,
              severity = null,
              severityUnit = null,
              articleUrl = null,
              bbox = null,
              alertLevel = null,
              affectedZone = null,
              centroid = null))

  @Before
  fun setUp() {
    hiltRule.inject()
    hazardsService = HazardsService(hazardsRepositoryMock, gpsMock, errorDisplayManager)
  }

  @After
  fun tearDown() {
    // rien à faire ici
  }

  @Test
  fun testHazardsLoadedIntoService() = runBlocking {
    // val service = HazardsService(hazardProvider, gps, errorDisplayManager)
    delay(500)
    val hazards = hazardsService.fetcherState.value.hazards
    Assert.assertNotNull(hazards)
    Assert.assertTrue(hazards.isNotEmpty())
    Assert.assertEquals(hazardsService.fetcherState.value.hazards, hazards)
    hazardsService.close()
  }
}
