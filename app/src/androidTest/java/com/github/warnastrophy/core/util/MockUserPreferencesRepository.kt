package com.github.warnastrophy.core.ui.util

import com.github.warnastrophy.core.data.interfaces.UserPreferencesRepository
import com.github.warnastrophy.core.data.repository.DangerModePreferences
import com.github.warnastrophy.core.data.repository.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A mock implementation of [UserPreferencesRepository] for testing purposes. */
class MockUserPreferencesRepository : UserPreferencesRepository {

  private val _preferences =
      MutableStateFlow(
          UserPreferences(
              dangerModePreferences =
                  DangerModePreferences(
                      alertMode = false,
                      inactivityDetection = false,
                      automaticSms = false,
                      automaticCalls = false),
              themePreferences = false))

  override val getUserPreferences: StateFlow<UserPreferences> = _preferences.asStateFlow()

  override suspend fun setAlertMode(enabled: Boolean) {
    val current = _preferences.value
    _preferences.value =
        current.copy(
            dangerModePreferences = current.dangerModePreferences.copy(alertMode = enabled))
  }

  override suspend fun setInactivityDetection(enabled: Boolean) {
    val current = _preferences.value
    _preferences.value =
        current.copy(
            dangerModePreferences =
                current.dangerModePreferences.copy(inactivityDetection = enabled))
  }

  override suspend fun setAutomaticSms(enabled: Boolean) {
    val current = _preferences.value
    _preferences.value =
        current.copy(
            dangerModePreferences = current.dangerModePreferences.copy(automaticSms = enabled))
  }

  override suspend fun setAutomaticCalls(enabled: Boolean) {
    val current = _preferences.value
    _preferences.value =
        current.copy(
            dangerModePreferences = current.dangerModePreferences.copy(automaticCalls = enabled))
  }

  override suspend fun setDarkMode(isDark: Boolean) {}
}
