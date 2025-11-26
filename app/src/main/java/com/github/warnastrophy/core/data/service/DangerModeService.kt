package com.github.warnastrophy.core.data.service

import com.github.warnastrophy.core.model.Hazard
import com.github.warnastrophy.core.permissions.AppPermissions
import com.github.warnastrophy.core.permissions.PermissionManagerInterface
import com.github.warnastrophy.core.permissions.PermissionResult
import com.github.warnastrophy.core.ui.features.dashboard.DangerModeCapability
import com.github.warnastrophy.core.ui.features.dashboard.DangerModePreset
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class DangerLevel {
  LOW,
  MEDIUM,
  HIGH,
  CRITICAL
}

/**
 * Service responsible for managing the Danger Mode feature within the application.
 *
 * Danger Mode is a special operational state that can be activated in response to certain hazards,
 * or manually by the user.
 *
 * When activated, Danger Mode will monitor user activities and environmental conditions more
 * closely, and take appropriate actions to ensure user safety (contact emergency contacts, send
 * alerts, etc.).
 */
class DangerModeService(
    /**
     * Source of the current active hazard. By default, uses [StateManagerService.activeHazardFlow],
     * which is updated by [HazardChecker].
     */
    private val activeHazardFlow: StateFlow<Hazard?> = StateManagerService.activeHazardFlow,
    private val permissionManager: PermissionManagerInterface,

    /** Scope used internally to collect flows and manage coroutines. */
    private val serviceScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
  /* TODO: Define any dependencies, ideally as state flows, that DangerModeService might need
  /!\ Add them with default values to minimize complicated conflicts */

  /**
   * Represents the current state of Danger Mode in the application. Nullable fields should be null
   * when [isActive] is false.
   */
  data class DangerModeState(
      val isActive: Boolean = false,
      val activationTime: TimeSource.Monotonic.ValueTimeMark? = null,
      /**
       * The hazard that triggered the activation of Danger Mode, should stay null if activated
       * manually
       */
      val activatingHazard: Hazard? = null,
      /** Current preset mode for Danger Mode, dictates monitoring behavior */
      val preset: DangerModePreset = DangerModePreset.DEFAULT_MODE,
      /** The capabilities (what can it monitor, take which actions) given to danger mode */
      val capabilities: Set<DangerModeCapability> = emptySet(),
      /** Current danger level, can be used in communication */
      val dangerLevel: DangerLevel = DangerLevel.LOW
  )

  sealed class DangerModeEvent {
    object MissingSmsPermission : DangerModeEvent()
  }

  private val _state = MutableStateFlow(DangerModeState())
  val state: StateFlow<DangerModeState> = _state.asStateFlow()
  private val _events = MutableStateFlow<DangerModeEvent?>(null)
  val events: StateFlow<DangerModeEvent?> = _events.asStateFlow()

  init {
    serviceScope.launch {
      activeHazardFlow.collectLatest { activeHazard -> handleActiveHazardChanged(activeHazard) }
    }
  }

  /**
   * Sets the preset mode for Danger Mode.
   *
   * @param preset The preset mode to activate.
   */
  fun setPreset(preset: DangerModePreset) {
    _state.value = _state.value.copy(preset = preset)
  }

  /**
   * This function sets the capabilities for Danger Mode after validating that the necessary
   * permissions are granted.
   *
   * If any required permission is missing, it returns a failure [Result] with an
   * [IllegalStateException] indicating which permission is missing.
   *
   * If all permissions are granted, it updates the Danger Mode state with the new capabilities and
   * returns a successful [Result].
   *
   * Note: The CALL capability is currently not supported and will always result in a failure.
   *
   * @param capabilities The set of capabilities to enable.
   */
  fun setCapabilities(capabilities: Set<DangerModeCapability>): Result<Unit> {
    val validated = mutableSetOf<DangerModeCapability>()

    for (cap in capabilities) {

      // CALL capability is not supported yet
      if (cap == DangerModeCapability.CALL) {
        return Result.failure(IllegalStateException("CALL capability is not supported yet."))
      }

      val requiredPermission =
          when (cap) {
            DangerModeCapability.LOCATION -> AppPermissions.LocationFine
            DangerModeCapability.SMS -> AppPermissions.SendEmergencySms
            DangerModeCapability.CALL -> null
          }

      requiredPermission?.let { perm ->
        val result = permissionManager.getPermissionResult(perm)
        if (result != PermissionResult.Granted) {
          return Result.failure(
              IllegalStateException("Missing permission for ${cap.label}: ${perm.key}"))
        }
      }

      validated.add(cap)
    }

    _state.value = _state.value.copy(capabilities = validated)

    return Result.success(Unit)
  }

  /**
   * Sets the danger level for Danger Mode.
   *
   * @param level The danger level to set, coerced to be in [0, 3]
   */
  fun setDangerLevel(level: DangerLevel) {
    _state.value =
        _state.value.copy(dangerLevel = level.coerceIn(DangerLevel.LOW, DangerLevel.CRITICAL))
  }

  /** Manually activates Danger Mode. */
  fun manualActivate() {
    _state.value =
        _state.value.copy(
            isActive = true,
            activationTime = TimeSource.Monotonic.markNow(),
            activatingHazard = null)
  }

  /** Manually deactivates Danger Mode. */
  fun manualDeactivate() {
    _state.value = DangerModeState()
  }

  private fun handleActiveHazardChanged(activeHazard: Hazard?) {
    val current = _state.value

    val isManuallyActive = current.isActive && current.activatingHazard == null
    if (isManuallyActive) {
      return
    }

    if (activeHazard != null) {
      if (!current.isActive || current.activatingHazard?.id != activeHazard.id) {
        autoActivate(activeHazard)
      }
    } else {
      if (current.isActive) {
        autoDeactivate()
      }
    }
  }

  private fun autoActivate(hazard: Hazard) {
    _state.value =
        _state.value.copy(
            isActive = true,
            activationTime = TimeSource.Monotonic.markNow(),
            activatingHazard = hazard,
        )
  }

  private fun autoDeactivate() {
    val current = _state.value
    _state.value =
        current.copy(
            isActive = false,
            activationTime = null,
            activatingHazard = null,
        )
  }
}
