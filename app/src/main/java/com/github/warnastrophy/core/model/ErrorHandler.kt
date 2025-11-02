package com.github.warnastrophy.core.model

import androidx.lifecycle.ViewModel
import com.github.warnastrophy.core.ui.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** ViewModel responsible for managing error messages associated with different screens. */
class ErrorHandler : ViewModel() {
  private val _state = MutableStateFlow(ErrorState())

  /** Public state flow exposing the current error state. */
  val state: StateFlow<ErrorState> = _state

  /**
   * Adds an error message associated with a specific screen to the error state.
   *
   * @param message The error message to be added.
   * @param screen The screen associated with the error.
   */
  fun addError(message: String, screen: Screen) {
    if (message.isBlank()) return
    if (_state.value.errors.any { it.screenType == screen && it.message == message }) return
    _state.value = _state.value.copy(errors = _state.value.errors + Error(message, screen))
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
  return errors.filter { it.screenType == screen }.joinToString(separator = "\n") { it.message }
}

/**
 * Data class representing an individual error with a message and associated screen type.
 *
 * @property message The error message.
 * @property screenType The screen associated with the error.
 */
data class Error(val message: String, val screenType: Screen)
