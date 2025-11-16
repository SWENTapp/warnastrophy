package com.github.warnastrophy.core.domain.error

import com.github.warnastrophy.core.ui.navigation.Screen
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Data class representing an individual error with a message and associated screen type.
 *
 * @property message The error message.
 * @property screenType The screen associated with the error.
 */
data class Error(val message: String, val screenType: Screen)

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
 * An interface defining the contract for a component responsible for managing and exposing the
 * current application error state.
 *
 * It uses a [StateFlow] to allow collectors to react to changes in the error status.
 */
interface ErrorDisplayManager {
  val errorState: StateFlow<ErrorState>

  /**
   * Adds a new error message associated with a specific screen context.
   *
   * @param message The non-blank error message to display.
   * @param screen The [Screen] context where the error occurred or should be displayed.
   */
  fun addError(message: String, screen: Screen)

  /**
   * Clears all currently stored errors, resetting the [errorState] to an empty state. This is
   * typically called when an error has been acknowledged by the user or is no longer relevant
   */
  fun clearError()
}

/**
 * The concrete implementation of [ErrorDisplayManager], using dependency injection.
 *
 * This handler uses a [MutableStateFlow] to internally manage the error state and exposes it as a
 * read-only [StateFlow]. It ensures also that duplicate errors (same message and screen) are not
 * added.
 */
@Singleton
class ErrorDisplayHandlerImpl @Inject constructor() : ErrorDisplayManager {
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
