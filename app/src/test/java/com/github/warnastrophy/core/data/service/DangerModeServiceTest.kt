package com.github.warnastrophy.core.data.service

import com.github.warnastrophy.core.domain.model.Hazard
import com.github.warnastrophy.core.permissions.AppPermissions
import com.github.warnastrophy.core.permissions.PermissionManagerInterface
import com.github.warnastrophy.core.permissions.PermissionResult
import com.github.warnastrophy.core.ui.features.dashboard.DangerModePreset
import junit.framework.TestCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private class PermissionManagerMock(private val result: PermissionResult) :
    PermissionManagerInterface {

  override fun getPermissionResult(permissionType: AppPermissions): PermissionResult {
    return result
  }

  override fun getPermissionResult(
      permissionType: AppPermissions,
      activity: android.app.Activity
  ): PermissionResult {
    return result
  }

  override fun markPermissionsAsAsked(permissionType: AppPermissions) {}

  override fun isPermissionAskedBefore(permissionType: AppPermissions) = false
}

@RunWith(org.junit.runners.JUnit4::class)
class DangerModeServiceTest {
  private lateinit var service: DangerModeService

  @Before
  fun setUp() {
    // Assuming DangerModeService can be instantiated directly
    service = DangerModeService(permissionManager = PermissionManagerMock(PermissionResult.Granted))
  }

  @Test
  fun `isDangerModeActive is false by default`() {
    assertFalse(service.state.value.isActive)
  }

  @Test
  fun `deactivateDangerMode sets danger mode to false`() {
    // First activate
    service.manualActivate()
    assertTrue(service.state.value.isActive)

    // Then deactivate
    service.manualDeactivate()
    assertFalse(service.state.value.isActive)
  }

  @Test
  fun basicSetsWork() {
    service.setDangerLevel(3)
    assertTrue(service.state.value.dangerLevel == 3)
    service.setPreset(DangerModePreset.CLIMBING_MODE)
    assertTrue(service.state.value.preset == DangerModePreset.CLIMBING_MODE)
    val set = setOf("GPS_MONITORING", "ALERT_SENDING")
    service.setCapabilities(set)
    assertEquals(set, service.state.value.capabilities)
  }

  /**
   * Helper to create a DangerModeService with a controllable flow + scope that uses the same
   * TestScheduler as this TestScope.
   */
  private fun TestScope.createService(
      hazardFlow: MutableStateFlow<Hazard?> = MutableStateFlow(null),
      permissionManager: PermissionManagerInterface
  ): Pair<DangerModeService, MutableStateFlow<Hazard?>> {
    // Use the testScheduler of this TestScope
    val dispatcher = StandardTestDispatcher(testScheduler)
    val scope = CoroutineScope(dispatcher)

    val service =
        DangerModeService(
            activeHazardFlow = hazardFlow,
            serviceScope = scope,
            permissionManager = permissionManager)

    return service to hazardFlow
  }

  /** To test that when the user enters a hazard zone, the dangerMode activates automatically */
  @Test
  fun auto_activates_when_active_hazard_appears() = runTest {
    val fakePm = PermissionManagerMock(PermissionResult.Granted)
    val (service, hazardFlow) = createService(permissionManager = fakePm)

    val hazard = Hazard(id = 1, alertLevel = 3.0)
    hazardFlow.value = hazard

    advanceUntilIdle()

    val state = service.state.value
    TestCase.assertTrue(state.isActive)
    TestCase.assertEquals(hazard, state.activatingHazard)
  }

  /**
   * To test that, when the dangerMode was automatically activated, it will automatically deactivate
   * if the user leaves the hazard zone.
   */
  @Test
  fun auto_deactivates_when_active_hazard_disappears_and_it_was_auto_enabled() = runTest {
    val fakePm = PermissionManagerMock(PermissionResult.Granted)
    val (service, hazardFlow) = createService(permissionManager = fakePm)

    val hazard = Hazard(id = 1, alertLevel = 3.0)
    hazardFlow.value = hazard
    advanceUntilIdle()
    TestCase.assertTrue(service.state.value.isActive)

    hazardFlow.value = null
    advanceUntilIdle()

    val state = service.state.value
    TestCase.assertFalse(state.isActive)
    TestCase.assertEquals(null, state.activatingHazard)
  }

  /**
   * To test that when the dangerMode is manually activated, it will not automatically deactivate
   * when the user leaves the hazard zone.
   */
  @Test
  fun manual_activation_is_not_auto_toggled_when_hazard_flow_changes() = runTest {
    val fakePm = PermissionManagerMock(PermissionResult.Granted)
    val (service, hazardFlow) = createService(permissionManager = fakePm)

    service.manualActivate()
    advanceUntilIdle()
    TestCase.assertTrue(service.state.value.isActive)
    TestCase.assertEquals(null, service.state.value.activatingHazard)

    val hazard = Hazard(id = 1, alertLevel = 3.0)
    hazardFlow.value = hazard
    advanceUntilIdle()

    val stateAfterHazard = service.state.value
    TestCase.assertTrue(stateAfterHazard.isActive)
    TestCase.assertEquals(null, stateAfterHazard.activatingHazard)

    hazardFlow.value = null
    advanceUntilIdle()

    val finalState = service.state.value
    TestCase.assertTrue(finalState.isActive)
    TestCase.assertEquals(null, finalState.activatingHazard)
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
    val fakePm = PermissionManagerMock(PermissionResult.Granted)
    val (service, _) = createService(permissionManager = fakePm)

    service.setPreset(DangerModePreset.HIKING_MODE)
    service.setCapabilities(setOf("CALL", "LOCATION"))
    service.setDangerLevel(5) // coerced to 3

    advanceUntilIdle()
    val state = service.state.value

    TestCase.assertEquals(DangerModePreset.HIKING_MODE, state.preset)
    TestCase.assertEquals(setOf("CALL", "LOCATION"), state.capabilities)
    TestCase.assertEquals(3, state.dangerLevel)
  }

  /**
   * Verifies that manual activation is blocked when SMS permissions are missing.
   *
   * In particular, this test ensures that:
   * - calling manualActivate() does not activate Danger Mode when the SEND_SMS permission is
   *   denied,
   * - the DangerModeState remains inactive, and
   * - the MissingSmsPermission event is emitted to inform the UI layer.
   */
  @Test
  fun manual_activation_blocked_when_sms_permission_missing() = runTest {
    val fakePm = PermissionManagerMock(PermissionResult.Denied(listOf("sms")))
    val (service, _) = createService(permissionManager = fakePm)

    service.manualActivate()
    advanceUntilIdle()

    val state = service.state.value
    TestCase.assertFalse(state.isActive)
    TestCase.assertEquals(
        DangerModeService.DangerModeEvent.MissingSmsPermission, service.events.value)
  }

  /**
   * Verifies that manual activation succeeds when SMS permissions are granted.
   *
   * This test checks that:
   * - calling manualActivate() successfully activates Danger Mode when the SEND_SMS permission is
   *   granted,
   * - the resulting DangerModeState reflects an active state, and
   * - no error or warning event is emitted.
   */
  @Test
  fun manual_activation_succeeds_when_sms_permission_granted() = runTest {
    val fakePm = PermissionManagerMock(PermissionResult.Granted)
    val (service, _) = createService(permissionManager = fakePm)

    service.manualActivate()
    advanceUntilIdle()

    val state = service.state.value
    TestCase.assertTrue(state.isActive)
    TestCase.assertEquals(null, service.events.value)
  }

  /**
   * Verifies that automatic activation is blocked when SMS permissions are missing.
   *
   * This test ensures that:
   * - updating the activeHazardFlow with a non-null hazard does NOT trigger automatic activation
   *   when SEND_SMS permission is denied,
   * - Danger Mode remains inactive, and
   * - the MissingSmsPermission event is emitted to notify the UI about the blocked activation.
   */
  @Test
  fun auto_activation_blocked_when_sms_permission_missing() = runTest {
    val fakePm = PermissionManagerMock(PermissionResult.Denied(listOf("sms")))
    val hazardFlow = MutableStateFlow<Hazard?>(null)
    val (service, flow) = createService(hazardFlow = hazardFlow, permissionManager = fakePm)

    val hazard = Hazard(id = 10, alertLevel = 2.0)
    flow.value = hazard
    advanceUntilIdle()

    val state = service.state.value
    TestCase.assertFalse(state.isActive)
    TestCase.assertEquals(
        DangerModeService.DangerModeEvent.MissingSmsPermission, service.events.value)
  }

  /**
   * Verifies that automatic activation succeeds when SMS permissions are granted.
   *
   * This test checks that:
   * - updating the activeHazardFlow with a non-null hazard automatically activates Danger Mode when
   *   the SEND_SMS permission is granted,
   * - the activatingHazard field is set to the new hazard, and
   * - no MissingSmsPermission event is emitted.
   */
  @Test
  fun auto_activation_succeeds_when_sms_permission_granted() = runTest {
    val fakePm = PermissionManagerMock(PermissionResult.Granted)
    val hazardFlow = MutableStateFlow<Hazard?>(null)
    val (service, flow) = createService(hazardFlow = hazardFlow, permissionManager = fakePm)

    val hazard = Hazard(id = 99, alertLevel = 4.0)
    flow.value = hazard
    advanceUntilIdle()

    val state = service.state.value
    TestCase.assertTrue(state.isActive)
    TestCase.assertEquals(hazard, state.activatingHazard)
    TestCase.assertEquals(null, service.events.value)
  }
}
