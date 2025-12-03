package com.github.warnastrophy.core.ui.features.dashboard

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.warnastrophy.core.data.service.DangerLevel
import com.github.warnastrophy.core.data.service.StateManagerService
import com.github.warnastrophy.core.domain.model.startForegroundGpsService
import com.github.warnastrophy.core.domain.model.stopForegroundGpsService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Capabilities of the danger mode that can be enabled in Danger Mode with associated labels. */
enum class DangerModeCapability(val label: String) {
  CALL("Call"),
  SMS("SMS"),
}

/** Preset modes for Danger Mode with associated labels. */
enum class DangerModePreset(val label: String) {
  CLIMBING_MODE("Climbing mode"),
  HIKING_MODE("Hiking mode"),
  DEFAULT_MODE("Default mode")
}

/**
 * ViewModel for managing the state of the Danger Mode card in the dashboard UI.
 *
 * @param startService Lambda function to start the foreground GPS service (can be overridden for
 *   testing).
 * @param stopService Lambda function to stop the foreground GPS service (can be overridden for
 *   testing).
 */
class DangerModeCardViewModel(
    private val startService: (Context) -> Unit = { ctx -> startForegroundGpsService(ctx) },
    private val stopService: (Context) -> Unit = { ctx -> stopForegroundGpsService(ctx) }
) : ViewModel() {
  private val dangerModeService = StateManagerService.dangerModeService

  val isDangerModeEnabled =
      dangerModeService.state
          .map { it.isActive }
          .stateIn(viewModelScope, SharingStarted.Lazily, false)

  val currentMode =
      dangerModeService.state
          .map { it.preset }
          .stateIn(viewModelScope, SharingStarted.Lazily, DangerModePreset.DEFAULT_MODE)

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
  fun onDangerModeToggled(enabled: Boolean, context: Context? = null) {
    if (enabled) {
      context?.let { startService(it) }
      dangerModeService.manualActivate()
    } else {
      context?.let { stopService(it) }
      dangerModeService.manualDeactivate()
    }
  }

  /**
   * Sets the selected Danger Mode preset
   *
   * @param mode The selected Danger Mode preset.
   */
  fun onModeSelected(mode: DangerModePreset) {
    dangerModeService.setPreset(mode)
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
}
