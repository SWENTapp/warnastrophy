package com.github.warnastrophy.core.data.interfaces

import com.github.warnastrophy.core.data.repository.UserPreferences
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing and persisting user preferences. This interface provides methods to read
 * and update various settings related to the application's behavior.
 */
interface UserPreferencesRepository {
  /**
   * Retrieves a cold flow of the user's preferences. This flow emits a new
   * [com.github.warnastrophy.core.data.repository.UserPreferences] object whenever any preference
   * value changes.
   */
  val getUserPreferences: Flow<UserPreferences>

  /**
   * Enables or disables the main alert mode.
   *
   * @param enabled `true` to enable alert mode, `false` to disable it.
   */
  suspend fun setAlertMode(enabled: Boolean)

  /**
   * Enables or disables the inactivity detection feature.
   *
   * @param enabled `true` to enable inactivity detection, `false` to disable it.
   */
  suspend fun setInactivityDetection(enabled: Boolean)

  /**
   * Enables or disables the automatic sending of SMS alerts.
   *
   * @param enabled `true` to enable automatic SMS, `false` to disable it.
   */
  suspend fun setAutomaticSms(enabled: Boolean)

  /**
   * Enables or disables automatic phone calls.
   *
   * @param enabled `true` to enable automatic calls, `false` to disable it.
   */
  suspend fun setAutomaticCalls(enabled: Boolean)

  /*
   * Enables or disables microphone access for the app.
   */
  suspend fun setMicrophoneAccess(enabled: Boolean)

  /**
   * Enables or disables dark mode for the app's UI.
   *
   * @param isDark `true` to enable dark mode, `false` to disable it (enabling light mode).
   */
  suspend fun setDarkMode(isDark: Boolean)
}
