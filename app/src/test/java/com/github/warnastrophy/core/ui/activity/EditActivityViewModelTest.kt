package com.github.warnastrophy.core.ui.activity

import com.github.warnastrophy.core.data.repository.MockActivityRepository
import com.github.warnastrophy.core.data.service.MovementConfig
import com.github.warnastrophy.core.model.Activity
import com.github.warnastrophy.core.ui.features.dashboard.activity.EditActivityViewModel
import com.github.warnastrophy.core.util.AppConfig
import junit.framework.TestCase
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditActivityViewModelTest {
  private lateinit var repository: MockActivityRepository
  private lateinit var viewModel: EditActivityViewModel
  private val testDispatcher = StandardTestDispatcher()
  private val act_1 =
      Activity(
          "1",
          "Climbing",
          MovementConfig(
              preDangerThreshold = 60.0,
              preDangerTimeout = 15.seconds,
              dangerAverageThreshold = 2.0))
  private val act_2 = Activity("2", "Hiking")

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    repository = MockActivityRepository()
    viewModel = EditActivityViewModel(repository, AppConfig.defaultUserId, testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  private suspend fun prepareRepository() {
    repository.addActivity(AppConfig.defaultUserId, act_1)
    repository.addActivity(AppConfig.defaultUserId, act_2)
  }

  @Test
  fun `load Activity Populate UI state`() = runTest {
    prepareRepository()
    advanceUntilIdle()
    viewModel.loadActivity("1")
    advanceUntilIdle()
    val uiState = viewModel.uiState.first()
    assertEquals(act_1.activityName, uiState.activityName)
    assertNull(uiState.errorMsg)
  }

  @Test
  fun `load Activity populates movement config in UI state`() = runTest {
    prepareRepository()
    advanceUntilIdle()
    viewModel.loadActivity("1")
    advanceUntilIdle()
    val uiState = viewModel.uiState.first()
    assertEquals("60.0", uiState.preDangerThresholdStr)
    assertEquals("15s", uiState.preDangerTimeoutStr)
    assertEquals("2.0", uiState.dangerAverageThresholdStr)
  }

  @Test
  fun `Edit contact with valid contact update repository and emits navigateBack event`() = runTest {
    prepareRepository()
    advanceUntilIdle()
    val navigateBackEvent = async { viewModel.navigateBack.firstOrNull() }
    viewModel.setActivityName("Football")
    viewModel.editActivity("1")

    advanceUntilIdle()

    val updated = repository.getActivity("1", AppConfig.defaultUserId).getOrNull()!!
    advanceUntilIdle()
    assertEquals("Football", updated.activityName)
    assertNotNull(navigateBackEvent.await())
  }

  @Test
  fun `edit activity updates movement config in repository`() = runTest {
    prepareRepository()
    advanceUntilIdle()
    viewModel.loadActivity("1")
    advanceUntilIdle()

    viewModel.setPreDangerThreshold("80.0")
    viewModel.setPreDangerTimeout("20s")
    viewModel.setDangerAverageThreshold("3.5")
    viewModel.editActivity("1")
    advanceUntilIdle()

    val updated = repository.getActivity("1", AppConfig.defaultUserId).getOrNull()!!
    assertEquals(80.0, updated.movementConfig.preDangerThreshold)
    assertEquals(20.seconds, updated.movementConfig.preDangerTimeout)
    assertEquals(3.5, updated.movementConfig.dangerAverageThreshold)
  }

  @Test
  fun `Delete contact remove contact and emits navigateBack event`() = runTest {
    prepareRepository()
    advanceUntilIdle()
    val navigateBackEvent = async { viewModel.navigateBack.firstOrNull() }
    viewModel.deleteActivity("2")
    advanceUntilIdle()

    val result = repository.getActivity("2", AppConfig.defaultUserId)
    assertEquals(true, result.isFailure)

    assertNotNull(navigateBackEvent.await())
  }

  @Test
  fun `edit activity with invalid UI state sets error message`() = runTest {
    prepareRepository()
    advanceUntilIdle()

    viewModel.setActivityName("")

    viewModel.editActivity("1")
    advanceUntilIdle()
    TestCase.assertEquals("At least one field is not valid!", viewModel.uiState.value.errorMsg)
    val event = withTimeoutOrNull(1.milliseconds) { viewModel.navigateBack.firstOrNull() }
    assertNull(event)
  }

  @Test
  fun `edit activity with invalid preDangerThreshold sets error message`() = runTest {
    prepareRepository()
    advanceUntilIdle()
    viewModel.loadActivity("1")
    advanceUntilIdle()

    viewModel.setPreDangerThreshold("invalid")
    viewModel.editActivity("1")
    advanceUntilIdle()

    assertEquals("At least one field is not valid!", viewModel.uiState.value.errorMsg)
  }

  @Test
  fun `edit activity with invalid preDangerTimeout sets error message`() = runTest {
    prepareRepository()
    advanceUntilIdle()
    viewModel.loadActivity("1")
    advanceUntilIdle()

    viewModel.setPreDangerTimeout("invalid")
    viewModel.editActivity("1")
    advanceUntilIdle()

    assertEquals("At least one field is not valid!", viewModel.uiState.value.errorMsg)
  }

  @Test
  fun `set error message when load activity fails`() = runTest {
    prepareRepository()
    advanceUntilIdle()
    repository.shouldThrowException = true
    viewModel.loadActivity("1")
    advanceUntilIdle()
    assertTrue(
        viewModel.uiState.value.errorMsg?.startsWith("Failed to load activity:") ?: false,
        "Error message is not set")
  }

  @Test
  fun `set error message when edit activity fails`() = runTest {
    prepareRepository()
    advanceUntilIdle()
    repository.shouldThrowException = true
    viewModel.setActivityName("Football")
    viewModel.editActivity("1")
    advanceUntilIdle()
    assertTrue(
        viewModel.uiState.value.errorMsg?.startsWith("Failed to edit activity:") ?: false,
        "Error message is not set")
  }
}
