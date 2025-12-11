package com.github.warnastrophy.core.ui.features.dashboard.activity

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.warnastrophy.core.data.repository.ActivityRepository
import com.github.warnastrophy.core.data.service.StateManagerService
import com.github.warnastrophy.core.model.Activity
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
 * ViewModel for managing Activity forms. Handles form input changes, validation, and creating new
 * activities.
 *
 * @property repository The data source dependency used for activity persistence.
 * @property userId id of user using the app
 * @property dispatcher dispatcher default to [Dispatchers.IO]
 */
open class AddActivityViewModel(
    protected val repository: ActivityRepository = StateManagerService.activityRepository,
    protected val userId: String,
    protected val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

  protected val _uiState = MutableStateFlow(ActivityFormState())
  val uiState: StateFlow<ActivityFormState> = _uiState.asStateFlow()

  protected val _navigateBack = MutableSharedFlow<Unit>(replay = 0)
  val navigateBack: SharedFlow<Unit> = _navigateBack.asSharedFlow()

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.value = _uiState.value.copy(errorMsg = null)
  }

  /** Sets an error message in the UI state. */
  protected fun setErrorMsg(errorMsg: String) {
    _uiState.value = _uiState.value.copy(errorMsg = errorMsg)
  }

  /** Adds a new activity. */
  fun addActivity() {
    val state = _uiState.value
    if (!state.isValid) {
      setErrorMsg("At least one field is not valid!")
      return
    }
    val config =
        state.toMovementConfig()
            ?: run {
              setErrorMsg("Invalid movement configuration!")
              return
            }
    val activity =
        Activity(
            id = repository.getNewUid(), activityName = state.activityName, movementConfig = config)
    executeRepositoryOperation(
        operation = { repository.addActivity(userId, activity) }, actionName = "add activity")
  }

  fun setActivityName(activityName: String) {
    _uiState.value = _uiState.value.copy(activityName = activityName)
  }

  fun setPreDangerThreshold(value: String) {
    _uiState.value = _uiState.value.copy(preDangerThresholdStr = value)
  }

  fun setPreDangerTimeout(value: String) {
    _uiState.value = _uiState.value.copy(preDangerTimeoutStr = value)
  }

  fun setDangerAverageThreshold(value: String) {
    _uiState.value = _uiState.value.copy(dangerAverageThresholdStr = value)
  }

  /**
   * Executes a repository operation and handles success/failure. Useful to reduce Add/Edit screen
   * code duplication.
   *
   * @param operation The suspend function doing the repository operation.
   * @param actionName A descriptive name of the action for logging and error messages.
   */
  protected fun <T> executeRepositoryOperation(
      operation: suspend () -> Result<T>,
      actionName: String
  ) {
    viewModelScope.launch(dispatcher) {
      operation()
          .onSuccess {
            clearErrorMsg()
            _navigateBack.emit(Unit)
          }
          .onFailure { exception ->
            Log.e("ActivityViewModel", "Error $actionName", exception)
            setErrorMsg("Failed to $actionName: ${exception.message ?: "Unknown error"}")
          }
    }
  }
}
