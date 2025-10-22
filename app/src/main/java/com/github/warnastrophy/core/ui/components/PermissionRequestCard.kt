package com.github.warnastrophy.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

object PermissionUiTags {
  const val CARD = "permCard"
  const val BTN_ALLOW = "permAllowBtn"
  const val BTN_SETTINGS = "permSettingsBtn"
}

@Composable
fun PermissionRequestCard(
    title: String,
    message: String,
    showAllowButton: Boolean,
    onAllowClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  Surface(
      modifier = modifier.testTag(PermissionUiTags.CARD),
      tonalElevation = 6.dp,
      shadowElevation = 6.dp) {
        Column(Modifier.padding(16.dp)) {
          Text(title, style = MaterialTheme.typography.titleMedium)
          Spacer(Modifier.height(6.dp))
          Text(message, style = MaterialTheme.typography.bodyMedium)
          Spacer(Modifier.height(10.dp))
          Row {
            if (showAllowButton) {
              Button(
                  onClick = onAllowClick, modifier = Modifier.testTag(PermissionUiTags.BTN_ALLOW)) {
                    Text("Allow location")
                  }
              Spacer(Modifier.width(12.dp))
            }
            OutlinedButton(
                onClick = onOpenSettingsClick,
                modifier = Modifier.testTag(PermissionUiTags.BTN_SETTINGS)) {
                  Text("Open settings")
                }
          }
        }
      }
}
