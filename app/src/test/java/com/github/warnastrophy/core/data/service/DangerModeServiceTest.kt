package com.github.warnastrophy.core.data.service

import com.github.warnastrophy.core.model.Hazard
import com.github.warnastrophy.core.permissions.AppPermissions
import com.github.warnastrophy.core.permissions.PermissionManagerInterface
import com.github.warnastrophy.core.permissions.PermissionResult
import com.github.warnastrophy.core.ui.features.dashboard.DangerModeCapability
import com.github.warnastrophy.core.ui.features.dashboard.DangerModePreset
import kotlin.test.assertNull
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

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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
    service.setDangerLevel(DangerLevel.CRITICAL)
    assertEquals(DangerLevel.CRITICAL, service.state.value.dangerLevel)

    service.setPreset(DangerModePreset.CLIMBING_MODE)
    assertEquals(DangerModePreset.CLIMBING_MODE, service.state.value.preset)

    val capabilities = setOf(DangerModeCapability.LOCATION)
    service.setCapabilities(capabilities)

    assertEquals(capabilities, service.state.value.capabilities)
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
    assertTrue(state.isActive)
    assertEquals(hazard, state.activatingHazard)
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
    assertTrue(service.state.value.isActive)

    hazardFlow.value = null
    advanceUntilIdle()

    val state = service.state.value
    assertFalse(state.isActive)
    assertNull(state.activatingHazard)
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
    assertTrue(service.state.value.isActive)
    assertNull(service.state.value.activatingHazard)

    val hazard = Hazard(id = 1, alertLevel = 3.0)
    hazardFlow.value = hazard
    advanceUntilIdle()

    val stateAfterHazard = service.state.value
    assertTrue(stateAfterHazard.isActive)
    assertNull(stateAfterHazard.activatingHazard)

    hazardFlow.value = null
    advanceUntilIdle()

    val finalState = service.state.value
    assertTrue(finalState.isActive)
    assertNull(finalState.activatingHazard)
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
    val caps = setOf(DangerModeCapability.LOCATION)
    service.setCapabilities(caps)

    service.setDangerLevel(DangerLevel.CRITICAL) // coerced to 3

    advanceUntilIdle()
    val state = service.state.value

    assertEquals(DangerModePreset.HIKING_MODE, state.preset)
    assertEquals(caps, state.capabilities)
    assertEquals(DangerLevel.CRITICAL, state.dangerLevel)
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
    assertTrue(state.isActive)
    assertNull(service.events.value)
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
    assertTrue(state.isActive)
    assertEquals(hazard, state.activatingHazard)
    assertNull(service.events.value)
  }

  /**
   * Ensures SMS capability cannot be enabled when the SEND_SMS permission is denied. The service
   * must reject the request and leave the capability set empty.
   */
  @Test
  fun sms_capability_cannot_be_enabled_without_sms_permission() = runTest {
    val pm = PermissionManagerMock(PermissionResult.Denied(listOf("SEND_SMS")))
    val (service, _) = createService(permissionManager = pm)

    val result = service.setCapabilities(setOf(DangerModeCapability.SMS))

    assertTrue(result.isFailure)
    assertTrue(service.state.value.capabilities.isEmpty())
  }

  /**
   * Ensures SMS capability is enabled when the SEND_SMS permission is granted. The service should
   * accept the capability and update the state accordingly.
   */
  @Test
  fun sms_capability_is_enabled_when_permission_granted() = runTest {
    val pm = PermissionManagerMock(PermissionResult.Granted)
    val (service, _) = createService(permissionManager = pm)

    val result = service.setCapabilities(setOf(DangerModeCapability.SMS))

    assertTrue(result.isSuccess)
    assertEquals(setOf(DangerModeCapability.SMS), service.state.value.capabilities)
  }

  /**
   * Verifies that the CALL capability is always rejected. This capability is intentionally
   * unsupported and must never appear in the state.
   */
  @Test
  fun call_capability_is_always_rejected() = runTest {
    val pm = PermissionManagerMock(PermissionResult.Granted)
    val (service, _) = createService(permissionManager = pm)

    val result = service.setCapabilities(setOf(DangerModeCapability.CALL))

    assertTrue(result.isFailure)
    assertTrue(service.state.value.capabilities.isEmpty())
  }

  /**
   * Confirms that manual activation works even when SMS permission is missing. Danger Mode should
   * activate normally and no error event should be emitted.
   */
  @Test
  fun manual_activation_succeeds_even_if_sms_permission_missing() = runTest {
    val pm = PermissionManagerMock(PermissionResult.Denied(listOf("SEND_SMS")))
    val (service, _) = createService(permissionManager = pm)

    service.manualActivate()
    advanceUntilIdle()

    assertTrue(service.state.value.isActive)
    assertNull(service.events.value)
  }

  /**
   * Confirms that automatic activation also works without SMS permission. The service should
   * activate based on hazards and emit no error event.
   */
  @Test
  fun auto_activation_succeeds_even_if_sms_permission_missing() = runTest {
    val pm = PermissionManagerMock(PermissionResult.Denied(listOf("SEND_SMS")))
    val hazardFlow = MutableStateFlow<Hazard?>(null)
    val (service, flow) = createService(hazardFlow = hazardFlow, permissionManager = pm)

    val hazard = Hazard(id = 1, alertLevel = 3.0)
    flow.value = hazard
    advanceUntilIdle()

    assertTrue(service.state.value.isActive)
    assertEquals(hazard, service.state.value.activatingHazard)
    assertNull(service.events.value)
  }

  /**
   * Ensures LOCATION capability cannot be enabled when location permission is denied. The service
   * must reject the capability and leave the state untouched.
   */
  @Test
  fun location_capability_fails_when_location_permission_missing() = runTest {
    val pm = PermissionManagerMock(PermissionResult.Denied(listOf("fine_location")))
    val (service, _) = createService(permissionManager = pm)

    val result = service.setCapabilities(setOf(DangerModeCapability.LOCATION))

    assertTrue(result.isFailure)
    assertTrue(service.state.value.capabilities.isEmpty())
  }

  /**
   * Ensures LOCATION capability is enabled when permission is granted. The capability should be
   * accepted and reflected in the service state.
   */
  @Test
  fun location_capability_succeeds_when_permission_granted() = runTest {
    val pm = PermissionManagerMock(PermissionResult.Granted)
    val (service, _) = createService(permissionManager = pm)

    val result = service.setCapabilities(setOf(DangerModeCapability.LOCATION))

    assertTrue(result.isSuccess)
    assertEquals(setOf(DangerModeCapability.LOCATION), service.state.value.capabilities)
  }
}
