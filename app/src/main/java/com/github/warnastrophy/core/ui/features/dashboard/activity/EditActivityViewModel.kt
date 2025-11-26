package com.github.warnastrophy.core.ui.features.dashboard.activity

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.warnastrophy.core.data.repository.ActivityRepository
import com.github.warnastrophy.core.data.repository.ActivityRepositoryProvider
import com.github.warnastrophy.core.domain.model.Activity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Represents the mutable state of the UI for the screen where a user can edit an activity. This
 * state is typically exposed by a ViewModel to be observed by a Composable function.
 *
 * @property activityName The current text input for the activity's name.
 * @property errorMsg A general error message to display, usually for repository/network failures.
 * @property invalidActivityName A specific error message for input validation failure on the
 *   activity name field.
 */
data class EditActivityUIState(
    val activityName: String = "",
    val errorMsg: String? = null,
    val invalidActivityName: String? = null
) {
  val isValid: Boolean
    get() = activityName.isNotBlank()
}

/**
 * ViewModel responsible for managing the state and logic for the Edit Activity screen.
 *
 * It handles form input changes, validates fields, and manages the asynchronous operation of
 * persisting a new activity via the repository.
 *
 * @property repository The data source dependency used for activity persistence.
 * @property userId id of user using the app
 * @property dispatcher dispatcher defaut to [Dispatchers.IO]
 */
class EditActivityViewModel(
    private val repository: ActivityRepository = ActivityRepositoryProvider.repository,
    private val userId: String,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
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

  private fun <T> executeRepositoryOperation(
      operation: suspend () -> Result<T>,
      actionName: String // e.g., "edit Contact" or "delete Contact"
  ) {
    viewModelScope.launch(dispatcher) {
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
    viewModelScope.launch(dispatcher) {
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
      setErrorMsg("At least one field is not valid!")
      return
    }
    val newActivity = Activity(id = id, activityName = state.activityName)
    executeRepositoryOperation(
        operation = { repository.editActivity(id, userId, newActivity) },
        actionName = "edit activity")
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

  fun setActivityName(activityName: String) {
    _uiState.value =
        _uiState.value.copy(
            activityName = activityName,
            invalidActivityName = if (activityName.isBlank()) "Full name cannot be empty" else null)
  }
}
