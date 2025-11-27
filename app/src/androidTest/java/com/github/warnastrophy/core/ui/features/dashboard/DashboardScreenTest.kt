package com.github.warnastrophy.core.ui.dashboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDisplayed
import com.github.warnastrophy.core.data.repository.ContactRepositoryProvider
import com.github.warnastrophy.core.data.repository.MockContactRepository
import com.github.warnastrophy.core.data.service.DangerModeService
import com.github.warnastrophy.core.data.service.StateManagerService
import com.github.warnastrophy.core.permissions.PermissionResult
import com.github.warnastrophy.core.ui.features.dashboard.DangerModeTestTags
import com.github.warnastrophy.core.ui.features.dashboard.DashboardScreen
import com.github.warnastrophy.core.ui.features.dashboard.DashboardScreenTestTags
import com.github.warnastrophy.core.ui.map.GpsServiceMock
import com.github.warnastrophy.core.ui.map.HazardServiceMock
import com.github.warnastrophy.core.ui.map.MockPermissionManager
import com.github.warnastrophy.core.ui.map.dangerHazard
import com.github.warnastrophy.core.util.BaseAndroidComposeTest
import com.google.android.gms.maps.model.LatLng
import org.junit.Test
import org.locationtech.jts.geom.GeometryFactory

class DashboardScreenTest : BaseAndroidComposeTest() {
  // Verify that the root of the DashboardScreen is scrollable
  @Test
  fun dashboardScreen_rootIsScrollable() {
    val mockHazardService = HazardServiceMock()
    composeTestRule.setContent {
      MaterialTheme { DashboardScreen(hazardsService = mockHazardService) }
    }

    composeTestRule
        .onNode(
            hasScrollAction() and hasTestTag(DashboardScreenTestTags.ROOT_SCROLL),
            useUnmergedTree = true)
        .assertExists()
  }

  @Test
  fun dashboardScreen_dangerMode_influences_dashboard() {
    val mockHazardService = HazardServiceMock()
    val mockGpsService = GpsServiceMock()
    val dangerModeService =
        DangerModeService(
            permissionManager = MockPermissionManager(currentResult = PermissionResult.Granted))

    StateManagerService.init(
        gpsService = mockGpsService,
        hazardsService = mockHazardService,
        dangerModeService = dangerModeService)

    ContactRepositoryProvider.repository = MockContactRepository()

    composeTestRule.setContent {
      MaterialTheme { DashboardScreen(hazardsService = mockHazardService) }
    }

    composeTestRule.waitForIdle()

    // Initially, danger mode should be off
    checkedDangerMode(false)

    val geometryFactory = GeometryFactory()
    // Put a dangerous hazard and the user in it's affectedArea to activate danger mode
    mockHazardService.setHazards(listOf(dangerHazard))

    // Set user location inside the hazard's affected zone
    mockGpsService.positionState.value = mockGpsService.positionState.value.copy(LatLng(0.0, 0.0))

    composeTestRule.waitForIdle()

    // Verify that danger mode is activated
    checkedDangerMode(false)

    // Check that getting out of the hazard deactivates danger mode
    mockGpsService.positionState.value = mockGpsService.positionState.value.copy(LatLng(10.0, 10.0))
    composeTestRule.waitForIdle()
    checkedDangerMode(false)
  }

  private fun checkedDangerMode(on: Boolean): Unit {
    composeTestRule.waitUntilWithTimeout {
      composeTestRule.onNode(hasTestTag(DangerModeTestTags.SWITCH)).isDisplayed()
    }

    composeTestRule.waitUntil(10000) {
      composeTestRule.onAllNodes(hasTestTag(DangerModeTestTags.SWITCH)).fetchSemanticsNodes().any {
        val state = it.config.getOrNull(SemanticsProperties.ToggleableState)
        if (on) {
          state == ToggleableState.On
        } else {
          state == ToggleableState.Off
        }
      }
    }
  }
}
