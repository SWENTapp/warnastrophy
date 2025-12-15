package com.github.warnastrophy.core.ui.features.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

object ConfirmationPopupTestTags {
  const val DIALOG = "confirmationPopupDialog"
  const val TITLE = "confirmationPopupTitle"
  const val MESSAGE = "confirmationPopupMessage"
  const val CONFIRM_BUTTON = "confirmationPopupConfirm"
  const val CANCEL_BUTTON = "confirmationPopupCancel"
  const val TITLE_TEXT = "confirmationPopupTitleText"
  const val DESCRIPTION_TEXT = "confirmationPopupDescriptionText"
}

@Composable
fun ConfirmationPopup(
    title: String,
    message: String,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
  AlertDialog(
      modifier = modifier.testTag(ConfirmationPopupTestTags.DIALOG),
      onDismissRequest = onCancel,
      title = {
        Text(
            modifier = Modifier.testTag(ConfirmationPopupTestTags.TITLE_TEXT),
            text = title,
            style = MaterialTheme.typography.titleLarge)
      },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          Text(
              modifier = Modifier.testTag(ConfirmationPopupTestTags.DESCRIPTION_TEXT),
              text = message,
              style = MaterialTheme.typography.bodyMedium)
          Spacer(modifier = Modifier.height(12.dp))
          Row(
              modifier =
                  Modifier.fillMaxWidth().semantics { contentDescription = "confirmationActions" },
              horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    modifier = Modifier.weight(1f).testTag(ConfirmationPopupTestTags.CANCEL_BUTTON),
                    onClick = onCancel) {
                      Text(text = cancelLabel)
                    }
                TextButton(
                    modifier =
                        Modifier.weight(1f).testTag(ConfirmationPopupTestTags.CONFIRM_BUTTON),
                    onClick = onConfirm) {
                      Text(text = confirmLabel)
                    }
              }
        }
      },
      confirmButton = {},
      dismissButton = {})
}

@Preview
@Composable
fun ConfirmationPopupPreview() {
  ConfirmationPopup(
      title = "Delete Item",
      message = "Are you sure you want to delete this item? This action cannot be undone.",
      confirmLabel = "Delete",
      cancelLabel = "Cancel",
      onConfirm = {},
      onCancel = {})
}
