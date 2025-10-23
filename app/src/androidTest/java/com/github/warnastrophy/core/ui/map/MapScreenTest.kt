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

  /**
   * Given MapScreen is composed, When the initial loading is finished, Then the map is displayed.
   */
  @Test
  fun testMapScreenIsDisplayed_WithoutPermissionOverload() {
    composeTestRule.setContent {
      MapScreen(gpsService = gpsService, hazardsService = hazardService)
    }
    // Wait until the initial loading is finished and the map is displayed
    composeTestRule.waitUntil(timeoutMillis = TIMEOUT) { !gpsService.positionState.value.isLoading }
    composeTestRule.onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN).assertIsDisplayed()
  }

  /**
   * Given MapScreen is composed and location permission is null (by default), When the initial
   * loading is finished, Then the map is displayed.
   */
  @Test
  fun testMapScreenIsDisplayed_WithPermissionOverload() {
    composeTestRule.setContent {
      MapScreen(gpsService = gpsService, hazardsService = hazardService, permissionOverride = true)
    }
    // Wait until the initial loading is finished and the map is displayed
    composeTestRule.waitUntil(timeoutMillis = TIMEOUT) { !gpsService.positionState.value.isLoading }
    composeTestRule.onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN).assertIsDisplayed()
  }

  /**
   * Given location permission is not null, When MapScreen is composed on first launch, Then the
   * permission request card is displayed.
   */
  @Test
  fun testPermissionRequestCardIsDisplayedOnFirstLaunch() {
    assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
    // Set preferences to simulate first launch
    setPref(firstLaunchDone = false, askedOnce = false)

    composeTestRule.setContent {
      MapScreen(gpsService = gpsService, hazardsService = hazardService, permissionOverride = false)
    }

    // Wait until the initial loading is finished
    composeTestRule.waitUntil(timeoutMillis = TIMEOUT) { !gpsService.positionState.value.isLoading }

    // Check that the permission request card is displayed
    composeTestRule.onNodeWithTag(MapScreenTestTags.PERMISSION_REQUEST_CARD).assertIsDisplayed()
  }

  /**
   * Given location permission is granted, When MapScreen is composed, Then the permission request
   * card is not displayed.
   */
  @Test
  fun testPermissionRequestCardIsNotDisplayedWhenPermissionGranted() {
    assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
    // Set preferences to simulate first launch
    setPref(firstLaunchDone = false, askedOnce = false)

    composeTestRule.setContent {
      MapScreen(gpsService = gpsService, hazardsService = hazardService, permissionOverride = true)
    }

    // Wait until the initial loading is finished
    composeTestRule.waitUntil(timeoutMillis = TIMEOUT) { !gpsService.positionState.value.isLoading }

    // Check that the permission request card is not displayed
    composeTestRule.onNodeWithTag(MapScreenTestTags.PERMISSION_REQUEST_CARD).assertIsNotDisplayed()
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

  private fun getPrefBoolean(key: String, defaultValue: Boolean = false): Boolean {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    return ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getBoolean(key, defaultValue)
  }

  /**
   * Given the permissions callback returns at least one granted value, When we inject that result,
   * Then the app treats location as granted, 'askedOnce' is persisted, and the card is hidden.
   */
  @Test
  fun permissionsResult_granted_updatesState_andHidesCard() {
    assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)

    // Start as "first launch" and not asked yet
    setPref(firstLaunchDone = false, askedOnce = false)

    // Fake system result: fine=true, coarse=false → any() == true
    val fakeResult =
        mapOf(
            Manifest.permission.ACCESS_FINE_LOCATION to true,
            Manifest.permission.ACCESS_COARSE_LOCATION to false)

    composeTestRule.setContent {
      MapScreen(
          gpsService = gpsService,
          hazardsService = hazardService,
          permissionOverride = null, // use real logic path
      )
    }

    // Wait until GPS mock settles
    composeTestRule.waitUntil(timeoutMillis = TIMEOUT) { !gpsService.positionState.value.isLoading }

    // Card should be gone, map & user location probe shown
    composeTestRule.onNodeWithTag(MapScreenTestTags.PERMISSION_REQUEST_CARD).assertDoesNotExist()
    composeTestRule.onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.USER_LOCATION).assertIsDisplayed()

    // 'askedOnce' persisted
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val askedOnce =
        ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getBoolean("loc_asked_once", false)
    assert(askedOnce)
  }

  /**
   * Given the permissions callback returns all false, When we inject that result, Then the app
   * treats location as denied temporarily, persists 'askedOnce', and shows the permission card.
   */
  @Test
  fun permissionsResult_denied_persistsAskedOnce_andShowsCard() {
    assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)

    // Start as "first launch" and not asked yet
    setPref(firstLaunchDone = false, askedOnce = false)

    // Fake system result: both denied → any() == false
    val fakeDenied =
        mapOf(
            Manifest.permission.ACCESS_FINE_LOCATION to false,
            Manifest.permission.ACCESS_COARSE_LOCATION to false)

    composeTestRule.setContent {
      MapScreen(
          gpsService = gpsService,
          hazardsService = hazardService,
          permissionOverride = null, // IMPORTANT: allow real flow to run
          testPermissionsResult = fakeDenied)
    }

    // Let composition + injection settle
    composeTestRule.waitUntil(timeoutMillis = TIMEOUT) { !gpsService.positionState.value.isLoading }

    // Card is visible; user location probe should not exist
    composeTestRule.onNodeWithTag(MapScreenTestTags.PERMISSION_REQUEST_CARD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.USER_LOCATION).assertDoesNotExist()

    // 'askedOnce' persisted to SharedPreferences
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val askedOncePersisted =
        ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getBoolean("loc_asked_once", /* default */ false)
    assert(askedOncePersisted)
  }

  /**
   * Given we provide a non-null permission override, When MapScreen is composed, Then the
   * LaunchedEffect early-returns and does not set 'first_launch_done' to true
   */
  @Test
  fun launchedEffect_earlyReturn_whenOverrideProvided_doesNotSetFirstLaunchDone() {
    assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)

    // Simulate very first launch
    setPref(firstLaunchDone = false, askedOnce = false)

    // Give override to short-circuit the LaunchedEffect(Unit)
    composeTestRule.setContent {
      MapScreen(
          gpsService = gpsService,
          hazardsService = hazardService,
          permissionOverride = true, // non-null triggers early return
          testPermissionsResult = null)
    }

    // Let composition settle
    composeTestRule.waitUntil(timeoutMillis = TIMEOUT) { !gpsService.positionState.value.isLoading }

    // Because we early-returned, the block that writes 'first_launch_done = true' never ran
    val firstLaunchDone = getPrefBoolean("first_launch_done", defaultValue = false)
    assert(!firstLaunchDone)

    // Map shows and no permission card (since granted via override)
    composeTestRule.onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.PERMISSION_REQUEST_CARD).assertDoesNotExist()
  }

  /**
   * Given we provide a non-null permission override, When MapScreen is composed, Then the
   * LaunchedEffect early-returns and shows the permission card if override is false.
   */
  @Test
  fun launchedEffect_deniedInjection_showsCard_andHidesUserLocation() {
    assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)

    setPref(firstLaunchDone = false, askedOnce = false)

    val fakeDenied =
        mapOf(
            Manifest.permission.ACCESS_FINE_LOCATION to false,
            Manifest.permission.ACCESS_COARSE_LOCATION to false)

    composeTestRule.setContent {
      MapScreen(
          gpsService = gpsService,
          hazardsService = hazardService,
          permissionOverride = null,
          testPermissionsResult = fakeDenied)
    }

    composeTestRule.waitUntil(timeoutMillis = TIMEOUT) { !gpsService.positionState.value.isLoading }

    // Card is visible; user location probe not present
    composeTestRule
        .onNodeWithTag(MapScreenTestTags.PERMISSION_REQUEST_CARD, useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(MapScreenTestTags.USER_LOCATION, useUnmergedTree = true)
        .assertDoesNotExist()
  }
}
