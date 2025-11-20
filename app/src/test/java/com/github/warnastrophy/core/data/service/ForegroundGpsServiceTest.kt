package com.github.warnastrophy.core.data.service

import android.app.Application
import android.app.Service
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.github.warnastrophy.core.domain.model.ForegroundGpsService
import com.github.warnastrophy.core.domain.model.GpsService
import com.github.warnastrophy.core.domain.model.startForegroundGpsService
import com.github.warnastrophy.core.domain.model.stopForegroundGpsService
import com.github.warnastrophy.core.ui.features.dashboard.DangerModeCardViewModel
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ForegroundGpsServiceTest {

  @Test
  fun onStartCommand_callsStartForegroundLocationUpdatesOnGpsService() {
    val mockGps = mockk<GpsService>(relaxed = true)
    val service = ForegroundGpsService()
    service.setGpsServiceForTest(mockGps)

    val context = ApplicationProvider.getApplicationContext<Context>()
    startForegroundGpsService(context)

    service.onCreate()
    service.onStartCommand(null, 0, 0)

    verify { mockGps.startForegroundLocationUpdates(service) }
  }

  @Test
  fun startForegroundGpsService_startsForegroundServiceIntent() {
    val context = ApplicationProvider.getApplicationContext<Application>()
    startForegroundGpsService(context)

    val started = shadowOf(context).nextStartedService
    assertNotNull("Expected a started service intent", started)
    assertEquals(
        "Started service should be ForegroundGpsService",
        ForegroundGpsService::class.java.name,
        started.component?.className)
  }

  @Test
  fun stopForegroundGpsService_recordsStoppedService() {
    val context = ApplicationProvider.getApplicationContext<Application>()
    startForegroundGpsService(context)

    val started = shadowOf(context).nextStartedService
    assertNotNull(started)

    stopForegroundGpsService(context)
    val stopped = shadowOf(context).nextStoppedService
    assertNotNull("Expected a stopped service intent", stopped)
    assertEquals(
        "Stopped service should be ForegroundGpsService",
        ForegroundGpsService::class.java.name,
        stopped.component?.className)
  }

  @Test
  fun onDestroy_callsClearErrorAndCloseOnInjectedGps() {
    val mockGps = mockk<GpsService>(relaxed = true)
    val service = ForegroundGpsService()
    service.setGpsServiceForTest(mockGps)

    // call lifecycle method under test
    service.onDestroy()

    verify { mockGps.clearErrorMsg() }
    verify { mockGps.close() }
  }

  @Test
  fun onStartCommand_returnsStartSticky_whenNoGpsInjected() {
    val service = ForegroundGpsService()

    val result = service.onStartCommand(null, 0, 0)

    assertEquals(Service.START_STICKY, result)
  }

  @Test
  fun `toggling on starts foreground service`() = runTest {
    var invoked = false
    var receivedContext: Context? = null

    val vm =
        DangerModeCardViewModel(
            startService = { ctx ->
              invoked = true
              receivedContext = ctx
            },
            stopService = { _ -> })

    val context = ApplicationProvider.getApplicationContext<Context>()
    vm.onDangerModeToggled(true, context)

    assertTrue("startService should be invoked", invoked)
    assertEquals("context passed to startService should match", context, receivedContext)
  }
}
