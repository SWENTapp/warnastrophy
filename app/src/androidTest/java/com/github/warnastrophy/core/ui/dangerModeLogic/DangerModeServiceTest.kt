@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package com.github.warnastrophy.core.ui.dangerModeLogic

import com.github.warnastrophy.core.data.service.DangerModeService
import com.github.warnastrophy.core.domain.model.Hazard
import com.github.warnastrophy.core.ui.features.dashboard.DangerModePreset
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DangerModeServiceTest {

  /**
   * Helper to create a DangerModeService with a controllable flow + scope that uses the same
   * TestScheduler as this TestScope.
   */
  private fun TestScope.createService(
      hazardFlow: MutableStateFlow<Hazard?> = MutableStateFlow(null)
  ): Pair<DangerModeService, MutableStateFlow<Hazard?>> {
    // Use the testScheduler of this TestScope
    val dispatcher = StandardTestDispatcher(testScheduler)
    val scope = CoroutineScope(dispatcher)

    val service = DangerModeService(activeHazardFlow = hazardFlow, serviceScope = scope)

    return service to hazardFlow
  }

  /** To test that when the user enters a hazard zone, the dangerMode activates automatically */
  @Test
  fun auto_activates_when_active_hazard_appears() = runTest {
    val (service, hazardFlow) = createService()

    val hazard = Hazard(id = 1, alertLevel = 3.0)
    hazardFlow.value = hazard

    advanceUntilIdle()

    val state = service.state.value
    assertTrue(state.isActive)
    assertEquals(hazard, state.activatingHazard)
  }

  /**
   * To test that, when the dangerMode was automatically activated, it will automatically deactivate
   * if the user leaves the hazard zone.
   */
  @Test
  fun auto_deactivates_when_active_hazard_disappears_and_it_was_auto_enabled() = runTest {
    val (service, hazardFlow) = createService()

    val hazard = Hazard(id = 1, alertLevel = 3.0)
    hazardFlow.value = hazard
    advanceUntilIdle()
    assertTrue(service.state.value.isActive)

    hazardFlow.value = null
    advanceUntilIdle()

    val state = service.state.value
    assertFalse(state.isActive)
    assertEquals(null, state.activatingHazard)
  }

  /**
   * To test that when the dangerMode is manually activated, it will not automatically deactivate
   * when the user leaves the hazard zone.
   */
  @Test
  fun manual_activation_is_not_auto_toggled_when_hazard_flow_changes() = runTest {
    val (service, hazardFlow) = createService()

    service.manualActivate()
    advanceUntilIdle()
    assertTrue(service.state.value.isActive)
    assertEquals(null, service.state.value.activatingHazard)

    val hazard = Hazard(id = 1, alertLevel = 3.0)
    hazardFlow.value = hazard
    advanceUntilIdle()

    val stateAfterHazard = service.state.value
    assertTrue(stateAfterHazard.isActive)
    assertEquals(null, stateAfterHazard.activatingHazard)

    hazardFlow.value = null
    advanceUntilIdle()

    val finalState = service.state.value
    assertTrue(finalState.isActive)
    assertEquals(null, finalState.activatingHazard)
  }

  /**
   * Verifies that the configuration mutators (setPreset, setCapabilities, setDangerLevel) correctly
   * update the exposed DangerModeState.
   *
   * In particular, this test checks that:
   * - the preset value is updated to the selected DangerModePreset,
   * - the capabilities set is stored as given, and
   * - the dangerLevel value is coerced into the valid [0, 3] range.
   */
  @Test
  fun setPreset_setCapabilities_and_setDangerLevel_update_state() = runTest {
    val (service, _) = createService()

    service.setPreset(DangerModePreset.HIKING_MODE)
    service.setCapabilities(setOf("CALL", "LOCATION"))
    service.setDangerLevel(5) // coerced to 3

    advanceUntilIdle()
    val state = service.state.value

    assertEquals(DangerModePreset.HIKING_MODE, state.preset)
    assertEquals(setOf("CALL", "LOCATION"), state.capabilities)
    assertEquals(3, state.dangerLevel)
  }
}
