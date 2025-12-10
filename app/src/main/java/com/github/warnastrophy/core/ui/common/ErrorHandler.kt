package com.github.warnastrophy.core.ui.common

import androidx.lifecycle.ViewModel
import com.github.warnastrophy.R
import com.github.warnastrophy.core.ui.navigation.Screen
import com.github.warnastrophy.core.util.isSubsequenceOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ErrorType(val message: Int) {
  NO_ERRORS(R.string.no_errors),
  LOCATION_NOT_GRANTED_ERROR(R.string.error_location_not_granted),
  LOCATION_UPDATE_ERROR(R.string.error_location_update_failed),
  FOREGROUND_ERROR(R.string.error_foreground_start_failed),
  LOCATION_ERROR(R.string.error_location_unavailable),
  HAZARD_FETCHING_ERROR(R.string.error_hazard_fetch_failed),
  SPEECH_RECOGNITION_ERROR(R.string.error_speech_recognition_failed),
  ACTIVITY_REPOSITORY_ERROR(R.string.error_activity_repository_failed),
  EMERGENCY_SMS_FAILED(R.string.error_emergency_sms_failed),
  EMERGENCY_CALL_FAILED(R.string.error_emergency_call_failed),
  EMERGENCY_ACTION_CANCELLED(R.string.error_emergency_action_cancelled),
  NO_EMERGENCY_CONTACT(R.string.error_no_emergency_contact),
}

/** ViewModel responsible for managing error messages associated with different screens. */
class ErrorHandler : ViewModel() {
  private val _state = MutableStateFlow(ErrorState())

  /** Public state flow exposing the current error state. */
  val state: StateFlow<ErrorState> = _state

  /**
   * Adds an error of a specific type associated with a given screen to the error state. Prevents
   * duplicate errors for the same type and screen.
   *
   * @param type The type of error to add.
   * @param screen The screen associated with the error.
   */
  fun addErrorToScreen(type: ErrorType, screen: Screen) {
    if (_state.value.errors.any { it.type == type && it.screenTypes.contains(screen) }) return
    _state.value = _state.value.copy(errors = _state.value.errors + Error(type, listOf(screen)))
  }

  /**
   * Adds an error of a specific type associated with given screens to the error state. Prevents
   * duplicate errors for the same type and screens.
   *
   * @param type The type of error to add.
   * @param screens The list of screens associated with the error.
   */
  fun addErrorToScreens(type: ErrorType, screens: List<Screen>) {
    if (screens.size == 1) addErrorToScreen(type, screens.first())
    else if (_state.value.errors.any { it.type == type && screens.isSubsequenceOf(it.screenTypes) })
        return
    else if (_state.value.errors.any {
      it.type == type && it.screenTypes.isSubsequenceOf(screens)
    }) {
      _state.value =
          _state.value.copy(
              errors =
                  _state.value.errors.map {
                    if (it.type == type) it.copy(screenTypes = screens) else it
                  })
    } else _state.value = _state.value.copy(errors = _state.value.errors + Error(type, screens))
  }

  /**
   * Clears errors of a specific type associated with a given screen from the error state.
   *
   * @param type The type of error to clear.
   * @param screen The screen for which to clear the error.
   */
  fun clearErrorFromScreen(type: ErrorType, screen: Screen) {
    _state.value =
        _state.value.copy(
            errors =
                _state.value.errors.filter {
                  !(it.type == type && it.screenTypes.contains(screen))
                })
  }

  /**
   * Clears errors of a specific type associated with given screens from the error state. Updates
   * existing errors to remove only the specified screens if they are a subset.
   *
   * @param type The type of error to clear.
   * @param screens The list of screens for which to clear the error.
   */
  fun clearErrorFromScreens(type: ErrorType, screens: List<Screen>) {
    _state.value =
        _state.value.copy(
            errors =
                _state.value.errors.flatMap {
                  when {
                    it.type != type -> listOf(it)
                    screens.isSubsequenceOf(it.screenTypes) && screens.size < it.screenTypes.size ->
                        listOf(it.copy(screenTypes = it.screenTypes - screens.toSet()))
                    else -> emptyList()
                  }
                })
  }

  /**
   * Clears all errors associated with a specific screen from the error state.
   *
   * @param screen The screen for which to clear all errors.
   */
  fun clearScreenErrors(screen: Screen) {
    _state.value =
        _state.value.copy(errors = _state.value.errors.filter { !it.screenTypes.contains(screen) })
  }

  /**
   * Clears all errors associated with specific screens from the error state.
   *
   * @param screens The list of screens for which to clear all errors.
   */
  fun clearScreenErrors(screens: List<Screen>) {
    _state.value =
        _state.value.copy(
            errors =
                _state.value.errors.filter {
                  !screens.any { screen -> it.screenTypes.contains(screen) }
                })
  }

  /** Clears all errors from the error state. */
  fun clearAll() {
    _state.value = ErrorState()
  }
}

/**
 * Data class representing the state of errors, containing a list of individual errors.
 *
 * @property errors List of errors currently stored in the state.
 */
data class ErrorState(val errors: List<Error> = emptyList())

/**
 * Extension function to retrieve error for a specific screen from the ErrorState.
 *
 * @param screen The screen for which to retrieve error messages.
 * @return List of errors associated with the specified screen.
 */
fun ErrorState.getScreenErrors(screen: Screen): List<Error> {
  return errors.filter { it.screenTypes.contains(screen) }
}

/**
 * Data class representing an individual error with a message and associated screen type.
 *
 * @property type The type of error.
 * @property screenTypes The list of screens associated with this error.
 */
data class Error(val type: ErrorType, val screenTypes: List<Screen>)
