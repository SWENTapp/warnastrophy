package com.github.warnastrophy.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** Data class to hold all user-configurable settings. */
data class UserPreferences(
    val dangerModePreferences: DangerModePreferences,
    // Add more preferences like storeInFirebase: Boolean, darkMode: Boolean, etc
)

/**
 * Data class to hold all user-configurable settings related to "danger mode".
 *
 * @property alertMode Indicates if the alert mode is enabled.
 * @property inactivityDetection Determines whether the app should monitor for user inactivity.
 * @property automaticSms Specifies if SMS messages should be sent automatically when an alert is
 *   triggered.
 */
data class DangerModePreferences(
    val alertMode: Boolean,
    val inactivityDetection: Boolean,
    val automaticSms: Boolean,
)

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

    // Add more keys for other preferences
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

  /**
   * Maps a [Preferences] object from DataStore to a [UserPreferences] data class. This function
   * extracts individual preference values using their respective keys and constructs a structured
   * [UserPreferences] object. If a preference key is not found, it defaults to `false`.
   *
   * @param preferences The [Preferences] object read from DataStore.
   * @return A [UserPreferences] object populated with the values from the preferences.
   */
  private fun mapUserPreferences(preferences: Preferences): UserPreferences {
    val alertMode = preferences[ALERT_MODE_KEY] ?: false
    val inactivityDetection = preferences[INACTIVITY_DETECTION_KEY] ?: false
    val automaticSms = preferences[AUTOMATIC_SMS_KEY] ?: false

    val dangerModePreferences = DangerModePreferences(alertMode, inactivityDetection, automaticSms)
    return UserPreferences(dangerModePreferences)
  }
}
