package com.github.warnastrophy.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.*
import com.github.warnastrophy.core.data.interfaces.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** Data class to hold all user-configurable settings. */
data class UserPreferences(
    val dangerModePreferences: DangerModePreferences,
    val themePreferences: Boolean
) {
  companion object {
    fun default() =
        UserPreferences(
            dangerModePreferences = DangerModePreferences.default(), themePreferences = false)
  }
}

/**
 * Data class to hold all user-configurable settings related to "danger mode".
 *
 * @property alertMode Indicates if the alert mode is enabled.
 * @property inactivityDetection Determines whether the app should monitor for user inactivity.
 * @property automaticSms Specifies if SMS messages should be sent automatically when an alert is
 *   triggered.
 * @property automaticCalls Specifies if automatic phone calls should be made when an alert is
 *   triggered.
 * @property microphoneAccess Indicates if microphone access is granted for voice communication.
 * @property autoActionsEnabled Enables automatic emergency actions overall.
 * @property touchConfirmationRequired Requires a tactile confirmation (button) before actions run.
 * @property voiceConfirmationEnabled Allows voice confirmation before actions run.
 */
data class DangerModePreferences(
    val alertMode: Boolean,
    val inactivityDetection: Boolean,
    val automaticSms: Boolean,
    val automaticCalls: Boolean,
    val microphoneAccess: Boolean = false,
    val autoActionsEnabled: Boolean = false,
    val touchConfirmationRequired: Boolean = false,
    val voiceConfirmationEnabled: Boolean = false,
) {
  companion object {
    fun default() =
        DangerModePreferences(
            alertMode = false,
            inactivityDetection = false,
            automaticSms = false,
            automaticCalls = false,
            microphoneAccess = false,
            autoActionsEnabled = false,
            touchConfirmationRequired = false,
            voiceConfirmationEnabled = false)
  }
}

/**
 * Repository for managing user preferences using Jetpack DataStore. It handles read errors and
 * exposes all preferences in a single, convenient data flow.
 */
class UserPreferencesRepositoryLocal(private val dataStore: DataStore<Preferences>) :
    UserPreferencesRepository {

  private companion object Keys {
    val ALERT_MODE_KEY = booleanPreferencesKey("alert_mode")
    val INACTIVITY_DETECTION_KEY = booleanPreferencesKey("inactivity_detection")
    val AUTOMATIC_SMS_KEY = booleanPreferencesKey("automatic_sms")
    val AUTOMATIC_CALLS_KEY = booleanPreferencesKey("automatic_calls")
    val MICROPHONE_KEY = booleanPreferencesKey("microphone")
    val AUTO_ACTIONS_KEY = booleanPreferencesKey("auto_actions_enabled")
    val TOUCH_CONFIRMATION_KEY = booleanPreferencesKey("touch_confirmation_required")
    val VOICE_CONFIRMATION_KEY = booleanPreferencesKey("voice_confirmation_enabled")
    val DARK_MODE_KEY = booleanPreferencesKey("theme_preferences")
  }

  override val getUserPreferences: Flow<UserPreferences> =
      dataStore.data
          .catch { exception ->
            if (exception is IOException) {
              emit(emptyPreferences())
            } else {
              throw exception
            }
          }
          .map { preferences -> mapUserPreferences(preferences) }

  override suspend fun setAlertMode(enabled: Boolean) {
    dataStore.edit { preferences -> preferences[ALERT_MODE_KEY] = enabled }
  }

  override suspend fun setInactivityDetection(enabled: Boolean) {
    dataStore.edit { preferences -> preferences[INACTIVITY_DETECTION_KEY] = enabled }
  }

  override suspend fun setAutomaticSms(enabled: Boolean) {
    dataStore.edit { preferences -> preferences[AUTOMATIC_SMS_KEY] = enabled }
  }

  override suspend fun setAutomaticCalls(enabled: Boolean) {
    dataStore.edit { preferences -> preferences[AUTOMATIC_CALLS_KEY] = enabled }
  }

  override suspend fun setMicrophoneAccess(enabled: Boolean) {
    dataStore.edit { preferences -> preferences[MICROPHONE_KEY] = enabled }
  }

  override suspend fun setAutoActionsEnabled(enabled: Boolean) {
    dataStore.edit { prefs -> prefs[AUTO_ACTIONS_KEY] = enabled }
  }

  override suspend fun setTouchConfirmationRequired(required: Boolean) {
    dataStore.edit { prefs -> prefs[TOUCH_CONFIRMATION_KEY] = required }
  }

  override suspend fun setVoiceConfirmationEnabled(enabled: Boolean) {
    dataStore.edit { prefs -> prefs[VOICE_CONFIRMATION_KEY] = enabled }
  }

  override suspend fun setDarkMode(isDark: Boolean) {
    dataStore.edit { preferences -> preferences[DARK_MODE_KEY] = isDark }
  }

  /** Maps a [Preferences] object from DataStore to a [UserPreferences] data class. */
  private fun mapUserPreferences(preferences: Preferences): UserPreferences {
    val alertMode = preferences[ALERT_MODE_KEY] ?: false
    val inactivityDetection = preferences[INACTIVITY_DETECTION_KEY] ?: false
    val automaticSms = preferences[AUTOMATIC_SMS_KEY] ?: false
    val automaticCalls = preferences[AUTOMATIC_CALLS_KEY] ?: false
    val microphoneAccess = preferences[MICROPHONE_KEY] ?: false
    val autoActionsEnabled = preferences[AUTO_ACTIONS_KEY] ?: false
    val touchConfirmationRequired = preferences[TOUCH_CONFIRMATION_KEY] ?: false
    val voiceConfirmationEnabled = preferences[VOICE_CONFIRMATION_KEY] ?: false
    val themePreferences = preferences[DARK_MODE_KEY] ?: false

    val dangerModePreferences =
        DangerModePreferences(
            alertMode = alertMode,
            inactivityDetection = inactivityDetection,
            automaticSms = automaticSms,
            automaticCalls = automaticCalls,
            microphoneAccess = microphoneAccess,
            autoActionsEnabled = autoActionsEnabled,
            touchConfirmationRequired = touchConfirmationRequired,
            voiceConfirmationEnabled = voiceConfirmationEnabled)
    return UserPreferences(dangerModePreferences, themePreferences)
  }
}
