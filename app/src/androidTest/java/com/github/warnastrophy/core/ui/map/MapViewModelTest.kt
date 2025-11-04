package com.github.warnastrophy.core.ui.map

import android.app.Activity
import com.github.warnastrophy.core.model.AppPermissions
import com.github.warnastrophy.core.model.PermissionResult
import com.google.android.gms.maps.model.LatLng
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModelTest {
  private val testDispatcher = StandardTestDispatcher()
  private lateinit var gpsService: GpsServiceMock
  private lateinit var hazardsService: HazardServiceMock
  private lateinit var permissionManager: MockPermissionManager
  private lateinit var viewModel: MapViewModel

  private val mockPerm = AppPermissions.LocationFine
  private val mockPos = LatLng(54.23, 23.23)

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)

    gpsService = GpsServiceMock()
    hazardsService = HazardServiceMock()
    permissionManager = MockPermissionManager()

    viewModel = MapViewModel(gpsService, hazardsService, permissionManager)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun initial_state_has_denied_permission_and_loading_true() = runTest {
    val uiState = viewModel.uiState.value
    assertTrue(uiState.permissionResult is PermissionResult.Denied)
    assertTrue(uiState.isLoading)
  }

  @Test
  fun applyPermissionsResult_updates_permission_state() = runTest {
    val activity = mockk<Activity>(relaxed = true)
    permissionManager.setPermissionResult(PermissionResult.Denied(mockPerm.permissions.toList()))

    viewModel.applyPermissionsResult(activity)

    val newState = viewModel.uiState.value
    assertTrue(newState.permissionResult is PermissionResult.Denied)
    assertFalse(newState.isOsRequestInFlight)
  }

  @Test
  fun setTracking_updates_tracking_state() = runTest {
    viewModel.setTracking(true)
    assertTrue(viewModel.uiState.value.isTrackingLocation)

    viewModel.setTracking(false)
    assertFalse(viewModel.uiState.value.isTrackingLocation)
  }

  @Test
  fun startLocationUpdate_starts_gps_service_methods() = runTest {
    gpsService.setPosition(position = mockPos)

    viewModel.startLocationUpdate()
    testDispatcher.scheduler.advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertTrue(gpsService.isLocationUpdated)
    assertEquals(mockPos, uiState.positionState.position)
  }

  @Test
  fun uiState_updates_when_gpsService_emits_new_data() = runTest {
    gpsService.setPosition(position = mockPos)
    testDispatcher.scheduler.advanceUntilIdle()

    val uiState = viewModel.uiState.value
    assertEquals(mockPos, uiState.positionState.position)
  }

  @Test
  fun stopLocationUpdate_calls_gpsService_methods() = runTest {
    gpsService.stopLocationUpdates()
    assertFalse(gpsService.isLocationUpdated)
  }
}
