package com.github.warnastrophy.core.ui.features.dashboard

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.warnastrophy.core.data.interfaces.ActivityRepository
import com.github.warnastrophy.core.data.service.DangerLevel
import com.github.warnastrophy.core.data.service.StateManagerService
import com.github.warnastrophy.core.data.service.StateManagerService.permissionManager
import com.github.warnastrophy.core.model.Activity as DangerActivity
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

/**
 * Represents the entire state of the Alert Mode feature in the UI at any given time.
 *
 * @property alertModeManualEnabled True if the user has manually toggled the feature ON/OFF. This
 *   controls the primary switch state on the screen.
 * @property alertModePermissionResult The current permission status related to running the Alert
 *   Mode feature.
 * @property waitingForUserResponse True if the ViewModel is waiting for an asynchronous response
 *   from the system or the user.
 */
data class AlertModeUiState(
    val alertModeManualEnabled: Boolean = false,
    val alertModePermissionResult: PermissionResult,
    val waitingForUserResponse: Boolean = false
)

data class DangerModePreferencesUiState(
    val autoActionsEnabled: Boolean = false,
    val confirmTouchRequired: Boolean = false,
    val confirmVoiceRequired: Boolean = false
)

/** Represents one-time, non-repeatable side effects that must be executed by the UI layer. */
sealed interface Effect {
  /** Request a specific set of permissions. */
  data class RequestPermissions(val permissionType: AppPermissions) : Effect

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

  private val _activities = MutableStateFlow<List<DangerActivity>>(emptyList())
  val activities: StateFlow<List<DangerActivity>> = _activities.asStateFlow()
  private val _effects = MutableSharedFlow<Effect>()
  val effects = _effects.asSharedFlow()

  private val _alertModeUiState =
      MutableStateFlow(
          AlertModeUiState(
              alertModePermissionResult =
                  permissionManager.getPermissionResult(alertModePermission)))
  val permissionUiState = _alertModeUiState.asStateFlow()

  // Sequence of permissions required to enable danger/alert mode.
  private val permissionSequence =
      listOf(
          AppPermissions.AlertModePermission, // location
          AppPermissions.MicrophonePermission, // microphone
          AppPermissions.SendEmergencySms, // sms
          AppPermissions.MakeEmergencyCall // call
          )

  // Tracks whether enabling was requested so we can continue chain after each grant.
  private var enableRequested = false

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

  private val _confirmTouchRequired = MutableStateFlow(false)
  val confirmTouchRequired: StateFlow<Boolean> = _confirmTouchRequired.asStateFlow()

  private val _confirmVoiceRequired = MutableStateFlow(false)
  val confirmVoiceRequired: StateFlow<Boolean> = _confirmVoiceRequired.asStateFlow()

  private val _dangerModePreferencesUiState = MutableStateFlow(DangerModePreferencesUiState())
  val dangerModePreferencesUiState = _dangerModePreferencesUiState.asStateFlow()

  /**
   * Handles the toggling of Danger Mode on or off and starts or stops the foreground GPS service
   * accordingly.
   *
   * @param enabled True to enable Danger Mode, false to disable it.
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
  fun onActivitySelected(activity: DangerActivity?) {
    dangerModeService.setActivity(activity)
  }

  /**
   * Updates the set of enabled capabilities for Danger Mode.
   *
   * @param newCapabilities The new set of enabled capabilities.
   */
  fun onCapabilitiesChanged(newCapabilities: Set<DangerModeCapability>) {
    if (dangerModeService.setCapabilities(newCapabilities).isFailure) {
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
    _dangerModePreferencesUiState.update { it.copy(confirmTouchRequired = enabled) }
  }

  fun onAutoActionsEnabled(enabled: Boolean) {
    _autoActionsEnabled.value = enabled
    _dangerModePreferencesUiState.update { it.copy(autoActionsEnabled = enabled) }
  }

  fun onConfirmVoiceChanged(enabled: Boolean) {
    _confirmVoiceRequired.value = enabled
    _dangerModePreferencesUiState.update { it.copy(confirmVoiceRequired = enabled) }
  }

  /**
   * Emits a one-time side effect to the UI.
   *
   * This function launches a coroutine on the specified [dispatcher] within the ViewModel's
   * lifecycle scope.
   *
   * The effect is emitted via the [_effects] SharedFlow, allowing the UI layer to collect it once
   * and then discard the event, preventing replays on configuration changes.
   *
   * @param effect The specific [Effect] to be emitted to the collector (typically the UI/Fragment).
   */
  @Suppress("RemoveRedundantDispatcherCall")
  private fun emitEffect(effect: Effect) {
    viewModelScope.launch(dispatcher) { _effects.emit(effect) }
  }

  /** Records that a permission request has been initiated. */
  fun onPermissionsRequestStart(permissionType: AppPermissions? = null) {
    _alertModeUiState.update { it.copy(waitingForUserResponse = true) }
  }

  /**
   * Backward-compatible single-parameter permission result handler. Delegates to the full handler
   * using the AlertModePermission.
   */
  fun onPermissionResult(activity: Activity) {
    onPermissionResult(activity, alertModePermission)
  }

  /**
   * Permission result handler which receives the Activity and the specific permission set.
   * Continues the permission chain if allowed, or cancels enabling if denied.
   */
  fun onPermissionResult(activity: Activity, permissionType: AppPermissions) {
    val newResult = permissionManager.getPermissionResult(permissionType, activity)

    // Update UI state if this was the alert mode permission
    if (permissionType == alertModePermission) {
      _alertModeUiState.update { it.copy(alertModePermissionResult = newResult) }
    }

    if (_alertModeUiState.value.waitingForUserResponse) {
      permissionManager.markPermissionsAsAsked(permissionType)
    }

    _alertModeUiState.update { it.copy(waitingForUserResponse = false) }

    // If granted and we were enabling, continue to next required permission
    if (newResult is PermissionResult.Granted && enableRequested) {
      requestNextRequiredPermission(activity)
    } else if (newResult !is PermissionResult.Granted) {
      // If any permission denied, cancel the enable flow
      enableRequested = false
    }
  }

  /** Starts the toggle flow. The Activity is required to query current permission statuses. */
  fun handleToggle(isChecked: Boolean, activity: Activity) {
    if (!isChecked) {
      onDangerModeToggled(false)
      return
    }
    // Start the enable flow
    enableRequested = true
    requestNextRequiredPermission(activity)
  }

  /**
   * Scan the permissionSequence and either enable the mode (if none missing) or emit an effect to
   * request the next missing permission (or open app settings if permanently denied).
   */
  private fun requestNextRequiredPermission(activity: Activity) {
    // Find the first permission that is not granted
    val missing =
        permissionSequence.firstNotNullOfOrNull { permissionType ->
          val result = permissionManager.getPermissionResult(permissionType, activity)
          if (result is PermissionResult.Granted) null else permissionType to result
        }

    if (missing == null) {
      // All required permissions are granted -> enable
      enableRequested = false
      onDangerModeToggled(true)
      return
    }

    val (permissionType, result) = missing
    when (result) {
      is PermissionResult.PermanentlyDenied -> {
        enableRequested = false
        emitEffect(Effect.ShowOpenAppSettings)
      }
      else -> {
        emitEffect(Effect.RequestPermissions(permissionType))
      }
    }
  }
}
