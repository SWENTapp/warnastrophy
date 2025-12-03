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

/** Represents actions that are pending a permission result from the user. */
enum class PendingAction {
  TOGGLE_ALERT_MODE,
  TOGGLE_INACTIVITY_DETECTION,
  TOGGLE_AUTOMATIC_SMS,
  TOGGLE_AUTOMATIC_CALLS
}

/** UI state for the Danger Mode Preferences screen. */
data class DangerModePreferencesUiState(
    val alertModeAutomaticEnabled: Boolean = false,
    val inactivityDetectionEnabled: Boolean = false,
    val automaticSmsEnabled: Boolean = false,
    val automaticCallsEnabled: Boolean = false,
    val alertModePermissionResult: PermissionResult,
    val inactivityDetectionPermissionResult: PermissionResult,
    val smsPermissionResult: PermissionResult,
    val callPermissionResult: PermissionResult,
    val pendingPermissionAction: PendingAction? = null
) {
  val isOsRequestInFlight: Boolean
    get() = pendingPermissionAction != null
}

class DangerModePreferencesViewModel(
    private val permissionManager: PermissionManagerInterface,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
  val alertModePermissions = AppPermissions.AlertModePermission
  val inactivityDetectionPermissions = alertModePermissions
  val smsPermissions = AppPermissions.SendEmergencySms
  val callPermissions = AppPermissions.MakeEmergencyCall

  private val _uiState =
      MutableStateFlow(
          DangerModePreferencesUiState(
              alertModePermissionResult =
                  permissionManager.getPermissionResult(alertModePermissions),
              inactivityDetectionPermissionResult =
                  permissionManager.getPermissionResult(inactivityDetectionPermissions),
              smsPermissionResult = permissionManager.getPermissionResult(smsPermissions),
              callPermissionResult = permissionManager.getPermissionResult(callPermissions)))

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

  fun onInactivityDetectionToggled(enabled: Boolean) {
    viewModelScope.launch {
      userPreferencesRepository.setInactivityDetection(enabled)
      if (!enabled) {
        userPreferencesRepository.setAutomaticSms(false)
        userPreferencesRepository.setAutomaticCalls(false)
      }
    }
  }

  fun onAutomaticSmsToggled(enabled: Boolean) {
    viewModelScope.launch { userPreferencesRepository.setAutomaticSms(enabled) }
  }

  fun onAutomaticCallsToggled(enabled: Boolean) {
    viewModelScope.launch { userPreferencesRepository.setAutomaticCalls(enabled) }
  }

  fun onPermissionsRequestStart(action: PendingAction) {
    _uiState.update { it.copy(pendingPermissionAction = action) }
  }

  fun onPermissionsResult(activity: Activity) {
    val newAlertModeResult = permissionManager.getPermissionResult(alertModePermissions, activity)
    val newInactivityResult =
        permissionManager.getPermissionResult(inactivityDetectionPermissions, activity)
    val newSmsResult = permissionManager.getPermissionResult(smsPermissions, activity)
    val newCallResult = permissionManager.getPermissionResult(callPermissions, activity)

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
      null -> {}
    }

    _uiState.update { it.copy(pendingPermissionAction = null) }
  }

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
