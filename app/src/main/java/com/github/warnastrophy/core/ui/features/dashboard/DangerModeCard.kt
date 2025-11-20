package com.github.warnastrophy.core.ui.features.dashboard

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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.warnastrophy.core.ui.components.StandardDashboardButton
import com.github.warnastrophy.core.ui.components.StandardDashboardCard

object DangerModeTestTags {
  const val CARD = "dangerModeCard"
  const val SWITCH = "dangerModeSwitch"
  const val TITLE = "dangerModeTitle"
  const val MODE_LABEL = "dangerModeCurrentMode"
  const val SENDS_ROW = "dangerModeSendsRow"
  const val COLOR_ROW = "dangerModeColorRow"
  const val CAPABILITY_PREFIX = "dangerModeContactButton"
  const val MODE_PREFIX = "dangerModePresetButton"
  const val COLOR_BOX_PREFIX = "dangerModeColorBox"

  fun capabilityTag(capability: DangerModeCapability) = CAPABILITY_PREFIX + capability.label

  fun modeTag(mode: DangerModePreset) = MODE_PREFIX + mode.label

  fun dangerLevelTag(level: Int) = COLOR_BOX_PREFIX + level
}

/**
 * This Composable displays a dashboard card for Danger Mode settings. It uses a light red
 * background color and darker red accents to indicate caution. The card includes a title, a switch
 * to enable/disable danger mode, current mode label, options for what to send in danger mode, color
 * presets for danger levels, and an "Open" button.
 *
 * @param modifier Modifier to be applied to the card.
 * @param viewModel The ViewModel managing the state of the Danger Mode card.
 * @param onOpenClick Lambda function to be invoked when the "Open" button is clicked
 */
@Composable
fun DangerModeCard(
    modifier: Modifier = Modifier,
    viewModel: DangerModeCardViewModel = viewModel(),
    onOpenClick: () -> Unit = {}
) {
  val isDangerModeEnabled by viewModel.isDangerModeEnabled.collectAsState()
  val currentModeName by viewModel.currentMode.collectAsState()
  val capabilities by viewModel.capabilities.collectAsState()
  val dangerLevel by viewModel.dangerLevel.collectAsState()
  val colorScheme = MaterialTheme.colorScheme

  StandardDashboardCard(
      modifier = modifier.fillMaxWidth().testTag(DangerModeTestTags.CARD),
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
                  StandardDashboardButton(
                      color = colorScheme.error,
                      label = currentModeName.label,
                      onClick = { expanded = true },
                      textColor = colorScheme.onError,
                      icon = {
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "Dropdown Arrow",
                            tint = colorScheme.onError)
                      })
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
                        modifier = Modifier.testTag(DangerModeTestTags.modeTag(mode)))
                  }
                }
          }
        }
        Spacer(modifier = Modifier.width(24.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.testTag(DangerModeTestTags.COLOR_ROW)) {
              Text(text = "Danger Level", color = colorScheme.onError, fontSize = 13.sp)
              listOf(
                      Color(0xFF4CAF50), // green
                      Color(0xFFFFEB3B), // yellow
                      Color(0xFFFFC107), // amber-ish
                      Color(0xFFD32F2F) // red
                      )
                  .forEachIndexed { index, it ->
                    val alpha = if (index == dangerLevel) 1f else 0.1f
                    DangerColorBox(
                        it,
                        modifier = Modifier.testTag(DangerModeTestTags.dangerLevelTag(index)),
                        onClick = { viewModel.onDangerLevelChanged(index) },
                        borderColor = colorScheme.onError.copy(alpha = alpha))
                  }
            }
      }

      Spacer(modifier = Modifier.height(4.dp))
      Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.testTag(DangerModeTestTags.SENDS_ROW)) {
            Text(text = "Sends", color = colorScheme.onError, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              DangerModeCapability.entries.forEach { capability ->
                val selected = capabilities.contains(capability)
                val (color, textColor) =
                    if (selected) {
                      Pair(colorScheme.secondaryContainer, colorScheme.onSecondaryContainer)
                    } else {
                      Pair(colorScheme.error, colorScheme.onError)
                    }

                StandardDashboardButton(
                    color = color,
                    textColor = textColor,
                    label = capability.label,
                    onClick = { viewModel.onCapabilityToggled(capability) },
                    modifier =
                        Modifier.testTag(DangerModeTestTags.capabilityTag(capability)).semantics {
                          this.selected = selected
                        })
              }
            }
          }
    }
  }
}

/**
 * A small box representing a danger level color option.
 *
 * @param color The background color of the box.
 * @param modifier Modifier to be applied to the box.
 * @param onClick Lambda function to be invoked when the box is clicked.
 * @param borderColor The color of the border around the box.
 */
@Composable
private fun DangerColorBox(
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    borderColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
) {
  Surface(
      shape = RoundedCornerShape(6.dp),
      color = color,
      onClick = onClick,
      modifier = modifier.size(width = 28.dp, height = 28.dp),
      border = BorderStroke(width = 2.dp, color = borderColor),
  ) {}
}
