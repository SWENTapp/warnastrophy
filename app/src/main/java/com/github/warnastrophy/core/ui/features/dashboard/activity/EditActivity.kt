package com.github.warnastrophy.core.ui.features.dashboard.activity

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

object EditActivityTestTags {
  const val INPUT_ACTIVITY_NAME = "inputActivityName"
  const val SAVE_BUTTON = "activitySave"
  const val DELETE_BUTTON = "activityDelete"
  const val ERROR_MESSAGE = "errorMessage"
}

/**
 * The screen composable for editing an existing activity.
 *
 * @param activityID The unique identifier of the activity to be loaded and edited.
 * @param editActivityViewModel The ViewModel responsible for handling the business logic and state
 *   for editing/deleting an activity.
 * @param onDone Lambda function to be invoked when an operation (save or delete) is successful and
 *   the screen should be navigated back.
 */
@Composable
fun EditActivityScreen(
    activityID: String = "1", // just for testing purpose
    editActivityViewModel: EditActivityViewModel = viewModel(),
    onDone: () -> Unit = {}
) {
  LaunchedEffect(activityID) { editActivityViewModel.loadActivity(activityID) }

  val activityUIState by editActivityViewModel.uiState.collectAsState()
  val errorMsg = activityUIState.errorMsg
  val isSaveButtonValid = activityUIState.isValid

  val context = LocalContext.current

  LaunchedEffect(errorMsg) {
    if (errorMsg != null) {
      Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
      editActivityViewModel.clearErrorMsg()
    }
  }

  LaunchedEffect(Unit) { editActivityViewModel.navigateBack.collect { onDone() } }

  Column(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Edit Activity Form",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp))

        OutlinedTextField(
            value = activityUIState.activityName,
            onValueChange = { editActivityViewModel.setActivityName(it) },
            label = { Text("Full Name") },
            isError = activityUIState.invalidActivityNameMsg != null,
            supportingText = {
              activityUIState.invalidActivityNameMsg?.let {
                Text(it, modifier = Modifier.testTag(EditActivityTestTags.ERROR_MESSAGE))
              }
            },
            modifier =
                Modifier.fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .testTag(EditActivityTestTags.INPUT_ACTIVITY_NAME))

        Button(
            onClick = { editActivityViewModel.editActivity(activityID) },
            enabled = isSaveButtonValid,
            modifier =
                Modifier.fillMaxWidth().height(50.dp).testTag(EditActivityTestTags.SAVE_BUTTON)) {
              Text("Save")
            }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { editActivityViewModel.deleteActivity(activityID) },
            colors =
                ButtonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White,
                    disabledContainerColor = Color.Gray,
                    disabledContentColor = Color.DarkGray),
            modifier =
                Modifier.fillMaxWidth().height(50.dp).testTag(EditActivityTestTags.DELETE_BUTTON)) {
              Text("Delete", color = Color.White)
            }
      }
}
