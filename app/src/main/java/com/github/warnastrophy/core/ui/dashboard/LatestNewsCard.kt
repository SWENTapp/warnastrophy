package com.github.warnastrophy.core.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LatestNewsCard() {
  Column(
      modifier =
          Modifier.fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .border(
                  width = 1.dp,
                  color = Color(0xFFBDBDBD).copy(alpha = 0.4f),
                  shape = RoundedCornerShape(12.dp))) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .background(Color(0xFFFFEBEE))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
              Text(
                  text = "Latest news",
                  color = Color(0xFFD32F2F),
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp)

              Text(text = "2 min ago • Weather", color = Color(0xFF616161), fontSize = 12.sp)
            }

        Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFF6F4F4)).padding(16.dp)) {
          Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                      text = "Tropical cyclone just hit Jamaica",
                      color = Color.Black,
                      fontWeight = FontWeight.SemiBold,
                      fontSize = 16.sp,
                      maxLines = 2,
                      overflow = TextOverflow.Ellipsis)

                  Spacer(modifier = Modifier.height(4.dp))

                  Text(
                      text = "Authorities report strong winds and heavy rainfall.",
                      color = Color.DarkGray,
                      fontSize = 13.sp,
                      lineHeight = 16.sp,
                      maxLines = 3,
                      overflow = TextOverflow.Ellipsis)
                }

                Box(
                    modifier =
                        Modifier.size(64.dp)
                            .border(
                                width = 1.dp,
                                color = Color(0xFFBDBDBD),
                                shape = RoundedCornerShape(8.dp))
                            .background(Color.White, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center) {
                      Text(text = "Image", color = Color(0xFF9E9E9E), fontSize = 12.sp)
                    }
              }
        }
      }
}
