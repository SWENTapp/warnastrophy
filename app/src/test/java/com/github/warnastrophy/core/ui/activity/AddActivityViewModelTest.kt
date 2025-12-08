package com.github.warnastrophy.core.ui.activity

import com.github.warnastrophy.core.data.repository.MockActivityRepository
import com.github.warnastrophy.core.model.Activity
import com.github.warnastrophy.core.ui.features.dashboard.activity.AddActivityViewModel
import com.github.warnastrophy.core.util.AppConfig
import junit.framework.TestCase
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
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
class AddActivityViewModelTest {
  private lateinit var repository: MockActivityRepository
  private lateinit var viewModel: AddActivityViewModel
  private val testDispatcher = StandardTestDispatcher()

  private val activity = Activity("1", "Skiing")

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    repository = MockActivityRepository()
    viewModel =
        AddActivityViewModel(repository = repository, AppConfig.defaultUserId, testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `add activity successfully emits navigateBack and clears error`() = runTest {
    val navigateBackEvent = async { viewModel.navigateBack.firstOrNull() }
    viewModel.setActivityName(activity.activityName)

    viewModel.addActivity()
    advanceUntilIdle()

    val added = repository.getAllActivities().getOrNull()!!
    TestCase.assertEquals(1, added.size)
    TestCase.assertEquals(activity.activityName, added[0].activityName)

    TestCase.assertNotNull(navigateBackEvent.await())

    TestCase.assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun `add activity with invalid UI state sets error message`() = runTest {
    viewModel.setActivityName("")

    viewModel.addActivity()
    advanceUntilIdle()
    TestCase.assertEquals("At least one field is not valid!", viewModel.uiState.value.errorMsg)

    val event = withTimeoutOrNull(1000.milliseconds) { viewModel.navigateBack.firstOrNull() }
    TestCase.assertNull(event)
  }

  @Test
  fun `add activity saves movement config with default values`() = runTest {
    viewModel.setActivityName("Cycling")

    viewModel.addActivity()
    advanceUntilIdle()

    val added = repository.getAllActivities().getOrNull()!!
    TestCase.assertEquals(1, added.size)
    TestCase.assertEquals(50.0, added[0].movementConfig.preDangerThreshold)
    TestCase.assertEquals(10.seconds, added[0].movementConfig.preDangerTimeout)
    TestCase.assertEquals(1.0, added[0].movementConfig.dangerAverageThreshold)
  }

  @Test
  fun `add activity saves custom movement config values`() = runTest {
    viewModel.setActivityName("Mountain Biking")
    viewModel.setPreDangerThreshold("75.0")
    viewModel.setPreDangerTimeout("15s")
    viewModel.setDangerAverageThreshold("2.5")

    viewModel.addActivity()
    advanceUntilIdle()

    val added = repository.getAllActivities().getOrNull()!!
    TestCase.assertEquals(1, added.size)
    TestCase.assertEquals(75.0, added[0].movementConfig.preDangerThreshold)
    TestCase.assertEquals(15.seconds, added[0].movementConfig.preDangerTimeout)
    TestCase.assertEquals(2.5, added[0].movementConfig.dangerAverageThreshold)
  }

  @Test
  fun `add activity with invalid preDangerThreshold sets error message`() = runTest {
    viewModel.setActivityName("Skiing")
    viewModel.setPreDangerThreshold("invalid")

    viewModel.addActivity()
    advanceUntilIdle()

    TestCase.assertEquals("At least one field is not valid!", viewModel.uiState.value.errorMsg)
  }

  @Test
  fun `add activity with invalid preDangerTimeout sets error message`() = runTest {
    viewModel.setActivityName("Skiing")
    viewModel.setPreDangerTimeout("invalid")

    viewModel.addActivity()
    advanceUntilIdle()

    TestCase.assertEquals("At least one field is not valid!", viewModel.uiState.value.errorMsg)
  }

  @Test
  fun `add activity with invalid dangerAverageThreshold sets error message`() = runTest {
    viewModel.setActivityName("Skiing")
    viewModel.setDangerAverageThreshold("invalid")

    viewModel.addActivity()
    advanceUntilIdle()

    TestCase.assertEquals("At least one field is not valid!", viewModel.uiState.value.errorMsg)
  }

  @Test
  fun `setPreDangerThreshold updates UI state`() = runTest {
    viewModel.setPreDangerThreshold("100.0")
    TestCase.assertEquals("100.0", viewModel.uiState.value.preDangerThresholdStr)
  }

  @Test
  fun `setPreDangerTimeout updates UI state`() = runTest {
    viewModel.setPreDangerTimeout("30s")
    TestCase.assertEquals("30s", viewModel.uiState.value.preDangerTimeoutStr)
  }

  @Test
  fun `setDangerAverageThreshold updates UI state`() = runTest {
    viewModel.setDangerAverageThreshold("5.0")
    TestCase.assertEquals("5.0", viewModel.uiState.value.dangerAverageThresholdStr)
  }
}
