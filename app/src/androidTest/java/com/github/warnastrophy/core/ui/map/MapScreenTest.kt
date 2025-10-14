package com.github.warnastrophy.core.ui.map

import android.Manifest
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.rule.GrantPermissionRule
import org.junit.Rule
import org.junit.Test

class MapScreenTest {

  @get:Rule val composeTestRule = createComposeRule()

  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(
          Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

  val viewModel = MapViewModel()

  @Test
  fun testMapScreenIsDisplayed() {
    composeTestRule.setContent { MapScreen(viewModel) }
    composeTestRule.onNodeWithTag(MapScreenTestTags.GOOGLE_MAP_SCREEN).assertExists()
  }

  @Test
  fun testRefreshUIState_updatesLocations() {
    composeTestRule.setContent { MapScreen(viewModel) }
    viewModel.refreshUIState()
    val hazards = viewModel.uiState.value.hazards

    composeTestRule.waitUntil(timeoutMillis = 3000) { hazards.isNotEmpty() }
  }
}
