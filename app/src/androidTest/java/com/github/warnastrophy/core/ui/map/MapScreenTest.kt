package com.github.warnastrophy.core.ui.map

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.compose.runtime.MutableState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.github.warnastrophy.core.model.Hazard
import com.github.warnastrophy.core.model.HazardsDataService
import com.github.warnastrophy.core.model.Location
import com.github.warnastrophy.core.model.PositionService
import com.github.warnastrophy.core.ui.components.PermissionUiTags
import com.github.warnastrophy.core.ui.util.BaseAndroidComposeTest
import com.github.warnastrophy.core.util.AppConfig
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.rememberCameraPositionState
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MapScreenTest : BaseAndroidComposeTest() {
  private lateinit var gpsService: PositionService
  private lateinit var hazardService: HazardsDataService

  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

  private val hazards =
      listOf(
          Hazard(
              id = 1,
              type = "FL", // will map to HUE_GREEN
              country = null,
              date = null,
              severity = null,
              severityUnit = null,
              reportUrl = null,
              alertLevel = null,
              coordinates = listOf(Location(18.55, -72.34))),
          Hazard(
              id = 2,
              type = "EQ", // will map to HUE_RED
              country = null,
              date = null,
              severity = null,
              severityUnit = null,
              reportUrl = null,
              alertLevel = null,
              coordinates = listOf(Location(18.61, -72.22), Location(18.64, -72.10))))

  private val defaultPosition = AppConfig.defaultPosition

  @Before
  override fun setUp() {
    super.setUp()
    gpsService = GpsServiceMock(defaultPosition)
    hazardService = HazardServiceMock(hazards, defaultPosition)
    val context = ApplicationProvider.getApplicationContext<Context>()
    MapsInitializer.initialize(context)
  }

  private fun setContent(
      testHooks: MapScreenTestHooks? = null,
      relaunchKey: MutableState<Int>? = null
  ) {
    composeTestRule.setContent {
      val content =
          @androidx.compose.runtime.Composable {
            if (testHooks != null) {
              MapScreen(
                  gpsService = gpsService, hazardsService = hazardService, testHooks = testHooks)
            } else MapScreen(gpsService = gpsService, hazardsService = hazardService)
          }
      relaunchKey?.let { key(it.value) { content() } } ?: content()
    }
  }

  @Test
  fun showsFallbackError_whenNoActivityContextAvailable() {
    // Arrange: create a fake LocalContext that is NOT an Activity or ContextWrapper of one
    val applicationContext =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    composeTestRule.setContent {
      // Temporarily override LocalContext
      androidx.compose.runtime.CompositionLocalProvider(LocalContext provides applicationContext) {
        MapScreen(
            gpsService = gpsService,
            hazardsService = hazardService,
            testHooks = MapScreenTestHooks())
      }
    }

    // Assert: the fallback box should be displayed
    composeTestRule.onNodeWithTag(MapScreenTestTags.FALLBACK_ACTIVITY_ERROR).assertIsDisplayed()
  }

  /**
   * Given MapScreen is composed, When the initial loading is finished, Then the map is displayed.
   */
  @Test
  fun testMapScreenIsDisplayed_WithoutPermissionOverload() {
    setPref(firstLaunchDone = false, askedOnce = false)

    setContent()

    composeTestRule.waitUntilWithTimeout { !gpsService.positionState.value.isLoading }

    composeTestRule.onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN).assertIsDisplayed()
  }

  /**
   * Given MapScreen is composed and location permission is null (by default), When the initial
   * loading is finished, Then the map is displayed.
   */
  @Test
  fun testMapScreenIsDisplayed_WithPermissionOverload() {
    setContent(MapScreenTestHooks(forceLocationPermission = true))

    // Wait until the initial loading is finished and the map is displayed
    composeTestRule.waitUntilWithTimeout { !gpsService.positionState.value.isLoading }

    composeTestRule.onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN).assertIsDisplayed()
  }

  /**
   * Given location permission is not null, When MapScreen is composed on first launch, Then the
   * permission request card is displayed.
   */
  //  @Test
  //  fun testPermissionRequestCardIsDisplayedOnFirstLaunch() {
  //    assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
  //
  //    // First launch
  //    setPref(firstLaunchDone = false, askedOnce = false)
  //
  //    // Force the permission UI path deterministically
  //    val fakeDenied =
  //        mapOf(
  //            Manifest.permission.ACCESS_FINE_LOCATION to false,
  //            Manifest.permission.ACCESS_COARSE_LOCATION to false)
  //
  //    setContent(MapScreenTestHooks(
  //        forceLocationPermission = null,
  //        mockPermissionsResult = fakeDenied) // allow launcher path)
  //    )
  //
  //    composeTestRule.waitUntilWithTimeout { !gpsService.positionState.value.isLoading }
  //
  //    // Card is visible
  //    composeTestRule.onNodeWithTag(PermissionUiTags.CARD, useUnmergedTree =
  // true).assertIsDisplayed()
  //
  //    // Tap Settings to cover onOpenSettingsClick -> startActivity(...)
  //    composeTestRule
  //        .onNodeWithTag(PermissionUiTags.BTN_SETTINGS, useUnmergedTree = true)
  //        .assertIsDisplayed()
  //        .performClick()
  //  }

  /**
   * Given location permission is granted, When MapScreen is composed, Then the permission request
   * card is not displayed.
   */
  @Test
  fun testPermissionRequestCardIsNotDisplayedWhenPermissionGranted() {
    assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
    // Set Preferences to simulate first launch
    setPref(firstLaunchDone = false, askedOnce = false)

    setContent(MapScreenTestHooks(forceLocationPermission = true))

    // Wait until the initial loading is finished
    composeTestRule.waitUntilWithTimeout { !gpsService.positionState.value.isLoading }

    // Check that the permission request card is not displayed
    composeTestRule.onNodeWithTag(PermissionUiTags.CARD).assertIsNotDisplayed()
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

    setContent(MapScreenTestHooks(forceLocationPermission = true), relaunchKey)

    // Assert first composition
    composeTestRule.waitUntilWithTimeout { !gpsService.positionState.value.isLoading }
    composeTestRule.onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.USER_LOCATION).assertExists()

    // 2) "Second launch": update prefs and bump the key
    setPref(firstLaunchDone = true, askedOnce = true)
    composeTestRule.runOnUiThread { relaunchKey.value++ } // disposes MapScreen subtree
    composeTestRule.mainClock.advanceTimeByFrame() // Allow recomposition to settle
    composeTestRule.waitForIdle()

    // Assert second composition
    composeTestRule.waitUntilWithTimeout { !gpsService.positionState.value.isLoading }
    composeTestRule.onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN).assertIsDisplayed()
    composeTestRule.onNodeWithTag(MapScreenTestTags.USER_LOCATION).assertExists()
  }

  /**
   * Given permission is denied definitely, When MapScreen is composed, Then the map is shown but
   * the location is not displayed on both first launch and subsequent launches.
   */
  //  @Test
  //  fun testLocationPermissionDenied() {
  //    assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
  //
  //    // Permanent denial path: askedOnce=true and not granted
  //    setPref(firstLaunchDone = true, askedOnce = true)
  //
  //    val relaunchKey = mutableStateOf(0)
  //
  //    setContent(MapScreenTestHooks(
  //        forceLocationPermission = false // your test seam for "denied"
  //    ), relaunchKey)
  //
  //    composeTestRule.waitUntilWithTimeout { !gpsService.positionState.value.isLoading }
  //    composeTestRule.onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN).assertIsDisplayed()
  //
  //    // Card shown, but PERMANENT denial hides the "Allow" button
  //    composeTestRule.onNodeWithTag(PermissionUiTags.CARD, useUnmergedTree =
  // true).assertIsDisplayed()
  //    composeTestRule
  //        .onNodeWithTag(PermissionUiTags.BTN_ALLOW, useUnmergedTree = true)
  //        .assertDoesNotExist()
  //
  //    // No user location probe
  //    composeTestRule.onNodeWithTag(MapScreenTestTags.USER_LOCATION).assertIsNotDisplayed()
  //
  //    // Recompose to simulate subsequent launch; still permanent denial
  //    setPref(firstLaunchDone = true, askedOnce = true)
  //    composeTestRule.runOnUiThread { relaunchKey.value++ }
  //    composeTestRule.waitForIdle()
  //
  //    composeTestRule.waitUntilWithTimeout { !gpsService.positionState.value.isLoading }
  //
  //    composeTestRule.onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN).assertIsDisplayed()
  //    composeTestRule.onNodeWithTag(MapScreenTestTags.USER_LOCATION).assertIsNotDisplayed()
  //  }

  /**
   * Given permission is only given once, When MapScreen is composed, Then the map is shown and the
   * location is displayed on first launch, but on subsequent launches the location is not
   * displayed.
   */
  //  @Test
  //  fun testLocationPermissionAllowedOnce() {
  //    assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
  //
  //    // 1) First "launch": first run
  //    setPref(firstLaunchDone = false, askedOnce = false)
  //
  //    // State the test can bump to simulate a relaunch
  //    val relaunchKey = mutableStateOf(0)
  //    val permState = mutableStateOf(true)
  //
  //    setContent(
  //        testHooks = MapScreenTestHooks(
  //            forceLocationPermission = permState.value // your test seam
  //        ),
  //        relaunchKey = relaunchKey
  //    )
  //
  //    // Assert first composition
  //    composeTestRule.waitUntilWithTimeout { !gpsService.positionState.value.isLoading }
  //    composeTestRule.onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN).assertIsDisplayed()
  //    composeTestRule.onNodeWithTag(MapScreenTestTags.USER_LOCATION).assertIsDisplayed()
  //
  //    // 2) "Second launch": update prefs and bump the key
  //    setPref(firstLaunchDone = true, askedOnce = true)
  //    composeTestRule.runOnUiThread {
  //      permState.value = false // simulate user denying the permission on the second launch
  //      relaunchKey.value++
  //    } // disposes MapScreen subtree
  //    composeTestRule.waitForIdle()
  //
  //    // Assert second composition -- now becomes denied
  //    composeTestRule.waitUntilWithTimeout { !gpsService.positionState.value.isLoading }
  //    composeTestRule.onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN).assertIsDisplayed()
  //    composeTestRule.onNodeWithTag(MapScreenTestTags.USER_LOCATION).assertIsNotDisplayed()
  //  }

  private fun setPref(firstLaunchDone: Boolean, askedOnce: Boolean) {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val prefs = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    prefs
        .edit()
        .putBoolean("first_launch_done", firstLaunchDone)
        .putBoolean("loc_asked_once", askedOnce)
        .apply()
  }

  @Test
  fun trackLocationButtonSwitches() {
    val isTracking = mutableStateOf(false)

    composeTestRule.setContent { Box { TrackLocationButton(isTracking) } }

    // Click the track location button
    composeTestRule
        .onNodeWithTag(MapScreenTestTags.TRACK_LOCATION_BUTTON)
        .assertIsDisplayed()
        .performClick()

    assert(isTracking.value)
  }

  @Test
  fun trackLocationButtonAnimatesOnClick() {
    lateinit var cameraPositionState: CameraPositionState

    composeTestRule.setContent {
      cameraPositionState = rememberCameraPositionState()
      cameraPositionState.position =
          CameraPosition.fromLatLngZoom(
              LatLng(defaultPosition.latitude + 1, defaultPosition.longitude + 1),
              1f) // start away from default position

      MapScreen(
          gpsService = gpsService,
          hazardsService = hazardService,
          cameraPositionState = cameraPositionState)
    }

    composeTestRule.waitForIdle()
    composeTestRule.waitUntilWithTimeout { !cameraPositionState.isMoving }

    val initialPosition = cameraPositionState.position.target
    // Click the track location button to start tracking and trigger the animation
    composeTestRule
        .onNodeWithTag(MapScreenTestTags.TRACK_LOCATION_BUTTON)
        .assertIsDisplayed()
        .performClick()

    // The click should cause the camera to move.
    composeTestRule.waitForIdle()
    composeTestRule.waitUntilWithTimeout(10000) { !cameraPositionState.isMoving }
    composeTestRule.waitUntilWithTimeout(10000) {
      initialPosition != cameraPositionState.position.target
    }
  }
}
