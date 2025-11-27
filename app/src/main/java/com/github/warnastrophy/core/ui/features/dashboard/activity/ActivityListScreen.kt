package com.github.warnastrophy.core.ui.features.dashboard.activity

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.warnastrophy.core.model.Activity

object ActivityListScreenTestTags {
  const val ADD_ACTIVITY_BUTTON = "addButton"
  const val ACTIVITIES_LIST = "activitiesList"

  fun getTestTagForActivityItem(activity: Activity): String = "activityItem${activity.id}"
}

@Composable
private fun ActivityItem(activity: Activity, onActivityClick: () -> Unit) {
  Card(
      modifier =
          Modifier.fillMaxWidth()
              .padding(vertical = 4.dp, horizontal = 8.dp)
              .testTag(ActivityListScreenTestTags.getTestTagForActivityItem(activity))
              .clickable { onActivityClick() }) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween) {
              Column(modifier = Modifier.weight(1f)) {
                Text(text = activity.activityName, style = MaterialTheme.typography.titleMedium)
              }
            }
      }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityListScreen(
    activityListViewModel: ActivityListViewModel = viewModel(),
    onActivityClick: (Activity) -> Unit = {},
    onAddButtonClick: () -> Unit = {}
) {
  val context = LocalContext.current
  val uiState by activityListViewModel.uiState.collectAsState()
  val activities = uiState.activities

  LaunchedEffect(Unit) { activityListViewModel.refreshUIState() }

  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let { message ->
      Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
      activityListViewModel.clearErrorMsg()
    }
  }

  Scaffold(
      floatingActionButton = {
        FloatingActionButton(
            onClick = { onAddButtonClick() },
            modifier = Modifier.testTag(ActivityListScreenTestTags.ADD_ACTIVITY_BUTTON)) {
              Icon(Icons.Filled.Add, contentDescription = "Add Activity")
            }
      }) { paddingValues ->
        if (activities.isEmpty()) {
          Column(
              modifier = Modifier.fillMaxSize().padding(paddingValues),
              verticalArrangement = Arrangement.Center,
              horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No activities found. Tap '+' to add one.")
              }
        } else {
          LazyColumn(
              modifier =
                  Modifier.fillMaxSize()
                      .padding(paddingValues)
                      .testTag(ActivityListScreenTestTags.ACTIVITIES_LIST)) {
                items(activities, key = { it.id }) { activity ->
                  ActivityItem(activity = activity, onActivityClick = { onActivityClick(activity) })
                  HorizontalDivider(
                      Modifier,
                      DividerDefaults.Thickness,
                      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
              }
        }
      }
}

@Preview
@Composable
fun ActivityListScreenEmptyPreview() {
  MaterialTheme { ActivityListScreen() }
}
