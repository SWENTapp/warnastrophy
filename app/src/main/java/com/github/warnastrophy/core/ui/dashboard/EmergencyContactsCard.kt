package com.github.warnastrophy.core.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.warnastrophy.core.ui.components.StandardDashboardButton
import com.github.warnastrophy.core.ui.components.StandardDashboardCard

@Composable
fun EmergencyContactsCard(modifier: Modifier = Modifier) {
  StandardDashboardCard(
      modifier = modifier,
      backgroundColor = Color(0xFFFFFDE7),
      minHeight = 140.dp,
      maxHeight = 160.dp) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
          Text(
              text = "Emergency Contacts",
              color = Color(0xFF5D4037),
              fontWeight = FontWeight.SemiBold,
              fontSize = 15.sp)

          Spacer(modifier = Modifier.height(4.dp))

          Text(
              text = "Manage who to notify",
              color = Color(0xFF5D4037),
              fontSize = 13.sp,
              lineHeight = 16.sp)

          Spacer(modifier = Modifier.height(24.dp))

          StandardDashboardButton(label = "Open")
        }
      }
}
