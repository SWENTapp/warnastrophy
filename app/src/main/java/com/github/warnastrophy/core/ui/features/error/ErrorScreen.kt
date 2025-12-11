package com.github.warnastrophy.core.ui.features.error

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.warnastrophy.R
import com.github.warnastrophy.core.ui.common.Error
import com.github.warnastrophy.core.ui.common.ErrorType
import com.github.warnastrophy.core.ui.theme.extendedColors

/** Object containing test tags for the ErrorScreen composable. */
object ErrorScreenTestTags {
  const val ERROR_SUGGESTION = "error_suggestion"
  const val ERROR_SUGGESTION_TEXT = "error_suggestion_text"
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

  val noErrorColor = Color(0xFF2E7D32)

  androidx.compose.ui.window.Dialog(onDismissRequest = { onDismiss() }) {
    Surface(
        modifier =
            Modifier.fillMaxWidth()
                .widthIn(max = 360.dp)
                .background(
                    MaterialTheme.extendedColors.backgroundOffWhite,
                    shape = RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp) {
          Column(modifier = Modifier.padding(16.dp)) {
            ErrorHeader(
                message = message,
                errors = errors,
                onDismiss = onDismiss,
                noErrorColor = noErrorColor)
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            ErrorContent(errors = errors)
          }
        }
  }
}

@Composable
private fun ErrorHeader(
    message: String,
    errors: List<Error>,
    onDismiss: () -> Unit,
    noErrorColor: Color
) {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    val iconTint = if (errors.isEmpty()) noErrorColor else MaterialTheme.colorScheme.error
    val icon = if (errors.isEmpty()) Icons.Filled.CheckCircle else Icons.Filled.Warning

    Box(
        modifier =
            Modifier.background(color = iconTint.copy(alpha = 0.12f), shape = CircleShape)
                .padding(8.dp),
        contentAlignment = Alignment.Center) {
          Icon(imageVector = icon, contentDescription = null, tint = iconTint)
        }
    Spacer(modifier = Modifier.padding(start = 12.dp))
    Text(
        text = message,
        modifier = Modifier.weight(1f).padding(end = 12.dp),
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.onSurface)
    Button(
        onClick = { onDismiss() },
        modifier =
            Modifier.align(Alignment.CenterVertically)
                .defaultMinSize(minHeight = 36.dp)
                .heightIn(max = 56.dp)) {
          Text(text = "Close")
        }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ErrorContent(errors: List<Error>) {
  val scrollState = rememberScrollState()
  Column(
      modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp).verticalScroll(scrollState),
      verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (errors.isEmpty()) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(ErrorType.NO_ERRORS.message),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium)
          }
        } else {
          FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)) {
                errors.forEach { err -> ErrorChip(err) }
              }
        }
      }
}

@Composable
private fun ErrorChip(err: Error) {
  Box(
      modifier =
          Modifier.background(
                  color = MaterialTheme.colorScheme.surfaceVariant,
                  shape = RoundedCornerShape(16.dp))
              .padding(horizontal = 12.dp, vertical = 8.dp)
              .testTag(ErrorScreenTestTags.ERROR_SUGGESTION),
      contentAlignment = Alignment.Center) {
        Text(
            modifier = Modifier.testTag(ErrorScreenTestTags.ERROR_SUGGESTION_TEXT),
            text = ErrorSuggestion(err),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp)
      }
}

@Composable
private fun ErrorSuggestion(err: Error): String {
  return when (err.type) {
    ErrorType.LOCATION_NOT_GRANTED_ERROR ->
        stringResource(R.string.error_suggestion_enable_location)
    ErrorType.LOCATION_UPDATE_ERROR -> stringResource(R.string.error_suggestion_enable_location)
    ErrorType.FOREGROUND_ERROR -> stringResource(R.string.error_suggestion_allow_notifications)
    ErrorType.LOCATION_ERROR -> stringResource(R.string.error_suggestion_enable_location)
    ErrorType.HAZARD_FETCHING_ERROR -> stringResource(R.string.error_suggestion_check_internet)
    ErrorType.SPEECH_RECOGNITION_ERROR ->
        stringResource(R.string.error_suggestion_speech_recognition)
    ErrorType.TEXT_TO_SPEECH_ERROR -> stringResource(R.string.error_suggestion_retry)
    ErrorType.NO_ERRORS -> ""
    ErrorType.ACTIVITY_REPOSITORY_ERROR -> stringResource(R.string.error_activity_repository_failed)
    ErrorType.TEXT_TO_SPEECH_INIT_ERROR -> "salut"
  }
}
