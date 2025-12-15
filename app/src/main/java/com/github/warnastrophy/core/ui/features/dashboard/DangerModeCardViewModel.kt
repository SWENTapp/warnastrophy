package com.github.warnastrophy.core.ui.features.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.warnastrophy.core.data.repository.ActivityRepository
import com.github.warnastrophy.core.data.service.DangerLevel
import com.github.warnastrophy.core.data.service.StateManagerService
import com.github.warnastrophy.core.data.service.StateManagerService.permissionManager
import com.github.warnastrophy.core.model.Activity
import com.github.warnastrophy.core.permissions.AppPermissions
import com.github.warnastrophy.core.permissions.PermissionResult
import com.github.warnastrophy.core.util.AppConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Capabilities of the danger mode that can be enabled in Danger Mode with associated labels. */
enum class DangerModeCapability(val label: String) {
  CALL("Call"),
  SMS("SMS"),
}

data class AlertModeUiState(
    val alertModeManualEnabled: Boolean = false,
    val alertModePermissionResult: PermissionResult,
    val waitingForUserResponse: Boolean = false
)

sealed interface Effect {
  object RequestLocationPermission : Effect

  object StartForegroundService : Effect

  object StopForegroundService : Effect

  object ShowOpenAppSettings : Effect
}

/**
 * ViewModel for managing the state of the Danger Mode card in the dashboard UI.
 *
 * @param repository The ActivityRepository to use for fetching activities.
 * @param userId The user ID for loading activities. Defaults to current Firebase user or fallback.
 * @param dispatcher The coroutine dispatcher to use for async operations.
 */
class DangerModeCardViewModel(
    private val repository: ActivityRepository = StateManagerService.activityRepository,
    private val userId: String =
        FirebaseAuth.getInstance().currentUser?.uid ?: AppConfig.defaultUserId,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {
  val alertModePermission = AppPermissions.AlertModePermission
  private val dangerModeService = StateManagerService.dangerModeService

  private val _activities = MutableStateFlow<List<Activity>>(emptyList())
  val activities: StateFlow<List<Activity>> = _activities.asStateFlow()
  private val _effects = MutableSharedFlow<Effect>()
  val effects = _effects.asSharedFlow()

  private val _alertModeUiState =
      MutableStateFlow(
          AlertModeUiState(
              alertModePermissionResult =
                  permissionManager.getPermissionResult(alertModePermission)))
  val permissionUiState = _alertModeUiState.asStateFlow()

  init {
    refreshActivities()
  }

  /** Refreshes the activities list from the repository. */
  fun refreshActivities() {
    viewModelScope.launch(dispatcher) {
      val result = repository.getAllActivities(userId)
      result.fold(
          onSuccess = { activityList -> _activities.value = activityList },
          onFailure = { e ->
            Log.e("DangerModeCardViewModel", "Error fetching activities", e)
            _activities.value = emptyList()
          })
    }
  }

  val isDangerModeEnabled =
      dangerModeService.state
          .map { it.isActive }
          .stateIn(viewModelScope, SharingStarted.Lazily, false)

  val currentActivity =
      dangerModeService.state
          .map { it.activity }
          .stateIn(viewModelScope, SharingStarted.Lazily, null)

  val dangerLevel =
      dangerModeService.state
          .map { it.dangerLevel }
          .stateIn(viewModelScope, SharingStarted.Lazily, DangerLevel.LOW)

  val capabilities =
      dangerModeService.state
          .map { it.capabilities }
          .stateIn(viewModelScope, SharingStarted.Lazily, emptySet())

  private val _autoActionsEnabled = MutableStateFlow(false)
  val autoActionsEnabled: StateFlow<Boolean> = _autoActionsEnabled.asStateFlow()
  private val _confirmTouchRequired =
      MutableStateFlow(
          false) // This is tactile confirmation before the app takes actions like calling/emergency
  // SMS
  val confirmTouchRequired: StateFlow<Boolean> = _confirmTouchRequired.asStateFlow()

  private val _confirmVoiceRequired =
      MutableStateFlow(
          false) // This is audio confirmation before the app takes actions like calling/emergency
  // SMS
  val confirmVoiceRequired: StateFlow<Boolean> = _confirmVoiceRequired.asStateFlow()

  /**
   * Handles the toggling of Danger Mode on or off and starts or stops the foreground GPS service
   * accordingly.
   *
   * @param enabled True to enable Danger Mode, false to disable it.
   * @param context The context used to start or stop the GPS service.
   */
  fun onDangerModeToggled(enabled: Boolean) {
    if (enabled) {
      dangerModeService.manualActivate()
      emitEffect(Effect.StartForegroundService)
    } else {
      dangerModeService.manualDeactivate()
      emitEffect(Effect.StopForegroundService)
    }
  }

  /**
   * Sets the selected Activity
   *
   * @param activity The selected Activity.
   */
  fun onActivitySelected(activity: Activity?) {
    dangerModeService.setActivity(activity)
  }

  /**
   * Updates the set of enabled capabilities for Danger Mode.
   *
   * @param newCapabilities The new set of enabled capabilities.
   */
  fun onCapabilitiesChanged(newCapabilities: Set<DangerModeCapability>) {
    if (dangerModeService.setCapabilities(newCapabilities).isFailure) {
      // TODO
      Log.e("DangerModeCardViewModel", "Failed to set capabilities: $newCapabilities")
    }
  }

  /**
   * Toggles a specific capability for Danger Mode.
   *
   * @param capability The capability to be toggled.
   */
  fun onCapabilityToggled(capability: DangerModeCapability) {
    val current = dangerModeService.state.value.capabilities
    val future =
        if (current.contains(capability)) {
          current - capability
        } else {
          current + capability
        }

    onCapabilitiesChanged(future)
  }

  /**
   * Sets the danger level, ensuring it stays within the valid range of 0 to 3.
   *
   * @param level The new danger level to be set.
   */
  fun onDangerLevelChanged(level: DangerLevel) {
    dangerModeService.setDangerLevel(level)
  }

  fun onConfirmTouchChanged(enabled: Boolean) {
    _confirmTouchRequired.value = enabled
    // TODO: Persist & enforce tactile confirmation before actions.
  }

  fun onAutoActionsEnabled(enabled: Boolean) {
    _autoActionsEnabled.value = enabled
    // TODO: Persist & enforce tactile confirmation before actions.
  }

  fun onConfirmVoiceChanged(enabled: Boolean) {
    _confirmVoiceRequired.value = enabled
    // TODO: Persist & enforce voice confirmation before actions.
  }

  private fun emitEffect(effect: Effect) {
    viewModelScope.launch(dispatcher) { _effects.emit(effect) }
  }

  fun onPermissionsRequestStart() {
    _alertModeUiState.update { it.copy(waitingForUserResponse = true) }
  }

  fun onPermissionResult(activity: android.app.Activity) {
    val newAlertModeResult = permissionManager.getPermissionResult(alertModePermission, activity)
    _alertModeUiState.update { it.copy(alertModePermissionResult = newAlertModeResult) }
    if (_alertModeUiState.value.waitingForUserResponse) {
      permissionManager.markPermissionsAsAsked(alertModePermission)
      if (newAlertModeResult is PermissionResult.Granted) {
        onDangerModeToggled(true)
      }
    }
    _alertModeUiState.update { it.copy(waitingForUserResponse = false) }
  }

  /**
   * Handles the logic when user toggle the button, checking permissions and dispatching actions.
   *
   * @param isChecked The new state of the preference toggle.
   * @param permissionResult The current permission status for this feature.
   */
  fun handleToggle(isChecked: Boolean, permissionResult: PermissionResult) {
    if (isChecked) {
      when (permissionResult) {
        PermissionResult.Granted -> {
          onDangerModeToggled(true)
        }
        is PermissionResult.Denied -> emitEffect(Effect.RequestLocationPermission)
        is PermissionResult.PermanentlyDenied -> emitEffect(Effect.ShowOpenAppSettings)
      }
    } else {
      onDangerModeToggled(false)
    }
  }
}
