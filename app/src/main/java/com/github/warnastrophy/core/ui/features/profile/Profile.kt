package com.github.warnastrophy.core.ui.features.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.warnastrophy.R
import com.github.warnastrophy.core.ui.features.auth.SignInViewModel
import com.github.warnastrophy.core.ui.navigation.NavigationTestTags

/**
 * A Composable function representing the Profile Screen, displaying a list of actions such as
 * "Health Card", "Emergency Contacts", and "Logout". It shows a confirmation dialog when the user
 * attempts to log out, and handles any errors related to signing out.
 *
 * @param viewModel The [SignInViewModel] instance that handles the logic for sign-in and sign-out.
 * @param onHealthCardClick A callback function to navigate to the health card screen when clicked.
 * @param onEmergencyContactsClick A callback function to navigate to the emergency contacts screen
 *   when clicked.
 * @param onLogout A callback function to handle the user logout process.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: SignInViewModel = viewModel(),
    onHealthCardClick: () -> Unit = {},
    onEmergencyContactsClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    onDangerModePreferencesClick: () -> Unit = {}
) {

  val uiState by viewModel.uiState.collectAsState()
  var showLogoutDialog by remember { mutableStateOf(false) }

  LaunchedEffect(uiState.signedOut) {
    if (uiState.signedOut) {
      onLogout()
    }
  }

  Column(verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
    ProfileListItem(
        icon = Icons.Default.FavoriteBorder,
        label = "Health Card",
        onClick = onHealthCardClick,
        modifier = Modifier.testTag(NavigationTestTags.HEALTH_CARD))

    Spacer(modifier = Modifier.height(8.dp))

    ProfileListItem(
        icon = Icons.Filled.Call,
        label = "Emergency contacts",
        onClick = onEmergencyContactsClick,
        modifier = Modifier.testTag(NavigationTestTags.CONTACT_LIST))

    Spacer(modifier = Modifier.height(8.dp))

    ProfileListItem(
        icon = Icons.Default.Settings,
        label = "Danger Mode Preferences",
        onClick = onDangerModePreferencesClick)

    Spacer(modifier = Modifier.height(8.dp))

    ProfileListItem(
        icon = Icons.AutoMirrored.Filled.ExitToApp,
        label = "Logout",
        onClick = { showLogoutDialog = true },
        modifier = Modifier.testTag(NavigationTestTags.LOGOUT),
        tintColor = MaterialTheme.colorScheme.error)
  }

  if (showLogoutDialog) {
    BasicAlertDialog(onDismissRequest = { showLogoutDialog = false }) {
      Card {
        Column(modifier = Modifier.padding(24.dp)) {
          Text(
              text = stringResource(R.string.logout_string),
              style = MaterialTheme.typography.headlineSmall,
              modifier = Modifier.padding(bottom = 16.dp))

          Text(
              text = stringResource(R.string.logout_message),
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(bottom = 24.dp))

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { showLogoutDialog = false }) {
              Text(stringResource(R.string.cancel_logout))
            }
            Button(
                onClick = {
                  showLogoutDialog = false
                  viewModel.signOut()
                },
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError),
                modifier = Modifier.padding(start = 8.dp)) {
                  Text(stringResource(R.string.logout_string))
                }
          }
        }
      }
    }
  }

  uiState.errorMsg?.let { error ->
    BasicAlertDialog(onDismissRequest = { viewModel.clearErrorMsg() }) {
      Card {
        Column(modifier = Modifier.padding(24.dp)) {
          Text(
              text = stringResource(R.string.logout_error),
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.headlineSmall,
              modifier = Modifier.padding(bottom = 16.dp))

          Text(
              text = error,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(bottom = 24.dp))

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { viewModel.clearErrorMsg() }) {
              Text("OK", color = MaterialTheme.colorScheme.error)
            }
          }
        }
      }
    }
  }
}

/**
 * A Composable function representing a list item in the Profile screen with an icon, label, and
 * clickable functionality. The item shows an icon on the left and an arrow on the right for
 * navigation.
 *
 * @param icon The icon to display on the left side of the item.
 * @param label The label text displayed next to the icon.
 * @param onClick A callback function that gets triggered when the item is clicked.
 * @param modifier An optional [Modifier] to apply to the layout of this item.
 * @param tintColor The color to tint the icon and text.
 */
@Composable
private fun ProfileListItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tintColor: Color = MaterialTheme.colorScheme.onSurface
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(onClick = onClick) // Makes the entire row clickable
                .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
          // Left side: Icon and Label
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier.size(24.dp).padding(end = 8.dp))
            Text(text = label, style = MaterialTheme.typography.bodyLarge, color = tintColor)
          }
          Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = "Navigate",
              tint = tintColor,
              modifier = Modifier.size(24.dp))
        }

    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 1.dp,
        modifier = Modifier.padding(top = 4.dp, start = 16.dp, end = 16.dp))
  }
}
