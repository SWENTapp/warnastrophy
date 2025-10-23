package com.github.warnastrophy.core.ui.map

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import com.github.warnastrophy.core.model.Hazard
import com.github.warnastrophy.core.model.HazardsDataService
import com.github.warnastrophy.core.model.PositionService
import com.github.warnastrophy.core.util.AppConfig
import com.google.android.gms.maps.MapsInitializer
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MapScreenTest {
  private lateinit var gpsService: PositionService
  private lateinit var hazardService: HazardsDataService
  private val TIMEOUT = 5_000L

  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

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
  }

  @Test
  fun testMapScreenIsDisplayed() {
    composeTestRule.setContent {
      MapScreen(gpsService = gpsService, hazardsService = hazardService)
    }
    // Wait until the initial loading is finished and the map is displayed
    composeTestRule.waitUntil(timeoutMillis = TIMEOUT) { !gpsService.positionState.value.isLoading }
    composeTestRule.onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN).assertIsDisplayed()
  }

  /**
   * Given location permission is granted, When MapScreen is composed, Then the map is shown the
   * location is displayed on both first launch and subsequent launches.
   */
  @Test
  fun testLocationPermissionGrantedAlways() {
    assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)

    // 1) First "launch": first run
    setPref(firstLaunchDone = false, askedOnce = false)

    // State the test can bump to simulate a relaunch
    val relaunchKey = mutableStateOf(0)

    composeTestRule.setContent {
      key(relaunchKey.value) {
        MapScreen(
            gpsService = gpsService,
            hazardsService = hazardService,
            permissionOverride = true // your test seam for "granted"
            )
      }
    }

    // Assert first composition
    composeTestRule.waitUntil(timeoutMillis = TIMEOUT) { !gpsService.positionState.value.isLoading }
    composeTestRule.onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.USER_LOCATION).assertIsDisplayed()

    // 2) "Second launch": update prefs and bump the key
    setPref(firstLaunchDone = true, askedOnce = true)
    composeTestRule.runOnUiThread { relaunchKey.value++ } // disposes MapScreen subtree
    composeTestRule.waitForIdle()

    // Assert second composition
    composeTestRule.waitUntil(timeoutMillis = TIMEOUT) { !gpsService.positionState.value.isLoading }
    composeTestRule.onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.USER_LOCATION).assertIsDisplayed()
  }

  /**
   * Given permission is denied definitely, When MapScreen is composed, Then the map is shown but
   * the location is not displayed on both first launch and subsequent launches.
   */
  @Test
  fun testLocationPermissionDenied() {
    assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)

    // 1) First "launch": first run
    setPref(firstLaunchDone = false, askedOnce = false)

    // State the test can bump to simulate a relaunch
    val relaunchKey = mutableStateOf(0)

    composeTestRule.setContent {
      key(relaunchKey.value) {
        MapScreen(
            gpsService = gpsService,
            hazardsService = hazardService,
            permissionOverride = false // your test seam for "not granted"
            )
      }
    }

    // Assert first composition
    composeTestRule.waitUntil(timeoutMillis = TIMEOUT) { !gpsService.positionState.value.isLoading }
    composeTestRule.onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.USER_LOCATION).assertIsNotDisplayed()

    // 2) "Second launch": update prefs and bump the key
    setPref(firstLaunchDone = true, askedOnce = true)
    composeTestRule.runOnUiThread { relaunchKey.value++ } // disposes MapScreen subtree
    composeTestRule.waitForIdle()

    // Assert second composition -- still denied
    composeTestRule.waitUntil(timeoutMillis = TIMEOUT) { !gpsService.positionState.value.isLoading }
    composeTestRule.onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.USER_LOCATION).assertIsNotDisplayed()
  }

  /**
   * Given permission is only given once, When MapScreen is composed, Then the map is shown and the
   * location is displayed on first launch, but on subsequent launches the location is not
   * displayed.
   */
  @Test
  fun testLocationPermissionAllowedOnce() {
    assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)

    // 1) First "launch": first run
    setPref(firstLaunchDone = false, askedOnce = false)

    // State the test can bump to simulate a relaunch
    val relaunchKey = mutableStateOf(0)
    var perm = true

    composeTestRule.setContent {
      key(relaunchKey.value) {
        MapScreen(
            gpsService = gpsService,
            hazardsService = hazardService,
            permissionOverride = perm // your test seam for "granted"
            )
      }
    }

    // Assert first composition
    composeTestRule.waitUntil(timeoutMillis = TIMEOUT) { !gpsService.positionState.value.isLoading }
    composeTestRule.onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.USER_LOCATION).assertIsDisplayed()

    // 2) "Second launch": update prefs and bump the key
    setPref(firstLaunchDone = true, askedOnce = true)
    composeTestRule.runOnUiThread {
      perm = false // change to denied
      relaunchKey.value++ // dispose & recompose MapScreen subtree
    }
    composeTestRule.runOnUiThread { relaunchKey.value++ } // disposes MapScreen subtree
    composeTestRule.waitForIdle()

    // Assert second composition -- now becomes denied
    composeTestRule.waitUntil(timeoutMillis = TIMEOUT) { !gpsService.positionState.value.isLoading }
    composeTestRule.onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.USER_LOCATION).assertIsNotDisplayed()
  }

  private fun setPref(firstLaunchDone: Boolean, askedOnce: Boolean) {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val prefs = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    prefs
        .edit()
        .putBoolean("first_launch_done", firstLaunchDone)
        .putBoolean("loc_asked_once", askedOnce)
        .apply()
  }
}
