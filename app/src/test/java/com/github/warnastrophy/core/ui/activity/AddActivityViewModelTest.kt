package com.github.warnastrophy.core.ui.activity

import com.github.warnastrophy.core.data.repository.MockActivityRepository
import com.github.warnastrophy.core.domain.model.Activity
import com.github.warnastrophy.core.ui.features.dashboard.activity.AddActivityViewModel
import com.github.warnastrophy.core.util.AppConfig
import junit.framework.TestCase
import kotlin.time.Duration.Companion.milliseconds
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
class AddActivityViewModelTest {
  private lateinit var repository: MockActivityRepository
  private lateinit var viewModel: AddActivityViewModel
  private val testDispatcher = StandardTestDispatcher()

  private val activity = Activity("1", "Skiing")

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    repository = MockActivityRepository()
    viewModel = AddActivityViewModel(repository = repository, AppConfig.defaultUserId)
  }

  @After
  fun tearDown() {
    repository.shouldThrowException = false
    Dispatchers.resetMain()
  }

  @Test
  /**
   * Tests that [addActivity] successfully adds a valid activity to the repository, emits
   * [navigateBack] event, and ensures that any previous error message in [uiState] is cleared upon
   * success.
   */
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
  /**
   * Tests that [addActivity] correctly handles an invalid UI state (e.g., missing required field).
   * It should set an appropriate error message in [uiState].
   */
  fun `add activity with invalid UI state sets error message`() = runTest {
    // Setup: Collect the navigateBack event flow concurrently with a timeout
    val navigateBackEvent = async {
      withTimeoutOrNull(100.milliseconds) {
        viewModel.navigateBack.first() // first() throws if timeout reached before emission
      }
    }

    viewModel.setActivityName("")

    viewModel.addActivity()
    advanceUntilIdle()

    TestCase.assertNull(navigateBackEvent.await())

    TestCase.assertEquals("At least one field is not valid!", viewModel.uiState.value.errorMsg)
  }

  @Test
  /**
   * Tests the core principle of using [MutableSharedFlow] for navigation events.
   * * It ensures that after the ViewModel successfully emits the [navigateBack] event upon activity
   *   creation (consuming the first event), no subsequent event remains buffered or is immediately
   *   re-emitted to a new collector. This validates that the event is truly **transient**
   *   (fire-and-forget) and confirms that the separate [resetNavigation] call is correctly obsolete
   *   for this pattern.
   */
  fun `MapsBack event is transient (no need for reset)`() = runTest {
    // 1. Setup: Collect the first event
    val firstEvent = async {
      withTimeoutOrNull(100.milliseconds) { viewModel.navigateBack.first() }
    }

    viewModel.setActivityName(activity.activityName)

    viewModel.addActivity()
    advanceUntilIdle()

    TestCase.assertNotNull(firstEvent.await())

    val secondEvent = withTimeoutOrNull(10.milliseconds) { viewModel.navigateBack.first() }
    TestCase.assertNull(secondEvent)
  }
}
