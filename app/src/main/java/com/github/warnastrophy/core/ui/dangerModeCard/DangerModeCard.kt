package com.github.warnastrophy.core.ui.dangerModeCard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.warnastrophy.core.ui.components.StandardDashboardButton
import com.github.warnastrophy.core.ui.components.StandardDashboardCard

object DangerModeTestTags {
  const val CARD = "dangerModeCard"
  const val SWITCH = "dangerModeSwitch"
  const val TITLE = "dangerModeTitle"
  const val MODE_LABEL = "dangerModeCurrentMode"
  const val SENDS_ROW = "dangerModeSendsRow"
  const val COLOR_ROW = "dangerModeColorRow"
  const val OPEN_BUTTON = "dangerModeOpenBtn"

  const val CONTACT_BUTTON = "dangerModeContactButton"
}

object DangerModeCardColors {
  val BACKGROUND_COLOR: Color = Color(0xFFFFEBEE) // light red
  val BORDER_COLOR: Color = Color(0xFFE57373) // darker red

  val TITLE_COLOR: Color = Color(0xFF424242) // dark grey

  val MODE_COLOR: Color = Color(0xFF757575) // medium grey

  val BUTTON_TEXT: Color = Color(0xFFC6E3C6) // light green

  val DANGER_COLOR_GREEN: Color = Color(0xFF4CAF50) // green

  val DANGER_COLOR_YELLOW: Color = Color(0xFFFFEB3B) // yellow
  val DANGER_COLOR_AMBER: Color = Color(0xFFFFC107) // amber-ish
  val DANGER_COLOR_RED: Color = Color(0xFFD32F2F) // red

  val OPEN_BUTTON_BORDER: Color = Color(0xFFE0E0E0) // light grey

  val OPEN_BUTTON_COLOR: Color = Color(0xFF757575) // medium grey

  val DANGER_BUTTON_BORDER: Color = Color(0xFF424242) // dark grey
}

/*
This Composable displays a dashboard card for Danger Mode settings.
It uses a light red background color and darker red accents to indicate caution.
The card includes a title, a switch to enable/disable danger mode, current mode label,
options for what to send in danger mode, color presets for danger levels, and an "Open
" button.
 */
@Composable
fun DangerModeCard(modifier: Modifier = Modifier) {
  var dangerEnabled by remember { mutableStateOf(false) }

  StandardDashboardCard(
      modifier = Modifier.fillMaxWidth().testTag(DangerModeTestTags.CARD),
      backgroundColor = DangerModeCardColors.BACKGROUND_COLOR,
      borderColor = DangerModeCardColors.BORDER_COLOR) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
              modifier = Modifier.fillMaxWidth().offset(y = (-10).dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.testTag(DangerModeTestTags.TITLE),
                    text = "Danger Mode",
                    color = DangerModeCardColors.TITLE_COLOR,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp)

                Switch(
                    checked = dangerEnabled,
                    onCheckedChange = { dangerEnabled = it },
                    modifier = Modifier.testTag(DangerModeTestTags.SWITCH))
              }
          Column() {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(text = "Mode", color = DangerModeCardColors.MODE_COLOR, fontSize = 13.sp)
              Spacer(modifier = Modifier.width(25.dp))
              Text(
                  text = "Climbing mode",
                  color = Color.Black,
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Medium,
                  modifier = Modifier.testTag(DangerModeTestTags.MODE_LABEL))
            }
            Spacer(modifier = Modifier.width(24.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.testTag(DangerModeTestTags.SENDS_ROW)) {
                  Text(text = "Sends", color = DangerModeCardColors.MODE_COLOR, fontSize = 13.sp)
                  Spacer(modifier = Modifier.width(20.dp))
                  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StandardDashboardButton(
                        color = DangerModeCardColors.BUTTON_TEXT,
                        "Call",
                        modifier = Modifier.testTag(DangerModeTestTags.CONTACT_BUTTON))
                    StandardDashboardButton(
                        color = DangerModeCardColors.BUTTON_TEXT,
                        "SMS",
                        modifier = Modifier.testTag(DangerModeTestTags.CONTACT_BUTTON))
                    StandardDashboardButton(
                        color = DangerModeCardColors.BUTTON_TEXT,
                        "Location",
                        modifier = Modifier.testTag(DangerModeTestTags.CONTACT_BUTTON))
                  }
                }
          }

          Spacer(modifier = Modifier.height(4.dp))
          Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.testTag(DangerModeTestTags.COLOR_ROW)) {
                DangerColorBox(DangerModeCardColors.DANGER_COLOR_GREEN)
                DangerColorBox(DangerModeCardColors.DANGER_COLOR_YELLOW)
                DangerColorBox(DangerModeCardColors.DANGER_COLOR_AMBER)
                DangerColorBox(DangerModeCardColors.DANGER_COLOR_RED)
              }
          Spacer(Modifier.height(8.dp))

          Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Surface(
                shape = RoundedCornerShape(40.dp),
                color = DangerModeCardColors.OPEN_BUTTON_BORDER,
                tonalElevation = 0.dp,
                modifier =
                    Modifier.fillMaxWidth(0.9f)
                        .height(36.dp)
                        .testTag(DangerModeTestTags.OPEN_BUTTON),
            ) {
              Box(
                  modifier =
                      Modifier.width(20.dp).clickable {
                        // TODO: handle click
                      },
                  contentAlignment = Alignment.Center) {
                    Text(
                        text = "Open",
                        color = DangerModeCardColors.OPEN_BUTTON_COLOR,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp)
                  }
            }
          }
        }
      }
}

@Composable
private fun DangerColorBox(color: Color, onClick: () -> Unit = {}) {
  Surface(
      shape = RoundedCornerShape(6.dp),
      color = color,
      onClick = onClick,
      modifier = Modifier.size(width = 28.dp, height = 28.dp),
      border =
          BorderStroke(
              width = 2.dp, color = DangerModeCardColors.DANGER_BUTTON_BORDER.copy(alpha = 0.2f)),
  ) {}
}
