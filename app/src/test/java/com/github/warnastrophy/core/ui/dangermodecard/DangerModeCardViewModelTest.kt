package com.github.warnastrophy.core.ui.dangermodecard

import com.github.warnastrophy.core.ui.dashboard.DangerModeCapability
import com.github.warnastrophy.core.ui.dashboard.DangerModeCardViewModel
import com.github.warnastrophy.core.ui.dashboard.DangerModePreset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class DangerModeCardViewModelTest {
  private lateinit var viewModel: DangerModeCardViewModel

  @Before
  fun setup() {
    viewModel = DangerModeCardViewModel()
  }

  @Test
  fun `isDangerModeEnabled initial state is false`() = runTest {
    assertEquals(false, viewModel.isDangerModeEnabled)
  }

  @Test
  fun `onDangerModeToggled updates isDangerModeEnabled`() = runTest {
    viewModel.onDangerModeToggled(true)
    assertEquals(true, viewModel.isDangerModeEnabled)

    viewModel.onDangerModeToggled(false)
    assertEquals(false, viewModel.isDangerModeEnabled)
  }

  @Test
  fun `currentModeName initial state is CLIMBING_MODE`() = runTest {
    assertEquals(DangerModePreset.CLIMBING_MODE, viewModel.currentMode)
  }

  @Test
  fun `onModeSelected updates currentModeName`() = runTest {
    viewModel.onModeSelected(DangerModePreset.HIKING_MODE)
    assertEquals(DangerModePreset.HIKING_MODE, viewModel.currentMode)

    viewModel.onModeSelected(DangerModePreset.DEFAULT_MODE)
    assertEquals(DangerModePreset.DEFAULT_MODE, viewModel.currentMode)
  }

  @Test
  fun `capabilities initial state is empty set`() = runTest {
    assertEquals(emptySet<DangerModeCapability>(), viewModel.capabilities.first())
  }

  @Test
  fun `onCapabilitiesChanged updates capabilities`() = runTest {
    val newCapabilities = setOf(DangerModeCapability.CALL, DangerModeCapability.SMS)
    viewModel.onCapabilitiesChanged(newCapabilities)
    assertEquals(newCapabilities, viewModel.capabilities.first())
  }

  @Test
  fun `onCapabilityToggled adds capability when not present`() = runTest {
    viewModel.onCapabilityToggled(DangerModeCapability.CALL)
    assertEquals(setOf(DangerModeCapability.CALL), viewModel.capabilities.first())
  }

  @Test
  fun `onCapabilityToggled removes capability when present`() = runTest {
    viewModel.onCapabilitiesChanged(setOf(DangerModeCapability.CALL))
    viewModel.onCapabilityToggled(DangerModeCapability.CALL)
    assertEquals(emptySet<DangerModeCapability>(), viewModel.capabilities.first())
  }
}
