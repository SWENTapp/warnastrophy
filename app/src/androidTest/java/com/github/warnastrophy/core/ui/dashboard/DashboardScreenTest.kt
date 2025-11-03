package com.github.warnastrophy.core.ui.dashboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import com.github.warnastrophy.core.ui.util.BaseAndroidComposeTest
import org.junit.Test

class DashboardScreenTest : BaseAndroidComposeTest() {
  @Test
  fun dashboardScreen_rootIsScrollable() {
    composeTestRule.setContent { MaterialTheme { DashboardScreen() } }

    // ROOT_SCROLL must expose scroll semantics because of .verticalScroll(...)
    composeTestRule
        .onNode(
            hasScrollAction() and hasTestTag(DashboardScreenTestTags.ROOT_SCROLL),
            useUnmergedTree = true)
        .assertExists()
  }
}
