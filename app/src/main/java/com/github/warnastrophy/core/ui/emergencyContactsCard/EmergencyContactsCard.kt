package com.github.warnastrophy.core.ui.dashboard

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.warnastrophy.core.ui.components.StandardDashboardButton
import com.github.warnastrophy.core.ui.components.StandardDashboardCard

object EmergencyContactsTestTags {
  const val CARD = "emergencyContactsCard"
  const val TITLE = "emergencyContactsTitle"
  const val SUBTITLE = "emergencyContactsSubtitle"
  const val OPEN_BUTTON = "emergencyContactsOpenButton"
}

@Composable
fun EmergencyContactsCard(modifier: Modifier = Modifier) {
  StandardDashboardCard(
      modifier = modifier.testTag(EmergencyContactsTestTags.CARD),
      backgroundColor = Color(0xFFFFFDE7),
      minHeight = 140.dp,
      maxHeight = 160.dp) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
          Text(
              text = "Emergency Contacts",
              color = Color(0xFF5D4037),
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.testTag(EmergencyContactsTestTags.TITLE),
              fontSize = 15.sp)

          Spacer(modifier = Modifier.height(4.dp))

          Text(
              text = "Manage who to notify",
              modifier = Modifier.testTag(EmergencyContactsTestTags.SUBTITLE),
              color = Color(0xFF5D4037),
              fontSize = 13.sp,
              lineHeight = 16.sp)

          Spacer(modifier = Modifier.height(24.dp))

          Box(modifier = Modifier.testTag(EmergencyContactsTestTags.OPEN_BUTTON)) {
            StandardDashboardButton(label = "Open")
          }
        }
      }
}
