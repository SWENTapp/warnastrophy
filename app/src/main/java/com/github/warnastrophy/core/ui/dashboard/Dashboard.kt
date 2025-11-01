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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.warnastrophy.core.ui.theme.MainAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {
  Scaffold(
      containerColor = Color(0xFFF5F5F5) // light neutral behind cards
      ) { innerPadding ->
        Column(
            modifier =
                Modifier.padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .background(Color(0xFFF5F5F5))) {
              SafeZoneTopBar()

              Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                LatestNewsCard()

                Spacer(modifier = Modifier.height(12.dp))

                MapPreviewCard()

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                      EmergencyContactsCard(modifier = Modifier.weight(1f))
                      HealthCardPreview(modifier = Modifier.weight(1f))
                    }

                Spacer(modifier = Modifier.height(12.dp))

                DangerModeCard()

                Spacer(modifier = Modifier.height(80.dp)) // breathing room above bottom bar
              }
            }
      }
}

@Preview
@Composable
fun DashboardScreenPreview() {
  MainAppTheme { DashboardScreen() }
}
