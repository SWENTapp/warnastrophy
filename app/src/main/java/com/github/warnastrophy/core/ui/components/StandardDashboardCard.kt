package com.github.warnastrophy.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
This Composable represents a standard dashboard card with customizable background and border colors.
It features rounded corners and optional height constraints.
 */
@Composable
fun StandardDashboardCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    borderColor: Color? = null,
    minHeight: Dp? = null,
    maxHeight: Dp? = null,
    content: @Composable () -> Unit
) {
  Surface(
      shape = RoundedCornerShape(12.dp),
      color = backgroundColor,
      border = borderColor?.let { BorderStroke(1.dp, it) },
      tonalElevation = 0.dp,
      shadowElevation = 0.dp,
      modifier = modifier.heightIn(min = minHeight ?: 0.dp, max = maxHeight ?: Dp.Infinity)) {
        content()
      }
}
