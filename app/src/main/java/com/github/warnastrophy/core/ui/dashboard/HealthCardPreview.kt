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
fun HealthCardPreview(modifier: Modifier = Modifier) {
  StandardDashboardCard(
      modifier = modifier,
      backgroundColor = Color(0xFFE8F5E9),
      minHeight = 140.dp,
      maxHeight = 160.dp) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
          Text(
              text = "Health",
              color = Color(0xFF1B5E20),
              fontWeight = FontWeight.SemiBold,
              fontSize = 15.sp)

          Spacer(modifier = Modifier.height(4.dp))

          Text(
              text = "Medical info,\nallergies, meds",
              color = Color(0xFF1B5E20),
              fontSize = 13.sp,
              lineHeight = 16.sp)

          Spacer(modifier = Modifier.height(12.dp))

          StandardDashboardButton(label = "Open")
        }
      }
}
