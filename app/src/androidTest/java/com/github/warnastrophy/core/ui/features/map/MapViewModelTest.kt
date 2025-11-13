package com.github.warnastrophy.core.ui.features.map

import android.app.Activity
import com.github.warnastrophy.core.data.permissions.AppPermissions
import com.github.warnastrophy.core.data.permissions.PermissionResult
import com.github.warnastrophy.core.util.GpsServiceMock
import com.github.warnastrophy.core.util.HazardServiceMock
import com.github.warnastrophy.core.util.MockPermissionManager
import com.github.warnastrophy.core.util.createHazard
import com.google.android.gms.maps.model.LatLng
import io.mockk.mockk
import junit.framework.TestCase
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
    println(viewModel.uiState.value.hazardState.hazards)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  /**
   * Verifies that the initial state of the ViewModel's UI state has denied permissions and is in a
   * loading state.
   */
  @Test
  fun initial_state_has_denied_permission_and_loading_true() = runTest {
    val uiState = viewModel.uiState.value
    TestCase.assertTrue(uiState.permissionResult is PermissionResult.Denied)
    TestCase.assertTrue(uiState.isLoading)
  }
  /** Tests if applying a permission result correctly updates the UI state. */
  @Test
  fun applyPermissionsResult_updates_permission_state() = runTest {
    val activity = mockk<Activity>(relaxed = true)
    permissionManager.setPermissionResult(PermissionResult.Denied(mockPerm.permissions.toList()))

    viewModel.applyPermissionsResult(activity)

    val newState = viewModel.uiState.value
    TestCase.assertTrue(newState.permissionResult is PermissionResult.Denied)
    TestCase.assertFalse(newState.isOsRequestInFlight)
  }

  /** Checks if the tracking state in the UI is correctly updated when `setTracking` is called. */
  @Test
  fun setTracking_updates_tracking_state() = runTest {
    viewModel.setTracking(true)
    TestCase.assertTrue(viewModel.uiState.value.isTrackingLocation)

    viewModel.setTracking(false)
    TestCase.assertFalse(viewModel.uiState.value.isTrackingLocation)
  }

  /**
   * Ensures that starting location updates triggers the GPS service and updates the position in the
   * UI state.
   */
  @Test
  fun startLocationUpdate_starts_gps_service_methods() = runTest {
    gpsService.setPosition(position = mockPos)

    viewModel.startLocationUpdate()
    testDispatcher.scheduler.advanceUntilIdle()

    val uiState = viewModel.uiState.value
    TestCase.assertTrue(gpsService.isLocationUpdated)
    TestCase.assertEquals(mockPos, uiState.positionState.position)
  }

  /** Verifies that the UI state is updated when the GPS service emits new location data. */
  @Test
  fun uiState_updates_when_gpsService_emits_new_data() = runTest {
    gpsService.setPosition(position = mockPos)
    testDispatcher.scheduler.advanceUntilIdle()

    val uiState = viewModel.uiState.value
    TestCase.assertEquals(mockPos, uiState.positionState.position)
  }

  /**
   * Tests if stopping location updates correctly calls the corresponding method in the GPS service.
   */
  @Test
  fun stopLocationUpdate_calls_gpsService_methods() = runTest {
    gpsService.stopLocationUpdates()
    testDispatcher.scheduler.advanceUntilIdle()
    TestCase.assertFalse(gpsService.isLocationUpdated)
  }

  /**
   * Verifies that the UI state correctly combines and reflects updates from both the GPS and
   * hazards services.
   */
  @Test
  fun uiState_correctly_combines_updates_from_both_gpsService_and_hazardsService() = runTest {
    val initialHazards =
        listOf(
            createHazard(type = "Flood", severity = 0.8),
            createHazard(type = "Flood", severity = 0.3))
    val newHazards =
        listOf(
            createHazard(type = "Water", severity = 0.5),
            createHazard(type = "Water", severity = 1.0),
            createHazard(type = "Fire", severity = 2.0),
            createHazard(type = "Fire", severity = 3.5),
        )
    val newPosition = LatLng(10.0, 20.0)

    // Set initial state for hazards
    hazardsService.setHazards(initialHazards)
    // Advance the dispatcher to process the initial hazard state from combine
    testDispatcher.scheduler.advanceUntilIdle()

    // --- Assert initial state before acting ---
    val initialState = viewModel.uiState.value
    TestCase.assertEquals(
        "Initial hazard count should be correct", 2, initialState.hazardState.hazards.size)
    TestCase.assertEquals(
        "Initial severities for Flood should be calculated",
        Pair(0.3, 0.8),
        initialState.severitiesByType["Flood"])

    // Emit new values from services. This will trigger the combine logic again.
    gpsService.setPosition(position = newPosition)
    hazardsService.setHazards(newHazards)

    // Advance the dispatcher
    testDispatcher.scheduler.advanceUntilIdle()

    val finalState = viewModel.uiState.value

    // Verify position state was updated
    TestCase.assertFalse("Position should no longer be loading", finalState.positionState.isLoading)
    TestCase.assertEquals(
        "Position should be updated to the new value",
        newPosition,
        finalState.positionState.position)

    // Verify hazard state was updated
    TestCase.assertEquals(
        "Hazard list should be updated to the new list",
        newHazards.size,
        finalState.hazardState.hazards.size)
    TestCase.assertEquals(
        "Hazard data should match the new hazards", newHazards, finalState.hazardState.hazards)

    // Verify the derived 'severities' state was re-calculated and updated
    TestCase.assertEquals(
        "Severities for Flood should be updated",
        Pair(0.5, 1.0),
        finalState.severitiesByType["Water"])
    TestCase.assertEquals(
        "Severities for Fire should be updated",
        Pair(2.0, 3.5),
        finalState.severitiesByType["Fire"])
    TestCase.assertFalse(
        "Old severity key 'Flood' should no longer exist",
        finalState.severitiesByType.containsKey("Flood"))
  }

  /** Ensures that severities from hazards are correctly combined and calculated in the UI state. */
  @Test
  fun uiState_correctly_combines_severities() = runTest {
    val hazards =
        listOf(
            createHazard(type = "Flood", severity = 0.8),
            createHazard(type = "Flood", severity = 0.3), // Same type, different severity
            createHazard(type = "Fire", severity = 2.0),
            createHazard(type = "Fire", severity = 3.5), // Same type, different severity
            createHazard(type = null, severity = 1.0), // Null type, should be ignored
            createHazard(type = "EQ", severity = null), // Null severity, should be ignored
            createHazard(type = "Storm", severity = 1.2) // Single hazard type
            )

    val expectedResult =
        mapOf(
            "Flood" to (0.3 to 0.8), // min and max severity for Flood
            "Fire" to (2.0 to 3.5),
            "Storm" to (1.2 to 1.2) // Only one severity, min=max=1.2
            )

    // Set initial state for hazards
    hazardsService.setHazards(hazards)
    // Advance the dispatcher to process the initial hazard state from combine
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value

    TestCase.assertEquals(
        "Hazard count should be correct", hazards.size, state.hazardState.hazards.size)
    TestCase.assertEquals("Severities should be correct", expectedResult, state.severitiesByType)
  }

  /**
   * Checks if the `isOsRequestInFlight` flag is correctly set when a permission request is
   * initiated.
   */
  @Test
  fun onPermissionsRequestStart_works_as_expected() = runTest {
    TestCase.assertFalse(
        "The OS pop-up should not be displayed", viewModel.uiState.value.isOsRequestInFlight)
    viewModel.onPermissionsRequestStart()
    TestCase.assertTrue(
        "The OS pop-up should be displayed", viewModel.uiState.value.isOsRequestInFlight)
  }
}
