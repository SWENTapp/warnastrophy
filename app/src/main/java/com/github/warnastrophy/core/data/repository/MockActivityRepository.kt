package com.github.warnastrophy.core.data.repository

import com.github.warnastrophy.core.model.Activity
import java.util.UUID

/** A mock repository for testing purpose */
class MockActivityRepository : ActivityRepository {
  private val exceptionMessage = "Forced failure"

  private val contactsByUser = mutableMapOf<String, MutableMap<String, Activity>>()
  var shouldThrowException: Boolean = false

  private fun bucket(userId: String): MutableMap<String, Activity> =
      contactsByUser.getOrPut(userId) { mutableMapOf() }

  override fun getNewUid(): String = UUID.randomUUID().toString()

  override suspend fun getAllActivities(userId: String): Result<List<Activity>> = runCatching {
    if (shouldThrowException) throw Exception(exceptionMessage)
    bucket(userId).values.toList()
  }

  override suspend fun getActivity(activityId: String, userId: String): Result<Activity> =
      runCatching {
        if (shouldThrowException) throw Exception(exceptionMessage)
        bucket(userId)[activityId]
            ?: throw NoSuchElementException(getNotFoundErrorMessage(activityId, userId))
      }

  override suspend fun editActivity(
      activityId: String,
      userId: String,
      newActivity: Activity
  ): Result<Unit> = runCatching {
    if (shouldThrowException) throw Exception(exceptionMessage)
    require(activityId == newActivity.id) { "Activity ID mismatch" }
    val userMap = bucket(userId)
    if (!userMap.containsKey(activityId)) {
      throw NoSuchElementException(getNotFoundErrorMessage(activityId, userId))
    }
    userMap[activityId] = newActivity
  }

  override suspend fun deleteActivity(userId: String, activityId: String): Result<Unit> =
      runCatching {
        if (shouldThrowException) throw Exception(exceptionMessage)
        val userMap = bucket(userId)
        if (userMap.remove(activityId) == null) {
          throw NoSuchElementException(getNotFoundErrorMessage(activityId, userId))
        }
      }

  override suspend fun addActivity(userId: String, activity: Activity): Result<Unit> = runCatching {
    if (shouldThrowException) throw Exception(exceptionMessage)
    val userMap = bucket(userId)
    require(!userMap.containsKey(activity.id)) {
      "Activity ${activity.id} already exists for user $userId"
    }
    userMap[activity.id] = activity
  }

  private fun getNotFoundErrorMessage(activityId: String, userId: String): String {
    return "Activity $activityId not found for user $userId"
  }
}
