package com.github.warnastrophy.core.ui.activity

import com.github.warnastrophy.core.data.repository.MockActivityRepository
import com.github.warnastrophy.core.model.Activity
import com.github.warnastrophy.core.ui.features.dashboard.activity.ActivityListViewModel
import com.github.warnastrophy.core.util.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ActivityListViewModelTest() {
  private lateinit var repository: MockActivityRepository
  private lateinit var viewModel: ActivityListViewModel
  private val testDispatcher = StandardTestDispatcher()

  private val activity = Activity("1", "Climbing")

  @Before
  fun setUp() = runTest {
    Dispatchers.setMain(testDispatcher)
    repository = MockActivityRepository()
    viewModel = ActivityListViewModel(repository, AppConfig.defaultUserId, testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    repository.shouldThrowException = false
  }

  @Test
  fun `init handles repository failure`() = runTest {
    repository.addActivity(AppConfig.defaultUserId, activity)
    advanceUntilIdle()
    repository.shouldThrowException = true

    viewModel.refreshUIState()

    advanceUntilIdle()

    val uiState = viewModel.uiState.value
    Assert.assertNotNull(uiState.errorMsg)
  }

  @Test
  fun `initialization loadsActivities On Success`() = runTest {
    repository.addActivity(AppConfig.defaultUserId, activity)
    advanceUntilIdle()
    viewModel.refreshUIState()
    val expectedActivities = listOf(activity)
    advanceUntilIdle()
    val finalState = viewModel.uiState.value
    assertEquals(expectedActivities, finalState.activities)
    assertNull(finalState.errorMsg)
  }
}
