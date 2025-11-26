package com.github.warnastrophy.core.ui.features.error

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.warnastrophy.core.ui.common.Error

/** Object containing test tags for the ErrorScreen composable. */
object ErrorScreenTestTags {
  const val ERROR_MESSAGE = "error_message"
}

/**
 * A composable function that displays an error screen with a list of error messages.
 *
 * @param message The main error message to display.
 * @param onDismiss A callback function to be invoked when the error screen is dismissed.
 * @param expanded A boolean indicating whether the error screen is expanded or not.
 * @param errors A list of [Error] objects representing the errors to be displayed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ErrorScreen(
    message: String,
    onDismiss: () -> Unit,
    expanded: Boolean = true,
    errors: List<Error>
) {
  if (!expanded) return

  androidx.compose.ui.window.Dialog(onDismissRequest = { onDismiss() }) {
    Surface(
        modifier = Modifier.fillMaxWidth().widthIn(max = 360.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    val iconTint =
                        if (errors.isEmpty()) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                    val icon =
                        if (errors.isEmpty()) Icons.Filled.CheckCircle else Icons.Filled.Warning
                    Box(
                        modifier =
                            Modifier.background(
                                    color = iconTint.copy(alpha = 0.12f), shape = CircleShape)
                                .padding(8.dp),
                        contentAlignment = Alignment.Center) {
                          Icon(imageVector = icon, contentDescription = null, tint = iconTint)
                        }
                    Text(
                        text = message,
                        modifier = Modifier.padding(start = 12.dp),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface)
                  }

                  Button(onClick = { onDismiss() }) { Text(text = "Close") }
                }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            val scrollState = rememberScrollState()
            Column(
                modifier =
                    Modifier.fillMaxWidth().heightIn(max = 220.dp).verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  if (errors.isEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(
                          "No errors",
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                          fontWeight = FontWeight.Medium)
                    }
                  } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                          errors.forEach { err ->
                            val errText =
                                runCatching { err.type.message }.getOrDefault(err.toString())
                            // Simple chip
                            Box(
                                modifier =
                                    Modifier.background(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(16.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .testTag(ErrorScreenTestTags.ERROR_MESSAGE),
                                contentAlignment = Alignment.Center) {
                                  Text(
                                      text = errText,
                                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                                      fontSize = 14.sp)
                                }
                          }
                        }
                  }
                }
          }
        }
  }
}
