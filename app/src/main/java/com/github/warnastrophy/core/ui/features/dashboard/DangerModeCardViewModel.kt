package com.github.warnastrophy.core.ui.features.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.warnastrophy.core.data.interfaces.ActivityRepository
import com.github.warnastrophy.core.data.interfaces.UserPreferencesRepository
import com.github.warnastrophy.core.data.provider.UserPreferencesRepositoryProvider
import com.github.warnastrophy.core.data.repository.UserPreferences
import com.github.warnastrophy.core.data.service.DangerLevel
import com.github.warnastrophy.core.data.service.DangerModeService
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
import kotlinx.coroutines.flow.first
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

/**
 * Represents one-time, non-repeatable side effects that must be executed by the UI layer. They are
 * typically used for:
 * 1. Navigation.
 * 2. Displaying Toast messages or Dialogs.
 * 3. Starting/Stopping services.
 * 4. Requesting system permissions.
 */
sealed interface Effect {
  object RequestLocationPermission : Effect

  object StartForegroundService : Effect

  object StopForegroundService : Effect

  object ShowOpenAppSettings : Effect

  data class RequestCapabilityPermission(val permissions: AppPermissions) : Effect
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
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val userPreferencesRepository: UserPreferencesRepository =
        UserPreferencesRepositoryProvider.repository,
    private val dangerModeService: DangerModeService = StateManagerService.dangerModeService
) : ViewModel() {
  val alertModePermission = AppPermissions.AlertModePermission

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

  // UI-facing optimistic capabilities state. The UI should consume this for immediate
  // feedback when toggling capabilities. It is kept in sync with the service-backed
  // `capabilities` and will be reverted if the service rejects the change.
  private val _capabilitiesInternal = MutableStateFlow<Set<DangerModeCapability>>(emptySet())
  val capabilitiesInternal: StateFlow<Set<DangerModeCapability>> =
      _capabilitiesInternal.asStateFlow()

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

  init {
    refreshActivities()
    viewModelScope.launch(dispatcher) {
      dangerModeService.events.collect { event ->
        when (event) {
          DangerModeService.DangerModeEvent.MissingSmsPermission ->
              emitEffect(Effect.RequestCapabilityPermission(AppPermissions.SendEmergencySms))
          DangerModeService.DangerModeEvent.MissingCallPermission ->
              emitEffect(Effect.RequestCapabilityPermission(AppPermissions.MakeEmergencyCall))
          null -> Unit
        }
      }
    }
    // Collect user preferences from the repository. Run collection on the provided dispatcher
    // but update UI state on the Main thread. Guard the collector against unexpected exceptions
    // so a single bad read won't crash the ViewModel.
    viewModelScope.launch(dispatcher) {
      try {
        userPreferencesRepository.getUserPreferences.collect { prefs ->
          try {
            // Ensure updates happen on main thread
            viewModelScope.launch(Dispatchers.Main.immediate) { applyPreferencesInternal(prefs) }
          } catch (e: Throwable) {
            Log.e("DangerModeCardViewModel", "Failed to schedule applyPreferences", e)
          }
        }
      } catch (e: Throwable) {
        Log.e("DangerModeCardViewModel", "Error collecting user preferences", e)
      }
    }
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
  fun onActivitySelected(activity: Activity?) {
    dangerModeService.setActivity(activity)
  }

  /**
   * Updates the set of enabled capabilities for Danger Mode.
   *
   * @param newCapabilities The new set of enabled capabilities.
   */
  fun onCapabilitiesChanged(newCapabilities: Set<DangerModeCapability>) {
    // Optimistically update the UI-facing capabilities so the UI reacts immediately.
    val previous = _capabilitiesInternal.value
    _capabilitiesInternal.value = newCapabilities

    val result =
        try {
          dangerModeService.setCapabilities(newCapabilities)
        } catch (t: Throwable) {
          Result.failure<Unit>(t)
        }

    if (result.isFailure) {
      // revert optimistic update and log
      _capabilitiesInternal.value = previous
      Log.e("DangerModeCardViewModel", "Failed to set capabilities: $newCapabilities")
    }
  }

  /**
   * Toggles a specific capability for Danger Mode.
   *
   * @param capability The capability to be toggled.
   */
  fun onCapabilityToggled(capability: DangerModeCapability) {
    // Use the UI-facing internal capabilities as the source of truth for immediate toggling
    val current = _capabilitiesInternal.value
    val enabling = !current.contains(capability)

    val newCaps: Set<DangerModeCapability> = if (enabling) setOf(capability) else emptySet()

    // Do not implicitly toggle auto-actions when changing capability; keep user control.

    onCapabilitiesChanged(newCaps)

    viewModelScope.launch(dispatcher) {
      when (capability) {
        DangerModeCapability.CALL -> {
          userPreferencesRepository.setAutomaticCalls(enabling)
          if (enabling) userPreferencesRepository.setAutomaticSms(false)
        }
        DangerModeCapability.SMS -> {
          userPreferencesRepository.setAutomaticSms(enabling)
          if (enabling) userPreferencesRepository.setAutomaticCalls(false)
        }
      }
    }
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
    if (enabled) _confirmVoiceRequired.value = false
    viewModelScope.launch(dispatcher) {
      userPreferencesRepository.setTouchConfirmationRequired(enabled)
      if (enabled) userPreferencesRepository.setVoiceConfirmationEnabled(false)
    }
  }

  fun onAutoActionsEnabled(enabled: Boolean) {
    _autoActionsEnabled.value = enabled
    viewModelScope.launch(dispatcher) { userPreferencesRepository.setAutoActionsEnabled(enabled) }
  }

  fun onConfirmVoiceChanged(enabled: Boolean) {
    _confirmVoiceRequired.value = enabled
    if (enabled) _confirmTouchRequired.value = false
    viewModelScope.launch(dispatcher) {
      userPreferencesRepository.setVoiceConfirmationEnabled(enabled)
      if (enabled) userPreferencesRepository.setTouchConfirmationRequired(false)
    }
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

  /** Records that a permission request has been initiated by update UIState. */
  fun onPermissionsRequestStart() {
    _alertModeUiState.update { it.copy(waitingForUserResponse = true) }
  }

  /**
   * Updates the permission results in the UI state after the user has responded to a system
   * permission dialog.
   *
   * @param activity The current `Activity`, required to check the latest permission statuses.
   */
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

  // Internal suspend function that performs the actual state updates. Must be called on the
  // Main dispatcher to safely update StateFlow-backed UI state.
  private fun applyPreferencesInternal(prefs: UserPreferences) {
    runCatching {
          _autoActionsEnabled.value = prefs.dangerModePreferences.autoActionsEnabled
          _confirmTouchRequired.value = prefs.dangerModePreferences.touchConfirmationRequired
          _confirmVoiceRequired.value = prefs.dangerModePreferences.voiceConfirmationEnabled

          val caps = mutableSetOf<DangerModeCapability>()
          if (prefs.dangerModePreferences.automaticCalls) caps.add(DangerModeCapability.CALL)
          if (prefs.dangerModePreferences.automaticSms) caps.add(DangerModeCapability.SMS)

          try {
            // Apply persisted capabilities to the UI-facing state directly without calling
            // `onCapabilitiesChanged` to avoid triggering a service call which can overwrite
            // optimistic UI updates during tests or startup.
            _capabilitiesInternal.value = caps
          } catch (e: Throwable) {
            Log.e("DangerModeCardViewModel", "Failed to apply capabilities from prefs", e)
          }
        }
        .onFailure { e -> Log.e("DangerModeCardViewModel", "Failed to apply user preferences", e) }
  }

  // Initial sync: read the current service state once and seed the UI-facing capabilities.
  // We intentionally do not continuously overwrite the UI-facing optimistic state to avoid
  // reverting user actions during transient service updates.
  init {
    viewModelScope.launch(dispatcher) {
      try {
        val initial = dangerModeService.state.first()
        _capabilitiesInternal.value = initial.capabilities
      } catch (t: Throwable) {
        Log.w("DangerModeCardViewModel", "Failed to initial-sync capabilities: ${t.message}")
      }
    }
  }
}
