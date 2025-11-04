package com.github.warnastrophy.core.ui.map

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.IdlingRegistry
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.github.warnastrophy.core.model.AppPermissions
import com.github.warnastrophy.core.model.PermissionResult
import com.github.warnastrophy.core.ui.components.PermissionUiTags
import com.github.warnastrophy.core.ui.util.BaseAndroidComposeTest
import com.github.warnastrophy.core.util.AppConfig.defaultPosition
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.rememberCameraPositionState
import junit.framework.TestCase.assertTrue
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

  @Test
  fun showsFallbackError_whenNoActivityContextAvailable() {
    // Arrange: create a fake LocalContext that is NOT an Activity or ContextWrapper of one
    val applicationContext =
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext

    composeTestRule.setContent {
      // Temporarily override LocalContext
      CompositionLocalProvider(LocalContext provides applicationContext) {
        MapScreen(viewModel = viewModel)
      }
    }

    // Assert: the fallback box should be displayed
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

  /**
   * Given location permission is not null, When MapScreen is composed on first launch, Then the
   * permission request card is displayed.
   */
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

  /**
   * Given location permission is granted, When MapScreen is composed, Then the permission request
   * card is not displayed.
   */
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

    // Recompose to simulate subsequent launch; still permanent denial
    setPref(firstLaunchDone = true, askedOnce = true)
    composeTestRule.runOnUiThread { relaunchKey.value++ }
    composeTestRule.waitForIdle()

    composeTestRule.waitUntilWithTimeout { !gpsService.positionState.value.isLoading }
    waitForMapReadyAndAssertVisibility(permissionCardVisible = true, allowButtonVisible = false)
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

    setContent(relaunchKey = relaunchKey)
    applyPerm(PermissionResult.Granted)

    // Assert first composition
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
              LatLng(defaultPosition.latitude + 1, defaultPosition.longitude + 1),
              1f) // start away from default position

      MapScreen(viewModel = viewModel, cameraPositionState = cameraPositionState)
    }

    applyPerm(PermissionResult.Granted)

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

  private fun waitForMapReady(timeout: Long = 5_000L) {
    composeTestRule.waitUntilWithTimeout(timeout) { !gpsService.positionState.value.isLoading }
  }

  private fun assertCardDisplayed(isVisible: Boolean) {
    if (isVisible) composeTestRule.onNodeWithTag(PermissionUiTags.CARD).assertIsDisplayed()
    else composeTestRule.onNodeWithTag(PermissionUiTags.CARD).assertIsNotDisplayed()
  }

  private fun waitForMapReadyAndAssertVisibility(
      mapVisible: Boolean = true,
      permissionCardVisible: Boolean? = null,
      allowButtonVisible: Boolean? = null,
      timeout: Long = 5_000L
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

  private fun setContent(
      relaunchKey: MutableState<Int>? = null,
  ) {
    composeTestRule.setContent {
      val content = @androidx.compose.runtime.Composable { MapScreen(viewModel = viewModel) }
      relaunchKey?.let { key(it.value) { content() } } ?: content()
    }
  }

  private fun applyPerm(permissionResult: PermissionResult) {
    permissionManager.setPermissionResult(permissionResult)
    viewModel.applyPermissionsResult(composeTestRule.activity)
  }
}
