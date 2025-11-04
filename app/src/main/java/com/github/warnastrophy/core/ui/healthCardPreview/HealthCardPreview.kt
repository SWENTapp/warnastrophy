package com.github.warnastrophy.core.ui.healthCardPreview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.warnastrophy.core.ui.components.StandardDashboardButton
import com.github.warnastrophy.core.ui.components.StandardDashboardCard

object HealthCardPreviewTestTags {
  const val CARD = "healthCard"
  const val TITLE = "healthCardTitle"
  const val SUBTITLE = "healthCardSubtitle"
  const val OPEN_BUTTON = "healthCardOpenButton"
}

object HealthCardPreviewColors {
  val BACKGROUND_COLOR: Color = Color(0xFFE8F5E9) // light green
  val TEXT_COLOR: Color = Color(0xFF1B5E20) // dark green
}

/**
 * This Composable displays a dashboard card for Health information. It uses a light green
 * background color and dark green text to indicate health and wellness. The card includes a title,
 * subtitle, and an "Open" button for accessing health details.
 */
@Composable
fun HealthCardPreview(modifier: Modifier = Modifier) {
  StandardDashboardCard(
      modifier = modifier.testTag(HealthCardPreviewTestTags.CARD),
      backgroundColor = HealthCardPreviewColors.BACKGROUND_COLOR,
      minHeight = 120.dp,
      maxHeight = 140.dp) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
          Text(
              text = "Health",
              modifier = Modifier.testTag(HealthCardPreviewTestTags.TITLE),
              color = HealthCardPreviewColors.TEXT_COLOR,
              fontWeight = FontWeight.SemiBold,
              fontSize = 15.sp)

          Spacer(modifier = Modifier.height(4.dp))

          Text(
              text = "Medical info,\nallergies, meds",
              color = HealthCardPreviewColors.TEXT_COLOR,
              fontSize = 13.sp,
              modifier = Modifier.testTag(HealthCardPreviewTestTags.SUBTITLE),
              lineHeight = 16.sp)

          Spacer(Modifier.weight(1f))

          Box(modifier = Modifier.testTag(HealthCardPreviewTestTags.OPEN_BUTTON)) {
            StandardDashboardButton(label = "Open")
          }
        }
      }
}
