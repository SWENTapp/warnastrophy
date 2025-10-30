package com.github.warnastrophy.core.model

import androidx.lifecycle.ViewModel
import com.github.warnastrophy.core.ui.navigation.Screen
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ErrorHandler : ViewModel() {
  private val _errors = MutableStateFlow(ErrorState())
  val errors: StateFlow<ErrorState> = _errors

  fun addError(message: String, screen: Screen) {
    if (message.isBlank()) return
    _errors.value = _errors.value.copy(errors = _errors.value.errors + Error(message, screen))
  }

  fun getScreenErrors(screen: Screen): String {
    return _errors.value.errors
        .filter { it.screenType == screen }
        .joinToString(separator = "\n") { it.message }
  }

  fun clear() {
    _errors.value = ErrorState()
  }
}

data class ErrorState(val errors: List<Error> = emptyList())

data class Error(val message: String, val screenType: Screen)
