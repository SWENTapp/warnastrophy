package com.github.warnastrophy.core.data.repository

import com.github.warnastrophy.core.data.interfaces.UserPreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

/** A remote implementation of UserPreferencesRepository that interacts with Firestore. */
class UserPreferencesRepositoryRemote(private val firestore: FirebaseFirestore) :
    UserPreferencesRepository {

  private val auth = FirebaseAuth.getInstance()

  private companion object {
    const val COLLECTION_NAME = "userPreferences"
    const val FIELD_ALERT_MODE = "alertMode"
    const val FIELD_INACTIVITY_DETECTION = "inactivityDetection"
    const val FIELD_AUTOMATIC_SMS = "automaticSms"
    const val FIELD_AUTOMATIC_CALLS = "automaticCalls"
    const val FIELD_MICROPHONE_ACCESS = "microphoneAccess"
    const val FIELD_AUTO_ACTIONS = "autoActionsEnabled"
    const val FIELD_TOUCH_CONFIRMATION = "touchConfirmationRequired"
    const val FIELD_VOICE_CONFIRMATION = "voiceConfirmationEnabled"
    const val FIELD_DARK_MODE = "darkMode"
  }

  private fun doc() = getUserId()?.let { firestore.collection(COLLECTION_NAME).document(it) }

  private fun getUserId(): String? = auth.currentUser?.uid

  private fun isUserAuthenticated(): Boolean = auth.currentUser != null

  /**
   * Returns a cold [Flow] of the user's preferences, emitting the latest [UserPreferences] object
   * whenever the preferences are updated in Firestore.
   *
   * The flow listens for changes in the user's preferences document and emits the updated
   * preferences to collectors. If no preferences exist for the user, it will emit default
   * preferences.
   */
  override val getUserPreferences: Flow<UserPreferences> =
      if (!isUserAuthenticated()) {
        flowOf(UserPreferences.default())
      } else {
        callbackFlow {
          var listenerRegistration: ListenerRegistration? = null

          try {
            val document = doc()
            if (document == null) {
              trySend(UserPreferences.default())
              close()
              return@callbackFlow
            }

            listenerRegistration =
                document.addSnapshotListener { snapshot, error ->
                  if (error != null) {
                    close(error)
                    return@addSnapshotListener
                  }

                  if (snapshot != null && snapshot.exists()) {
                    val preferences = mapDocumentToUserPreferences(snapshot.data)
                    trySend(preferences)
                  } else {
                    trySend(UserPreferences.default())
                  }
                }
          } catch (e: Exception) {
            close(e)
          }

          awaitClose { listenerRegistration?.remove() }
        }
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

  override suspend fun setAutomaticCalls(enabled: Boolean) {
    updateField(FIELD_AUTOMATIC_CALLS, enabled)
  }

  override suspend fun setMicrophoneAccess(enabled: Boolean) {
    updateField(FIELD_MICROPHONE_ACCESS, enabled)
  }

  override suspend fun setAutoActionsEnabled(enabled: Boolean) {
    updateField(FIELD_AUTO_ACTIONS, enabled)
  }

  override suspend fun setTouchConfirmationRequired(required: Boolean) {
    updateField(FIELD_TOUCH_CONFIRMATION, required)
  }

  override suspend fun setVoiceConfirmationEnabled(enabled: Boolean) {
    updateField(FIELD_VOICE_CONFIRMATION, enabled)
  }

  override suspend fun setDarkMode(isDark: Boolean) {
    updateField(FIELD_DARK_MODE, isDark)
  }

  /**
   * Updates a specific field in the user's preferences document in Firestore. If the field does not
   * exist, it is created with the provided value.
   *
   * This method assumes the user is authenticated (checked by caller).
   *
   * @param fieldName The field to update in the preferences document.
   * @param value The new value for the field.
   */
  private suspend fun updateField(fieldName: String, value: Any) {
    val document = doc() ?: return

    try {
      document.update(fieldName, value).await()
    } catch (_: Exception) {
      // NOSONAR - This is a Firestore set() method, not a map accessor
      document.set(mapOf(fieldName to value), SetOptions.merge()).await()
    }
  }

  /**
   * Maps a Firestore document to a [UserPreferences] object.
   *
   * @param data The Firestore document data to map to a [UserPreferences] object.
   * @return The mapped [UserPreferences] object.
   */
  private fun mapDocumentToUserPreferences(data: Map<String, Any>?): UserPreferences {
    val alertMode = data?.get(FIELD_ALERT_MODE) as? Boolean ?: false
    val inactivityDetection = data?.get(FIELD_INACTIVITY_DETECTION) as? Boolean ?: false
    val automaticSms = data?.get(FIELD_AUTOMATIC_SMS) as? Boolean ?: false
    val automaticCalls = data?.get(FIELD_AUTOMATIC_CALLS) as? Boolean ?: false
    val microphoneAccess = data?.get(FIELD_MICROPHONE_ACCESS) as? Boolean ?: false
    val autoActionsEnabled = data?.get(FIELD_AUTO_ACTIONS) as? Boolean ?: false
    val touchConfirmationRequired = data?.get(FIELD_TOUCH_CONFIRMATION) as? Boolean ?: false
    val voiceConfirmationEnabled = data?.get(FIELD_VOICE_CONFIRMATION) as? Boolean ?: false
    val darkMode = data?.get(FIELD_DARK_MODE) as? Boolean ?: false

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

    return UserPreferences(
        dangerModePreferences = dangerModePreferences, themePreferences = darkMode)
  }
}
