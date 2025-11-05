package com.github.warnastrophy.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * This Composable represents a standard dashboard button with customizable color and label. It
 * features rounded corners, a border, and padding for better touch interaction.
 */
@Composable
fun StandardDashboardButton(
    color: Color = Color(0xFFF5F5F5), // light gray
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    textColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    borderColor: Color = MaterialTheme.colorScheme.onError
) {
  Surface(
      onClick = onClick,
      shape = RoundedCornerShape(20.dp),
      color = color,
      modifier = modifier,
      tonalElevation = 0.dp,
      border = BorderStroke(1.dp, borderColor)) { // shadow grey
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
      }
}
