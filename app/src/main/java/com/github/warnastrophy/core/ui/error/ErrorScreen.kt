package com.github.warnastrophy.core.ui.error

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ErrorScreen(message: String, onDismiss: () -> Unit) {
  Dialog(
      onDismissRequest = onDismiss,
      properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier =
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable {
                  onDismiss()
                },
            contentAlignment = Alignment.Center) {
              Card(
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.padding(16.dp).wrapContentWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp).widthIn(min = 200.dp, max = 360.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                          Text(
                              text = message,
                              style = MaterialTheme.typography.bodyMedium,
                              color = Color.Black)
                          Spacer(modifier = Modifier.height(12.dp))
                          Button(onClick = onDismiss) { Text(text = "Dismiss") }
                        }
                  }
            }
      }
}

@Preview
@Composable
fun ErrorScreenPreview() {
  ErrorScreen(message = "An unexpected error occurred.") {}
}
