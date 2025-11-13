package com.github.warnastrophy.core.ui.features.error

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
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
@Composable
fun ErrorScreen(
    message: String,
    onDismiss: () -> Unit,
    expanded: Boolean = true,
    errors: List<Error>
) {
  DropdownMenu(
      expanded = expanded,
      onDismissRequest = { onDismiss() },
      modifier =
          Modifier.fillMaxWidth()
              .widthIn(max = 300.dp)
              .background(Color.White, shape = RoundedCornerShape(8.dp))) {
        if (errors.isEmpty()) {
          DropdownMenuItem(text = { Text("No errors") }, onClick = { onDismiss() })
        } else {
          errors.forEach { err ->
            val errText = runCatching { err.message }.getOrDefault(err.toString())
            DropdownMenuItem(
                text = {
                  Text(errText, modifier = Modifier.testTag(ErrorScreenTestTags.ERROR_MESSAGE))
                },
                onClick = { onDismiss() })
          }
        }
      }
}
