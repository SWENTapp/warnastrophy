package com.github.warnastrophy.core.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
public fun SafeZoneTopBar() {
  // green banner "You are in a safe zone"
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .background(Color(0xFF2E7D32)) // deep-ish green
              .padding(vertical = 8.dp),
      contentAlignment = Alignment.Center) {
        Text(
            text = "You are in a safe zone",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp)
      }
}
