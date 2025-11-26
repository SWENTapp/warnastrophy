package com.github.warnastrophy.core.ui.common

import androidx.lifecycle.ViewModel
import com.github.warnastrophy.core.ui.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class ErrorType(val message: String) {
  LOCATION_NOT_GRANTED_ERROR("Location permission not granted!"),
  LOCATION_UPDATE_ERROR("Location update failed!"),
  FOREGROUND_ERROR("Failed to start foreground GPS service"),
  LOCATION_ERROR("No location available"),
  HAZARD_FETCHING_ERROR("Failed to fetch hazard data"),
}

/** ViewModel responsible for managing error messages associated with different screens. */
class ErrorHandler : ViewModel() {
  private val _state = MutableStateFlow(ErrorState())

  /** Public state flow exposing the current error state. */
  val state: StateFlow<ErrorState> = _state

  /**
   * Adds an error message associated with a specific screen to the error state.
   *
   * @param type The type of error to add.
   * @param screen The screen associated with the error.
   */
  fun addError(type: ErrorType, screen: Screen) {
    if (_state.value.errors.any { it.screenType == screen && it.type == type }) return
    _state.value = _state.value.copy(errors = _state.value.errors + Error(type, screen))
  }

  /**
   * Clears a specific error type associated with a given screen from the error state.
   *
   * @param type The type of error to clear.
   * @param screen The screen associated with the error.
   */
  fun clearError(type: ErrorType, screen: Screen) {
    _state.value =
        _state.value.copy(
            errors = _state.value.errors.filter { it.type != type || it.screenType != screen })
  }

  fun clearScreenErrors(screen: Screen) {
    _state.value =
        _state.value.copy(errors = _state.value.errors.filter { it.screenType != screen })
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
 * Extension function to retrieve error messages for a specific screen from the ErrorState.
 *
 * @param screen The screen for which to retrieve error messages.
 * @return A concatenated string of error messages associated with the specified screen.
 */
fun ErrorState.getScreenErrors(screen: Screen): String {
  return errors
      .filter { it.screenType == screen }
      .joinToString(separator = "\n") { it.type.message }
}

/**
 * Data class representing an individual error with a message and associated screen type.
 *
 * @property type The type of error.
 * @property screenType The screen associated with the error.
 */
data class Error(val type: ErrorType, val screenType: Screen)
