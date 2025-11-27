package com.github.warnastrophy.core.ui.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.warnastrophy.core.ui.components.StandardDashboardCard
import com.github.warnastrophy.core.ui.theme.extendedColors

object MapPreviewTestTags {
  const val PLACEHOLDER = "mapPreviewPlaceholder"
  const val MAP_CONTENT = "mapPreviewMapContent"
}

/*
This Composable displays a dashboard card with a map preview.
It features a placeholder background and a marker indicating the user's location.
 */
@Composable
fun MapPreviewCard(modifier: Modifier = Modifier, mapContent: (@Composable () -> Unit)? = null) {
  val extendedColors = MaterialTheme.extendedColors

  StandardDashboardCard(
      backgroundColor = Color.White, minHeight = 50.dp, maxHeight = 100.dp, modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
          Box(modifier = Modifier.matchParentSize()) {
            if (mapContent != null) {
              Box(modifier = Modifier.testTag(MapPreviewTestTags.MAP_CONTENT)) { mapContent() }
            } else {
              Box(
                  modifier =
                      Modifier.matchParentSize()
                          .background(extendedColors.mapPreview.background)
                          .testTag(MapPreviewTestTags.PLACEHOLDER))

              Column(
                  modifier = Modifier.align(Alignment.Center),
                  horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier =
                            Modifier.size(16.dp)
                                .background(extendedColors.mapPreview.mapMarker, CircleShape)
                                .border(width = 2.dp, color = Color.White, shape = CircleShape))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "You",
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium)
                  }
            }
          }
        }
      }
}
