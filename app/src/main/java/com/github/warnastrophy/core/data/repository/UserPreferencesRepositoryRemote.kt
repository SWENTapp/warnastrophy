package com.github.warnastrophy.core.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * A remote implementation of [UserPreferencesRepository] that interacts with Firestore to manage
 * user preferences, such as alert mode, inactivity detection, SMS alerts, and dark mode.
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

  private companion object {
    const val COLLECTION_NAME = "user_preferences"
    const val FIELD_ALERT_MODE = "alertMode"
    const val FIELD_INACTIVITY_DETECTION = "inactivityDetection"
    const val FIELD_AUTOMATIC_SMS = "automaticSms"
    const val FIELD_DARK_MODE = "darkMode"
  }

  /**
   * Retrieves the Firestore document reference for the current user's preferences.
   *
   * @return A [DocumentReference] pointing to the current user's preferences document in Firestore.
   */
  private fun doc() = firestore.collection(COLLECTION_NAME).document(getUserId())

  /**
   * Retrieves the current user's UID from Firebase Authentication. If the user is not
   * authenticated, returns "anonymous".
   *
   * @return The current user's UID.
   */
  private fun getUserId(): String {
    return FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
  }

  /**
   * Returns a cold [Flow] of the user's preferences, emitting the latest [UserPreferences] object
   * whenever the preferences are updated in Firestore.
   *
   * The flow listens for changes in the user's preferences document and emits the updated
   * preferences to collectors. If no preferences exist for the user, it will emit default
   * preferences.
   */
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

  /**
   * Updates a specific field in the user's preferences document in Firestore. If the field does not
   * exist, it is created with the provided value.
   *
   * @param fieldName The field to update in the preferences document.
   * @param value The new value for the field.
   */
  private suspend fun updateField(fieldName: String, value: Any) {
    try {
      doc().update(fieldName, value).await()
    } catch (e: Exception) {
      val defaultMap = mutableMapOf(fieldName to value)
      doc().set(defaultMap, SetOptions.merge()).await()
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
    val darkMode = data?.get(FIELD_DARK_MODE) as? Boolean ?: false

    val dangerModePreferences =
        DangerModePreferences(
            alertMode = alertMode,
            inactivityDetection = inactivityDetection,
            automaticSms = automaticSms)

    return UserPreferences(
        dangerModePreferences = dangerModePreferences, themePreferences = darkMode)
  }

  /**
   * Returns the default user preferences when no preferences exist in Firestore.
   *
   * @return A [UserPreferences] object with default values.
   */
  private fun getDefaultPreferences(): UserPreferences {
    return UserPreferences(
        dangerModePreferences =
            DangerModePreferences(
                alertMode = false, inactivityDetection = false, automaticSms = false),
        themePreferences = false)
  }
}
