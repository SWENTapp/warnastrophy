package com.github.warnastrophy.core.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DangerModeCard() {
  var dangerEnabled by remember { mutableStateOf(false) }

  Surface(
      shape = RoundedCornerShape(12.dp),
      color = Color(0xFFFFEBEE), // pale pink/red background
      border = BorderStroke(1.dp, Color(0xFFE57373)),
      tonalElevation = 0.dp,
      modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {

          // Header row with title + switch
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Danger Mode",
                    color = Color(0xFF424242),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp)

                Switch(checked = dangerEnabled, onCheckedChange = { dangerEnabled = it })
              }

          Spacer(modifier = Modifier.height(12.dp))

          // Mode label (e.g. "Climbing mode")
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(text = "Mode", color = Color(0xFF757575), fontSize = 13.sp)

              Spacer(modifier = Modifier.width(4.dp))

              Text(
                  text = "Climbing mode",
                  color = Color.Black,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Medium)
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // "Sends" row (chips: Call / SMS / Location)
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(text = "Sends", color = Color(0xFF757575), fontSize = 13.sp)

              Spacer(modifier = Modifier.width(8.dp))

              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniTagChip("Call")
                MiniTagChip("SMS")
                MiniTagChip("Location")
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Color squares row (danger level presets)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DangerColorBox(Color(0xFF4CAF50)) // green
            DangerColorBox(Color(0xFFFFEB3B)) // yellow
            DangerColorBox(Color(0xFFFFC107)) // amber-ish
            DangerColorBox(Color(0xFFD32F2F)) // red
          }

          Spacer(modifier = Modifier.height(16.dp))

          // "Open" big button placeholder
          Surface(
              shape = RoundedCornerShape(20.dp),
              color = Color(0xFFE0E0E0),
              tonalElevation = 0.dp,
              modifier = Modifier.fillMaxWidth().height(36.dp),
          ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Text(
                  text = "Open",
                  color = Color(0xFF757575),
                  fontWeight = FontWeight.Medium,
                  fontSize = 14.sp)
            }
          }
        }
      }
}

@Composable
private fun MiniTagChip(text: String) {
  Surface(
      shape = RoundedCornerShape(20.dp),
      color = Color.White,
      border = BorderStroke(1.dp, Color(0xFFBDBDBD)),
      tonalElevation = 0.dp) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = Color.Black,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
      }
}

@Composable
private fun DangerColorBox(color: Color) {
  Surface(
      shape = RoundedCornerShape(6.dp),
      color = color,
      modifier = Modifier.size(width = 28.dp, height = 28.dp),
      border = BorderStroke(width = 2.dp, color = Color(0xFF424242).copy(alpha = 0.2f))) {}
}
