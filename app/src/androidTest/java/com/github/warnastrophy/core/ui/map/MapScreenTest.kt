package com.github.warnastrophy.core.ui.map

import android.Manifest
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import com.github.warnastrophy.core.ui.viewModel.MapViewModel
import com.google.android.gms.maps.MapsInitializer
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MapScreenTest {
  private lateinit var viewModel: MapViewModel
  private val TIMEOUT = 5_000L

  @get:Rule val composeTestRule = createComposeRule()

  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    MapsInitializer.initialize(context)
    viewModel = MapViewModel()
    composeTestRule.setContent { MapScreen(viewModel) }
  }

  @Test
  fun testMapScreenIsDisplayed() {
    // Wait until the initial loading is finished and the map is displayed
    composeTestRule.waitUntil(timeoutMillis = TIMEOUT) { !viewModel.uiState.value.isLoading }
    composeTestRule.onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN).assertIsDisplayed()
  }

  // TODO check if hazard could be already fetched during the time the map is loading
  @Test
  fun testRefreshUIState_updatesLocations() {
    composeTestRule.waitUntil(timeoutMillis = TIMEOUT) { !viewModel.uiState.value.isLoading }
    viewModel.refreshUIState()
    val hazards = viewModel.uiState.value.hazards

    composeTestRule.waitUntil(timeoutMillis = TIMEOUT) { hazards != null && hazards.isNotEmpty() }
  }
}
