package com.github.warnastrophy.core.ui.activity

import com.github.warnastrophy.core.data.repository.MockActivityRepository
import com.github.warnastrophy.core.domain.model.Activity
import com.github.warnastrophy.core.ui.features.dashboard.activity.EditActivityViewModel
import com.github.warnastrophy.core.util.AppConfig
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

class EditActivityViewModelTest {
  private lateinit var repository: MockActivityRepository
  private lateinit var viewModel: EditActivityViewModel
  private val testDispatcher = StandardTestDispatcher()
  private val act_1 = Activity("1", "Climbing")
  private val act_2 = Activity("2", "Hiking")

  @Before
  fun setUp() = runTest {
    Dispatchers.setMain(testDispatcher)
    repository = MockActivityRepository()
    repository.addActivity(AppConfig.defaultUserId, act_1)
    repository.addActivity(AppConfig.defaultUserId, act_2)
    viewModel = EditActivityViewModel(repository, AppConfig.defaultUserId)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `load Activity Populate UI state`() = runTest {
    viewModel.loadActivity("1")
    advanceUntilIdle() // This ensures loadContact() completes and updates uiState.
    val uiState = viewModel.uiState.first()
    assertEquals(act_1.activityName, uiState.activityName)
    assertNull(uiState.errorMsg)
  }

  @Test
  fun `Edit contact with valid contact update repository and emits navigateBack event`() = runTest {
    val navigateBackEvent = async { viewModel.navigateBack.firstOrNull() }
    viewModel.setActivityName("Football")
    viewModel.editActivity("1")

    advanceUntilIdle()

    val updated = repository.getActivity("1", AppConfig.defaultUserId).getOrNull()!!

    assertEquals("Football", updated.activityName)
    assertNotNull(navigateBackEvent.await())
  }

  @Test
  fun `Delete contact remove contact and emits navigateBack event`() = runTest {
    val navigateBackEvent = async { viewModel.navigateBack.firstOrNull() }
    viewModel.deleteActivity("2")
    advanceUntilIdle()

    val result = repository.getActivity("2", AppConfig.defaultUserId)
    assertEquals(true, result.isFailure)

    assertNotNull(navigateBackEvent.await())
  }
}
