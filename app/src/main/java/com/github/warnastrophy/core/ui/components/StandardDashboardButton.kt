package com.github.warnastrophy.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StandardDashboardButton(
    color: Color = Color(0xFFF5F5F5),
    label: String,
    onClick: () -> Unit = {}
) {
  Surface(
      onClick = onClick,
      shape = RoundedCornerShape(20.dp),
      color = color,
      tonalElevation = 0.dp,
      border = BorderStroke(1.dp, Color(0xFFBDBDBD))) {
        Text(
            text = label,
            color = Color.Black,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
      }
}
