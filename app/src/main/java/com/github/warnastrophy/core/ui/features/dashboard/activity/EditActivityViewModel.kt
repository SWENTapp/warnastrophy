package com.github.warnastrophy.core.ui.features.dashboard.activity

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.github.warnastrophy.core.data.interfaces.ActivityRepository
import com.github.warnastrophy.core.data.service.StateManagerService
import com.github.warnastrophy.core.model.Activity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel for editing existing activities. Extends AddActivityViewModel and adds edit and delete
 * functionality.
 *
 * @property repository The data source dependency used for activity persistence.
 * @property userId id of user using the app
 * @property dispatcher dispatcher default to [Dispatchers.IO]
 */
class EditActivityViewModel(
    repository: ActivityRepository = StateManagerService.activityRepository,
    userId: String,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : AddActivityViewModel(repository, userId, dispatcher) {

  /**
   * Loads an activity by its ID and updates the UI state.
   *
   * @param activityId The ID of the activity to be loaded.
   */
  fun loadActivity(activityId: String) {
    viewModelScope.launch(dispatcher) {
      repository
          .getActivity(activityId, userId)
          .fold(
              onSuccess = { activity ->
                _uiState.value =
                    ActivityFormState(
                        activityName = activity.activityName,
                        preDangerThresholdStr =
                            activity.movementConfig.preDangerThreshold.toString(),
                        preDangerTimeoutStr = activity.movementConfig.preDangerTimeout.toString(),
                        dangerAverageThresholdStr =
                            activity.movementConfig.dangerAverageThreshold.toString())
              },
              onFailure = { e ->
                Log.e("EditActivityViewModel", "Error loading activity", e)
                setErrorMsg("Failed to load activity: ${e.message}")
              })
    }
  }

  /**
   * Updates an existing activity with the current form values.
   *
   * @param id The activity ID to update.
   */
  fun editActivity(id: String) {
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
    val activity = Activity(id = id, activityName = state.activityName, movementConfig = config)
    executeRepositoryOperation(
        operation = { repository.editActivity(id, userId, activity) }, actionName = "edit activity")
  }

  /**
   * Deletes an activity by its ID.
   *
   * @param activityID The ID of the activity to delete.
   */
  fun deleteActivity(activityID: String) {
    executeRepositoryOperation(
        operation = { repository.deleteActivity(userId, activityID) },
        actionName = "delete activity")
  }
}
