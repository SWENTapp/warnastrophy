package com.github.warnastrophy.core.ui.features.dashboard

import android.app.Activity
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

/** Represents the entire state of the Alert Mode feature in the UI at any given time. */
data class AlertModeUiState(
    val alertModeManualEnabled: Boolean = false,
    val alertModePermissionResult: PermissionResult,
    val waitingForUserResponse: Boolean = false,
    val pendingPermissionType: AppPermissions? = null,
)

/** One-time effects emitted to the UI. */
sealed interface Effect {
  /** Request a specific permission type (new API). */
  data class RequestPermissions(val permissionType: AppPermissions) : Effect

  /** Legacy effect for location permission request. */
  object StartForegroundService : Effect

  object StopForegroundService : Effect

  object ShowOpenAppSettings : Effect

  data class RequestCapabilityPermission(val permissions: AppPermissions) : Effect
}

/** ViewModel for managing the state of the Danger Mode card in the dashboard UI. */
class DangerModeCardViewModel(
    private val repository: ActivityRepository = StateManagerService.activityRepository,
    private val userId: String =
        FirebaseAuth.getInstance().currentUser?.uid ?: AppConfig.defaultUserId,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val userPreferencesRepository: UserPreferencesRepository =
        UserPreferencesRepositoryProvider.repository,
    private val dangerModeService: DangerModeService = StateManagerService.dangerModeService,
) : ViewModel() {

  val alertModePermission = AppPermissions.AlertModePermission

  // Sequence of permissions required to enable danger/alert mode.
  private val permissionSequence =
      listOf(
          AppPermissions.AlertModePermission,
          AppPermissions.MicrophonePermission,
          AppPermissions.SendEmergencySms,
          AppPermissions.MakeEmergencyCall)

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

  private val _capabilitiesInternal = MutableStateFlow<Set<DangerModeCapability>>(emptySet())
  val capabilities: StateFlow<Set<DangerModeCapability>> = _capabilitiesInternal.asStateFlow()
  // Alias for backward compatibility
  val capabilitiesInternal: StateFlow<Set<DangerModeCapability>> = capabilities

  private val _autoActionsEnabled = MutableStateFlow(false)
  val autoActionsEnabled: StateFlow<Boolean> = _autoActionsEnabled.asStateFlow()

  private val _confirmTouchRequired = MutableStateFlow(false)
  val confirmTouchRequired: StateFlow<Boolean> = _confirmTouchRequired.asStateFlow()

  private val _confirmVoiceRequired = MutableStateFlow(false)
  val confirmVoiceRequired: StateFlow<Boolean> = _confirmVoiceRequired.asStateFlow()

  private var enableRequested = false

  init {
    refreshActivities()

    // Listen for capability-related events from the service
    viewModelScope.launch(dispatcher) {
      dangerModeService.events.collect { event ->
        when (event) {
          DangerModeService.DangerModeEvent.MissingSmsPermission ->
              emitEffect(Effect.RequestCapabilityPermission(AppPermissions.SendEmergencySms))
          DangerModeService.DangerModeEvent.MissingCallPermission ->
              emitEffect(Effect.RequestCapabilityPermission(AppPermissions.MakeEmergencyCall))
          else -> Unit
        }
      }
    }

    // Collect user preferences
    viewModelScope.launch(dispatcher) {
      try {
        userPreferencesRepository.getUserPreferences.collect { prefs ->
          viewModelScope.launch(Dispatchers.Main.immediate) { applyPreferencesInternal(prefs) }
        }
      } catch (e: Throwable) {
        Log.e("DangerModeCardViewModel", "Error collecting user preferences", e)
      }
    }

    // Initial sync: seed capabilities from service state
    viewModelScope.launch(dispatcher) {
      try {
        val initial = dangerModeService.state.first()
        _capabilitiesInternal.value = initial.capabilities
      } catch (t: Throwable) {
        Log.w("DangerModeCardViewModel", "Failed to initial-sync capabilities: ${t.message}")
      }
    }
  }

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

  fun onDangerModeToggled(enabled: Boolean) {
    if (enabled) {
      dangerModeService.manualActivate()
      emitEffect(Effect.StartForegroundService)
    } else {
      dangerModeService.manualDeactivate()
      emitEffect(Effect.StopForegroundService)
    }
  }

  fun onActivitySelected(activity: DangerActivity?) {
    dangerModeService.setActivity(activity)
  }

  fun onCapabilitiesChanged(newCapabilities: Set<DangerModeCapability>) {
    val previous = _capabilitiesInternal.value
    _capabilitiesInternal.value = newCapabilities

    val result =
        try {
          dangerModeService.setCapabilities(newCapabilities)
        } catch (t: Throwable) {
          Result.failure<Unit>(t)
        }

    if (result.isFailure) {
      _capabilitiesInternal.value = previous
      Log.e("DangerModeCardViewModel", "Failed to set capabilities: $newCapabilities")
    }
  }

  fun onCapabilityToggled(capability: DangerModeCapability) {
    val current = _capabilitiesInternal.value
    val enabling = !current.contains(capability)
    val newCaps: Set<DangerModeCapability> = if (enabling) setOf(capability) else emptySet()

    _capabilitiesInternal.value = newCaps
    if (enabling) {
      _autoActionsEnabled.value = true
      viewModelScope.launch(dispatcher) { userPreferencesRepository.setAutoActionsEnabled(true) }
    }

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

  private fun emitEffect(effect: Effect) {
    viewModelScope.launch(dispatcher) { _effects.emit(effect) }
  }

  fun onPermissionsRequestStart(permissionType: AppPermissions? = null) {
    _alertModeUiState.update {
      it.copy(waitingForUserResponse = true, pendingPermissionType = permissionType)
    }
  }

  /** Legacy overload without permission type. */
  fun onPermissionsRequestStart() {
    onPermissionsRequestStart(null)
  }

  fun onPermissionResult(activity: Activity) {
    onPermissionResult(activity, alertModePermission)
  }

  fun onPermissionResult(activity: Activity, permissionType: AppPermissions) {
    val newResult = permissionManager.getPermissionResult(permissionType, activity)

    if (permissionType == alertModePermission) {
      _alertModeUiState.update { it.copy(alertModePermissionResult = newResult) }
    }

    if (_alertModeUiState.value.waitingForUserResponse) {
      permissionManager.markPermissionsAsAsked(permissionType)
    }

    _alertModeUiState.update {
      it.copy(waitingForUserResponse = false, pendingPermissionType = null)
    }

    if (newResult is PermissionResult.Granted && enableRequested) {
      requestNextRequiredPermission(activity)
    } else if (newResult !is PermissionResult.Granted) {
      enableRequested = false
    }
  }

  /** New API: starts the toggle flow with Activity for permission queries. */
  fun handleToggle(isChecked: Boolean, activity: Activity) {
    if (!isChecked) {
      onDangerModeToggled(false)
      return
    }
    enableRequested = true
    requestNextRequiredPermission(activity)
  }

  private fun requestNextRequiredPermission(activity: Activity) {
    val missing =
        permissionSequence.firstNotNullOfOrNull { permissionType ->
          val result = permissionManager.getPermissionResult(permissionType, activity)
          if (result is PermissionResult.Granted) null else permissionType to result
        }

    if (missing == null) {
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
      else -> emitEffect(Effect.RequestPermissions(permissionType))
    }
  }

  private fun applyPreferencesInternal(prefs: UserPreferences) {
    runCatching {
          _autoActionsEnabled.value = prefs.dangerModePreferences.autoActionsEnabled
          _confirmTouchRequired.value = prefs.dangerModePreferences.touchConfirmationRequired
          _confirmVoiceRequired.value = prefs.dangerModePreferences.voiceConfirmationEnabled

          val caps = mutableSetOf<DangerModeCapability>()
          if (prefs.dangerModePreferences.automaticCalls) caps.add(DangerModeCapability.CALL)
          if (prefs.dangerModePreferences.automaticSms) caps.add(DangerModeCapability.SMS)

          _capabilitiesInternal.value = caps
        }
        .onFailure { e -> Log.e("DangerModeCardViewModel", "Failed to apply user preferences", e) }
  }
}
