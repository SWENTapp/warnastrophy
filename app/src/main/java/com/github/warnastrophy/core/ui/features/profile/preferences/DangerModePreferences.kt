package com.github.warnastrophy.core.ui.features.profile.preferences

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.warnastrophy.R
import com.github.warnastrophy.core.data.service.StateManagerService
import com.github.warnastrophy.core.ui.components.ActivityFallback
import com.github.warnastrophy.core.util.findActivity
import com.github.warnastrophy.core.util.openAppSettings

object DangerModePreferencesScreenTestTags {
  const val SCROLL_CONTAINER = "dangerModePreferencesScrollContainer"
  const val ALERT_MODE_ITEM = "alertModeItem"
  const val INACTIVITY_DETECTION_ITEM = "inactivityDetectionItem"
  const val AUTOMATIC_SMS_ITEM = "automaticSmsItem"
  const val AUTOMATIC_CALLS_ITEM = "automaticCallsItem"

  const val MICROPHONE_ACCESS = "microphoneAccessItem"
  const val ALERT_MODE_SWITCH = "alertModeSwitch"
  const val INACTIVITY_DETECTION_SWITCH = "inactivitySwitch"
  const val AUTOMATIC_SMS_SWITCH = "automaticSmsSwitch"
  const val AUTOMATIC_CALLS_SWITCH = "automaticCallsSwitch"
  const val MICROPHONE_ACCESS_SWITCH = "microphoneAccessSwitch"
}

/**
 * Composable screen for managing "Danger Mode" preferences.
 *
 * This screen displays a list of preferences related to Danger Mode, allowing the user to enable or
 * disable features like "Alert Mode," "Inactivity Detection," and "Automatic SMS." It handles the
 * necessary permission requests for each feature and directs the user to the app settings if
 * permissions are permanently denied.
 *
 * The UI is composed of several [PreferenceItem]s, each representing a toggleable setting. The
 * logic for handling state changes and permission flows is delegated to the provided [viewModel].
 *
 * @param viewModel The [DangerModePreferencesViewModel] that provides the state for the UI and
 *   handles user interactions and business logic.
 */
@Composable
fun DangerModePreferencesScreen(viewModel: DangerModePreferencesViewModel) {
  val context = LocalContext.current
  val activity = context.findActivity()

  if (activity == null) {
    ActivityFallback()
    return
  }

  val uiState by viewModel.uiState.collectAsState()

  val launcher =
      rememberLauncherForActivityResult(
          contract = ActivityResultContracts.RequestMultiplePermissions(),
          onResult = { viewModel.onPermissionsResult(activity = activity) })

  /**
   * Requests the necessary permissions for a given [PendingAction].
   *
   * @param action The pending action that requires permissions.
   */
  fun requestPermission(action: PendingAction) {
    val permSet =
        when (action) {
          PendingAction.TOGGLE_ALERT_MODE -> viewModel.alertModePermissions
          PendingAction.TOGGLE_INACTIVITY_DETECTION -> viewModel.inactivityDetectionPermissions
          PendingAction.TOGGLE_AUTOMATIC_SMS -> viewModel.smsPermissions
          PendingAction.TOGGLE_AUTOMATIC_CALLS -> viewModel.callPermissions
          PendingAction.TOGGLE_MICROPHONE -> viewModel.microphonePermissions
        }

    viewModel.onPermissionsRequestStart(action = action)
    launcher.launch(permSet.permissions)
  }

  Column(
      modifier =
          Modifier.fillMaxSize()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 16.dp, vertical = 24.dp)
              .testTag(DangerModePreferencesScreenTestTags.SCROLL_CONTAINER),
      verticalArrangement = Arrangement.spacedBy(24.dp)) {
        PreferenceItem(
            modifier = Modifier.testTag(DangerModePreferencesScreenTestTags.ALERT_MODE_ITEM),
            data =
                PreferenceItemData(
                    title = stringResource(R.string.danger_mode_alert_mode_title),
                    description = stringResource(R.string.danger_mode_alert_mode_description),
                    checked = uiState.alertModeAutomaticEnabled,
                    onCheckedChange = { isChecked ->
                      viewModel.handlePreferenceChange(
                          isChecked = isChecked,
                          permissionResult = uiState.alertModePermissionResult,
                          onToggle = { viewModel.onAlertModeToggled(it) },
                          onPermissionDenied = {
                            requestPermission(PendingAction.TOGGLE_ALERT_MODE)
                          },
                          onPermissionPermDenied = { openAppSettings(activity) },
                      )
                    },
                    isRequestInFlight = uiState.isOsRequestInFlight,
                ),
            switchTestTag = DangerModePreferencesScreenTestTags.ALERT_MODE_SWITCH)

        PreferenceItem(
            modifier =
                Modifier.testTag(DangerModePreferencesScreenTestTags.INACTIVITY_DETECTION_ITEM),
            data =
                PreferenceItemData(
                    title = stringResource(R.string.danger_mode_inactivity_detection_title),
                    description =
                        stringResource(R.string.danger_mode_inactivity_detection_description),
                    extraDescription =
                        stringResource(R.string.danger_mode_inactivity_detection_extra_description),
                    checked = uiState.inactivityDetectionEnabled,
                    onCheckedChange = { isChecked ->
                      viewModel.handlePreferenceChange(
                          isChecked = isChecked,
                          permissionResult = uiState.inactivityDetectionPermissionResult,
                          onToggle = { viewModel.onInactivityDetectionToggled(it) },
                          onPermissionDenied = {
                            requestPermission(PendingAction.TOGGLE_INACTIVITY_DETECTION)
                          },
                          onPermissionPermDenied = { openAppSettings(activity) },
                      )
                    },
                    enabled = uiState.alertModeAutomaticEnabled,
                    isRequestInFlight = uiState.isOsRequestInFlight),
            switchTestTag = DangerModePreferencesScreenTestTags.INACTIVITY_DETECTION_SWITCH)

        PreferenceItem(
            modifier = Modifier.testTag(DangerModePreferencesScreenTestTags.AUTOMATIC_SMS_ITEM),
            data =
                PreferenceItemData(
                    title = stringResource(R.string.danger_mode_automatic_sms_title),
                    description = stringResource(R.string.danger_mode_automatic_sms_description),
                    checked = uiState.automaticSmsEnabled,
                    onCheckedChange = { isChecked ->
                      viewModel.handlePreferenceChange(
                          isChecked = isChecked,
                          permissionResult = uiState.smsPermissionResult,
                          onToggle = { viewModel.onAutomaticSmsToggled(it) },
                          onPermissionDenied = {
                            requestPermission(PendingAction.TOGGLE_AUTOMATIC_SMS)
                          },
                          onPermissionPermDenied = { openAppSettings(activity) },
                      )
                    },
                    enabled = uiState.inactivityDetectionEnabled,
                    isRequestInFlight = uiState.isOsRequestInFlight),
            switchTestTag = DangerModePreferencesScreenTestTags.AUTOMATIC_SMS_SWITCH)
        PreferenceItem(
            modifier = Modifier.testTag(DangerModePreferencesScreenTestTags.MICROPHONE_ACCESS),
            data =
                PreferenceItemData(
                    title = stringResource(R.string.danger_mode_microphone_access_title),
                    description =
                        stringResource(R.string.danger_mode_microphone_access_description),
                    checked = uiState.microphoneAccessEnabled,
                    onCheckedChange = { isChecked ->
                      viewModel.handlePreferenceChange(
                          isChecked = isChecked,
                          permissionResult = uiState.microphonePermissionResult,
                          onToggle = { viewModel.onMicrophoneToggled(it) },
                          onPermissionDenied = {
                            requestPermission(PendingAction.TOGGLE_MICROPHONE)
                          },
                          onPermissionPermDenied = { openAppSettings(activity) },
                      )
                    },
                    enabled = uiState.inactivityDetectionEnabled,
                    isRequestInFlight = uiState.isOsRequestInFlight),
            switchTestTag = DangerModePreferencesScreenTestTags.MICROPHONE_ACCESS_SWITCH)

        PreferenceItem(
            modifier = Modifier.testTag(DangerModePreferencesScreenTestTags.AUTOMATIC_CALLS_ITEM),
            data =
                PreferenceItemData(
                    title = stringResource(R.string.danger_mode_automatic_calls_title),
                    description = stringResource(R.string.danger_mode_automatic_calls_description),
                    checked = uiState.automaticCallsEnabled,
                    onCheckedChange = { isChecked ->
                      viewModel.handlePreferenceChange(
                          isChecked = isChecked,
                          permissionResult = uiState.callPermissionResult,
                          onToggle = { viewModel.onAutomaticCallsToggled(it) },
                          onPermissionDenied = {
                            requestPermission(PendingAction.TOGGLE_AUTOMATIC_CALLS)
                          },
                          onPermissionPermDenied = { openAppSettings(activity) },
                      )
                    },
                    enabled = uiState.inactivityDetectionEnabled,
                    isRequestInFlight = uiState.isOsRequestInFlight),
            switchTestTag = DangerModePreferencesScreenTestTags.AUTOMATIC_CALLS_SWITCH)

        // Voice confirmation - uses local state, not persisted
        var voiceConfirmationEnabled by remember { mutableStateOf(false) }
        val orchestrator = remember { StateManagerService.dangerModeOrchestrator }
      }
}

/**
 * Data class representing the state and configuration for a single preference item.
 *
 * This class is used to abstract the details of a preference, such as its title, description,
 * current state, and the logic to handle its changes, including permission checks.
 *
 * @param title The main title of the preference item.
 * @param description A detailed explanation of what the preference does.
 * @param checked The current state of the preference (true for enabled, false for disabled).
 * @param onCheckedChange A lambda function that is invoked when the user interacts with the
 *   preference's switch.
 * @param enabled A boolean indicating whether the preference item is interactable. If false, the
 *   item is visually disabled. Defaults to true.
 * @param extraDescription An optional, additional piece of text displayed with emphasis (e.g.,
 *   bold).
 * @param isRequestInFlight A boolean to indicate if a permission request. This is used to
 *   temporarily disable interaction to prevent race conditions.
 */
private data class PreferenceItemData(
    val title: String,
    val description: String,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit,
    val enabled: Boolean = true,
    val extraDescription: String? = null,
    val isRequestInFlight: Boolean = false
)

/**
 * Displays a preference item with a title, description, and a switch.
 *
 * @param data The data to display in the preference item.
 * @param modifier The modifier to be applied to the `Row` container.
 */
@Composable
private fun PreferenceItem(
    data: PreferenceItemData,
    modifier: Modifier = Modifier,
    switchTestTag: String
) {
  val alpha = if (data.enabled) 1f else 0.5f
  Row(
      modifier = modifier.fillMaxWidth(),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f).alpha(alpha)) {
          Text(text = data.title, style = MaterialTheme.typography.titleLarge)
          Spacer(modifier = Modifier.height(8.dp))
          Text(
              text = data.description,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant)
          if (data.extraDescription != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = data.extraDescription,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            modifier = Modifier.testTag(switchTestTag),
            checked = data.checked,
            onCheckedChange = data.onCheckedChange,
            enabled = data.enabled && !data.isRequestInFlight)
      }
}
