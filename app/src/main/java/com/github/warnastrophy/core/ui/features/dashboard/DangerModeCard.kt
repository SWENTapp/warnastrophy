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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.warnastrophy.core.data.service.DangerLevel
import com.github.warnastrophy.core.ui.components.StandardDashboardButton
import com.github.warnastrophy.core.ui.components.StandardDashboardCard
import com.github.warnastrophy.core.ui.navigation.NavigationTestTags
import com.github.warnastrophy.core.ui.theme.extendedColors

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

  const val ADVANCED_SECTION = "dangerModeAdvancedSection"
  const val AUTO_CALL_SWITCH = "dangerModeAutoCallSwitch"
  const val AUTO_MESSAGE_SWITCH = "dangerModeAutoMessageSwitch"
  const val CONFIRM_TOUCH_SWITCH = "dangerModeConfirmTouchSwitch"
  const val CONFIRM_VOICE_SWITCH = "dangerModeConfirmVoiceSwitch"

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
 * @param onManageActivitiesClick Lambda function to be invoked when the "Manage" button is clicked
 */
@Composable
fun DangerModeCard(
    modifier: Modifier = Modifier,
    viewModel: DangerModeCardViewModel = viewModel(),
    onManageActivitiesClick: () -> Unit = {}
) {
  val isDangerModeEnabled by viewModel.isDangerModeEnabled.collectAsState(false)
  val currentModeName by viewModel.currentMode.collectAsState(DangerModePreset.DEFAULT_MODE)
  val capabilities by viewModel.capabilities.collectAsState(emptySet())
  val dangerLevel by viewModel.dangerLevel.collectAsState(DangerLevel.LOW)

  val confirmTouchRequired by viewModel.confirmTouchRequired.collectAsState(false)
  val confirmVoiceRequired by viewModel.confirmVoiceRequired.collectAsState(false)

  val colorScheme = MaterialTheme.colorScheme
  val extendedColors = MaterialTheme.extendedColors

  StandardDashboardCard(
      modifier = modifier.fillMaxWidth().testTag(DangerModeTestTags.CARD),
      backgroundColor = colorScheme.error,
      borderColor = colorScheme.error,
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

            DangerModeSwitch(checked = isDangerModeEnabled, viewModel = viewModel)
          }
      Column {
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
                      label = currentModeName.label,
                      color = colorScheme.errorContainer,
                      onClick = { expanded = true },
                      textColor = colorScheme.onErrorContainer,
                      icon = {
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "Dropdown Arrow",
                            tint = colorScheme.onErrorContainer)
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
          Spacer(modifier = Modifier.width(8.dp))
          StandardDashboardButton(
              label = "Manage Activities",
              color = colorScheme.errorContainer, // You might want a different color
              modifier = Modifier.testTag(NavigationTestTags.BUTTON_MANAGE_ACTIVITY_DANGER_MODE),
              onClick = { onManageActivitiesClick() },
              textColor = colorScheme.onErrorContainer)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.testTag(DangerModeTestTags.COLOR_ROW)) {
              Text(text = "Danger Level", color = colorScheme.onError, fontSize = 13.sp)
              listOf(
                      extendedColors.dangerLevels.green, // green
                      extendedColors.dangerLevels.yellow, // yellow
                      extendedColors.dangerLevels.amber, // amber-ish
                      extendedColors.dangerLevels.red // red
                      )
                  .forEachIndexed { index, it ->
                    val alpha = if (index == dangerLevel.ordinal) 1f else 0.1f
                    DangerColorBox(
                        it,
                        modifier = Modifier.testTag(DangerModeTestTags.dangerLevelTag(index)),
                        onClick = { viewModel.onDangerLevelChanged(DangerLevel.entries[index]) },
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
                    label = capability.label,
                    modifier =
                        Modifier.testTag(DangerModeTestTags.capabilityTag(capability)).semantics {
                          this.selected = selected
                        },
                    color = color,
                    borderColor = colorScheme.onError,
                    onClick = { viewModel.onCapabilityToggled(capability) },
                    textColor = textColor)
              }
            }
          }
      if (capabilities.contains(DangerModeCapability.CALL)) {
        Spacer(modifier = Modifier.height(12.dp))
        DangerModeAdvancedOptionsSection(
            confirmTouchRequired = confirmTouchRequired,
            confirmVoiceRequired = confirmVoiceRequired,
            onConfirmTouchChanged = viewModel::onConfirmTouchChanged,
            onConfirmVoiceChanged = viewModel::onConfirmVoiceChanged,
        )
      }

      Spacer(modifier = Modifier.height(4.dp))
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

@Composable
private fun DangerModeAdvancedOptionsSection(
    confirmTouchRequired: Boolean,
    confirmVoiceRequired: Boolean,
    onConfirmTouchChanged: (Boolean) -> Unit,
    onConfirmVoiceChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
  val colors = MaterialTheme.colorScheme

  Column(modifier = modifier.fillMaxWidth().testTag(DangerModeTestTags.ADVANCED_SECTION)) {
    Text(
        text = "Automatic actions",
        color = colors.onError,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text =
            "When Danger Mode is triggered, calls and messages can be sent automatically. " +
                "Use the options below to choose what happens, and how you confirm it.",
        color = colors.onError.copy(alpha = 0.9f),
        fontSize = 11.sp)

    Spacer(modifier = Modifier.height(8.dp))

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Confirmation methods",
        color = colors.onError,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "For extra control, you can require a quick confirmation before actions are sent.",
        color = colors.onError.copy(alpha = 0.9f),
        fontSize = 11.sp)

    Spacer(modifier = Modifier.height(6.dp))

    // Tactile confirmation
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
      Text(
          text = "Require touch confirmation",
          color = colors.onError,
          fontSize = 13.sp,
          modifier = Modifier.weight(1f))
      Switch(
          checked = confirmTouchRequired,
          onCheckedChange = onConfirmTouchChanged,
          modifier = Modifier.testTag(DangerModeTestTags.CONFIRM_TOUCH_SWITCH))
    }

    // Voice confirmation
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
      Text(
          text = "Allow voice confirmation",
          color = colors.onError,
          fontSize = 13.sp,
          modifier = Modifier.weight(1f))
      Switch(
          checked = confirmVoiceRequired,
          onCheckedChange = onConfirmVoiceChanged,
          modifier = Modifier.testTag(DangerModeTestTags.CONFIRM_VOICE_SWITCH))
    }
  }
}

@Composable
private fun DangerModeSwitch(
    checked: Boolean,
    viewModel: DangerModeCardViewModel,
    modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  Switch(
      checked = checked,
      onCheckedChange = { viewModel.onDangerModeToggled(it, context) },
      modifier = modifier.testTag(DangerModeTestTags.SWITCH))
}
