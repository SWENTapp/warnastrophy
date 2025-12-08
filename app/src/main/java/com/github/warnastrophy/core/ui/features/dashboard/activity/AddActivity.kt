package com.github.warnastrophy.core.ui.features.dashboard.activity

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

object AddActivityTestTags {
  const val INPUT_ACTIVITY_NAME = "inputActivityName"
  const val ERROR_MESSAGE = "errorMessage"
  const val SAVE_BUTTON = "activitySave"
  const val PRE_DANGER_THRESHOLD_INPUT = "inputPreDangerThreshold"
  const val PRE_DANGER_TIMEOUT_INPUT = "inputPreDangerTimeout"
  const val DANGER_AVERAGE_THRESHOLD_INPUT = "inputDangerAverageThreshold"
}

/**
 * The screen composable for adding a new activity. This screen provides a form where the user can
 * input the name of a new activity.
 *
 * @param addActivityViewModel The ViewModel responsible for handling the business logic and state
 *   for adding an activity.
 * @param onDone Lambda function to be invoked when the activity is successfully saved and the
 *   screen should be navigated back.
 */
@Composable
fun AddActivityScreen(
    addActivityViewModel: AddActivityViewModel = viewModel(),
    onDone: () -> Unit = {}
) {
  val activityUIState by addActivityViewModel.uiState.collectAsState()
  val errorMsg = activityUIState.errorMsg
  val isSaveButtonValid = activityUIState.isValid

  val context = LocalContext.current
  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      addActivityViewModel.clearErrorMsg()
    }
  }

  LaunchedEffect(Unit) { addActivityViewModel.navigateBack.collect { onDone() } }

  Column(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Add Activity Form",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp))

        ActivityFormFields(
            viewModel = addActivityViewModel,
            activityNameTestTag = AddActivityTestTags.INPUT_ACTIVITY_NAME,
            errorMessageTestTag = AddActivityTestTags.ERROR_MESSAGE,
            preDangerThresholdTestTag = AddActivityTestTags.PRE_DANGER_THRESHOLD_INPUT,
            preDangerTimeoutTestTag = AddActivityTestTags.PRE_DANGER_TIMEOUT_INPUT,
            dangerAverageThresholdTestTag = AddActivityTestTags.DANGER_AVERAGE_THRESHOLD_INPUT)

        Button(
            onClick = { addActivityViewModel.addActivity() },
            enabled = isSaveButtonValid,
            modifier =
                Modifier.fillMaxWidth().height(50.dp).testTag(AddActivityTestTags.SAVE_BUTTON)) {
              Text("Save")
            }
      }
}
