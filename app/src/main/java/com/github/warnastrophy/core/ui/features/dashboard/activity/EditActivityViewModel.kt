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

data class EditActivityUIState(
    val activityName: String = "",
    val errorMsg: String? = null,
    val invalidActivityName: String? = null
) {
  val isValid: Boolean
    get() = activityName.isNotBlank()
}

class EditActivityViewModel(
    private val repository: ActivityRepository = ActivityRepositoryProvider.repository,
    private val userId: String
) : ViewModel() {
  private val _uiState = MutableStateFlow(EditActivityUIState())
  val uiState: StateFlow<EditActivityUIState> = _uiState.asStateFlow()

  private val _navigateBack = MutableSharedFlow<Unit>(replay = 0)
  val navigateBack: SharedFlow<Unit> = _navigateBack.asSharedFlow()

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /** Sets an error message in the UI state. */
  private fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  // Helper function
  private fun <T> executeRepositoryOperation(
      operation: suspend () -> Result<T>,
      actionName: String // e.g., "edit Contact" or "delete Contact"
  ) {
    viewModelScope.launch {
      val result = operation()

      result
          .onSuccess {
            clearErrorMsg()
            _navigateBack.emit(Unit)
          }
          .onFailure { exception ->
            val logTag = "EditActivityViewModel"
            val errorMessage = "Failed to $actionName: ${exception.message ?: "Unknown error"}"

            Log.e(logTag, "Error $actionName", exception)
            setErrorMsg(errorMessage)
          }
    }
  }

  /**
   * Loads a Activity by its ID and updates the UI state.
   *
   * @param activityId The ID of the Contact to be loaded.
   */
  fun loadActivity(activityId: String) {
    viewModelScope.launch {
      val res = repository.getActivity(activityId, userId)
      res.fold(
          onSuccess = { activity ->
            _uiState.value = EditActivityUIState(activityName = activity.activityName)
          },
          onFailure = { e ->
            Log.e("EditActivityViewModel", "Error fetching contacts", e)
            setErrorMsg("Failed to load contacts: ${e.message}")
          })
    }
  }

  /**
   * Adds a Activity document.
   *
   * @param id The activity document to be added.
   */
  fun editActivity(id: String) {
    val state = _uiState.value
    if (!state.isValid) {
      setErrorMsg("At least one field is not valid")
      return
    }
    val newActivity = Activity(id = id, activityName = state.activityName)
    executeRepositoryOperation(
        operation = { repository.editActivity(id, userId, newActivity) },
        actionName = "edit activtiy")
  }

  /**
   * Deletes a Contact document by its ID.
   *
   * @param activityID The ID of the Contact document to be deleted.
   */
  fun deleteActivity(activityID: String) {
    executeRepositoryOperation(
        operation = { repository.deleteActivity(userId, activityID) },
        actionName = "delete activity")
  }

  // Functions to update the UI state.
  fun setActivityName(activityName: String) {
    _uiState.value =
        _uiState.value.copy(
            activityName = activityName,
            invalidActivityName = if (activityName.isBlank()) "Full name cannot be empty" else null)
  }
}
