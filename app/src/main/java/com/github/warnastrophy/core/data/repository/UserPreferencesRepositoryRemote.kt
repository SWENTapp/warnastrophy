package com.github.warnastrophy.core.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserPreferencesRepositoryRemote(private val firestore: FirebaseFirestore) :
    UserPreferencesRepository {

  private companion object {
    const val COLLECTION_NAME = "user_preferences"
    const val FIELD_ALERT_MODE = "alertMode"
    const val FIELD_INACTIVITY_DETECTION = "inactivityDetection"
    const val FIELD_AUTOMATIC_SMS = "automaticSms"
    const val FIELD_DARK_MODE = "darkMode"
  }

  private fun doc() = firestore.collection(COLLECTION_NAME).document(getUserId())

  private fun getUserId(): String {
    return FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
  }

  override val getUserPreferences: Flow<UserPreferences> = callbackFlow {
    var listenerRegistration: ListenerRegistration? = null

    try {
      listenerRegistration =
          doc().addSnapshotListener { snapshot, error ->
            if (error != null) {
              close(error)
              return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
              val preferences = mapDocumentToUserPreferences(snapshot.data)
              trySend(preferences)
            } else {
              trySend(getDefaultPreferences())
            }
          }
    } catch (e: Exception) {
      close(e)
    }

    awaitClose { listenerRegistration?.remove() }
  }

  override suspend fun setAlertMode(enabled: Boolean) {
    updateField(FIELD_ALERT_MODE, enabled)
  }

  override suspend fun setInactivityDetection(enabled: Boolean) {
    updateField(FIELD_INACTIVITY_DETECTION, enabled)
  }

  override suspend fun setAutomaticSms(enabled: Boolean) {
    updateField(FIELD_AUTOMATIC_SMS, enabled)
  }

  override suspend fun setDarkMode(isDark: Boolean) {
    updateField(FIELD_DARK_MODE, isDark)
  }

  private suspend fun updateField(fieldName: String, value: Any) {
    try {
      doc().update(fieldName, value).await()
    } catch (e: Exception) {
      val defaultMap = mutableMapOf(fieldName to value)
      doc().set(defaultMap, SetOptions.merge()).await()
    }
  }

  private fun mapDocumentToUserPreferences(data: Map<String, Any>?): UserPreferences {
    val alertMode = data?.get(FIELD_ALERT_MODE) as? Boolean ?: false
    val inactivityDetection = data?.get(FIELD_INACTIVITY_DETECTION) as? Boolean ?: false
    val automaticSms = data?.get(FIELD_AUTOMATIC_SMS) as? Boolean ?: false
    val darkMode = data?.get(FIELD_DARK_MODE) as? Boolean ?: false

    val dangerModePreferences =
        DangerModePreferences(
            alertMode = alertMode,
            inactivityDetection = inactivityDetection,
            automaticSms = automaticSms)

    return UserPreferences(
        dangerModePreferences = dangerModePreferences, themePreferences = darkMode)
  }

  private fun getDefaultPreferences(): UserPreferences {
    return UserPreferences(
        dangerModePreferences =
            DangerModePreferences(
                alertMode = false, inactivityDetection = false, automaticSms = false),
        themePreferences = false)
  }
}
