package com.github.warnastrophy.core.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.ServiceTestRule
import com.github.warnastrophy.core.data.repository.usecase.RefreshHazardsIfMovedUseCase
import com.github.warnastrophy.core.data.service.HazardTrackingService
import com.github.warnastrophy.core.ui.map.GpsServiceMock
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HazardTrackingServiceTest {
  private lateinit var context: Context
  private val mockGpsService = GpsServiceMock()
  private val mockUseCase = mockk<RefreshHazardsIfMovedUseCase>(relaxed = true)
  // Mock Android Context and NotificationManager
  private val mockContext = mockk<Context>(relaxed = true)
  private val mockNotificationManager = mockk<NotificationManager>(relaxed = true)
  @get:Rule val serviceRule = ServiceTestRule()

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    every { mockContext.getSystemService(NotificationManager::class.java) } returns
        mockNotificationManager
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun serviceCallsExecuteWhenGPSUpdates() = runTest {
    val testScope = TestScope(StandardTestDispatcher(testScheduler))

    val service =
        HazardTrackingService(mockGpsService, mockUseCase, testScope, enableForegroud = false)
    service.onCreate()
    advanceUntilIdle()
    verify { mockUseCase.execute(any()) }
    service.stopTrackingForTest()
  }

  /*
  @Test
  fun testStartForegroundService_isCalled_whenEnabled() = runTest (StandardTestDispatcher()){

      // 1. Create a Spy of the service: A spy lets us call the real code while monitoring method calls.
      val serviceSpy = spyk(
          HazardTrackingService(
              testGpsService = mockGpsService,
              testUseCase = mockUseCase,
              enableForegroud = true // IMPORTANT: Ensure foreground is enabled
          )
      )

      // Mock the final system call startForeground() to prevent RuntimeExceptions
      // Since startForeground is a final method on Service, we often need to use deep stubbing or a specialized runner.
      // In a true MockK/Robolectric setup, we would typically mock the Service's protected Context methods,
      // but for simplicity, we treat startForeground as the observable outcome.
      // We'll mock the internal call dependencies instead:
      every { serviceSpy.getSystemService(any()) } returns mockNotificationManager

      // 2. Start the service. This triggers onCreate() and startForegroundService().
      serviceSpy.onCreate()
      testScheduler.advanceUntilIdle()
      serviceSpy.onStartCommand(Intent(), 0, 1) // Call onStartCommand to complete the lifecycle start

      // 3. Verification: Verify that the system-level function `startForeground` was called.
      // The service's protected function 'startForeground' is the actual goal of verification.
      verify(exactly = 1) { serviceSpy.startForeground(eq(1), any()) }
      serviceSpy.onDestroy()
  }

   */
  @Test
  fun start() {
    val serviceIntent =
        Intent(ApplicationProvider.getApplicationContext(), HazardTrackingService::class.java)
    val binder = serviceRule.startService(serviceIntent)
    assertNotNull(binder)
  }
}
