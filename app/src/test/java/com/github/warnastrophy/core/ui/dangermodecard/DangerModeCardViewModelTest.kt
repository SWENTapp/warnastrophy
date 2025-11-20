package com.github.warnastrophy.core.ui.dangermodecard

import androidx.test.core.app.ApplicationProvider
import com.github.warnastrophy.core.data.service.ServiceStateManager
import com.github.warnastrophy.core.ui.features.dashboard.DangerModeCapability
import com.github.warnastrophy.core.ui.features.dashboard.DangerModeCardViewModel
import com.github.warnastrophy.core.ui.features.dashboard.DangerModePreset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class DangerModeCardViewModelTest {
  private lateinit var viewModel: DangerModeCardViewModel

  @Before
  fun setup() {
    ServiceStateManager.init(ApplicationProvider.getApplicationContext())
    viewModel = DangerModeCardViewModel()
  }

  @Test
  fun `isDangerModeEnabled initial state is false`() = runTest {
    assertEquals(false, viewModel.isDangerModeEnabled.first())
  }

  @Test
  fun `onDangerModeToggled updates isDangerModeEnabled`() = runTest {
    viewModel.onDangerModeToggled(true)
    assertEquals(true, viewModel.isDangerModeEnabled.first())

    viewModel.onDangerModeToggled(false)
    assertEquals(false, viewModel.isDangerModeEnabled.first())
  }

  @Test
  fun `currentModeName initial state is DEFAULT_MODE`() = runTest {
    assertEquals(DangerModePreset.DEFAULT_MODE, viewModel.currentMode.first())
  }

  @Test
  fun `onModeSelected updates currentModeName`() = runTest {
    viewModel.onModeSelected(DangerModePreset.HIKING_MODE)
    assertEquals(DangerModePreset.HIKING_MODE, viewModel.currentMode.first())

    viewModel.onModeSelected(DangerModePreset.DEFAULT_MODE)
    assertEquals(DangerModePreset.DEFAULT_MODE, viewModel.currentMode.first())
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
    assertEquals(setOf(DangerModeCapability.CALL), viewModel.capabilities.first())
    viewModel.onCapabilityToggled(DangerModeCapability.CALL)
    assertEquals(emptySet<DangerModeCapability>(), viewModel.capabilities.first())
  }

  @Test
  fun `setCurrentModeDangerLevel sets level within bounds`() = runTest {
    viewModel.onDangerLevelChanged(2)
    assertEquals(2, viewModel.dangerLevel.first())
    viewModel.onDangerLevelChanged(-1)
    assertEquals(0, viewModel.dangerLevel.first())
    viewModel.onDangerLevelChanged(5)
    assertEquals(3, viewModel.dangerLevel.first())
  }
}
