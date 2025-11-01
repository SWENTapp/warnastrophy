package com.github.warnastrophy.core.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EmergencyContactsCard(modifier: Modifier = Modifier) {
  Surface(
      shape = RoundedCornerShape(12.dp),
      color = Color(0xFFFFFDE7), // pale yellow
      tonalElevation = 0.dp,
      modifier = modifier.requiredHeightIn(min = 140.dp, max = 160.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
          Column {
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
          }

          Spacer(modifier = Modifier.height(12.dp))

          AssistChipPlaceholder(label = "Open")
        }
      }
}

@Composable
fun AssistChipPlaceholder(label: String) {
  Surface(
      shape = RoundedCornerShape(20.dp),
      color = Color(0xFFF5F5F5),
      tonalElevation = 0.dp,
      border = BorderStroke(1.dp, Color(0xFFBDBDBD))) {
        Text(
            text = label,
            color = Color.Black,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
      }
}
