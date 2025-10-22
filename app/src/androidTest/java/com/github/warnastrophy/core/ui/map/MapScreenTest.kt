package com.github.warnastrophy.core.ui.map

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.github.warnastrophy.core.model.Hazard
import com.github.warnastrophy.core.model.HazardsDataService
import com.github.warnastrophy.core.model.PositionService
import com.github.warnastrophy.core.ui.components.PermissionUiTags
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

  @Test
  fun mapScreen_displays_when_permissions_granted() {
    // Compose with default (granted) state
    composeTestRule.setContent {
      MapScreen(gpsService = gpsService, hazardsService = hazardService)
    }
    // Wait until the map node appears (position may still be loading briefly)
    waitForNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN)
    composeTestRule.onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN).assertIsDisplayed()
  }

  @Test
  fun showsPermissionCard_whenPermissionDeniedTemporarily_showsAllowAndSettings_andMapStillRenders() {

    assumeTrue(Build.VERSION.SDK_INT >= 23) // runtime perms

    revokeLocationPermissions()
    setPref(firstLaunchDone = true, askedOnce = false) // will classify as DENIED_TEMP

    composeTestRule.setContent {
      MapScreen(gpsService = gpsService, hazardsService = hazardService)
    }

    // Map should still render (limited functionality)
    waitForNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN)
    composeTestRule.onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN).assertIsDisplayed()

    // Permission card with BOTH buttons in temporary denial
    waitForNodeWithTag(PermissionUiTags.CARD)
    composeTestRule.onNodeWithTag(PermissionUiTags.CARD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PermissionUiTags.BTN_ALLOW).assertIsDisplayed()
    composeTestRule.onNodeWithTag(PermissionUiTags.BTN_SETTINGS).assertIsDisplayed()
  }

  @Test
  fun showsPermissionCard_whenPermissionDeniedPermanently_hidesAllow_showsSettings_andMapRenders() {
    assumeTrue(Build.VERSION.SDK_INT >= 23)

    revokeLocationPermissions()
    setPref(firstLaunchDone = true, askedOnce = true) // !rationale && askedOnce -> DENIED_PERMANENT

    composeTestRule.setContent {
      MapScreen(gpsService = gpsService, hazardsService = hazardService)
    }

    waitForNodeWithTag(PermissionUiTags.CARD)
    composeTestRule.onNodeWithTag(PermissionUiTags.CARD).assertIsDisplayed()

    // "Allow" hidden in permanent denial
    composeTestRule.onNodeWithTag(PermissionUiTags.BTN_ALLOW).assertDoesNotExist()
    composeTestRule.onNodeWithTag(PermissionUiTags.BTN_SETTINGS).assertIsDisplayed()

    // Map is still there underneath
    waitForNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN)
    composeTestRule.onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN).assertIsDisplayed()
  }

  // ---- Helpers ----

  private fun revokeLocationPermissions() {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val pkg = ctx.packageName
    val ui = InstrumentationRegistry.getInstrumentation().uiAutomation
    ui.executeShellCommand("pm revoke $pkg android.permission.ACCESS_FINE_LOCATION").close()
    ui.executeShellCommand("pm revoke $pkg android.permission.ACCESS_COARSE_LOCATION").close()
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

  private fun waitForNodeWithTag(tag: String) {
    composeTestRule.waitUntil(timeoutMillis = TIMEOUT) {
      composeTestRule
          .onAllNodes(hasTestTag(tag), useUnmergedTree = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }
}
