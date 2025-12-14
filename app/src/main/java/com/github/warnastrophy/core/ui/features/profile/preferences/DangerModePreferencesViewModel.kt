package com.github.warnastrophy.core.ui.features.profile.preferences

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.warnastrophy.core.data.repository.UserPreferencesRepository
import com.github.warnastrophy.core.permissions.AppPermissions
import com.github.warnastrophy.core.permissions.PermissionManagerInterface
import com.github.warnastrophy.core.permissions.PermissionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Represents actions that are pending a permission result from the user.
 *
 * When a user tries to enable a feature that requires a permission, and that permission has not yet
 * been granted, the app will request the permission. This enum is used to track which feature's
 * toggle action initiated the permission request, so the correct action can be completed if the
 * user grants the permission.
 */
enum class PendingAction {
  TOGGLE_ALERT_MODE,
  TOGGLE_INACTIVITY_DETECTION,
  TOGGLE_AUTOMATIC_SMS,
  TOGGLE_AUTOMATIC_CALLS,
  TOGGLE_MICROPHONE
}

/**
 * UI state for the Danger Mode Preferences screen.
 *
 * @property alertModeAutomaticEnabled State of the "Alert Mode Automatic" switch.
 * @property inactivityDetectionEnabled State of the "Inactivity Detection" switch.
 * @property automaticSmsEnabled State of the "Automatic SMS" switch.
 * @property alertModePermissionResult The current status of the permissions required for Alert
 *   Mode.
 * @property inactivityDetectionPermissionResult The current status of the permissions required for
 *   Inactivity Detection.
 * @property smsPermissionResult The current status of the permission required for sending SMS.
 * @property pendingPermissionAction The action that is waiting for a permission result.
 */
data class DangerModePreferencesUiState(
    val alertModeAutomaticEnabled: Boolean = false,
    val inactivityDetectionEnabled: Boolean = false,
    val automaticSmsEnabled: Boolean = false,
    val automaticCallsEnabled: Boolean = false,
    val microphoneAccessEnabled: Boolean = false,
    val alertModePermissionResult: PermissionResult,
    val inactivityDetectionPermissionResult: PermissionResult,
    val smsPermissionResult: PermissionResult,
    val callPermissionResult: PermissionResult,
    val pendingPermissionAction: PendingAction? = null,
    val microphonePermissionResult: PermissionResult
) {
  /** A computed property that is true if a permission request is in flight. */
  val isOsRequestInFlight: Boolean
    get() = pendingPermissionAction != null
}

/**
 * ViewModel for the Danger Mode Preferences screen.
 *
 * This ViewModel manages the UI state and business logic for the Danger Mode settings.
 *
 * @param permissionManager An interface for checking and managing app permissions.
 */
class DangerModePreferencesViewModel(
    private val permissionManager: PermissionManagerInterface,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
  val alertModePermissions = AppPermissions.AlertModePermission
  val inactivityDetectionPermissions = alertModePermissions
  val smsPermissions = AppPermissions.SendEmergencySms
  val callPermissions = AppPermissions.MakeEmergencyCall

  val microphonePermissions = AppPermissions.Microphone

  private val _uiState =
      MutableStateFlow(
          DangerModePreferencesUiState(
              alertModePermissionResult =
                  permissionManager.getPermissionResult(alertModePermissions),
              inactivityDetectionPermissionResult =
                  permissionManager.getPermissionResult(inactivityDetectionPermissions),
              smsPermissionResult = permissionManager.getPermissionResult(smsPermissions),
              callPermissionResult = permissionManager.getPermissionResult(callPermissions),
              microphonePermissionResult =
                  permissionManager.getPermissionResult(microphonePermissions)))

  val uiState = _uiState.asStateFlow()

  init {
    userPreferencesRepository.getUserPreferences
        .onEach { prefs ->
          _uiState.update {
            it.copy(
                alertModeAutomaticEnabled = prefs.dangerModePreferences.alertMode,
                inactivityDetectionEnabled = prefs.dangerModePreferences.inactivityDetection,
                automaticSmsEnabled = prefs.dangerModePreferences.automaticSms,
                automaticCallsEnabled = prefs.dangerModePreferences.automaticCalls)
          }
        }
        .launchIn(viewModelScope)
  }

  /**
   * Handles the toggling of the "Alert Mode Automatic" feature. If turned off, it also disables
   * related features like "Inactivity Detection" and "Automatic SMS".
   *
   * @param enabled The new state of the "Alert Mode Automatic" switch.
   */
  fun onAlertModeToggled(enabled: Boolean) {
    viewModelScope.launch {
      userPreferencesRepository.setAlertMode(enabled)
      if (!enabled) {
        userPreferencesRepository.setInactivityDetection(false)
        userPreferencesRepository.setAutomaticSms(false)
        userPreferencesRepository.setAutomaticCalls(false)
      }
    }
  }

  /**
   * Handles the toggling of the "Inactivity Detection" feature. If turned off, it also disables
   * "Automatic SMS".
   *
   * @param enabled The new state of the "Inactivity Detection" switch.
   */
  fun onInactivityDetectionToggled(enabled: Boolean) {
    viewModelScope.launch {
      userPreferencesRepository.setInactivityDetection(enabled)
      if (!enabled) {
        userPreferencesRepository.setAutomaticSms(false)
        userPreferencesRepository.setAutomaticCalls(false)
      }
    }
  }

  /**
   * Handles the toggling of the "Automatic SMS" feature.
   *
   * @param enabled The new state of the "Automatic SMS" switch.
   */
  fun onAutomaticSmsToggled(enabled: Boolean) {
    viewModelScope.launch { userPreferencesRepository.setAutomaticSms(enabled) }
  }

  /**
   * Handles the toggling of the "Automatic Calls" feature.
   *
   * @param enabled The new state of the "Automatic Calls" switch.
   */
  fun onAutomaticCallsToggled(enabled: Boolean) {
    viewModelScope.launch { userPreferencesRepository.setAutomaticCalls(enabled) }
  }

  fun onMicrophoneToggled(enabled: Boolean) {
    TODO("Not yet implemented")
  }

  /**
   * Records that a permission request has been initiated for a specific action.
   *
   * @param action The action that is waiting for a permission result.
   */
  fun onPermissionsRequestStart(action: PendingAction) {
    _uiState.update { it.copy(pendingPermissionAction = action) }
  }

  /**
   * Updates the permission results in the UI state after the user has responded to a system
   * permission dialog.
   *
   * @param activity The current `Activity`, required to check the latest permission statuses.
   */
  fun onPermissionsResult(activity: Activity) {
    val newAlertModeResult = permissionManager.getPermissionResult(alertModePermissions, activity)
    val newInactivityResult =
        permissionManager.getPermissionResult(inactivityDetectionPermissions, activity)
    val newSmsResult = permissionManager.getPermissionResult(smsPermissions, activity)
    val newCallResult = permissionManager.getPermissionResult(callPermissions, activity)
    val newMicrophoneResult = permissionManager.getPermissionResult(microphonePermissions, activity)

    _uiState.update {
      it.copy(
          alertModePermissionResult = newAlertModeResult,
          inactivityDetectionPermissionResult = newInactivityResult,
          smsPermissionResult = newSmsResult,
          callPermissionResult = newCallResult,
      )
    }

    when (_uiState.value.pendingPermissionAction) {
      PendingAction.TOGGLE_ALERT_MODE -> {
        permissionManager.markPermissionsAsAsked(alertModePermissions)
        if (newAlertModeResult is PermissionResult.Granted) {
          onAlertModeToggled(true)
        }
      }
      PendingAction.TOGGLE_INACTIVITY_DETECTION -> {
        if (newInactivityResult is PermissionResult.Granted) {
          onInactivityDetectionToggled(true)
        }
      }
      PendingAction.TOGGLE_AUTOMATIC_SMS -> {
        permissionManager.markPermissionsAsAsked(smsPermissions)
        if (newSmsResult is PermissionResult.Granted) {
          onAutomaticSmsToggled(true)
        }
      }
      PendingAction.TOGGLE_AUTOMATIC_CALLS -> {
        permissionManager.markPermissionsAsAsked(callPermissions)
        if (newCallResult is PermissionResult.Granted) {
          onAutomaticCallsToggled(true)
        }
      }
      PendingAction.TOGGLE_MICROPHONE -> {
        permissionManager.markPermissionsAsAsked(microphonePermissions)
        if (newMicrophoneResult is PermissionResult.Granted) {
          onMicrophoneToggled(true)
        }
      }
      null -> {}
    }

    _uiState.update { it.copy(pendingPermissionAction = null) }
  }

  /**
   * Handles the logic for a preference change, checking permissions and dispatching actions.
   *
   * @param isChecked The new state of the preference toggle.
   * @param permissionResult The current permission status for this feature.
   * @param onToggle The ViewModel function to call when the toggle state changes.
   * @param onPermissionDenied Callback invoked if the necessary permissions were denied.
   * @param onPermissionPermDenied Callback invoked if the necessary permissions were permanently
   *   denied.
   */
  fun handlePreferenceChange(
      isChecked: Boolean,
      permissionResult: PermissionResult,
      onToggle: (Boolean) -> Unit,
      onPermissionDenied: () -> Unit,
      onPermissionPermDenied: () -> Unit
  ) {
    if (isChecked) {
      when (permissionResult) {
        PermissionResult.Granted -> onToggle(true)
        is PermissionResult.Denied -> onPermissionDenied()
        is PermissionResult.PermanentlyDenied -> onPermissionPermDenied()
      }
    } else {
      onToggle(false)
    }
  }
}
