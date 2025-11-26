package com.github.warnastrophy.core.ui.map

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.IdlingRegistry
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.github.warnastrophy.core.permissions.AppPermissions
import com.github.warnastrophy.core.permissions.PermissionResult
import com.github.warnastrophy.core.ui.components.PermissionUiTags
import com.github.warnastrophy.core.ui.features.map.MapScreen
import com.github.warnastrophy.core.ui.features.map.MapScreenTestTags
import com.github.warnastrophy.core.ui.features.map.MapViewModel
import com.github.warnastrophy.core.ui.repository.GeocodeRepository
import com.github.warnastrophy.core.util.AnimationIdlingResource
import com.github.warnastrophy.core.util.AppConfig
import com.github.warnastrophy.core.util.AppConfig.defaultPosition
import com.github.warnastrophy.core.util.BaseAndroidComposeTest
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.rememberCameraPositionState
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapScreenTest : BaseAndroidComposeTest() {
  private lateinit var gpsService: GpsServiceMock
  private lateinit var hazardService: HazardServiceMock
  private lateinit var permissionManager: MockPermissionManager
  private lateinit var viewModel: MapViewModel
  private val mockPerm = AppPermissions.LocationFine

  private lateinit var nominatimRepository: GeocodeRepository
  /**
   * An idling resource to wait for camera animations to complete during tests. This is crucial for
   * Espresso tests involving map camera movements, as it prevents test actions from executing
   * prematurely while the map is still animating.
   */
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
    nominatimRepository = MockNominatimRepository()
    val context = ApplicationProvider.getApplicationContext<Context>()
    MapsInitializer.initialize(context)

    viewModel = MapViewModel(gpsService, hazardService, permissionManager, nominatimRepository)
    IdlingRegistry.getInstance().register(animationIdlingResource)
  }

  @After
  override fun tearDown() {
    super.tearDown()

    // Clear shared preferences
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val prefs = ctx.getSharedPreferences(AppConfig.PREF_FILE_NAME, Context.MODE_PRIVATE)
    prefs.edit().clear().apply()

    // Unregister idling resources
    IdlingRegistry.getInstance().resources.forEach { IdlingRegistry.getInstance().unregister(it) }
  }

  /** Waits for the map to be ready and loaded. */
  private fun waitForMapReady(timeout: Long = 10_000L) {
    // Increase timeout to 10 seconds to accommodate longer CI runs
    composeTestRule.waitUntil(timeout) {
      composeTestRule
          .onAllNodesWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN)
          .fetchSemanticsNodes()
          .isNotEmpty() && !gpsService.positionState.value.isLoading
    }
  }

  /** Asserts that the permission card's visibility matches the expected state. */
  private fun assertCardDisplayed(isVisible: Boolean) {
    if (isVisible) composeTestRule.onNodeWithTag(PermissionUiTags.CARD).assertIsDisplayed()
    else composeTestRule.onNodeWithTag(PermissionUiTags.CARD).assertIsNotDisplayed()
  }

  /** Waits for the map to be ready and asserts visibility of map and permission UI elements. */
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

  /** Sets shared preferences to simulate different app launch states. */
  private fun setPref(firstLaunchDone: Boolean, askedOnce: Boolean) {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val prefs = ctx.getSharedPreferences(AppConfig.PREF_FILE_NAME, Context.MODE_PRIVATE)
    prefs
        .edit()
        .putBoolean("first_launch_done", firstLaunchDone)
        .putBoolean("loc_asked_once", askedOnce)
        .apply()
  }

  /** Sets the Compose content for the test, allowing for a fake map or a real one. */
  private fun setContent(relaunchKey: MutableState<Int>? = null, useRealMap: Boolean = false) {
    composeTestRule.setContent {
      val content =
          @Composable {
            if (useRealMap) {
              MapScreen(viewModel = viewModel) // Keeps original behavior
            } else {
              MapScreen(viewModel = viewModel, googleMap = { _, _ -> FakeMapForTest() })
            }
          }
      relaunchKey?.let { key(it.value) { content() } } ?: content()
    }
  }

  /** Applies a given [PermissionResult] to the view model. */
  private fun applyPerm(permissionResult: PermissionResult) {
    // Use mock permission manager to simulate different permission states deterministically
    permissionManager.setPermissionResult(permissionResult)
    viewModel.applyPermissionsResult(composeTestRule.activity)
  }

  /**
   * A fake composable used in place of the real GoogleMap for testing UI without the map overhead.
   */
  @Composable
  private fun FakeMapForTest() {
    Box(modifier = Modifier.fillMaxSize().testTag(MapScreenTestTags.GOOGLE_MAP_SCREEN))
  }

  /** Verifies that a fallback error message is shown when the context is not an Activity. */
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

  /**
   * Tests that the permission request card is displayed on the first launch when permission is
   * denied.
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

  /** Tests that the permission card is not shown when permission has been granted. */
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

  /** Tests that if permission is granted, it remains granted across app relaunches. */
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

  /**
   * Tests the "allow once" permission flow, where permission is granted but then denied on a
   * subsequent launch.
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

  /**
   * Verifies that the track location button correctly updates the tracking state in the ViewModel.
   */
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

  /** Verifies that clicking the track location button triggers a camera animation. */
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
    composeTestRule.waitUntil(timeoutMillis = 10_000) {
      !cameraPositionState.isMoving && initialPosition != cameraPositionState.position.target
    }
  }

  @Test
  fun searchBarIsDisplayed() {
    setContent()
    applyPerm(PermissionResult.Granted)
    waitForMapReadyAndAssertVisibility()
    composeTestRule.onNodeWithTag(MapScreenTestTags.SEARCH_BAR).assertIsDisplayed()

    val dropdownbox = composeTestRule.onNodeWithTag(MapScreenTestTags.SEARCH_BAR_DROPDOWN)
    dropdownbox.assertIsNotDisplayed()
  }

  @Test
  fun searchBarAllowsInput() {
    setContent()
    applyPerm(PermissionResult.Granted)
    waitForMapReadyAndAssertVisibility()

    val testInput = "Test Location"
    val searchBarNode = composeTestRule.onNodeWithTag(MapScreenTestTags.SEARCH_BAR_TEXT_FIELD)

    searchBarNode.assertIsDisplayed()
    searchBarNode.performClick()
    searchBarNode.performTextInput(testInput)

    searchBarNode.assert(hasText(testInput))
  }

  @Test
  fun searchBarShowsResultsOnInput_verifyItemsMatchHardcodedLocations() {
    setContent()
    applyPerm(PermissionResult.Granted)
    waitForMapReadyAndAssertVisibility()
    val repo = nominatimRepository as MockNominatimRepository
    val hardcodedlocs = repo.locations
    val testInput = "Test Location"
    val searchBarNode = composeTestRule.onNodeWithTag(MapScreenTestTags.SEARCH_BAR_TEXT_FIELD)

    searchBarNode.assertIsDisplayed()
    searchBarNode.performClick()
    searchBarNode.performTextInput(testInput)

    searchBarNode.assert(hasText(testInput))
    val dropdownbox = composeTestRule.onNodeWithTag(MapScreenTestTags.SEARCH_BAR_DROPDOWN)
    composeTestRule.waitForIdle()
    dropdownbox.assertIsDisplayed()

    val items = composeTestRule.onAllNodesWithTag(MapScreenTestTags.SEARCH_BAR_DROPDOWN_ITEM)
    items.assertCountEquals(hardcodedlocs.size)

    // Fetch semantics nodes and compare text one by one with hardcoded locations
    val semanticsList = items.fetchSemanticsNodes()
    for (i in hardcodedlocs.indices) {
      val nodeText =
          semanticsList[i]
              .config
              .getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Text)
              ?.joinToString { it.text } ?: ""
      org.junit.Assert.assertEquals(hardcodedlocs[i].name, nodeText)
    }
  }
}
