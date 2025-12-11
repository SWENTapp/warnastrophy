package com.github.warnastrophy.core.ui.features.dashboard.activity

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.github.warnastrophy.R

/**
 * Shared composable for activity form input fields. Used by both AddActivity and EditActivity
 * screens.
 *
 * @param viewModel The ViewModel (AddActivityViewModel or EditActivityViewModel)
 * @param activityNameTestTag Test tag for the activity name field
 * @param errorMessageTestTag Test tag for error messages
 * @param preDangerThresholdTestTag Test tag for pre-danger threshold field
 * @param preDangerTimeoutTestTag Test tag for pre-danger timeout field
 * @param dangerAverageThresholdTestTag Test tag for danger average threshold field
 */
@Composable
fun ActivityFormFields(
    viewModel: AddActivityViewModel,
    activityNameTestTag: String,
    errorMessageTestTag: String,
    preDangerThresholdTestTag: String,
    preDangerTimeoutTestTag: String,
    dangerAverageThresholdTestTag: String
) {
  val state by viewModel.uiState.collectAsState()

  OutlinedTextField(
      value = state.activityName,
      onValueChange = { viewModel.setActivityName(it) },
      label = { Text(stringResource(R.string.activity_name_label)) },
      isError = state.activityName.isBlank(),
      modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).testTag(activityNameTestTag))

  OutlinedTextField(
      value = state.preDangerThresholdStr,
      onValueChange = { viewModel.setPreDangerThreshold(it) },
      label = { Text(stringResource(R.string.pre_danger_threshold_label)) },
      isError = state.isPreDangerThresholdError,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).testTag(preDangerThresholdTestTag))

  OutlinedTextField(
      value = state.preDangerTimeoutStr,
      onValueChange = { viewModel.setPreDangerTimeout(it) },
      label = { Text(stringResource(R.string.pre_danger_timeout_label)) },
      isError = state.isPreDangerTimeoutError,
      modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp).testTag(preDangerTimeoutTestTag))

  OutlinedTextField(
      value = state.dangerAverageThresholdStr,
      onValueChange = { viewModel.setDangerAverageThreshold(it) },
      label = { Text(stringResource(R.string.danger_average_threshold_label)) },
      isError = state.isDangerAverageThresholdError,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
      modifier =
          Modifier.fillMaxWidth().padding(bottom = 16.dp).testTag(dangerAverageThresholdTestTag))
}
