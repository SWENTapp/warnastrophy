package com.github.warnastrophy.core.model

import androidx.lifecycle.ViewModel
import com.github.warnastrophy.core.ui.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ErrorHandler : ViewModel() {
  private val _state = MutableStateFlow(ErrorState())
  val state: StateFlow<ErrorState> = _state

  fun addError(message: String, screen: Screen) {
    if (message.isBlank()) return
    if (_state.value.errors.any { it.screenType == screen && it.message == message }) return
    _state.value = _state.value.copy(errors = _state.value.errors + Error(message, screen))
  }

  fun clearAll() {
    _state.value = ErrorState()
  }
}

data class ErrorState(val errors: List<Error> = emptyList())

fun ErrorState.getScreenErrors(screen: Screen): String {
  return errors.filter { it.screenType == screen }.joinToString(separator = "\n") { it.message }
}

data class Error(val message: String, val screenType: Screen)
