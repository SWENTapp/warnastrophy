package com.github.warnastrophy.core.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.warnastrophy.core.ui.safeZoneTopBar.SafeZoneTopBar
import com.github.warnastrophy.core.ui.theme.MainAppTheme

object DashboardScreenTestTags {
  const val ROOT_SCROLL = "dashboard_rootScroll"
  const val TOP_BAR = "dashboard_topBar"
  const val LATEST_NEWS_SECTION = "dashboard_latestNewsSection"
  const val MAP_PREVIEW_SECTION = "dashboard_mapPreviewSection"
  const val ROW_TWO_SMALL_CARDS = "dashboard_twoSmallCardsRow"
  const val DANGER_MODE_SECTION = "dashboard_dangerModeSection"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {
  Scaffold(containerColor = Color.LightGray) { innerPadding ->
    Column(
        modifier =
            Modifier.padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(Color.LightGray)
                .testTag(DashboardScreenTestTags.ROOT_SCROLL)) {
          SafeZoneTopBar(modifier = Modifier.testTag(DashboardScreenTestTags.TOP_BAR))

          Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            LatestNewsCard(modifier = Modifier.testTag(DashboardScreenTestTags.LATEST_NEWS_SECTION))

            Spacer(modifier = Modifier.height(12.dp))

            MapPreviewCard(modifier = Modifier.testTag(DashboardScreenTestTags.MAP_PREVIEW_SECTION))

            Row(
                modifier =
                    Modifier.fillMaxWidth().testTag(DashboardScreenTestTags.ROW_TWO_SMALL_CARDS),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                  EmergencyContactsCard(
                      modifier = Modifier.weight(1f).testTag(EmergencyContactsTestTags.CARD))
                  HealthCardPreview(
                      modifier = Modifier.testTag(HealthCardPreviewTestTags.CARD).weight(1f))
                }
          }
        }
  }
}

@Preview
@Composable
private fun DashboardScreenPreview() {
  MainAppTheme { DashboardScreen() }
}
