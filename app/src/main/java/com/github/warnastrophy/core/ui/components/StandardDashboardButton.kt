package com.github.warnastrophy.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * This Composable represents a standard dashboard button with customizable color and label. It
 * features rounded corners, a border, and padding for better touch interaction.
 *
 * @param color The background color of the button. Default is light gray.
 * @param label The text label displayed on the button.
 * @param modifier Modifier to be applied to the button.
 * @param onClick Lambda function to be invoked when the button is clicked.
 * @param textColor The color of the text label. Default is the onSecondaryContainer color from the
 *   Material theme.
 * @param borderColor The color of the button border. Default is the onError color from the Material
 *   theme.
 * @param icon A Composable function representing an optional icon to be displayed alongside the
 */
@Composable
fun StandardDashboardButton(
    label: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondaryContainer,
    onClick: () -> Unit = {},
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    borderColor: Color = MaterialTheme.colorScheme.surface,
    icon: @Composable () -> Unit = {},
    isSelected: Boolean? = null
) {
  val effectiveModifier =
      if (isSelected != null) modifier.semantics { selected = isSelected } else modifier

  Surface(
      onClick = onClick,
      shape = RoundedCornerShape(20.dp),
      color = color,
      modifier = effectiveModifier,
      tonalElevation = 0.dp,
      border = BorderStroke(1.dp, borderColor)) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(text = label, color = textColor, fontSize = 13.sp)

          icon()
        }
      }
}
