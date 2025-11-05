package com.github.warnastrophy.core.ui.dangerModeCard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlin.collections.emptySet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class DangerModeCapability(val label: String) {
  CALL("Call"),
  SMS("SMS"),
  LOCATION("Location")
}

enum class DangerModePreset(val label: String) {
  CLIMBING_MODE("Climbing mode"),
  HIKING_MODE("Hiking mode"),
  DEFAULT_MODE("Default mode")
}

class DangerModeCardViewModel : ViewModel() {
  var isDangerModeEnabled by mutableStateOf(false)
    private set

  var currentModeName by mutableStateOf(DangerModePreset.CLIMBING_MODE)
    private set

  private val _capabilities = MutableStateFlow<Set<DangerModeCapability>>(emptySet())
  val capabilities = _capabilities.asStateFlow()

  fun onDangerModeToggled(enabled: Boolean) {
    isDangerModeEnabled = enabled
  }

  fun onModeSelected(mode: DangerModePreset) {
    currentModeName = mode
  }

  fun onCapabilitiesChanged(newCapabilities: Set<DangerModeCapability>) {
    _capabilities.value = newCapabilities
  }

  fun onCapabilityToggled(capability: DangerModeCapability) {
    _capabilities.value =
        if (capabilities.value.contains(capability)) {
          capabilities.value - capability
        } else {
          capabilities.value + capability
        }
  }
}
