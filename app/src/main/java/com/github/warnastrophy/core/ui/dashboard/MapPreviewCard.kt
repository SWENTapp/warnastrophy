package com.github.warnastrophy.core.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MapPreviewCard() {
  // This is that rounded map preview box with the blue dot.
  Surface(
      shape = RoundedCornerShape(12.dp),
      tonalElevation = 1.dp,
      color = Color.White,
      modifier = Modifier.fillMaxWidth().height(140.dp)) {
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
                    text = "You",
                    color = Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium)
              }

          // Top-left "Léogâne" text mock (optional detail)
          Text(
              text = "Léogâne",
              color = Color.Black,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold,
              modifier =
                  Modifier.align(Alignment.TopStart)
                      .padding(8.dp)
                      .background(
                          color = Color.White.copy(alpha = 0.8f), shape = RoundedCornerShape(6.dp))
                      .padding(horizontal = 6.dp, vertical = 2.dp))
        }
      }
}
