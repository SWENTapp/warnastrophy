package com.github.warnastrophy.core.data.repository

import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
  val getUserPreferences: Flow<UserPreferences>

  suspend fun setAlertMode(enabled: Boolean)

  suspend fun setInactivityDetection(enabled: Boolean)

  suspend fun setAutomaticSms(enabled: Boolean)
}
