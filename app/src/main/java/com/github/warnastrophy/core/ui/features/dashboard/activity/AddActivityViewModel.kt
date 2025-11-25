package com.github.warnastrophy.core.ui.features.dashboard.activity

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.warnastrophy.core.data.repository.ActivityRepository
import com.github.warnastrophy.core.data.repository.ActivityRepositoryProvider
import com.github.warnastrophy.core.domain.model.Activity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AddActivityUIState(
    val activityName: String = "",
    val errorMsg: String? = null,
    val invalidActivityName: String? = null
) {
  val isValid: Boolean
    get() = activityName.isNotBlank()
}

class AddActivityViewModel(
    private val repository: ActivityRepository = ActivityRepositoryProvider.repository,
    private val userId: String
) : ViewModel() {
  private val _uiState = MutableStateFlow(AddActivityUIState())
  val uiState: StateFlow<AddActivityUIState> = _uiState.asStateFlow()

  private val _navigateBack = MutableSharedFlow<Unit>(replay = 0)
  var navigateBack: SharedFlow<Unit> = _navigateBack.asSharedFlow()

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /** Sets an error message in the UI state. */
  private fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  /** Adds a Activity document. */
  fun addActivity() {
    val state = _uiState.value
    if (!state.isValid) {
      setErrorMsg("At least one field is not valid!")
      return
    }
    addActivityToRepository(
        Activity(id = repository.getNewUid(), activityName = state.activityName))
  }

  // Functions to update the UI state.
  fun setActivityName(activityName: String) {
    _uiState.value =
        _uiState.value.copy(
            activityName = activityName,
            invalidActivityName = if (activityName.isBlank()) "Full name cannot be empty" else null)
  }

  private fun addActivityToRepository(activity: Activity) {
    viewModelScope.launch {
      val result = repository.addActivity(userId, activity)
      result
          .onSuccess {
            clearErrorMsg()
            _navigateBack.emit(Unit)
          }
          .onFailure { exception ->
            Log.e("ActivityListViewModel", "Error add Activity", exception)
            setErrorMsg("Failed to add Activity: ${exception.message ?: "Unknown error"}")
          }
    }
  }
}
