package com.github.warnastrophy.core.ui.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.warnastrophy.core.data.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel responsible for managing and exposing theme-related preferences in the app, such as
 * dark mode.
 *
 * This ViewModel interacts with the [UserPreferencesRepository] to read and update the user's theme
 * preferences, and it exposes a [StateFlow] to observe the current theme setting.
 *
 * @param userPreferencesRepository A repository for accessing and updating the user's preferences.
 * @param dispatcher A coroutine dispatcher. Defaults to [Dispatchers.Main]
 */
class ThemeViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

  /**
   * A [StateFlow] that exposes the current dark mode setting.
   *
   * This flow emits a nullable Boolean indicating whether dark mode is enabled (`true`) or disabled
   * (`false`). The value can be `null` if the theme preference is not set or is unavailable.
   */
  val isDarkMode: StateFlow<Boolean?> =
      userPreferencesRepository.getUserPreferences
          .map { preferences -> preferences.themePreferences }
          .stateIn(
              scope = viewModelScope,
              started = SharingStarted.WhileSubscribed(5000),
              initialValue = null)

  /**
   * Toggles the theme between light and dark mode.
   *
   * This method updates the theme preference by calling [UserPreferencesRepository.setDarkMode].
   *
   * @param isDark `true` to enable dark mode, `false` to disable it and switch to light mode.
   */
  fun toggleTheme(isDark: Boolean) {
    viewModelScope.launch { userPreferencesRepository.setDarkMode(isDark) }
  }
}
