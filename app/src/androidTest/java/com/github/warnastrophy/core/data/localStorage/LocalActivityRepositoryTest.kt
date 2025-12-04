package com.github.warnastrophy.core.data.localStorage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.github.warnastrophy.core.model.Activity
import com.github.warnastrophy.core.ui.common.ErrorHandler
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

class LocalActivityRepositoryTest {
  private lateinit var context: Context
  private lateinit var repository: LocalActivityRepository
  private lateinit var errorHandler: ErrorHandler
  private val userId = "testUser"

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    errorHandler = ErrorHandler()
    repository = LocalActivityRepository(context, errorHandler)
  }

  @After
  fun tearDown() {
    runBlocking { context.activityDataStore.edit { it.clear() } }
  }

  @Test
  fun testAddActivitySuccess() = runBlocking {
    val activity = Activity(id = "activity1", activityName = "Running")

    val result = repository.addActivity(userId, activity)

    assertTrue(result.isSuccess)
  }

  @Test
  fun testAddActivityFailsWhenIdExists() = runBlocking {
    val activity = Activity(id = "activity1", activityName = "Running")

    repository.addActivity(userId, activity)
    val result = repository.addActivity(userId, activity)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("already exists") == true)
  }

  @Test
  fun testGetAllActivitiesReturnsEmptyListInitially() = runBlocking {
    val result = repository.getAllActivities(userId)

    assertTrue(result.isSuccess)
    assertEquals(0, result.getOrNull()?.size)
  }

  @Test
  fun testGetAllActivitiesReturnsAllAddedActivities() = runBlocking {
    val activity1 = Activity(id = "activity1", activityName = "Running")
    val activity2 = Activity(id = "activity2", activityName = "Swimming")

    repository.addActivity(userId, activity1)
    repository.addActivity(userId, activity2)

    val result = repository.getAllActivities(userId)

    assertTrue(result.isSuccess)
    val activities = result.getOrNull()!!
    assertEquals(2, activities.size)
    assertTrue(activities.contains(activity1))
    assertTrue(activities.contains(activity2))
  }

  @Test
  fun testGetActivitySuccess() = runBlocking {
    val activity = Activity(id = "activity1", activityName = "Running")
    repository.addActivity(userId, activity)

    val result = repository.getActivity("activity1", userId)

    assertTrue(result.isSuccess)
    assertEquals(activity, result.getOrNull())
  }

  @Test
  fun testGetActivityFailsWhenNotFound() = runBlocking {
    val result = repository.getActivity("nonexistent", userId)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is NoSuchElementException)
  }

  @Test
  fun testEditActivitySuccess() = runBlocking {
    val activity = Activity(id = "activity1", activityName = "Running")
    repository.addActivity(userId, activity)

    val updatedActivity = activity.copy(activityName = "Jogging")
    val result = repository.editActivity("activity1", userId, updatedActivity)

    assertTrue(result.isSuccess)

    val retrieved = repository.getActivity("activity1", userId)
    assertEquals("Jogging", retrieved.getOrNull()?.activityName)
  }

  @Test
  fun testEditActivityFailsWhenIdMismatch() = runBlocking {
    val activity = Activity(id = "activity1", activityName = "Running")
    repository.addActivity(userId, activity)

    val wrongActivity = Activity(id = "activity2", activityName = "Swimming")
    val result = repository.editActivity("activity1", userId, wrongActivity)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull()?.message?.contains("ID mismatch") == true)
  }

  @Test
  fun testEditActivityFailsWhenNotFound() = runBlocking {
    val activity = Activity(id = "activity1", activityName = "Running")
    val result = repository.editActivity("activity1", userId, activity)

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is NoSuchElementException)
  }

  @Test
  fun testDeleteActivitySuccess() = runBlocking {
    val activity = Activity(id = "activity1", activityName = "Running")
    repository.addActivity(userId, activity)

    val result = repository.deleteActivity(userId, "activity1")

    assertTrue(result.isSuccess)

    val retrieved = repository.getActivity("activity1", userId)
    assertTrue(retrieved.isFailure)
  }

  @Test
  fun testDeleteActivityFailsWhenNotFound() = runBlocking {
    val result = repository.deleteActivity(userId, "nonexistent")

    assertTrue(result.isFailure)
    assertTrue(result.exceptionOrNull() is NoSuchElementException)
  }

  @Test
  fun testGetNewUidReturnsUniqueIds() {
    val uid1 = repository.getNewUid()
    val uid2 = repository.getNewUid()

    assertFalse(uid1.isEmpty())
    assertFalse(uid2.isEmpty())
    assertFalse(uid1 == uid2)
  }

  @Test
  fun testMultipleUsersHaveSeparateActivities() = runBlocking {
    val user1 = "user1"
    val user2 = "user2"
    val activity1 = Activity(id = "activity1", activityName = "User1 Activity")
    val activity2 = Activity(id = "activity2", activityName = "User2 Activity")

    repository.addActivity(user1, activity1)
    repository.addActivity(user2, activity2)

    val user1Activities = repository.getAllActivities(user1).getOrNull()!!
    val user2Activities = repository.getAllActivities(user2).getOrNull()!!

    assertEquals(1, user1Activities.size)
    assertEquals(1, user2Activities.size)
    assertEquals(activity1, user1Activities[0])
    assertEquals(activity2, user2Activities[0])
  }

  @Test
  fun testDataPersistsAcrossRepositoryInstances() = runBlocking {
    val activity = Activity(id = "activity1", activityName = "Running")
    repository.addActivity(userId, activity)

    // Create a new repository instance
    val newRepository = LocalActivityRepository(context, errorHandler)
    val result = newRepository.getActivity("activity1", userId)

    assertTrue(result.isSuccess)
    assertEquals(activity, result.getOrNull())
  }
}
