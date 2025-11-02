package com.github.warnastrophy.core.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.warnastrophy.core.ui.components.StandardDashboardCard

@Composable
fun MapPreviewCard() {
  StandardDashboardCard(backgroundColor = Color.White, minHeight = 140.dp, maxHeight = 160.dp) {
    Box(modifier = Modifier.fillMaxSize()) {
      // Placeholder background
      Box(modifier = Modifier.matchParentSize().background(Color(0xFFE0E0E0)))

      // Fake "you are here" marker
      Column(
          modifier = Modifier.align(Alignment.Center),
          horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier =
                    Modifier.size(16.dp)
                        .background(Color(0xFF1E88E5), CircleShape)
                        .border(width = 2.dp, color = Color.White, shape = CircleShape))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "You", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Medium)
          }
    }
  }
}
