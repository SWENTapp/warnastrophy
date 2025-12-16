package com.github.warnastrophy.core.data.repository

import com.github.warnastrophy.core.data.interfaces.UserPreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

/**
 * A remote implementation of
 * [com.github.warnastrophy.core.data.interfaces.UserPreferencesRepository] that interacts with
 * Firestore to manage user preferences, such as alert mode, inactivity detection, SMS alerts, and
 * dark mode.
 *
 * This repository provides functionality for both retrieving and updating user preferences stored
 * in Firestore. It listens to changes in Firestore and emits updated preferences to subscribers,
 * and it also provides methods to update individual preference fields.
 *
 * The preferences are stored in the "user_preferences" collection in Firestore, where each user has
 * a document identified by their unique Firebase UID. The repository listens for changes in the
 * user's document and reflects those changes in the application state.
 *
 * @param firestore The Firestore instance used to interact with Firestore for user preferences.
 */
class UserPreferencesRepositoryRemote(private val firestore: FirebaseFirestore) :
    UserPreferencesRepository {

  private val auth = FirebaseAuth.getInstance()

  private companion object {
    const val COLLECTION_NAME = "userPreferences"
    const val FIELD_ALERT_MODE = "alertMode"
    const val FIELD_INACTIVITY_DETECTION = "inactivityDetection"
    const val FIELD_AUTOMATIC_SMS = "automaticSms"
    const val FIELD_AUTOMATIC_CALLS = "automaticCalls"
    const val FIELD_DARK_MODE = "darkMode"
  }

  /**
   * Retrieves the Firestore document reference for the current user's preferences.
   *
   * @return A [DocumentReference] pointing to the current user's preferences document in Firestore.
   */
  private fun doc() = getUserId()?.let { firestore.collection(COLLECTION_NAME).document(it) }

  /**
   * Retrieves the current user's UID from Firebase Authentication. If the user is not
   * authenticated, returns "anonymous".
   *
   * @return The current user's UID.
   */
  private fun getUserId(): String? {
    return auth.currentUser?.uid
  }

  /**
   * Checks if a user is currently authenticated.
   *
   * @return true if a user is authenticated, false otherwise.
   */
  private fun isUserAuthenticated(): Boolean {
    return auth.currentUser != null
  }

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
    val darkMode = data?.get(FIELD_DARK_MODE) as? Boolean ?: false

    val dangerModePreferences =
        DangerModePreferences(
            alertMode = alertMode,
            inactivityDetection = inactivityDetection,
            automaticSms = automaticSms,
            automaticCalls = automaticCalls)

    return UserPreferences(
        dangerModePreferences = dangerModePreferences, themePreferences = darkMode)
  }
}
