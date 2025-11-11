package com.github.warnastrophy.core.service

import androidx.test.rule.ServiceTestRule
import com.github.warnastrophy.core.data.repository.usecase.RefreshHazardsIfMovedUseCase
import com.github.warnastrophy.core.data.service.HazardTrackingService
import com.github.warnastrophy.core.model.Location
import com.github.warnastrophy.core.ui.map.GpsServiceMock
import com.google.android.gms.maps.model.LatLng
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HazardTrackingServiceTest {
  private val mockGpsService = GpsServiceMock()
  private val mockUseCase = mockk<RefreshHazardsIfMovedUseCase>(relaxed = true)

  private lateinit var trackingService: HazardTrackingService

  private val testDispatcher = StandardTestDispatcher()
  private val testScope = TestScope(testDispatcher + SupervisorJob())
  @get:Rule val serviceRule = ServiceTestRule()

  @Before
  fun setup() {
    trackingService = HazardTrackingService(mockGpsService, mockUseCase, testScope)
  }

  @After
  fun tearDown() {
    trackingService.stopTracking()
    clearAllMocks()
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun serviceCallsExecuteWhenGPSUpdates() = runTest {
    trackingService.startTracking()
    testDispatcher.scheduler.runCurrent()

    coVerify(exactly = 1) { mockUseCase.execute(any()) }
    mockGpsService.setPosition(LatLng(20.0, 20.0))
    testDispatcher.scheduler.runCurrent()
    coVerify(exactly = 1) { mockUseCase.execute(Location(20.0, 20.0)) }
  }

  @Test
  fun stopTracking_cancelsUpdates() = runTest {
    // ARRANGE
    val latLng1 = LatLng(10.0, 20.0)
    val latLng2 = LatLng(30.0, 40.0)

    trackingService.startTracking()
    testDispatcher.scheduler.runCurrent()

    // 1. First update (should execute use case once for initial value, second for latLng1)
    mockGpsService.setPosition(latLng1)
    testDispatcher.scheduler.runCurrent()
    coVerify(exactly = 1) { mockUseCase.execute(Location(latLng1.latitude, latLng1.longitude)) }

    // ACT: Stop tracking
    trackingService.stopTracking()

    // 2. Second update (should NOT execute use case)
    mockGpsService.setPosition(latLng2)
    testDispatcher.scheduler.runCurrent()

    // ASSERT: The use case count remains 2
    coVerify(exactly = 2) { mockUseCase.execute(any()) }
  }
}
