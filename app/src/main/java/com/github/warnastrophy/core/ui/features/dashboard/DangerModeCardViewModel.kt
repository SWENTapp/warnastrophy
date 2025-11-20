package com.github.warnastrophy.core.ui.features.dashboard

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.warnastrophy.core.data.service.ServiceStateManager
import kotlin.collections.emptySet
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Capabilities of the danger mode that can be enabled in Danger Mode with associated labels. */
enum class DangerModeCapability(val label: String) {
  CALL("Call"),
  SMS("SMS"),
  LOCATION("Location")
}

/** Preset modes for Danger Mode with associated labels. */
enum class DangerModePreset(val label: String) {
  CLIMBING_MODE("Climbing mode"),
  HIKING_MODE("Hiking mode"),
  DEFAULT_MODE("Default mode")
}

/** ViewModel for managing the state of the Danger Mode card in the dashboard UI. */
class DangerModeCardViewModel : ViewModel() {
  private val dangerModeService = ServiceStateManager.dangerModeService

  val isDangerModeEnabled =
      dangerModeService.state
          .map { it.isActive }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

  val currentMode =
      dangerModeService.state
          .map { it.preset }
          .stateIn(
              viewModelScope, SharingStarted.WhileSubscribed(5000), DangerModePreset.DEFAULT_MODE)

  var dangerLevel =
      dangerModeService.state
          .map { it.dangerLevel }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

  val capabilities =
      dangerModeService.state
          .map { it.capabilities }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

  /** Toggles the Danger Mode on or off. */
  fun onDangerModeToggled(enabled: Boolean) {
    if (enabled) {
      dangerModeService.manualActivate()
    } else {
      dangerModeService.manualDeactivate()
    }
  }

  /** Sets the selected Danger Mode preset. */
  fun onModeSelected(mode: DangerModePreset) {
    dangerModeService.setPreset(mode)
  }

  /** Updates the set of enabled capabilities for Danger Mode. */
  fun onCapabilitiesChanged(newCapabilities: Set<DangerModeCapability>) {
    if (dangerModeService.setCapabilities(newCapabilities).isFailure) {
      // TODO
      Log.e("DangerModeCardViewModel", "Failed to set capabilities: $newCapabilities")
    }
  }

  /** Toggles a specific capability for Danger Mode. */
  fun onCapabilityToggled(capability: DangerModeCapability) {
    val future =
        if (capabilities.value.contains(capability)) {
          capabilities.value - capability
        } else {
          capabilities.value + capability
        }

    onCapabilitiesChanged(future)
  }

  /** Sets the danger level, ensuring it stays within the valid range of 0 to 3. */
  fun onDangerLevelChanged(level: Int) {
    dangerModeService.setDangerLevel(level)
  }
}
