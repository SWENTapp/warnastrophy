package com.github.warnastrophy.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Data class to hold all user-configurable settings. */
data class UserPreferences(
    val alertMode: Boolean,
    val inactivityDetection: Boolean,
    val automaticSms: Boolean,
    // Add more preferences like storeInFirebase: Boolean, darkMode: Boolean, etc
)

/**
 * Repository for managing user preferences using Jetpack DataStore. It handles read errors and
 * exposes all preferences in a single, convenient data flow.
 */
class UserPreferencesRepository(private val dataStore: DataStore<Preferences>) {

  private object Keys {
    val ALERT_MODE_KEY = booleanPreferencesKey("alert_mode")
    val INACTIVITY_DETECTION_KEY = booleanPreferencesKey("inactivity_detection")
    val AUTOMATIC_SMS_KEY = booleanPreferencesKey("automatic_sms")

    // Add more keys for other preferences
  }

  /**
   * A flow of user preferences, which combines all settings into a single [UserPreferences] object.
   */
  val getUserPreferences: Flow<UserPreferences> =
      dataStore.data.map { preferences -> mapUserPreferences(preferences) }

  /** Toggles the "alert mode" setting. */
  suspend fun setAlertMode(enabled: Boolean) {
    dataStore.edit { preferences -> preferences[Keys.ALERT_MODE_KEY] = enabled }
  }

  /** Toggles the "inactivity detection" setting. */
  suspend fun setInactivityDetection(enabled: Boolean) {
    dataStore.edit { preferences -> preferences[Keys.INACTIVITY_DETECTION_KEY] = enabled }
  }

  /** Toggles the "automatic SMS" setting. */
  suspend fun setAutomaticSms(enabled: Boolean) {
    dataStore.edit { preferences -> preferences[Keys.AUTOMATIC_SMS_KEY] = enabled }
  }

  private fun mapUserPreferences(preferences: Preferences): UserPreferences {
    val alertMode = preferences[Keys.ALERT_MODE_KEY] ?: false
    val inactivityDetection = preferences[Keys.INACTIVITY_DETECTION_KEY] ?: false
    val automaticSms = preferences[Keys.AUTOMATIC_SMS_KEY] ?: false

    return UserPreferences(alertMode, inactivityDetection, automaticSms)
  }
}
