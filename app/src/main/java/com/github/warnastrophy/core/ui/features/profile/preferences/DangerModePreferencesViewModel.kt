package com.github.warnastrophy.core.ui.features.profile.preferences

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.github.warnastrophy.core.permissions.AppPermissions
import com.github.warnastrophy.core.permissions.PermissionManager
import com.github.warnastrophy.core.permissions.PermissionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class PendingAction {
  TOGGLE_ALERT_MODE,
  TOGGLE_INACTIVITY_DETECTION,
  TOGGLE_AUTOMATIC_SMS
}

/**
 * UI state for the Danger Mode Preferences screen.
 *
 * @property alertModeAutomaticEnabled State of the "Alert Mode Automatic" switch.
 * @property inactivityDetectionEnabled State of the "Inactivity Detection" switch.
 * @property automaticSmsEnabled State of the "Automatic SMS" switch.
 * @property locationPermissionResult The current status of the fine location permission.
 * @property smsPermissionResult The current status of the send SMS permission.
 */
data class DangerModePreferencesUiState(
    val alertModeAutomaticEnabled: Boolean = false,
    val inactivityDetectionEnabled: Boolean = false,
    val automaticSmsEnabled: Boolean = false,
    val alertModePermissionResult: PermissionResult,
    val inactivityDetectionPermissionResult: PermissionResult,
    val smsPermissionResult: PermissionResult,
    val pendingPermissionAction: PendingAction? = null
) {
  /** A computed property that is true if a permission request is in flight. */
  val isOsRequestInFlight: Boolean
    get() = pendingPermissionAction != null
}

/**
 * ViewModel for the Danger Mode Preferences screen. It handles the business logic for toggling
 * preferences and managing required permissions (location and SMS).
 */
class DangerModePreferencesViewModel(private val permissionManager: PermissionManager) :
    ViewModel() {
  val alertModePermissions = AppPermissions.AlertModePermission

  // TODO Add set for Inactivity Detection if necessary or remove this one. For the moment it uses
  // alertModePermissions.
  val inactivityDetectionPermissions = alertModePermissions
  val smsPermissions = AppPermissions.SendEmergencySms

  private val _uiState =
      MutableStateFlow(
          DangerModePreferencesUiState(
              alertModePermissionResult =
                  permissionManager.getPermissionResult(alertModePermissions),
              inactivityDetectionPermissionResult =
                  permissionManager.getPermissionResult(inactivityDetectionPermissions),
              smsPermissionResult = permissionManager.getPermissionResult(smsPermissions)))

  val uiState = _uiState.asStateFlow()

  fun onAlertModeToggled(enabled: Boolean) {
    _uiState.update { it.copy(alertModeAutomaticEnabled = enabled) }
    // If "Alert Mode" is turned off, also turn off others
    if (!enabled) {
      _uiState.update { it.copy(inactivityDetectionEnabled = false, automaticSmsEnabled = false) }
    }
  }

  fun onInactivityDetectionToggled(enabled: Boolean) {
    _uiState.update { it.copy(inactivityDetectionEnabled = enabled) }
    // If "Inactivity Detection" is turned off, also turn off "Automatic SMS"
    if (!enabled) {
      _uiState.update { it.copy(automaticSmsEnabled = false) }
    }
  }

  fun onAutomaticSmsToggled(enabled: Boolean) {
    _uiState.update { it.copy(automaticSmsEnabled = enabled) }
  }

  fun onPermissionsRequestStart(action: PendingAction) {
    _uiState.update { it.copy(pendingPermissionAction = action) }
  }

  /**
   * Updates the permission results in the UI state after a system permission dialog has been shown.
   */
  fun onPermissionsResult(activity: Activity) {
    val newAlertModeResult = permissionManager.getPermissionResult(alertModePermissions, activity)
    val newInactivityResult =
        permissionManager.getPermissionResult(inactivityDetectionPermissions, activity)
    val newSmsResult = permissionManager.getPermissionResult(smsPermissions, activity)

    _uiState.update {
      it.copy(
          alertModePermissionResult = newAlertModeResult,
          inactivityDetectionPermissionResult = newInactivityResult,
          smsPermissionResult = newSmsResult,
      )
    }

    when (_uiState.value.pendingPermissionAction) {
      PendingAction.TOGGLE_ALERT_MODE -> {
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
        if (newSmsResult is PermissionResult.Granted) {
          onAutomaticSmsToggled(true)
        }
      }
      null -> {
        // No action was pending
      }
    }

    _uiState.update { it.copy(pendingPermissionAction = null) }
  }
}
