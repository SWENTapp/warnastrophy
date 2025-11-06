package com.github.warnastrophy.core.ui.dashboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import com.github.warnastrophy.core.ui.map.HazardServiceMock
import com.github.warnastrophy.core.ui.util.BaseAndroidComposeTest
import org.junit.Test

class DashboardScreenTest : BaseAndroidComposeTest() {
  // Verify that the root of the DashboardScreen is scrollable
  @Test
  fun dashboardScreen_rootIsScrollable() {
    val hazardsService = HazardServiceMock()
    composeTestRule.setContent { MaterialTheme { DashboardScreen(hazardsService) } }

    composeTestRule
        .onNode(
            hasScrollAction() and hasTestTag(DashboardScreenTestTags.ROOT_SCROLL),
            useUnmergedTree = true)
        .assertExists()
  }
}
