package com.github.warnastrophy.core.ui.features.dashboard.activity

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.warnastrophy.core.data.repository.ActivityRepository
import com.github.warnastrophy.core.data.repository.ActivityRepositoryProvider
import com.github.warnastrophy.core.domain.model.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ActivityListUIState(
    val activities: List<Activity> = emptyList(),
    val errorMsg: String? = null,
)

class ActivityListViewModel(
    private val repository: ActivityRepository = ActivityRepositoryProvider.repository,
    private val userId: String
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
    viewModelScope.launch {
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
