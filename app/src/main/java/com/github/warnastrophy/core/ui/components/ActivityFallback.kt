package com.github.warnastrophy.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.github.warnastrophy.R

// Test tag
const val FALLBACK_ACTIVITY_ERROR = "activityUnavailableFallback"

/**
 * A fallback Composable to display when the Activity context is not available.
 *
 * This component prevents crashes and provides a clear message to developers/users
 *
 * Usage:
 * ```
 * val activity = LocalContext.current.findActivity()
 * if (activity == null) {
 *     ActivityFallback()
 *     return
 *   }
 * ```
 *
 * @param modifier The modifier to be applied to the Column layout.
 * @param message The error message to be displayed. Defaults to a generic message.
 */
@Composable
fun ActivityFallback(
    modifier: Modifier = Modifier,
    message: String = stringResource(R.string.no_activity_fallback_message)
) {
  Column(
      modifier = modifier.fillMaxSize().testTag(FALLBACK_ACTIVITY_ERROR),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
        )
      }
}
