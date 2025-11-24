package com.github.warnastrophy.core.ui.profile.preferences

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.github.warnastrophy.core.data.service.MockPermissionManager
import com.github.warnastrophy.core.permissions.AppPermissions
import com.github.warnastrophy.core.permissions.PermissionResult
import com.github.warnastrophy.core.ui.features.profile.preferences.DangerModePreferencesViewModel
import com.github.warnastrophy.core.ui.features.profile.preferences.PendingAction
import com.github.warnastrophy.core.util.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.bouncycastle.crypto.params.Blake3Parameters.context
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
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
  private lateinit var viewModel: DangerModePreferencesViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    activity = Robolectric.buildActivity(Activity::class.java).get()
    context = activity.applicationContext
    prefs = context.getSharedPreferences(AppConfig.PREF_FILE_NAME, Context.MODE_PRIVATE)
    permissionManager = spy(MockPermissionManager())
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
    viewModel = DangerModePreferencesViewModel(permissionManager)
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
  fun onPermissionsResult_updatesStateAndEnablesFeature_whenPermissionGranted() = runTest {
    permissionManager.setPermissionResult(PermissionResult.Denied(listOf("FAKE_PERMISSION")))
    createViewModel()
    viewModel.onPermissionsRequestStart(PendingAction.TOGGLE_ALERT_MODE)

    assertEquals(PendingAction.TOGGLE_ALERT_MODE, viewModel.uiState.value.pendingPermissionAction)

    // Simulate the user granting the permission
    val grantedResult = PermissionResult.Granted
    permissionManager.setPermissionResult(grantedResult)

    viewModel.onPermissionsResult(mock())

    val finalState = viewModel.uiState.value

    assertEquals(grantedResult, finalState.alertModePermissionResult)
    assertTrue(finalState.alertModeAutomaticEnabled)
    assertNull(finalState.pendingPermissionAction)
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
    permissionManager.setPermissionResult(PermissionResult.Granted)
    createViewModel()

    viewModel.onAlertModeToggled(true)
    viewModel.onInactivityDetectionToggled(true)
    viewModel.onAutomaticSmsToggled(true)

    viewModel.onAlertModeToggled(false)

    val state = viewModel.uiState.value
    assertFalse(state.alertModeAutomaticEnabled)
    assertFalse(state.inactivityDetectionEnabled)
    assertFalse(state.automaticSmsEnabled)
  }

  @Test
  fun onPermissionsResult_marksAllPermissionsAsAsked() = runTest {
    permissionManager.setPermissionResult(
        PermissionResult.Denied(AppPermissions.AlertModePermission.permissions.toList()))
    createViewModel()

    viewModel.onPermissionsResult(activity = activity)

    testDispatcher.scheduler.advanceUntilIdle()

    verify(permissionManager).markPermissionsAsAsked(viewModel.alertModePermissions)
    verify(permissionManager).markPermissionsAsAsked(viewModel.smsPermissions)
  }
}
