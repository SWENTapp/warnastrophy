package com.github.warnastrophy.core.ui.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.warnastrophy.core.data.service.ServiceStateManager

/**
 * This Composable displays a top bar indicating that the user is in a safe zone. It features a
 * green background with white text to convey safety.
 */
@Composable
fun SafeZoneTopBar(modifier: Modifier = Modifier) {
  val state by ServiceStateManager.dangerModeService.state.collectAsState()
  val safe = !state.isActive
  val colorScheme = MaterialTheme.colorScheme
  Box(
      modifier =
          Modifier.fillMaxWidth()
              .background(if (safe) colorScheme.primary else colorScheme.error)
              .padding(vertical = 8.dp),
      contentAlignment = Alignment.Center) {
        Text(
            text = "You are in a safe zone",
            color = if (safe) colorScheme.onPrimary else colorScheme.onError,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp)
      }
}
