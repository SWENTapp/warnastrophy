package com.github.warnastrophy.core.data.service

import com.github.warnastrophy.core.domain.model.Hazard
import com.github.warnastrophy.core.ui.features.dashboard.DangerModePreset
import kotlin.time.TimeSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

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
class DangerModeService {
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
      val preset: DangerModePreset? = DangerModePreset.DEFAULT_MODE,
      /** The capabilities (what can it monitor, take which actions) given to danger mode */
      val capabilities: Set<String> = emptySet(),
      /** Current danger level, can be used in communication */
      val dangerLevel: Int? = 0,
  )

  private val _state = MutableStateFlow(DangerModeState())
  val state = _state.asStateFlow()

  init {
    // TODO: Initialize any required resources or listeners for Danger Mode
    // Add logic to automatically activate/deactivate Danger Mode based on hazards
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
   * Sets the capabilities for Danger Mode.
   *
   * @param capabilities The set of capabilities to enable.
   */
  fun setCapabilities(capabilities: Set<String>) {
    _state.value = _state.value.copy(capabilities = capabilities)
  }

  /**
   * Sets the danger level for Danger Mode.
   *
   * @param level The danger level to set, coerced to be in [0, 3]
   */
  fun setDangerLevel(level: Int) {
    _state.value = _state.value.copy(dangerLevel = level.coerceIn(0, 3))
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
}
