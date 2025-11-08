package com.github.warnastrophy.core.ui.features.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlin.collections.emptySet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

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
  var isDangerModeEnabled by mutableStateOf(false)
    private set

  var currentMode by mutableStateOf(DangerModePreset.CLIMBING_MODE)
    private set

  var dangerLevel by mutableIntStateOf(0)
    private set

  private val _capabilities = MutableStateFlow<Set<DangerModeCapability>>(emptySet())
  val capabilities = _capabilities.asStateFlow()

  /** Toggles the Danger Mode on or off. */
  fun onDangerModeToggled(enabled: Boolean) {
    isDangerModeEnabled = enabled
  }

  /** Sets the selected Danger Mode preset. */
  fun onModeSelected(mode: DangerModePreset) {
    currentMode = mode
  }

  /** Updates the set of enabled capabilities for Danger Mode. */
  fun onCapabilitiesChanged(newCapabilities: Set<DangerModeCapability>) {
    _capabilities.value = newCapabilities
  }

  /** Toggles a specific capability for Danger Mode. */
  fun onCapabilityToggled(capability: DangerModeCapability) {
    _capabilities.value =
        if (capabilities.value.contains(capability)) {
          capabilities.value - capability
        } else {
          capabilities.value + capability
        }
  }

  /** Sets the danger level, ensuring it stays within the valid range of 0 to 3. */
  fun onDangerLevelChanged(level: Int) {
    dangerLevel = level.coerceIn(0, 3)
  }
}
