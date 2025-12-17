package com.github.warnastrophy.core.ui.features.dashboard

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.warnastrophy.R
import com.github.warnastrophy.core.data.service.DangerLevel
import com.github.warnastrophy.core.model.Activity
import com.github.warnastrophy.core.ui.components.ActivityFallback
import com.github.warnastrophy.core.ui.components.StandardDashboardButton
import com.github.warnastrophy.core.ui.components.StandardDashboardCard
import com.github.warnastrophy.core.ui.navigation.NavigationTestTags
import com.github.warnastrophy.core.ui.theme.extendedColors
import com.github.warnastrophy.core.util.findActivity
import com.github.warnastrophy.core.util.openAppSettings
import com.github.warnastrophy.core.util.startForegroundGpsService
import com.github.warnastrophy.core.util.stopForegroundGpsService

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
  const val CONFIRM_TOUCH_SWITCH = "dangerModeConfirmTouchSwitch"
  const val CONFIRM_VOICE_SWITCH = "dangerModeConfirmVoiceSwitch"
  const val AUTO_CALL_SWITCH = "dangerModeAutoActionsSwitch"

  fun capabilityTag(capability: DangerModeCapability) = CAPABILITY_PREFIX + capability.label

  fun activityTag(activityName: String) = MODE_PREFIX + activityName

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
  // Refresh activities whenever this composable is displayed
  LaunchedEffect(Unit) { viewModel.refreshActivities() }

  val context = LocalContext.current
  val activity = context.findActivity()
  if (activity == null) {
    ActivityFallback()
    return
  }

  val launcher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestMultiplePermissions(),
          onResult = { viewModel.onPermissionResult(activity = activity) })

  LaunchedEffect(Unit) {
    viewModel.effects.collect { effect ->
      when (effect) {
        Effect.RequestLocationPermission -> {
          viewModel.onPermissionsRequestStart()
          launcher.launch(viewModel.alertModePermission.permissions)
        }
        Effect.StartForegroundService -> startForegroundGpsService(activity)
        Effect.StopForegroundService -> stopForegroundGpsService(context)
        Effect.ShowOpenAppSettings -> openAppSettings(context)
      }
    }
  }

  val isDangerModeEnabled by viewModel.isDangerModeEnabled.collectAsState(false)

  StandardDashboardCard(
      modifier = modifier.fillMaxWidth().testTag(DangerModeTestTags.CARD),
      backgroundColor = MaterialTheme.colorScheme.error,
      borderColor = MaterialTheme.colorScheme.error,
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      DangerModeHeader(
          isDangerModeEnabled = isDangerModeEnabled,
          onCheckedChange = {
            viewModel.handleToggle(it, viewModel.permissionUiState.value.alertModePermissionResult)
          })

      DangerModeBody(viewModel, onManageActivitiesClick)
    }
  }
}

/**
 * A composable that displays the header for the Danger Mode card. It includes a title and a switch
 * to enable or disable Danger Mode.
 *
 * @param isDangerModeEnabled A boolean indicating whether Danger Mode is currently enabled.
 * @param onCheckedChange A lambda function that is invoked when the switch state changes. It passes
 *   the new boolean state.
 */
@Composable
private fun DangerModeHeader(isDangerModeEnabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
  Row(
      modifier = Modifier.fillMaxWidth().offset(y = (-10).dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically) {
        Text(
            modifier = Modifier.testTag(DangerModeTestTags.TITLE),
            text = stringResource(R.string.danger_mode_card_title),
            color = MaterialTheme.colorScheme.onError,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp)
        DangerModeSwitch(checked = isDangerModeEnabled, onCheckedChange = onCheckedChange)
      }
}

/**
 * This composable function arranges the controls for configuring Danger Mode. It collects various
 * state properties from the [viewModel] (like the current activity, danger level, capabilities,
 * etc.) and displays them. It also provides callbacks to the [viewModel] for user interactions.
 *
 * The layout includes:
 * - An `ActivitySelectionRow` for choosing or managing the current activity preset.
 * - A `DangerLevelRow` to set the danger level, represented by colors.
 * - A `CapabilitiesRow` to toggle what actions are performed (e.g., SMS, Call).
 * - A `DangerModeAdvancedOptionsSection` which is conditionally displayed if the 'CALL' or 'SMS'
 *   capability is enabled, allowing for finer control over automation and confirmation steps.
 *
 * @param viewModel The ViewModel that provides state for the UI and handles user events.
 * @param onManageActivitiesClick A lambda function to be invoked when the user clicks the "Manage"
 *   button, typically to navigate to a screen for managing activities.
 */
@Composable
private fun DangerModeBody(
    viewModel: DangerModeCardViewModel,
    onManageActivitiesClick: () -> Unit
) {
  val currentActivity by viewModel.currentActivity.collectAsState(null)
  val activities by viewModel.activities.collectAsState()
  val dangerLevel by viewModel.dangerLevel.collectAsState(DangerLevel.LOW)
  val capabilities by viewModel.capabilities.collectAsState(emptySet())
  val autoActionsEnabled by viewModel.autoActionsEnabled.collectAsState(false)
  val confirmTouchRequired by viewModel.confirmTouchRequired.collectAsState(false)
  val confirmVoiceRequired by viewModel.confirmVoiceRequired.collectAsState(false)

  Column {
    ActivitySelectionRow(
        currentActivity = currentActivity,
        activities = activities,
        onActivitySelected = { viewModel.onActivitySelected(it) },
        onManageActivitiesClick = onManageActivitiesClick)
    Spacer(modifier = Modifier.height(8.dp))
    DangerLevelRow(
        dangerLevel = dangerLevel, onDangerLevelChanged = { viewModel.onDangerLevelChanged(it) })
  }

  Spacer(modifier = Modifier.height(4.dp))
  CapabilitiesRow(
      capabilities = capabilities, onCapabilityToggled = { viewModel.onCapabilityToggled(it) })

  if (capabilities.contains(DangerModeCapability.CALL) ||
      capabilities.contains(DangerModeCapability.SMS)) {
    Spacer(modifier = Modifier.height(12.dp))
    DangerModeAdvancedOptionsSection(
        autoActionsEnabled = autoActionsEnabled,
        confirmTouchRequired = confirmTouchRequired,
        confirmVoiceRequired = confirmVoiceRequired,
        onAutoActionsChanged = viewModel::onAutoActionsEnabled,
        onConfirmTouchChanged = viewModel::onConfirmTouchChanged,
        onConfirmVoiceChanged = viewModel::onConfirmVoiceChanged)
  }

  Spacer(modifier = Modifier.height(4.dp))
}

/**
 * A Composable that displays a row for selecting the current activity in Danger Mode.
 *
 * @param currentActivity The currently selected activity, or null if none is selected.
 * @param activities A list of available activities to choose from.
 * @param onActivitySelected A lambda function that is invoked when a new activity is selected from
 *   the dropdown.
 * @param onManageActivitiesClick A lambda function that is invoked when the "Manage" button is
 *   clicked.
 */
@Composable
private fun ActivitySelectionRow(
    currentActivity: Activity?,
    activities: List<Activity>,
    onActivitySelected: (Activity) -> Unit,
    onManageActivitiesClick: () -> Unit
) {
  val colorScheme = MaterialTheme.colorScheme

  Row(verticalAlignment = Alignment.CenterVertically) {
    Text(
        text = stringResource(R.string.danger_mode_card_mode_label),
        color = colorScheme.onError,
        fontSize = 13.sp)
    Spacer(modifier = Modifier.width(20.dp))

    // Use the new, cleaner dropdown composable
    ActivityDropdown(
        currentActivity = currentActivity,
        activities = activities,
        onActivitySelected = onActivitySelected)

    Spacer(modifier = Modifier.width(8.dp))

    StandardDashboardButton(
        label = stringResource(R.string.danger_mode_card_manage_activities),
        color = colorScheme.errorContainer,
        modifier = Modifier.testTag(NavigationTestTags.BUTTON_MANAGE_ACTIVITY_DANGER_MODE),
        onClick = onManageActivitiesClick,
        textColor = colorScheme.onErrorContainer)
  }
}

/**
 * A composable that provides a dropdown menu for selecting a predefined [Activity]. It displays the
 * `currentActivity` name on a button. When clicked, it expands a dropdown menu listing all
 * available `activities`. Selecting an item from the list triggers the `onActivitySelected`
 * callback.
 *
 * @param currentActivity The currently selected activity, or null if no activity is selected. This
 *   is displayed as the button's label.
 * @param activities The list of available activities to display in the dropdown menu.
 * @param onActivitySelected A lambda function that is invoked with the chosen [Activity] when an
 *   item is selected from the dropdown.
 * @param modifier A [Modifier] to be applied to the root [Box] of the composable.
 */
@Composable
private fun ActivityDropdown(
    currentActivity: Activity?,
    activities: List<Activity>,
    onActivitySelected: (Activity) -> Unit,
    modifier: Modifier = Modifier
) {
  val hasActivities = activities.isNotEmpty()
  val colorScheme = MaterialTheme.colorScheme
  var expanded by remember { mutableStateOf(false) }

  Box(modifier = modifier.wrapContentSize(Alignment.TopStart)) {
    // The button that triggers the dropdown
    StandardDashboardButton(
        label =
            currentActivity?.activityName
                ?: stringResource(R.string.danger_mode_card_select_activity),
        onClick = { if (hasActivities) expanded = true },
        color =
            if (hasActivities) colorScheme.errorContainer
            else colorScheme.errorContainer.copy(alpha = 0.5f),
        textColor =
            if (hasActivities) colorScheme.onErrorContainer
            else colorScheme.onErrorContainer.copy(alpha = 0.5f),
        icon = {
          Icon(
              imageVector = Icons.Filled.ArrowDropDown,
              contentDescription = stringResource(R.string.danger_mode_card_dropdown_arrow_cd),
              tint =
                  if (hasActivities) colorScheme.onErrorContainer
                  else colorScheme.onErrorContainer.copy(alpha = 0.5f))
        },
        modifier =
            Modifier.testTag(DangerModeTestTags.MODE_LABEL).clickable(enabled = hasActivities) {
              expanded = true
            })

    // The DropdownMenu itself
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier.testTag(DangerModeTestTags.MODE_LABEL)) {
          activities.forEach { activity ->
            DropdownMenuItem(
                text = { Text(activity.activityName) },
                onClick = {
                  onActivitySelected(activity)
                  expanded = false
                },
                modifier = Modifier.testTag(DangerModeTestTags.activityTag(activity.activityName)))
          }
        }
  }
}

/**
 * A Composable that displays a row for selecting the danger level. It shows a label and a series of
 * colored boxes representing different danger levels. The currently selected level is highlighted
 * with a more prominent border.
 *
 * @param dangerLevel The currently selected [DangerLevel].
 * @param onDangerLevelChanged A callback function that is invoked when a new danger level is
 *   selected.
 */
@Composable
private fun DangerLevelRow(dangerLevel: DangerLevel, onDangerLevelChanged: (DangerLevel) -> Unit) {
  val colorScheme = MaterialTheme.colorScheme
  val extendedColors = MaterialTheme.extendedColors

  Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.testTag(DangerModeTestTags.COLOR_ROW)) {
        Text(
            text = stringResource(R.string.danger_mode_card_danger_level_label),
            color = colorScheme.onError,
            fontSize = 13.sp)
        listOf(
                extendedColors.dangerLevels.green,
                extendedColors.dangerLevels.yellow,
                extendedColors.dangerLevels.amber,
                extendedColors.dangerLevels.red)
            .forEachIndexed { index, it ->
              val alpha = if (index == dangerLevel.ordinal) 1f else 0.1f
              DangerColorBox(
                  it,
                  modifier = Modifier.testTag(DangerModeTestTags.dangerLevelTag(index)),
                  onClick = { onDangerLevelChanged(DangerLevel.entries[index]) },
                  borderColor = colorScheme.onError.copy(alpha = alpha))
            }
      }
}

/**
 * A row of buttons representing the different communication capabilities of Danger Mode, such as
 * sending an SMS or making a call. Users can tap these buttons to toggle them on or off.
 *
 * @param capabilities The set of currently selected [DangerModeCapability]s.
 * @param onCapabilityToggled A lambda function invoked when a capability button is clicked, passing
 *   the toggled [DangerModeCapability].
 */
@Composable
private fun CapabilitiesRow(
    capabilities: Set<DangerModeCapability>,
    onCapabilityToggled: (DangerModeCapability) -> Unit
) {
  val colorScheme = MaterialTheme.colorScheme

  Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.testTag(DangerModeTestTags.SENDS_ROW)) {
        Text(
            text = stringResource(R.string.danger_mode_card_sends_label),
            color = colorScheme.onError,
            fontSize = 13.sp)
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
                onClick = { onCapabilityToggled(capability) },
                textColor = textColor)
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

/**
 * A composable that displays advanced options for Danger Mode, specifically for when "Call" or
 * "SMS" capabilities are enabled. This section allows users to configure automatic actions and
 * confirmation requirements.
 *
 * @param autoActionsEnabled Whether automatic actions (like auto-calling) are currently enabled.
 * @param confirmTouchRequired Whether a touch confirmation is required before actions are taken.
 * @param confirmVoiceRequired Whether a voice confirmation is required before actions are taken.
 * @param onAutoActionsChanged A lambda function to be invoked when the "Auto Actions" switch is
 *   toggled.
 * @param onConfirmTouchChanged A lambda function to be invoked when the "Touch Confirmation" switch
 *   is toggled.
 * @param onConfirmVoiceChanged A lambda function to be invoked when the "Voice Confirmation" switch
 *   is toggled.
 * @param modifier The [Modifier] to be applied to this composable.
 */
@Composable
private fun DangerModeAdvancedOptionsSection(
    autoActionsEnabled: Boolean,
    confirmTouchRequired: Boolean,
    confirmVoiceRequired: Boolean,
    onAutoActionsChanged: (Boolean) -> Unit,
    onConfirmTouchChanged: (Boolean) -> Unit,
    onConfirmVoiceChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
  val colors = MaterialTheme.colorScheme

  Column(modifier = modifier.fillMaxWidth().testTag(DangerModeTestTags.ADVANCED_SECTION)) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
      Text(
          text = stringResource(R.string.danger_mode_advanced_auto_actions_title),
          color = colors.onError,
          fontSize = 13.sp,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier.weight(1f))
      Switch(
          checked = autoActionsEnabled,
          onCheckedChange = onAutoActionsChanged,
          modifier = Modifier.testTag(DangerModeTestTags.AUTO_CALL_SWITCH))
    }
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.danger_mode_advanced_auto_actions_desc),
        color = colors.onError.copy(alpha = 0.9f),
        fontSize = 11.sp)

    Spacer(modifier = Modifier.height(8.dp))

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = stringResource(R.string.danger_mode_advanced_confirmation_title),
        color = colors.onError,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = stringResource(R.string.danger_mode_advanced_confirmation_desc),
        color = colors.onError.copy(alpha = 0.9f),
        fontSize = 11.sp)

    Spacer(modifier = Modifier.height(6.dp))

    // Tactile confirmation
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
      Text(
          text = stringResource(R.string.danger_mode_advanced_touch_confirmation_label),
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
          text = stringResource(R.string.danger_mode_advanced_voice_confirmation_label),
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

/**
 * A composable that displays a switch to toggle Danger Mode on or off.
 *
 * @param checked Whether the switch is currently in the "on" state.
 * @param onCheckedChange A lambda function that is invoked when the user toggles the switch. It
 *   receives the new checked state as a Boolean.
 * @param modifier A [Modifier] to be applied to the switch.
 */
@Composable
private fun DangerModeSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
  Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      modifier = modifier.testTag(DangerModeTestTags.SWITCH))
}
