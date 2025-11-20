package com.github.warnastrophy.core.ui.features.profile.preferences

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun DangerModePreferencesScreen() {
  var alertModeAutomatic by remember { mutableStateOf(true) }
  var inactivityDetection by remember { mutableStateOf(false) }
  var automaticSms by remember { mutableStateOf(false) }

  // When Alert Mode switch turned off, the other two are also turned off.
  LaunchedEffect(alertModeAutomatic) {
    if (!alertModeAutomatic) {
      inactivityDetection = false
    }
  }

  // When inactivity detection turned off, automatic SMS also turned off
  LaunchedEffect(inactivityDetection) {
    if (!inactivityDetection) {
      automaticSms = false
    }
  }

  Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp)) {
        PreferenceItem(
            title = "Alert Mode automatic",
            description =
                "If this option is enabled, you will receive an alert when you enter a dangerous area, i.e. when you enter an area where a disaster is occurring.\n\nThis mode require fine location permissions",
            checked = alertModeAutomatic,
            onCheckedChange = { alertModeAutomatic = it })

        PreferenceItem(
            title = "Inactivity Detection",
            description =
                "If this option is enabled and you are in Danger Mode, your phone will detect your activity and send an SMS alert to your registered emergency contacts if you remain inactive for a certain period of time in a dangerous area.\n\nThis mode require fine location permissions",
            extraDescription =
                "It is strongly recommended that you enable the automatic SMS feature with this functionality.",
            checked = inactivityDetection,
            onCheckedChange = { inactivityDetection = it },
            enabled = alertModeAutomatic)

        PreferenceItem(
            title = "Automatic SMS",
            description =
                "If this option is enabled and your phone detects that you are inactive, it will automatically send an emergency text message to all your registered emergency contacts to request assistance.\n\nThis mode require fine location and SMS sending permissions",
            checked = automaticSms,
            onCheckedChange = { automaticSms = it },
            enabled = inactivityDetection)
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
    extraDescription: String? = null
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
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
      }
}

@Preview(showBackground = true)
@Composable
fun PreferencesScreenPreview() {
  MaterialTheme { DangerModePreferencesScreen() }
}
