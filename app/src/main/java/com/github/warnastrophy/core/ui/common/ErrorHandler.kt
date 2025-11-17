package com.github.warnastrophy.core.ui.common


import com.github.warnastrophy.core.domain.error.Error
import com.github.warnastrophy.core.domain.error.ErrorDisplayManager
import com.github.warnastrophy.core.domain.error.ErrorState
import com.github.warnastrophy.core.ui.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** ViewModel responsible for managing error messages associated with different screens. */

// TODO: Remove this class next PR
class ErrorHandler : ErrorDisplayManager {
  private val _state = MutableStateFlow(ErrorState())

  /** Public state flow exposing the current error state. */
  override val errorState: StateFlow<ErrorState> = _state

  /**
   * Adds an error message associated with a specific screen to the error state.
   *
   * @param message The error message to be added.
   * @param screen The screen associated with the error.
   */
  override fun addError(message: String, screen: Screen) {
    if (message.isBlank()) return
    if (_state.value.errors.any { it.screenType == screen && it.message == message }) return
    _state.value = _state.value.copy(errors = _state.value.errors + Error(message, screen))
  }

  /** Clears all errors from the error state. */
  override fun clearError() {
    _state.value = ErrorState()
  }
}




