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
import com.github.warnastrophy.core.permissions.AppPermissions
import com.github.warnastrophy.core.permissions.PermissionResult
import com.github.warnastrophy.core.ui.features.dashboard.DashboardScreenTestTags
import com.github.warnastrophy.core.util.findActivity

@Composable
fun DangerModePreferencesScreen(viewModel: DangerModePreferencesViewModel) {
  val context = LocalContext.current
  val activity = context.findActivity()

  if (activity == null) {
    // A fallback UI to prevent crashes and inform developers.
    Column(
        modifier = Modifier.fillMaxSize().testTag(DashboardScreenTestTags.FALLBACK_ACTIVITY_ERROR),
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

  fun requestPermission(perms: AppPermissions) {
    println("Requesting permissions: $perms")
    viewModel.onPermissionsRequestStart()
    println("Launching permissions request: $perms")
    launcher.launch(perms.permissions)
  }

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
            title = "Alert Mode automatic",
            description =
                "If this option is enabled, you will receive an alert when you enter a dangerous area, i.e. when you enter an area where a disaster is occurring.\n\nThis mode require fine location permissions",
            checked = uiState.alertModeAutomaticEnabled,
            onCheckedChange = { isChecked ->
              if (isChecked) {
                when (uiState.alertModePermissionResult) {
                  is PermissionResult.Granted -> viewModel.onAlertModeToggled(true)
                  is PermissionResult.Denied -> requestPermission(viewModel.alertModePermissions)
                  is PermissionResult.PermanentlyDenied -> openAppSettings()
                }
              } else {
                viewModel.onAlertModeToggled(false)
              }
            },
            isRequestInFlight = uiState.isOsRequestInFlight)

        PreferenceItem(
            title = "Inactivity Detection",
            description =
                "If this option is enabled and you are in Danger Mode, your phone will detect your activity and send an SMS alert to your registered emergency contacts if you remain inactive for a certain period of time in a dangerous area.\n\nThis mode require fine location permissions",
            extraDescription =
                "It is strongly recommended that you enable the automatic SMS feature with this functionality.",
            checked = uiState.inactivityDetectionEnabled,
            onCheckedChange = { isChecked ->
              if (isChecked) {
                when (uiState.inactivityDetectionPermissionResult) {
                  is PermissionResult.Granted -> viewModel.onInactivityDetectionToggled(true)
                  is PermissionResult.Denied ->
                      requestPermission(viewModel.inactivityDetectionPermissions)
                  is PermissionResult.PermanentlyDenied -> openAppSettings()
                }
              } else {
                viewModel.onInactivityDetectionToggled(false)
              }
            },
            enabled = uiState.alertModeAutomaticEnabled,
            isRequestInFlight = uiState.isOsRequestInFlight)

        PreferenceItem(
            title = "Automatic SMS",
            description =
                "If this option is enabled and your phone detects that you are inactive, it will automatically send an emergency text message to all your registered emergency contacts to request assistance.\n\nThis mode require fine location and SMS sending permissions",
            checked = uiState.automaticSmsEnabled,
            onCheckedChange = { isChecked ->
              if (isChecked) {
                when (uiState.smsPermissionResult) {
                  is PermissionResult.Granted -> viewModel.onAutomaticSmsToggled(true)
                  is PermissionResult.Denied -> requestPermission(viewModel.smsPermissions)
                  is PermissionResult.PermanentlyDenied -> openAppSettings()
                }
              } else {
                viewModel.onAutomaticSmsToggled(false)
              }
            },
            enabled = uiState.inactivityDetectionEnabled,
            isRequestInFlight = uiState.isOsRequestInFlight)
      }
}

@Composable
private fun PreferenceItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    extraDescription: String? = null,
    isRequestInFlight: Boolean = false
) {
  val alpha = if (enabled) 1f else 0.5f

  Row(
      modifier = modifier.fillMaxWidth(),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f).alpha(alpha)) {
          Text(text = title, style = MaterialTheme.typography.titleLarge)
          Spacer(modifier = Modifier.height(8.dp))
          Text(
              text = description,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant)
          if (extraDescription != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = extraDescription,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
          }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled && !isRequestInFlight)
      }
}
