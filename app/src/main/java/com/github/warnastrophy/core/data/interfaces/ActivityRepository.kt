package com.github.warnastrophy.core.data.interfaces

import com.github.warnastrophy.core.model.Activity
import com.github.warnastrophy.core.util.AppConfig

interface ActivityRepository {
  /**
   * Adds a new Activity item to the repository.
   *
   * @param activity The Activity item to add.
   * @param userId user ID that is using the app. The default value is
   *   [com.github.warnastrophy.core.util.AppConfig.defaultUserId]
   * @return A [Result] containing Unit on success or a failure with the exception on error.
   */
  suspend fun addActivity(
      userId: String = AppConfig.defaultUserId,
      activity: Activity
  ): Result<Unit>

  /** Generates and returns a new unique identifier for a Contact item. */
  fun getNewUid(): String

  /**
   * Retrieves all Activity items from the repository.
   *
   * @param userId user ID that is using the app. The default value is [AppConfig.defaultUserId]
   * @return A [Result] containing a list of all Activity items on success, or a failure with the
   *   error.
   */
  suspend fun getAllActivities(userId: String = AppConfig.defaultUserId): Result<List<Activity>>

  /**
   * Retrieves a specific Activity item by its unique identifier.
   *
   * @param activityId The unique identifier of the Activity item to retrieve.
   * @param userId user ID that is using the app. The default value is [AppConfig.defaultUserId]
   * @return A [Result] containing the Activity item with the specified identifier on success, or a
   *   failure if not found or on error.
   */
  suspend fun getActivity(
      activityId: String,
      userId: String = AppConfig.defaultUserId
  ): Result<Activity>

  /**
   * Edits an existing Activity item in the repository.
   *
   * @param activityId The unique identifier of the Activity item to edit.
   * @param userId user ID that is using the app. The default value is [AppConfig.defaultUserId]
   * @param newActivity The new value for the Activity item.
   * @return A [Result] containing Unit on success or a failure with the exception if the edit
   *   failed.
   */
  suspend fun editActivity(
      activityId: String,
      userId: String = AppConfig.defaultUserId,
      newActivity: Activity
  ): Result<Unit>
  /**
   * Deletes a Activity item from the repository.
   *
   * @param userId user ID that is using the app. The default value is [AppConfig.defaultUserId]
   * @param activityId The unique identifier of the Contact item to delete.
   * @return A [Result] containing Unit on success or a failure with the exception if deletion
   *   failed.
   */
  suspend fun deleteActivity(
      userId: String = AppConfig.defaultUserId,
      activityId: String
  ): Result<Unit>
}
