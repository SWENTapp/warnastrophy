package com.github.warnastrophy.core.ui.features.profile.preferences

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.warnastrophy.core.util.findActivity

object DangerModePreferencesScreenTestTags {
  const val FALLBACK_ACTIVITY_ERROR = "fallbackActivityError"
}

/** Composable screen for managing "Danger Mode" preferences. */
@Composable
fun DangerModePreferencesScreen(viewModel: DangerModePreferencesViewModel) {
  val context = LocalContext.current
  val activity = context.findActivity()

  if (activity == null) {
    // A fallback UI to prevent crashes and inform developers.
    Column(
        modifier =
            Modifier.fillMaxSize()
                .testTag(DangerModePreferencesScreenTestTags.FALLBACK_ACTIVITY_ERROR),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally) {
          Text("Error: Dashboard cannot function without an Activity context.")
        }
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
        }

    viewModel.onPermissionsRequestStart(action = action)
    launcher.launch(permSet.permissions)
  }

  /** Opens the application's settings screen in the Android system settings. */
  fun openAppSettings() {
    val intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
          data = Uri.fromParts("package", context.packageName, null)
        }
    context.startActivity(intent)
  }

  Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp)) {
        PreferenceItem(
            data =
                PreferenceItemData(
                    title = "Alert Mode automatic",
                    description =
                        "If this option is enabled, you will receive an alert when you enter a dangerous area, i.e. when you enter an area where a disaster is occurring.\n\nThis mode require fine location permissions",
                    checked = uiState.alertModeAutomaticEnabled,
                    onCheckedChange = { isChecked ->
                      viewModel.handlePreferenceChange(
                          isChecked = isChecked,
                          permissionResult = uiState.alertModePermissionResult,
                          onToggle = { viewModel.onAlertModeToggled(it) },
                          onPermissionDenied = {
                            requestPermission(PendingAction.TOGGLE_ALERT_MODE)
                          },
                          onPermissionPermDenied = { openAppSettings() },
                      )
                    },
                    isRequestInFlight = uiState.isOsRequestInFlight))

        PreferenceItem(
            data =
                PreferenceItemData(
                    title = "Inactivity Detection",
                    description =
                        "If this option is enabled and you are in Danger Mode, your phone will detect your activity and send an SMS alert to your registered emergency contacts if you remain inactive for a certain period of time in a dangerous area.\n\nThis mode require fine location permissions",
                    extraDescription =
                        "It is strongly recommended that you enable the automatic SMS feature with this functionality.",
                    checked = uiState.inactivityDetectionEnabled,
                    onCheckedChange = { isChecked ->
                      viewModel.handlePreferenceChange(
                          isChecked = isChecked,
                          permissionResult = uiState.inactivityDetectionPermissionResult,
                          onToggle = { viewModel.onInactivityDetectionToggled(it) },
                          onPermissionDenied = {
                            requestPermission(PendingAction.TOGGLE_INACTIVITY_DETECTION)
                          },
                          onPermissionPermDenied = { openAppSettings() },
                      )
                    },
                    enabled = uiState.alertModeAutomaticEnabled,
                    isRequestInFlight = uiState.isOsRequestInFlight))

        PreferenceItem(
            data =
                PreferenceItemData(
                    title = "Automatic SMS",
                    description =
                        "If this option is enabled and your phone detects that you are inactive, it will automatically send an emergency text message to all your registered emergency contacts to request assistance.\n\nThis mode require fine location and SMS sending permissions",
                    checked = uiState.automaticSmsEnabled,
                    onCheckedChange = { isChecked ->
                      viewModel.handlePreferenceChange(
                          isChecked = isChecked,
                          permissionResult = uiState.smsPermissionResult,
                          onToggle = { viewModel.onAutomaticSmsToggled(it) },
                          onPermissionDenied = {
                            requestPermission(PendingAction.TOGGLE_AUTOMATIC_SMS)
                          },
                          onPermissionPermDenied = { openAppSettings() },
                      )
                    },
                    enabled = uiState.inactivityDetectionEnabled,
                    isRequestInFlight = uiState.isOsRequestInFlight))
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
private fun PreferenceItem(data: PreferenceItemData, modifier: Modifier = Modifier) {
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
            checked = data.checked,
            onCheckedChange = data.onCheckedChange,
            enabled = data.enabled && !data.isRequestInFlight)
      }
}
