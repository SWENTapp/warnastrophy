package com.github.warnastrophy.core.ui.profile.preferences

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.github.warnastrophy.core.data.repository.DangerModePreferences
import com.github.warnastrophy.core.data.repository.UserPreferences
import com.github.warnastrophy.core.data.repository.UserPreferencesRepository
import com.github.warnastrophy.core.data.service.MockPermissionManager
import com.github.warnastrophy.core.permissions.PermissionResult
import com.github.warnastrophy.core.ui.features.profile.preferences.DangerModePreferencesViewModel
import com.github.warnastrophy.core.ui.features.profile.preferences.PendingAction
import com.github.warnastrophy.core.util.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DangerModePreferencesViewModelTest {
  private lateinit var activity: Activity
  private lateinit var context: Context
  private val testDispatcher = StandardTestDispatcher()
  private lateinit var permissionManager: MockPermissionManager
  private lateinit var prefs: SharedPreferences
  private lateinit var userPreferencesRepository: UserPreferencesRepository
  private lateinit var viewModel: DangerModePreferencesViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    activity = Robolectric.buildActivity(Activity::class.java).get()
    context = activity.applicationContext
    prefs = context.getSharedPreferences(AppConfig.PREF_FILE_NAME, Context.MODE_PRIVATE)
    permissionManager = spy(MockPermissionManager())
    userPreferencesRepository = mock()

    // Provide a default empty flow to prevent crashes on init
    whenever(userPreferencesRepository.getUserPreferences)
        .thenReturn(
            flowOf(
                UserPreferences(
                    DangerModePreferences(
                        alertMode = false, inactivityDetection = false, automaticSms = false),
                    themePreferences = false)))
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    prefs.edit().clear().apply()
  }

  /**
   * Helper function to create the ViewModel. The state of the MockPermissionManager should be set
   * *before* this is called.
   */
  private fun createViewModel() {
    viewModel = DangerModePreferencesViewModel(permissionManager, userPreferencesRepository)
  }

  @Test
  fun initialStateIsCorrectWhenPermissionsAreDenied() = runTest {
    // Configure the fake manager to return a Denied result
    val deniedResult = PermissionResult.Denied(listOf("FAKE_PERMISSION"))
    permissionManager.setPermissionResult(deniedResult)

    createViewModel()
    val initialState = viewModel.uiState.value

    assertFalse(initialState.alertModeAutomaticEnabled)
    assertFalse(initialState.inactivityDetectionEnabled)
    assertFalse(initialState.automaticSmsEnabled)
    assertNull(initialState.pendingPermissionAction)
    assertEquals(deniedResult, initialState.alertModePermissionResult)
    assertEquals(deniedResult, initialState.inactivityDetectionPermissionResult)
    assertEquals(deniedResult, initialState.smsPermissionResult)
  }

  @Test
  fun handlePreferenceChange_enablesToggle_whenPermissionIsGranted() {
    permissionManager.setPermissionResult(PermissionResult.Granted)
    createViewModel()
    var toggleCalledWith: Boolean? = null

    viewModel.handlePreferenceChange(
        isChecked = true,
        permissionResult = viewModel.uiState.value.alertModePermissionResult,
        onToggle = { toggleCalledWith = it },
        onPermissionDenied = { /* Test would fail if this is called */},
        onPermissionPermDenied = { /* Test would fail if this is called */})

    assertEquals(true, toggleCalledWith)
  }

  @Test
  fun onAlertModeToggled_withFalse_disablesAlertModeAndDependencies() = runTest {
    createViewModel()

    viewModel.onAlertModeToggled(false)

    testDispatcher.scheduler.advanceUntilIdle()

    verify(userPreferencesRepository).setAlertMode(false)
    verify(userPreferencesRepository).setInactivityDetection(false)
    verify(userPreferencesRepository).setAutomaticSms(false)
  }

  @Test
  fun onPermissionsResult_AlertMode_whenPermissionIsGranted_enablesToggle() = runTest {
    createViewModel()
    viewModel.onPermissionsRequestStart(PendingAction.TOGGLE_ALERT_MODE)
    permissionManager.setPermissionResult(
        PermissionResult.Granted) // Simulate user granting permission

    viewModel.onPermissionsResult(activity)
    testDispatcher.scheduler.advanceUntilIdle()

    verify(permissionManager).markPermissionsAsAsked(viewModel.alertModePermissions)
    verify(userPreferencesRepository).setAlertMode(true) // Check that toggle was called
    assertNull(viewModel.uiState.value.pendingPermissionAction) // Action should be cleared
  }

  @Test
  fun onPermissionsResult_AlertMode_whenPermissionIsDenied_doesNotEnableToggle() = runTest {
    createViewModel()
    viewModel.onPermissionsRequestStart(PendingAction.TOGGLE_ALERT_MODE)
    permissionManager.setPermissionResult(
        PermissionResult.Denied(emptyList())) // Simulate user denying

    viewModel.onPermissionsResult(activity)
    testDispatcher.scheduler.advanceUntilIdle()

    verify(permissionManager).markPermissionsAsAsked(viewModel.alertModePermissions)
    verify(userPreferencesRepository, never()).setAlertMode(true) // Toggle should NOT be called
    assertNull(viewModel.uiState.value.pendingPermissionAction)
  }

  @Test
  fun onPermissionsResult_InactivityDetection_whenPermissionIsGranted_enablesToggle() = runTest {
    createViewModel()
    viewModel.onPermissionsRequestStart(PendingAction.TOGGLE_INACTIVITY_DETECTION)
    permissionManager.setPermissionResult(PermissionResult.Granted)

    viewModel.onPermissionsResult(activity)
    testDispatcher.scheduler.advanceUntilIdle()

    verify(permissionManager, never())
        .markPermissionsAsAsked(viewModel.inactivityDetectionPermissions)
    verify(userPreferencesRepository).setInactivityDetection(true)
    assertNull(viewModel.uiState.value.pendingPermissionAction)
  }

  @Test
  fun onPermissionsResult_Sms_whenPermissionIsGranted_enablesToggle() = runTest {
    createViewModel()
    viewModel.onPermissionsRequestStart(PendingAction.TOGGLE_AUTOMATIC_SMS)
    permissionManager.setPermissionResult(PermissionResult.Granted)

    viewModel.onPermissionsResult(activity)
    testDispatcher.scheduler.advanceUntilIdle()

    verify(permissionManager).markPermissionsAsAsked(viewModel.smsPermissions)
    verify(userPreferencesRepository).setAutomaticSms(true)
    assertNull(viewModel.uiState.value.pendingPermissionAction)
  }

  @Test
  fun onPermissionsResult_with_no_pending_action_does_nothing() = runTest {
    createViewModel()
    assertNull(viewModel.uiState.value.pendingPermissionAction)

    viewModel.onPermissionsResult(activity)
    testDispatcher.scheduler.advanceUntilIdle()

    verify(userPreferencesRepository, never()).setAlertMode(true)
    verify(userPreferencesRepository, never()).setInactivityDetection(true)
    verify(userPreferencesRepository, never()).setAutomaticSms(true)
    verify(permissionManager, never()).markPermissionsAsAsked(viewModel.alertModePermissions)
    verify(permissionManager, never()).markPermissionsAsAsked(viewModel.smsPermissions)
  }
}
