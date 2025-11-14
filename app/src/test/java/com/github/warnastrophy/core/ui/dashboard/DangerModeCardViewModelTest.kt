package com.github.warnastrophy.core.ui.dashboard

import com.github.warnastrophy.core.ui.features.dashboard.DangerModeCapability
import com.github.warnastrophy.core.ui.features.dashboard.DangerModeCardViewModel
import com.github.warnastrophy.core.ui.features.dashboard.DangerModePreset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert
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
    Assert.assertEquals(false, viewModel.isDangerModeEnabled)
  }

  @Test
  fun `onDangerModeToggled updates isDangerModeEnabled`() = runTest {
    viewModel.onDangerModeToggled(true)
    Assert.assertEquals(true, viewModel.isDangerModeEnabled)

    viewModel.onDangerModeToggled(false)
    Assert.assertEquals(false, viewModel.isDangerModeEnabled)
  }

  @Test
  fun `currentModeName initial state is CLIMBING_MODE`() = runTest {
    Assert.assertEquals(DangerModePreset.CLIMBING_MODE, viewModel.currentMode)
  }

  @Test
  fun `onModeSelected updates currentModeName`() = runTest {
    viewModel.onModeSelected(DangerModePreset.HIKING_MODE)
    Assert.assertEquals(DangerModePreset.HIKING_MODE, viewModel.currentMode)

    viewModel.onModeSelected(DangerModePreset.DEFAULT_MODE)
    Assert.assertEquals(DangerModePreset.DEFAULT_MODE, viewModel.currentMode)
  }

  @Test
  fun `capabilities initial state is empty set`() = runTest {
    Assert.assertEquals(emptySet<DangerModeCapability>(), viewModel.capabilities.first())
  }

  @Test
  fun `onCapabilitiesChanged updates capabilities`() = runTest {
    val newCapabilities = setOf(DangerModeCapability.CALL, DangerModeCapability.SMS)
    viewModel.onCapabilitiesChanged(newCapabilities)
    Assert.assertEquals(newCapabilities, viewModel.capabilities.first())
  }

  @Test
  fun `onCapabilityToggled adds capability when not present`() = runTest {
    viewModel.onCapabilityToggled(DangerModeCapability.CALL)
    Assert.assertEquals(setOf(DangerModeCapability.CALL), viewModel.capabilities.first())
  }

  @Test
  fun `onCapabilityToggled removes capability when present`() = runTest {
    viewModel.onCapabilitiesChanged(setOf(DangerModeCapability.CALL))
    viewModel.onCapabilityToggled(DangerModeCapability.CALL)
    Assert.assertEquals(emptySet<DangerModeCapability>(), viewModel.capabilities.first())
  }

  @Test
  fun `setCurrentModeDangerLevel sets level within bounds`() = runTest {
    viewModel.onDangerLevelChanged(2)
    Assert.assertEquals(2, viewModel.dangerLevel)
    viewModel.onDangerLevelChanged(-1)
    Assert.assertEquals(0, viewModel.dangerLevel)
    viewModel.onDangerLevelChanged(5)
    Assert.assertEquals(3, viewModel.dangerLevel)
  }
}
