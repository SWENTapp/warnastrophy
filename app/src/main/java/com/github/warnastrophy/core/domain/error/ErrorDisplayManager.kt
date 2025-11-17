package com.github.warnastrophy.core.domain.error

import com.github.warnastrophy.core.ui.navigation.Screen
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class Error(val message: String, val screenType: Screen)

data class ErrorState(val errors: List<Error> = emptyList())

fun ErrorState.getScreenErrors(screen: Screen): String {
  return errors.filter { it.screenType == screen }.joinToString(separator = "\n") { it.message }
}

interface ErrorDisplayManager {
  val errorState: StateFlow<ErrorState>

  fun addError(message: String, screen: Screen)

  fun clearError()
}

@Singleton
class ErrorDisplayHandler @Inject constructor() : ErrorDisplayManager {
  private val _errorState = MutableStateFlow(ErrorState())
  override val errorState: StateFlow<ErrorState> = _errorState.asStateFlow()

  override fun addError(message: String, screen: Screen) {
    if (message.isBlank()) return
    if (_errorState.value.errors.any { it.screenType == screen && it.message == message }) return
    _errorState.value =
        _errorState.value.copy(errors = _errorState.value.errors + Error(message, screen))
  }

  override fun clearError() {
    _errorState.value = ErrorState()
  }
}
