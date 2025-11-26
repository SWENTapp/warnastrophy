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
 * Represents the mutable state of the UI for the screen where a user can add an activity. This
 * state is typically exposed by a ViewModel to be observed by a Composable function.
 *
 * @property activityName The current text input for the activity's name.
 * @property errorMsg A general error message to display, usually for repository/network failures.
 * @property invalidActivityName A specific error message for input validation failure on the
 *   activity name field.
 */
data class AddActivityUIState(
    val activityName: String = "",
    val errorMsg: String? = null,
    val invalidActivityName: String? = null
) {
  val isValid: Boolean
    get() = activityName.isNotBlank()
}

/**
 * ViewModel responsible for managing the state and logic for the Add Activity screen.
 *
 * It handles form input changes, validates fields, and manages the asynchronous operation of
 * persisting a new activity via the repository.
 *
 * @property repository The data source dependency used for activity persistence.
 * @property userId id of user using the app
 */
class AddActivityViewModel(
    private val repository: ActivityRepository = ActivityRepositoryProvider.repository,
    private val userId: String,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
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

  fun setActivityName(activityName: String) {
    _uiState.value =
        _uiState.value.copy(
            activityName = activityName,
            invalidActivityName = if (activityName.isBlank()) "Full name cannot be empty" else null)
  }

  private fun addActivityToRepository(activity: Activity) {
    viewModelScope.launch(dispatcher) {
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
