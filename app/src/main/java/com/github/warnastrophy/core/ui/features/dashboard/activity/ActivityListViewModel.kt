package com.github.warnastrophy.core.ui.features.dashboard.activity

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.warnastrophy.core.data.repository.ActivityRepository
import com.github.warnastrophy.core.data.repository.ActivityRepositoryProvider
import com.github.warnastrophy.core.model.Activity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the mutable state of the UI for the screen where a user can see activities list. This
 * state is typically exposed by a ViewModel to be observed by a Composable function.
 *
 * @property activities The current list of Activity objects to be displayed. Defaults to an empty
 *   list.
 * @property errorMsg A general error message to display, usually for repository/network failures.
 */
data class ActivityListUIState(
    val activities: List<Activity> = emptyList(),
    val errorMsg: String? = null,
)

/**
 * ViewModel responsible for managing the UI state of the Activity List screen.
 *
 * This class acts as a communication bridge between the UI (View) and the data layer (Repository).
 * It loads contact data from the repository, handles asynchronous operations, applies business
 * logic, and exposes the result as an observable
 * [com.github.warnastrophy.core.ui.features.dashboard.activity.ActivityListUIState] that the UI
 * consumes.
 *
 * @property repository The dependency responsible for fetching, caching, and persisting activities
 *   data.
 *     @property userId id of user using the app
 *     @property dispatcher dispatcher defaut to [Dispatchers.IO]
 */
class ActivityListViewModel(
    private val repository: ActivityRepository = ActivityRepositoryProvider.repository,
    private val userId: String,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

  private val _uiState = MutableStateFlow(ActivityListUIState())
  val uiState: StateFlow<ActivityListUIState> = _uiState.asStateFlow()

  init {
    refreshUIState()
  }

  /** Refreshes the UI state by fetching all Contact items from the repository. */
  fun refreshUIState() {
    getAllActivities()
  }

  private fun getAllActivities() {
    viewModelScope.launch(dispatcher) {
      val result = repository.getAllActivities(userId)

      result.fold(
          onSuccess = { activities ->
            _uiState.value = ActivityListUIState(activities = activities)
          },
          onFailure = { e ->
            Log.e("ActivityListViewModel", "Error fetching contacts", e)
            setErrorMsg("Failed to load Activities: ${e.message}")
          })
    }
  }

  /** Sets an error message in the UI state. */
  private fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }
}
