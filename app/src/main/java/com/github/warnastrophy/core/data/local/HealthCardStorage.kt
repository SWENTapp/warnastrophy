package com.github.warnastrophy.core.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.warnastrophy.core.domain.model.HealthCard
import com.github.warnastrophy.core.util.CryptoUtils
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.healthCardDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "health_cards_encrypted")

/**
 * Repository responsible for securely storing and retrieving [HealthCard] instances per user.
 * - Values are serialized to JSON, encrypted using [CryptoUtils], and stored in DataStore.
 * - On retrieval, the reverse process is applied.
 * - Error handling is consistent through [StorageResult] and [StorageException].
 *
 * ## Typical usage
 *
 * ```kotlin
 * val result = HealthCardStorage.saveHealthCard(context, userId, healthCard)
 * if (result is StorageResult.Success) {
 *     // success
 * } else if (result is StorageResult.Error) {
 *     // handle result.exception
 * }
 * ```
 *
 * @see HealthCard
 * @see StorageResult
 * @see StorageException
 */
object HealthCardStorage {
  private const val TAG = "HealthCardStorage"
  private val gson = Gson()

  private fun getUserKey(userId: String) = stringPreferencesKey("health_card_$userId")

  /**
   * Saves a [HealthCard] securely for the specified [userId].
   *
   * The card is serialized to JSON, encrypted, and persisted in DataStore under a user-specific
   * key.
   *
   * @param context Android context to access DataStore.
   * @param userId Unique identifier for the user.
   * @param card The [HealthCard] to save.
   * @return [StorageResult.Success] on success or [StorageResult.Error] on failure.
   */
  suspend fun saveHealthCard(
      context: Context,
      userId: String,
      card: HealthCard
  ): StorageResult<Unit> {
    return try {

      val json = gson.toJson(card)

      val encrypted = CryptoUtils.encrypt(json)

      try {
        context.healthCardDataStore.edit { preferences ->
          preferences[getUserKey(userId)] = encrypted
        }
        Log.d(TAG, "Health card for user: $userId saved")
        StorageResult.Success(Unit)
      } catch (e: Exception) {
        StorageResult.Error(StorageException.DataStoreError(e))
      }
    } catch (e: Exception) {
      Log.e(TAG, "Unexpected error during save", e)
      StorageResult.Error(StorageException.DataStoreError(e))
    }
  }

  /**
   * Loads a [HealthCard] for the given [userId].
   *
   * If no data is found, returns [StorageResult.Success] with `null`.
   *
   * @param context Android context to access DataStore.
   * @param userId Unique identifier for the user.
   * @return A [StorageResult.Success] containing the card or null if not found, or a
   *   [StorageResult.Error] if decryption or deserialization fails.
   */
  suspend fun loadHealthCard(context: Context, userId: String): StorageResult<HealthCard?> {
    return try {
      val encrypted =
          context.healthCardDataStore.data
              .map { preferences -> preferences[getUserKey(userId)] }
              .first()

      if (encrypted == null) {
        Log.d(TAG, "No Health card found for user: $userId")
        return StorageResult.Success(null)
      }

      val decrypted =
          try {
            CryptoUtils.decrypt(encrypted)
          } catch (e: Exception) {
            Log.e(TAG, "Decryption error for user: $userId", e)
            return StorageResult.Error(StorageException.DecryptionError(e))
          }

      val healthCard =
          try {
            gson.fromJson(decrypted, HealthCard::class.java)
          } catch (e: Exception) {
            Log.e(TAG, "Deserialization error for user: $userId", e)
            return StorageResult.Error(StorageException.DeserializationError(e))
          }

      Log.d(TAG, "Health card loaded for user: $userId")
      StorageResult.Success(healthCard)
    } catch (e: IOException) {
      Log.e(TAG, "Access error to DataStore", e)
      StorageResult.Error(StorageException.DataStoreError(e))
    } catch (e: Exception) {
      Log.e(TAG, "Unexpected error during loading", e)
      StorageResult.Error(StorageException.DataStoreError(e))
    }
  }

  /**
   * Deletes the [HealthCard] associated with the given [userId].
   *
   * @param context Android context to access DataStore.
   * @param userId Unique identifier for the user.
   * @return [StorageResult.Success] on success or [StorageResult.Error] if DataStore fails.
   */
  suspend fun deleteHealthCard(context: Context, userId: String): StorageResult<Unit> {
    return try {
      context.healthCardDataStore.edit { preferences -> preferences.remove(getUserKey(userId)) }
      Log.d(TAG, "Health card for user: $userId deleted")
      StorageResult.Success(Unit)
    } catch (e: IOException) {
      Log.e(TAG, "Error during deletion", e)
      StorageResult.Error(StorageException.DataStoreError(e))
    }
  }

  /**
   * Updates the [HealthCard] associated with [userId] using the new [updatedCard].
   *
   * @param context Android context to access DataStore.
   * @param userId Unique identifier for the user.
   * @param updatedCard A function that takes the current card (nullable) and returns the updated
   *   card.
   * @return [StorageResult.Success] on success, or [StorageResult.Error] if loading or saving
   *   fails.
   */
  suspend fun updateHealthCard(
      context: Context,
      userId: String,
      updatedCard: HealthCard
  ): StorageResult<Unit> {
    return try {

      val existing = loadHealthCard(context, userId)
      if (existing is StorageResult.Error) {
        return existing
      }

      saveHealthCard(context, userId, updatedCard)
    } catch (e: Exception) {
      Log.e(TAG, "Error when updating health card for user: $userId", e)
      StorageResult.Error(StorageException.DataStoreError(e))
    }
  }
}
