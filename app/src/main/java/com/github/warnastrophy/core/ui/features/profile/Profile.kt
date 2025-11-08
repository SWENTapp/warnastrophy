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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.warnastrophy.core.ui.navigation.NavigationTestTags
import com.github.warnastrophy.core.ui.theme.MainAppTheme

@Composable
fun ProfileScreen(
    onHealthCardClick: () -> Unit = {}, // New handler
    onEmergencyContactsClick: () -> Unit = {} // New handler
) {
  Column(
      verticalArrangement = Arrangement.Center,
      // horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.fillMaxSize()) {
        // --- 2. Health Card Button ---
        ProfileListItem(
            icon = Icons.Default.FavoriteBorder,
            label = "Health Card",
            onClick = onHealthCardClick,
            modifier = Modifier.testTag(NavigationTestTags.HEALTH_CARD))

        Spacer(modifier = Modifier.height(8.dp)) // Small space between items

        // --- 3. Emergency Contacts Button ---
        ProfileListItem(
            icon = Icons.Filled.Call,
            label = "Emergency contacts",
            onClick = onEmergencyContactsClick,
            modifier = Modifier.testTag(NavigationTestTags.CONTACT_LIST))
      }
}

@Composable
fun ProfileListItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
  Column(modifier = modifier.fillMaxWidth()) {
    Row(
        modifier =
            Modifier.fillMaxWidth()
                .clickable(onClick = onClick) // Makes the entire row clickable
                .padding(vertical = 12.dp, horizontal = 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween // Pushes the arrow to the end
        ) {
          // Left side: Icon and Label
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp).padding(end = 8.dp))
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
          }
          Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = "Navigate",
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(24.dp))
        }

    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        thickness = 1.dp,
        modifier = Modifier.padding(top = 4.dp) // Adjust padding as needed
        )
  }
}

@Preview
@Composable
fun ProfileScreenPreview() {
  MainAppTheme { ProfileScreen() }
}
