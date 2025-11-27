package com.github.warnastrophy.core.ui.features.dashboard.activity

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

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

        OutlinedTextField(
            value = activityUIState.activityName,
            onValueChange = { addActivityViewModel.setActivityName(it) },
            label = { Text("Activity Name") },
            isError = activityUIState.invalidActivityName != null,
            supportingText = { activityUIState.invalidActivityName?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))

        Button(
            onClick = { addActivityViewModel.addActivity() },
            enabled = isSaveButtonValid,
            modifier = Modifier.fillMaxWidth().height(50.dp)) {
              Text("Save Activity")
            }
      }
}
