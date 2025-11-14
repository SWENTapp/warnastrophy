package com.github.warnastrophy.core.data.service

import com.github.warnastrophy.core.ui.features.dashboard.DangerModePreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DangerModeServiceTest {
  private lateinit var service: DangerModeService

  @Before
  fun setUp() {
    // Assuming DangerModeService can be instantiated directly
    service = DangerModeService()
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
}
