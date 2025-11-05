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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.warnastrophy.core.ui.components.StandardDashboardButton
import com.github.warnastrophy.core.ui.components.StandardDashboardCard
import com.github.warnastrophy.core.ui.theme.MainAppTheme

object DangerModeTestTags {
  const val CARD = "dangerModeCard"
  const val SWITCH = "dangerModeSwitch"
  const val TITLE = "dangerModeTitle"
  const val MODE_LABEL = "dangerModeCurrentMode"
  const val SENDS_ROW = "dangerModeSendsRow"
  const val COLOR_ROW = "dangerModeColorRow"
  const val OPEN_BUTTON = "dangerModeOpenBtn"
  const val MODE_DROPDOWN_ITEM = "dangerModeDropdownItem"

  const val CONTACT_BUTTON = "dangerModeContactButton"
}

/*
This Composable displays a dashboard card for Danger Mode settings.
It uses a light red background color and darker red accents to indicate caution.
The card includes a title, a switch to enable/disable danger mode, current mode label,
options for what to send in danger mode, color presets for danger levels, and an "Open
" button.
 */
@Composable
fun DangerModeCard(
    modifier: Modifier = Modifier,
    viewModel: DangerModeCardViewModel = viewModel()
) {
  val isDangerModeEnabled = viewModel.isDangerModeEnabled
  val currentModeName = viewModel.currentModeName
  val capabilities by viewModel.capabilities.collectAsState()
  val colorScheme = MaterialTheme.colorScheme

  StandardDashboardCard(
      modifier = Modifier.fillMaxWidth().testTag(DangerModeTestTags.CARD),
      backgroundColor = colorScheme.error,
      borderColor = colorScheme.errorContainer,
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
          modifier = Modifier.fillMaxWidth().offset(y = (-10).dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.testTag(DangerModeTestTags.TITLE),
                text = "Danger Mode",
                color = colorScheme.onError,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp)

            Switch(
                checked = isDangerModeEnabled,
                onCheckedChange = { viewModel.onDangerModeToggled(it) },
                modifier = Modifier.testTag(DangerModeTestTags.SWITCH))
          }
      Column() {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(text = "Mode", color = colorScheme.onError, fontSize = 13.sp)
          var expanded by remember { mutableStateOf(false) }
          Spacer(modifier = Modifier.width(20.dp))
          Box(modifier = Modifier.wrapContentSize(Alignment.TopStart)) {
            Row(
                modifier =
                    Modifier.testTag(DangerModeTestTags.MODE_LABEL).clickable { expanded = true },
                verticalAlignment = Alignment.CenterVertically) {
                  Text(
                      text = currentModeName.label, // Use ViewModel's currentModeName
                      color = Color.Black,
                      fontSize = 15.sp,
                      fontWeight = FontWeight.Medium)
                  Icon(Icons.Filled.ArrowDropDown, contentDescription = "Dropdown indicator")
                }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.testTag(DangerModeTestTags.MODE_LABEL)) {
                  DangerModePreset.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.label) },
                        onClick = {
                          viewModel.onModeSelected(mode)
                          expanded = false
                        },
                        modifier =
                            Modifier.testTag(
                                "${DangerModeTestTags.MODE_DROPDOWN_ITEM}_${mode.label}"))
                  }
                }
          }
        }
        Spacer(modifier = Modifier.width(24.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.testTag(DangerModeTestTags.SENDS_ROW)) {
              Text(text = "Sends", color = colorScheme.onError, fontSize = 13.sp)
              Spacer(modifier = Modifier.width(20.dp))
              Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DangerModeCapability.entries.forEach { capability ->
                  val (color, textColor) =
                      if (capabilities.contains(capability)) {
                        Pair(colorScheme.secondaryContainer, colorScheme.onSecondaryContainer)
                      } else {
                        Pair(colorScheme.error, colorScheme.onError)
                      }

                  StandardDashboardButton(
                      color = color,
                      textColor = textColor,
                      label = capability.label,
                      onClick = { viewModel.onCapabilityToggled(capability) },
                      modifier = Modifier.testTag(DangerModeTestTags.CONTACT_BUTTON))
                }
              }
            }
      }

      Spacer(modifier = Modifier.height(4.dp))
      Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.testTag(DangerModeTestTags.COLOR_ROW)) {
            DangerColorBox(Color(0xFF4CAF50)) // green
            DangerColorBox(Color(0xFFFFEB3B)) // yellow
            DangerColorBox(Color(0xFFFFC107)) // amber-ish
            DangerColorBox(Color(0xFFD32F2F)) // red
      }
      Spacer(Modifier.height(8.dp))

      Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(40.dp), // TODO: Why 40?
            color = colorScheme.surfaceVariant,
            tonalElevation = 0.dp,
            modifier =
                Modifier.fillMaxWidth(0.9f).height(36.dp).testTag(DangerModeTestTags.OPEN_BUTTON),
        ) {
          Box(
              modifier =
                  Modifier.width(20.dp).clickable {
                    // TODO: handle click
                  },
              contentAlignment = Alignment.Center) {
                Text(
                    text = "Open",
                    color = colorScheme.onSurfaceVariant,
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
              width = 2.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
  ) {}
}

@Preview
@Composable
fun DangerModeCardLightPreview() {
  MainAppTheme(darkTheme = false) { DangerModeCard() }
}

@Preview
@Composable
fun DangerModeCardDarkPreview() {
  MainAppTheme(darkTheme = true) { DangerModeCard() }
}
