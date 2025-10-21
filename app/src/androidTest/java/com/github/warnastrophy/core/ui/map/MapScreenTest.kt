package com.github.warnastrophy.core.ui.map

import android.Manifest
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import com.github.warnastrophy.core.model.Hazard
import com.github.warnastrophy.core.model.HazardsDataService
import com.github.warnastrophy.core.model.PositionService
import com.github.warnastrophy.core.util.AppConfig
import com.google.android.gms.maps.MapsInitializer
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MapScreenTest {
  private lateinit var gpsService: PositionService
  private lateinit var hazardService: HazardsDataService
  private val TIMEOUT = 5_000L
  @get:Rule val composeTestRule = createComposeRule()

  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

  private val hazards =
      listOf(
          Hazard(
              id = 1,
              type = null,
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
              country = null,
              date = null,
              severity = null,
              severityUnit = null,
              reportUrl = null,
              alertLevel = null,
              coordinates = null))

  @Before
  fun setup() {
    gpsService = GpsServiceMock(AppConfig.defaultPosition)
    hazardService = HazardServiceMock(hazards, AppConfig.defaultPosition)
    val context = ApplicationProvider.getApplicationContext<Context>()
    MapsInitializer.initialize(context)
    composeTestRule.setContent {
      MapScreen(gpsService = gpsService, hazardsService = hazardService)
    }
  }

  @Test
  fun testMapScreenIsDisplayed() {
    // Wait until the initial loading is finished and the map is displayed
    composeTestRule.waitUntil(timeoutMillis = TIMEOUT) { !gpsService.positionState.value.isLoading }
    composeTestRule.onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN).assertIsDisplayed()
  }
}
