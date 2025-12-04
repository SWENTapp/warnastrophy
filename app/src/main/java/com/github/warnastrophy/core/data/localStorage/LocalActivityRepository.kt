package com.github.warnastrophy.core.data.localStorage

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.warnastrophy.core.data.repository.ActivityRepository
import com.github.warnastrophy.core.model.Activity
import com.github.warnastrophy.core.ui.common.ErrorHandler
import com.github.warnastrophy.core.ui.common.ErrorType
import com.github.warnastrophy.core.ui.navigation.Screen
import com.github.warnastrophy.core.util.CryptoUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.activityDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "activities_encrypted")

/**
 * Local implementation of [ActivityRepository] using DataStore for persistence.
 *
 * Activities are serialized to JSON, encrypted using [CryptoUtils], and stored in DataStore. All
 * CRUD operations load from and persist to encrypted DataStore storage.
 *
 * @param context Android context to access DataStore.
 */
class LocalActivityRepository(
    private val context: Context,
    private val errorHandler: ErrorHandler
) : ActivityRepository {
  private val gson = Gson()

  companion object {
    private const val TAG = "LocalActivityRepository"

    private fun getUserKey(userId: String) = stringPreferencesKey("activities_$userId")
  }

  override fun getNewUid(): String = UUID.randomUUID().toString()

  override suspend fun addActivity(userId: String, activity: Activity): Result<Unit> = runCatching {
    val activities = loadActivitiesMap(userId)

    require(!activities.containsKey(activity.id)) {
      addError()
      "Activity ${activity.id} already exists for user $userId"
    }

    val updated = activities.toMutableMap()
    updated[activity.id] = activity

    saveActivitiesMap(userId, updated)
  }

  override suspend fun getAllActivities(userId: String): Result<List<Activity>> = runCatching {
    val activities = loadActivitiesMap(userId)
    activities.values.toList()
  }

  override suspend fun getActivity(activityId: String, userId: String): Result<Activity> =
      runCatching {
        val activities = loadActivitiesMap(userId)

        activities[activityId]
            ?: run {
              addError()
              throw NoSuchElementException(getNotFoundErrorMessage(activityId, userId))
            }
      }

  override suspend fun editActivity(
      activityId: String,
      userId: String,
      newActivity: Activity
  ): Result<Unit> = runCatching {
    require(activityId == newActivity.id) {
      addError()
      "Activity ID mismatch"
    }

    val activities = loadActivitiesMap(userId)

    if (!activities.containsKey(activityId)) {
      addError()
      throw NoSuchElementException(getNotFoundErrorMessage(activityId, userId))
    }

    val updated = activities.toMutableMap()
    updated[activityId] = newActivity

    saveActivitiesMap(userId, updated)
  }

  override suspend fun deleteActivity(userId: String, activityId: String): Result<Unit> =
      runCatching {
        val activities = loadActivitiesMap(userId)

        if (!activities.containsKey(activityId)) {
          throw NoSuchElementException(getNotFoundErrorMessage(activityId, userId))
        }

        val updated = activities.toMutableMap()
        updated.remove(activityId)

        saveActivitiesMap(userId, updated)
      }

  /**
   * Loads all activities for a user from encrypted DataStore.
   *
   * @param userId The user ID to load activities for.
   * @return Map of activity ID to Activity, or empty map if no data found.
   * @throws Exception if decryption or deserialization fails.
   */
  private suspend fun loadActivitiesMap(userId: String): Map<String, Activity> {
    return try {
      val encrypted =
          context.activityDataStore.data
              .map { preferences -> preferences[getUserKey(userId)] }
              .first()

      if (encrypted == null) {
        return emptyMap()
      }

      val decrypted =
          try {
            CryptoUtils.decrypt(encrypted)
          } catch (e: Exception) {
            Log.e(TAG, "Decryption error for user: $userId", e)
            addError()
            throw StorageException.DecryptionError(e)
          }

      val activities =
          try {
            val type = object : TypeToken<Map<String, Activity>>() {}.type
            gson.fromJson<Map<String, Activity>>(decrypted, type)
          } catch (e: Exception) {
            Log.e(TAG, "Deserialization error for user: $userId", e)
            addError()
            throw StorageException.DeserializationError(e)
          }

      activities ?: emptyMap()
    } catch (e: IOException) {
      Log.e(TAG, "DataStore access error", e)
      addError()
      throw StorageException.DataStoreError(e)
    } catch (e: StorageException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Unexpected error during load", e)
      addError()
      throw StorageException.DataStoreError(e)
    }
  }

  /**
   * Saves activities map to encrypted DataStore.
   *
   * @param userId The user ID to save activities for.
   * @param activities Map of activity ID to Activity to save.
   * @throws Exception if encryption or saving fails.
   */
  private suspend fun saveActivitiesMap(userId: String, activities: Map<String, Activity>) {
    try {
      val json = gson.toJson(activities)
      val encrypted = CryptoUtils.encrypt(json)

      context.activityDataStore.edit { preferences -> preferences[getUserKey(userId)] = encrypted }
    } catch (e: Exception) {
      Log.e(TAG, "Error saving activities for user: $userId", e)
      addError()
      throw StorageException.DataStoreError(e)
    }
  }

  private fun getNotFoundErrorMessage(activityId: String, userId: String): String {
    return "Activity $activityId not found for user $userId"
  }

  private fun addError() {
    errorHandler.addErrorToScreens(
        ErrorType.ACTIVITY_REPOSITORY_ERROR,
        listOf(
            Screen.AddActivity,
            Screen.ActivitiesList,
        ))
  }
}
