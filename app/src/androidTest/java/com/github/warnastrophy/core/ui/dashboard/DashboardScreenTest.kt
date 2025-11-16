package com.github.warnastrophy.core.ui.dashboard

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import com.github.warnastrophy.core.ui.features.dashboard.DashboardScreen
import com.github.warnastrophy.core.ui.features.dashboard.DashboardScreenTestTags
import com.github.warnastrophy.core.ui.util.BaseAndroidComposeTest
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test

@HiltAndroidTest
class DashboardScreenTest : BaseAndroidComposeTest() {

  @Test
  fun dashboardScreen_rootIsScrollable() {
    composeTestRule.setContent { MaterialTheme { DashboardScreen() } }
    composeTestRule
        .onNode(
            hasScrollAction() and hasTestTag(DashboardScreenTestTags.ROOT_SCROLL),
            useUnmergedTree = true)
        .assertExists()
  }
}
