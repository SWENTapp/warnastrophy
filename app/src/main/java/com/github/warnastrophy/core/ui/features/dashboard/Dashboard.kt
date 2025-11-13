package com.github.warnastrophy.core.ui.features.dashboard

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
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.github.warnastrophy.core.domain.model.HazardsDataService
import com.github.warnastrophy.core.ui.layout.SafeZoneTopBar

object DashboardScreenTestTags {
  const val ROOT_SCROLL = "dashboard_rootScroll"
  const val TOP_BAR = "dashboard_topBar"
  const val LATEST_NEWS_SECTION = "dashboard_latestNewsSection"
  const val MAP_PREVIEW_SECTION = "dashboard_mapPreviewSection"
  const val ROW_TWO_SMALL_CARDS = "dashboard_twoSmallCardsRow"
  const val DANGER_MODE_SECTION = "dashboard_dangerModeSection"
}

object DashboardColors {
  val BACKGROUND_COLOR: Color = Color(0xFFF5F5F5) // Light Grey
}

@Composable
fun DashboardScreen(
    mapScreen: (@Composable () -> Unit)? = null,
    onHealthCardClick: () -> Unit = {},
    onEmergencyContactsClick: () -> Unit = {},
    hazardsService: HazardsDataService
) {
  Scaffold(containerColor = DashboardColors.BACKGROUND_COLOR) { innerPadding ->
    Column(
        modifier =
            Modifier.padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(DashboardColors.BACKGROUND_COLOR)
                .testTag(DashboardScreenTestTags.ROOT_SCROLL)) {
          SafeZoneTopBar(modifier = Modifier.testTag(DashboardScreenTestTags.TOP_BAR))

          Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            LatestNewsCard(
                hazardsService = hazardsService,
                modifier = Modifier.testTag(DashboardScreenTestTags.LATEST_NEWS_SECTION))

            Spacer(modifier = Modifier.height(12.dp))

            MapPreviewCard(
                modifier = Modifier.testTag(DashboardScreenTestTags.MAP_PREVIEW_SECTION),
                mapContent = mapScreen)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier =
                    Modifier.fillMaxWidth().testTag(DashboardScreenTestTags.ROW_TWO_SMALL_CARDS),
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                  DashboardEmergencyContactsCardStateful(
                      onManageContactsClick = { onEmergencyContactsClick() },
                      modifier =
                          Modifier.weight(1f).testTag(DashboardEmergencyContactsTestTags.CARD))
                  DashboardHealthCardStateful(
                      onHealthCardClick = { onHealthCardClick() },
                      modifier = Modifier.testTag(DashboardHealthCardTestTags.CARD).weight(0.78f))
                }

            Spacer(modifier = Modifier.height(12.dp))

            DangerModeCard(modifier = Modifier.testTag(DashboardScreenTestTags.DANGER_MODE_SECTION))

            Spacer(modifier = Modifier.height(80.dp))
          }
        }
  }
}
