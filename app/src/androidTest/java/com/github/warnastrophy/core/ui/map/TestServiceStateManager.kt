package com.github.warnastrophy.core.ui.map

import com.github.warnastrophy.core.model.Hazard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object TestServiceStateManager {
  // Internal mutable flow to hold the active Hazard state.
  // It is nullable because there may be no active hazard.
  private val _activeHazardFlow = MutableStateFlow<Hazard?>(null)

  /** Publicly exposed StateFlow for observation in unit tests. */
  val activeHazardFlow: StateFlow<Hazard?> = _activeHazardFlow

  /**
   * Updates the currently active hazard. This is the function the HazardChecker will call after the
   * time threshold has passed.
   *
   * @param hazard The Hazard object that the user is stably inside, or null to clear the alert.
   */
  fun updateActiveHazard(hazard: Hazard?) {
    // In a real application, you might add logic here to trigger a notification service.
    _activeHazardFlow.value = hazard
    println("!!! Service State Updated: Active Hazard ID = ${hazard?.id}")
  }
}
