package com.github.warnastrophy.core.data.service

import com.github.warnastrophy.core.model.Location
import com.github.warnastrophy.core.util.GpsServiceMock
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
import org.junit.Test

class azardTrackingServiceTest {
  private val mockGpsService = GpsServiceMock()
  private val mockUseCase = mockk<RefreshHazardsIfMovedService>(relaxed = true)

  private lateinit var trackingService: HazardTrackingService

  private val testDispatcher = StandardTestDispatcher()
  private val testScope = TestScope(testDispatcher + SupervisorJob())

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
    val latLng1 = LatLng(10.0, 20.0)
    val latLng2 = LatLng(30.0, 40.0)

    trackingService.startTracking()
    testDispatcher.scheduler.runCurrent()

    mockGpsService.setPosition(latLng1)
    testDispatcher.scheduler.runCurrent()
    coVerify(exactly = 1) { mockUseCase.execute(Location(latLng1.latitude, latLng1.longitude)) }

    trackingService.stopTracking()

    mockGpsService.setPosition(latLng2)
    testDispatcher.scheduler.runCurrent()

    coVerify(exactly = 2) { mockUseCase.execute(any()) }
  }
}
