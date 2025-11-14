package com.github.warnastrophy.core.ui.features.dashboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import com.github.warnastrophy.core.util.BaseAndroidComposeTest
import com.github.warnastrophy.core.util.HazardServiceMock
import org.junit.Test

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
}
