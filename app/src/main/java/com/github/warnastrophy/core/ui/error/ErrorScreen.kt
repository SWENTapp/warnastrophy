package com.github.warnastrophy.core.ui.error

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.warnastrophy.core.model.Error
import com.github.warnastrophy.core.ui.navigation.Screen

object ErrorScreenTestTags {
  const val ERROR_MESSAGE = "error_message"
}

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

@Preview
@Composable
fun ErrorScreenPreview() {
  val sampleErrors =
      listOf(
          Error("First error message", Screen.MAP),
          Error("Second error message", Screen.HOME),
          Error("Third error message", Screen.PROFILE))

  ErrorScreen(
      message = "Sample error messages", onDismiss = {}, expanded = true, errors = sampleErrors)
}
