package com.github.warnastrophy.core.ui.emergencyContactsCard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.warnastrophy.core.ui.components.StandardDashboardButton
import com.github.warnastrophy.core.ui.components.StandardDashboardCard
import com.github.warnastrophy.core.ui.theme.MainAppTheme

object EmergencyContactsTestTags {
  const val CARD = "emergencyContactsCard"
  const val TITLE = "emergencyContactsTitle"
  const val SUBTITLE = "emergencyContactsSubtitle"
  const val OPEN_BUTTON = "emergencyContactsOpenButton"
}

object EmergencyContactsCardColors {
  val BACKGROUND_COLOR: Color = Color(0xFFFFFDE7) // light yellow
  val TEXT_COLOR: Color = Color(0xFF5D4037) // light brown
}

/**
 * This Composable displays a dashboard card for Emergency Contacts. It uses a light yellow
 * background color and brown text to indicate urgency. The card includes a title, subtitle, and an
 * "Open" button for managing emergency contacts.
 */
@Composable
fun EmergencyContactsCard(modifier: Modifier = Modifier) {
  StandardDashboardCard(
      modifier = modifier.testTag(EmergencyContactsTestTags.CARD),
      backgroundColor = EmergencyContactsCardColors.BACKGROUND_COLOR, // light yellow
      minHeight = 120.dp,
      maxHeight = 140.dp) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
          Text(
              text = "Emergency Contacts",
              color = EmergencyContactsCardColors.TEXT_COLOR, // light brown
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.testTag(EmergencyContactsTestTags.TITLE),
              fontSize = 15.sp)

          Spacer(modifier = Modifier.height(4.dp))

          Text(
              text = "Manage who to notify",
              modifier = Modifier.testTag(EmergencyContactsTestTags.SUBTITLE),
              color = Color(0xFF5D4037), // light brown
              fontSize = 13.sp,
              lineHeight = 16.sp)

          Spacer(Modifier.weight(1f))

          Box(modifier = Modifier.testTag(EmergencyContactsTestTags.OPEN_BUTTON)) {
            StandardDashboardButton(label = "Open")
          }
        }
      }
}

@Preview(showBackground = true)
@Composable
private fun EmergencyContactsCardPreview() {
  MainAppTheme { EmergencyContactsCard() }
}
