package com.github.warnastrophy.core.ui.features.profile.preferences

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.github.warnastrophy.core.permissions.AppPermissions
import com.github.warnastrophy.core.permissions.PermissionManagerInterface
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
 * ViewModel for the Danger Mode Preferences screen.
 *
 * This ViewModel manages the UI state and business logic for the Danger Mode settings.
 *
 * @param permissionManager An interface for checking and managing app permissions.
 */
class DangerModePreferencesViewModel(private val permissionManager: PermissionManagerInterface) :
    ViewModel() {
  val alertModePermissions = AppPermissions.AlertModePermission

  // !!! Add set for Inactivity Detection if necessary or remove this one. For the moment it uses
  // alertModePermissions. !!!
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

  /**
   * Handles the toggling of the "Alert Mode Automatic" feature. If turned off, it also disables
   * related features like "Inactivity Detection" and "Automatic SMS".
   *
   * @param enabled The new state of the "Alert Mode Automatic" switch.
   */
  fun onAlertModeToggled(enabled: Boolean) {
    _uiState.update { it.copy(alertModeAutomaticEnabled = enabled) }
    // If "Alert Mode" is turned off, also turn off others
    if (!enabled) {
      _uiState.update { it.copy(inactivityDetectionEnabled = false, automaticSmsEnabled = false) }
    }
  }

  /**
   * Handles the toggling of the "Inactivity Detection" feature. If turned off, it also disables
   * "Automatic SMS".
   *
   * @param enabled The new state of the "Inactivity Detection" switch.
   */
  fun onInactivityDetectionToggled(enabled: Boolean) {
    _uiState.update { it.copy(inactivityDetectionEnabled = enabled) }
    // If "Inactivity Detection" is turned off, also turn off "Automatic SMS"
    if (!enabled) {
      _uiState.update { it.copy(automaticSmsEnabled = false) }
    }
  }

  /**
   * Handles the toggling of the "Automatic SMS" feature.
   *
   * @param enabled The new state of the "Automatic SMS" switch.
   */
  fun onAutomaticSmsToggled(enabled: Boolean) {
    _uiState.update { it.copy(automaticSmsEnabled = enabled) }
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
