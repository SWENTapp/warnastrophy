package com.github.warnastrophy.core.ui.map

import android.Manifest
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.github.warnastrophy.core.model.AppPermissions
import com.github.warnastrophy.core.model.PermissionResult
import com.github.warnastrophy.core.ui.components.PermissionUiTags
import com.github.warnastrophy.core.ui.util.BaseAndroidComposeTest
import com.github.warnastrophy.core.util.AnimationIdlingResource
import com.github.warnastrophy.core.util.AppConfig.defaultPosition
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.rememberCameraPositionState
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MapScreenTest : BaseAndroidComposeTest() {
  private lateinit var gpsService: GpsServiceMock
  private lateinit var hazardService: HazardServiceMock
  private lateinit var permissionManager: MockPermissionManager
  private lateinit var viewModel: MapViewModel
  private val mockPerm = AppPermissions.LocationFine
  private val animationIdlingResource = AnimationIdlingResource()

  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

  @Before
  override fun setUp() {
    super.setUp()

    gpsService = GpsServiceMock()
    hazardService = HazardServiceMock()
    permissionManager = MockPermissionManager()
    val context = ApplicationProvider.getApplicationContext<Context>()
    MapsInitializer.initialize(context)

    viewModel = MapViewModel(gpsService, hazardService, permissionManager)
    IdlingRegistry.getInstance().register(animationIdlingResource)
  }

  @After
  override fun tearDown() {
    super.tearDown()

    // Clear shared preferences
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val prefs = ctx.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    prefs.edit().clear().apply()

    // Unregister idling resources
    IdlingRegistry.getInstance().resources.forEach { IdlingRegistry.getInstance().unregister(it) }
  }

  private fun waitForMapReady(timeout: Long = 10_000L) {
    // Increase timeout to 10 seconds to accommodate longer CI runs
    composeTestRule.waitUntil(timeout) { !gpsService.positionState.value.isLoading }
  }

  private fun assertCardDisplayed(isVisible: Boolean) {
    if (isVisible) composeTestRule.onNodeWithTag(PermissionUiTags.CARD).assertIsDisplayed()
    else composeTestRule.onNodeWithTag(PermissionUiTags.CARD).assertIsNotDisplayed()
  }

  private fun waitForMapReadyAndAssertVisibility(
      mapVisible: Boolean = true,
      permissionCardVisible: Boolean? = null,
      allowButtonVisible: Boolean? = null,
      timeout: Long = 10_000L
  ) {
    waitForMapReady(timeout)
    if (mapVisible) {
      composeTestRule.onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN).assertIsDisplayed()
    } else {
      composeTestRule.onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN).assertDoesNotExist()
    }
    permissionCardVisible?.let { assertCardDisplayed(it) }
    allowButtonVisible?.let {
      if (it) composeTestRule.onNodeWithTag(PermissionUiTags.BTN_ALLOW).assertIsDisplayed()
      else composeTestRule.onNodeWithTag(PermissionUiTags.BTN_ALLOW).assertIsNotDisplayed()
    }
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

  private fun setContent(relaunchKey: MutableState<Int>? = null) {
    composeTestRule.setContent {
      val content = @androidx.compose.runtime.Composable { MapScreen(viewModel = viewModel) }
      relaunchKey?.let { key(it.value) { content() } } ?: content()
    }
  }

  private fun applyPerm(permissionResult: PermissionResult) {
    // Use mock permission manager to simulate different permission states deterministically
    permissionManager.setPermissionResult(permissionResult)
    viewModel.applyPermissionsResult(composeTestRule.activity)
  }

  @Test
  fun showsFallbackError_whenNoActivityContextAvailable() {
    // Arrange: use non-activity context to verify fallback UI is displayed
    val applicationContext =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    composeTestRule.setContent {
      // Temporarily override LocalContext
      CompositionLocalProvider(LocalContext provides applicationContext) {
        MapScreen(viewModel = viewModel)
      }
    }
    composeTestRule.onNodeWithTag(MapScreenTestTags.FALLBACK_ACTIVITY_ERROR).assertIsDisplayed()
  }

  /**
   * Given MapScreen is composed, When the initial loading is finished, Then the map is displayed.
   */
  @Test
  fun testMapScreenIsDisplayed() {
    setPref(firstLaunchDone = false, askedOnce = false)
    setContent()
    waitForMapReadyAndAssertVisibility()
  }

  @Test
  fun testPermissionRequestCardIsDisplayedOnFirstLaunch() {
    assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)

    // First launch
    setPref(firstLaunchDone = false, askedOnce = false)
    setContent()
    applyPerm(PermissionResult.Denied(mockPerm.permissions.toList()))
    waitForMapReady()

    // Card is visible
    assertCardDisplayed(true)
  }

  @Test
  fun testPermissionRequestCardIsNotDisplayedWhenPermissionGranted() {
    assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
    // Set Preferences to simulate first launch
    setPref(firstLaunchDone = false, askedOnce = false)
    setContent()
    applyPerm(PermissionResult.Granted)

    // Wait until the initial loading is finished
    waitForMapReady()

    // Check that the permission request card is not displayed
    assertCardDisplayed(false)
  }

  @Test
  fun testLocationPermissionGrantedAlways() {
    assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)

    // 1) First "launch": first run
    setPref(firstLaunchDone = false, askedOnce = false)

    // State the test can bump to simulate a relaunch
    val relaunchKey = mutableStateOf(0)
    setContent(relaunchKey = relaunchKey)
    applyPerm(PermissionResult.Granted)

    // Assert first composition
    waitForMapReadyAndAssertVisibility(permissionCardVisible = false)

    // 2) "Second launch": update prefs and bump the key
    setPref(firstLaunchDone = true, askedOnce = true)
    composeTestRule.runOnUiThread { relaunchKey.value++ } // disposes MapScreen subtree
    composeTestRule.mainClock.advanceTimeByFrame() // Allow recomposition to settle
    composeTestRule.waitForIdle()

    // Assert second composition
    waitForMapReadyAndAssertVisibility(permissionCardVisible = false)
  }

  /**
   * Given permission is denied definitely, When MapScreen is composed, Then the map is shown but
   * the location is not displayed on both first launch and subsequent launches.
   */
  @Test
  fun testLocationPermissionDenied() {
    assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)

    // Permanent denial path: askedOnce=true and not granted
    setPref(firstLaunchDone = true, askedOnce = true)
    val relaunchKey = mutableStateOf(0)
    setContent(relaunchKey)
    applyPerm(PermissionResult.PermanentlyDenied(mockPerm.permissions.toList()))
    waitForMapReadyAndAssertVisibility(permissionCardVisible = true, allowButtonVisible = false)

    setPref(firstLaunchDone = true, askedOnce = true)
    composeTestRule.runOnUiThread { relaunchKey.value++ }

    // waitForIdle is managed by the test rule and is generally more reliable
    // than waitUntil with a timeout for UI recomposition.
    composeTestRule.waitForIdle()

    // The waitForMapReadyAndAssertVisibility function already waits for the loading to finish.
    // This makes the explicit waitUntilWithTimeout redundant and safer.
    waitForMapReadyAndAssertVisibility(permissionCardVisible = true, allowButtonVisible = false)
  }

  @Test
  fun location_denied_permanently_move_to_settings_onClick() = runTest {
    setPref(firstLaunchDone = true, askedOnce = true)
    setContent()
    applyPerm(PermissionResult.PermanentlyDenied(mockPerm.permissions.toList()))
    waitForMapReadyAndAssertVisibility(permissionCardVisible = true, allowButtonVisible = false)
    Intents.init()
    try {
      composeTestRule
          .onNodeWithTag(PermissionUiTags.BTN_SETTINGS)
          .assertIsDisplayed()
          .performClick()
      intended(
          allOf(
              hasAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS),
              hasData(
                  "package:${InstrumentationRegistry.getInstrumentation().targetContext.packageName}"
                      .toUri())))
    } finally {
      Intents.release()
    }
  }

  @Test
  fun testLocationPermissionAllowedOnce() {
    assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)

    // 1) First "launch": first run
    setPref(firstLaunchDone = false, askedOnce = false)

    // State the test can bump to simulate a relaunch
    val relaunchKey = mutableStateOf(0)
    setContent(relaunchKey = relaunchKey)
    applyPerm(PermissionResult.Granted)
    waitForMapReadyAndAssertVisibility()

    // 2) "Second launch": update prefs and bump the key
    setPref(firstLaunchDone = true, askedOnce = true)
    composeTestRule.runOnUiThread {
      applyPerm(PermissionResult.Denied(mockPerm.permissions.toList()))
      // simulate user denying the permission on the second launch
      relaunchKey.value++
    } // disposes MapScreen subtree
    composeTestRule.waitForIdle()

    // Assert second composition -- now becomes denied
    waitForMapReadyAndAssertVisibility(permissionCardVisible = true)
  }

  @Test
  fun trackLocationButtonSwitches() {
    setContent()
    applyPerm(PermissionResult.Granted)
    waitForMapReady()

    // Click the track location button
    composeTestRule
        .onNodeWithTag(MapScreenTestTags.TRACK_LOCATION_BUTTON)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()
    val uiState = viewModel.uiState.value
    assertTrue(uiState.isTrackingLocation)
  }

  @Test
  fun trackLocationButtonAnimatesOnClick() {
    lateinit var cameraPositionState: CameraPositionState

    composeTestRule.setContent {
      cameraPositionState = rememberCameraPositionState()
      cameraPositionState.position =
          CameraPosition.fromLatLngZoom(
              LatLng(defaultPosition.latitude + 1, defaultPosition.longitude + 1), 1f)
      MapScreen(viewModel = viewModel, cameraPositionState = cameraPositionState)
      applyPerm(PermissionResult.Granted)
    }

    composeTestRule.waitForIdle()

    // It's good practice to wait until the camera is not moving from its initial setup
    composeTestRule.waitUntil { !cameraPositionState.isMoving }

    val initialPosition = cameraPositionState.position.target

    // Click the track location button to start tracking and trigger the animation
    composeTestRule
        .onNodeWithTag(MapScreenTestTags.TRACK_LOCATION_BUTTON)
        .assertIsDisplayed()
        .performClick()

    // Wait for animation to finish using the idling resource
    composeTestRule.waitUntil {
      // Consider idle when animation is finished and position changed
      !cameraPositionState.isMoving && initialPosition != cameraPositionState.position.target
    }
  }
}
