package com.github.warnastrophy.core.ui.dashboard

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
fun HealthCardPreview(modifier: Modifier = Modifier) {
  Surface(
      shape = RoundedCornerShape(12.dp),
      color = Color(0xFFE8F5E9), // pale green
      tonalElevation = 0.dp,
      modifier = modifier.requiredHeightIn(min = 140.dp, max = 160.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
          Column {
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
          }

          Spacer(modifier = Modifier.height(12.dp))

          AssistChipPlaceholder(label = "Open")
        }
      }
}
