package com.github.warnastrophy.core.ui.features.dashboard

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.github.warnastrophy.core.domain.model.startForegroundGpsService
import com.github.warnastrophy.core.domain.model.stopForegroundGpsService
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
  var isDangerModeEnabled by mutableStateOf(false)
    private set

  var currentMode by mutableStateOf(DangerModePreset.CLIMBING_MODE)
    private set

  var dangerLevel by mutableIntStateOf(0)
    private set

  private val _capabilities = MutableStateFlow<Set<DangerModeCapability>>(emptySet())

  /** Backing property to expose capabilities as an immutable StateFlow */
  val capabilities = _capabilities.asStateFlow()

  /**
   * Handles the toggling of Danger Mode on or off and starts or stops the foreground GPS service
   * accordingly.
   *
   * @param enabled True to enable Danger Mode, false to disable it.
   * @param context The context used to start or stop the GPS service.
   */
  fun onDangerModeToggled(enabled: Boolean, context: Context? = null) {
    isDangerModeEnabled = enabled
    if (enabled) {
      context?.let { startService(it) }
    } else {
      context?.let { stopService(it) }
    }
  }

  /**
   * Sets the selected Danger Mode preset
   *
   * @param mode The selected Danger Mode preset.
   */
  fun onModeSelected(mode: DangerModePreset) {
    currentMode = mode
  }

  /**
   * Updates the set of enabled capabilities for Danger Mode.
   *
   * @param newCapabilities The new set of enabled capabilities.
   */
  fun onCapabilitiesChanged(newCapabilities: Set<DangerModeCapability>) {
    _capabilities.value = newCapabilities
  }

  /**
   * Toggles a specific capability for Danger Mode.
   *
   * @param capability The capability to be toggled.
   */
  fun onCapabilityToggled(capability: DangerModeCapability) {
    _capabilities.value =
        if (capabilities.value.contains(capability)) {
          capabilities.value - capability
        } else {
          capabilities.value + capability
        }
  }

  /**
   * Sets the danger level, ensuring it stays within the valid range of 0 to 3.
   *
   * @param level The new danger level to be set.
   */
  fun onDangerLevelChanged(level: Int) {
    dangerLevel = level.coerceIn(0, 3)
  }
}
