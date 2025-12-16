package com.github.warnastrophy.core.ui.feature.dashboard

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import com.github.warnastrophy.core.data.provider.UserPreferencesRepositoryProvider
import com.github.warnastrophy.core.data.repository.MockActivityRepository
import com.github.warnastrophy.core.data.service.DangerModeService
import com.github.warnastrophy.core.data.service.MockPermissionManager
import com.github.warnastrophy.core.data.service.StateManagerService
import com.github.warnastrophy.core.model.Activity
import com.github.warnastrophy.core.permissions.PermissionManagerInterface
import com.github.warnastrophy.core.permissions.PermissionResult
import com.github.warnastrophy.core.ui.features.dashboard.DangerModeCapability
import com.github.warnastrophy.core.ui.features.dashboard.DangerModeCardViewModel
import com.github.warnastrophy.core.ui.features.dashboard.Effect
import com.github.warnastrophy.core.util.AppConfig
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class DangerModeCardViewModelTest {
  private lateinit var viewModel: DangerModeCardViewModel
  private lateinit var repository: MockActivityRepository
  private val testDispatcher = StandardTestDispatcher()
  private lateinit var activity: android.app.Activity

  private lateinit var mockPermissionManager: PermissionManagerInterface

  private lateinit var mockDataStore: DataStore<Preferences>

  @Before
  fun setup() {
    activity = Robolectric.buildActivity(android.app.Activity::class.java).get()
    mockDataStore = mockk(relaxed = true)
    UserPreferencesRepositoryProvider.initLocal(mockDataStore)
    StateManagerService.init(ApplicationProvider.getApplicationContext())
    val realMockPermissionManager = MockPermissionManager(currentResult = PermissionResult.Granted)
    mockPermissionManager = spyk(realMockPermissionManager)
    StateManagerService.permissionManager = mockPermissionManager
    StateManagerService.dangerModeService =
        DangerModeService(permissionManager = StateManagerService.permissionManager)
    // Initialize the mock repository for testing
    repository = MockActivityRepository()
    viewModel =
        DangerModeCardViewModel(
            repository = repository, userId = AppConfig.defaultUserId, testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun `isDangerModeEnabled initial state is false`() = runTest {
    assertEquals(false, viewModel.isDangerModeEnabled.first())
  }

  @Test
  fun `onDangerModeToggled updates isDangerModeEnabled`() = runTest {
    viewModel.onDangerModeToggled(true)
    assertEquals(true, viewModel.isDangerModeEnabled.first())

    viewModel.onDangerModeToggled(false)
    assertEquals(false, viewModel.isDangerModeEnabled.first())
  }

  @Test
  fun `currentActivity initial state is null`() = runTest {
    assertEquals(null, viewModel.currentActivity.first())
  }

  @Test
  fun `onActivitySelected updates currentActivity`() = runTest {
    val hikingActivity = Activity(id = "1", activityName = "Hiking")
    viewModel.onActivitySelected(hikingActivity)
    assertEquals(hikingActivity, viewModel.currentActivity.first())

    viewModel.onActivitySelected(null)
    assertEquals(null, viewModel.currentActivity.first())
  }

  @Test
  fun `capabilities initial state is empty set`() = runTest {
    assertEquals(emptySet<DangerModeCapability>(), viewModel.capabilities.first())
  }

  @Test
  fun `onCapabilitiesChanged updates capabilities`() = runTest {
    val newCapabilities = setOf(DangerModeCapability.SMS)
    viewModel.onCapabilitiesChanged(newCapabilities)
    assertEquals(newCapabilities, viewModel.capabilities.first())
  }

  @Test
  fun `onCapabilityToggled adds capability when not present`() = runTest {
    viewModel.onCapabilityToggled(DangerModeCapability.SMS)
    assertEquals(setOf(DangerModeCapability.SMS), viewModel.capabilities.first())
  }

  @Test
  fun `capability exclusivity enforces single selection and enables autoActions`() = runTest {
    // Enable SMS capability
    viewModel.onCapabilityToggled(DangerModeCapability.SMS)
    assertEquals(setOf(DangerModeCapability.SMS), viewModel.capabilities.first())
    // autoActions should be enabled when capability turned on
    assertEquals(true, viewModel.autoActionsEnabled.first())

    // Now toggle CALL - should replace SMS with CALL
    viewModel.onCapabilityToggled(DangerModeCapability.CALL)
    assertEquals(setOf(DangerModeCapability.CALL), viewModel.capabilities.first())
    // SMS should be off
    assertFalse(viewModel.capabilities.first().contains(DangerModeCapability.SMS))
  }

  @Test
  fun `activities initial state is empty list`() = runTest {
    testDispatcher.scheduler.advanceUntilIdle()
    assertEquals(emptyList<Activity>(), viewModel.activities.first())
  }

  @Test
  fun `refreshActivities updates activities from repository`() = runTest {
    // Add activities to the repository
    val activity1 = Activity(id = "1", activityName = "Hiking")
    val activity2 = Activity(id = "2", activityName = "Climbing")
    repository.addActivity(userId = AppConfig.defaultUserId, activity = activity1)
    repository.addActivity(userId = AppConfig.defaultUserId, activity = activity2)

    // Refresh the activities
    viewModel.refreshActivities()

    // Wait for the coroutine to complete
    testDispatcher.scheduler.advanceUntilIdle()

    // Verify the activities are loaded
    val activities = viewModel.activities.first()
    assertEquals(2, activities.size)
  }

  @Test
  fun onPermissionsResult_markPermissionAsAsked() =
      runTest(testDispatcher) {
        viewModel.onPermissionsRequestStart()
        assertTrue(viewModel.permissionUiState.value.waitingForUserResponse)
        viewModel.onPermissionResult(activity)
        testDispatcher.scheduler.advanceUntilIdle()
        verify(exactly = 1) {
          mockPermissionManager.markPermissionsAsAsked(viewModel.alertModePermission)
        }
        assertFalse(viewModel.permissionUiState.value.waitingForUserResponse)
      }

  @Test
  fun handleToggle_ON_with_Granted_permission_activatesDangerMode_and_emitsStartServiceEffect() =
      runTest(testDispatcher) {
        val collectedEffects = mutableListOf<Effect>()
        launch { viewModel.effects.take(1).toList(collectedEffects) }
        viewModel.handleToggle(isChecked = true, permissionResult = PermissionResult.Granted)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, collectedEffects.size)
        assertEquals(collectedEffects[0], Effect.StartForegroundService)
      }

  @Test
  fun handleToggle_ON_with_Denied_permission_emits_RequestLocationPermissionEffect() =
      runTest(testDispatcher) {
        val collectedEffects = mutableListOf<Effect>()
        launch { viewModel.effects.take(1).toList(collectedEffects) }
        viewModel.handleToggle(
            isChecked = true, permissionResult = PermissionResult.Denied(emptyList()))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, collectedEffects.size)
        assertEquals(collectedEffects[0], Effect.RequestLocationPermission)
      }

  @Test
  fun handleToggle_ON_with_Permanent_Denied_permission_emits_OpenAppSettingsEffect() =
      runTest(testDispatcher) {
        val collectedEffects = mutableListOf<Effect>()
        launch { viewModel.effects.take(1).toList(collectedEffects) }
        viewModel.handleToggle(
            isChecked = true, permissionResult = PermissionResult.PermanentlyDenied(emptyList()))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, collectedEffects.size)
        assertEquals(collectedEffects[0], Effect.ShowOpenAppSettings)
      }

  @Test
  fun handleToggle_Off_emits_StopForegroundServiceEffect() =
      runTest(testDispatcher) {
        val collectedEffects = mutableListOf<Effect>()
        launch { viewModel.effects.take(1).toList(collectedEffects) }
        viewModel.handleToggle(isChecked = false, permissionResult = PermissionResult.Granted)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, collectedEffects.size)
        assertEquals(collectedEffects[0], Effect.StopForegroundService)
      }

  @Test
  fun `confirm switches are mutually exclusive`() = runTest {
    viewModel.onConfirmVoiceChanged(true)
    assertTrue(viewModel.confirmVoiceRequired.first())
    assertFalse(viewModel.confirmTouchRequired.first())

    viewModel.onConfirmTouchChanged(true)
    assertTrue(viewModel.confirmTouchRequired.first())
    assertFalse(viewModel.confirmVoiceRequired.first())
  }
}
