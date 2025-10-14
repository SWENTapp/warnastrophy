package com.github.warnastrophy.core.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import androidx.test.core.app.ApplicationProvider
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class MapViewModelTest {
  private lateinit var context: Context
  private lateinit var fusedClient: FusedLocationProviderClient
  private lateinit var viewModel: MapViewModel
  private val testDispatcher = StandardTestDispatcher()

  @SuppressLint("MissingPermission")
  @Before
  fun setup() = runBlocking {
    Dispatchers.setMain(testDispatcher)
    context = ApplicationProvider.getApplicationContext()
    fusedClient = LocationServices.getFusedLocationProviderClient(context)

    viewModel = MapViewModel()
  }

  @SuppressLint("MissingPermission")
  @After
  fun tearDown() {
    fusedClient.setMockMode(false)
    Dispatchers.resetMain()
  }

  /**
   * Tests that `requestCurrentLocation` correctly updates the ViewModel's UI state with the
   * device's current location. Here, we use a **mock** `FusedLocationProviderClient` and do not
   * rely on actual device location services.
   *
   * This test simulates a successful location fetch using a mock `FusedLocationProviderClient`. It
   * verifies that:
   * 1. The `target` in the `uiState` is updated to the coordinates returned by the mock client.
   * 2. The `isLoading` state is set to `false` after the location is fetched.
   */
  @SuppressLint("MissingPermission")
  @Test
  fun requestCurrentLocation_init_location_correctly() = runTest {
    val mockClient = mock<FusedLocationProviderClient>()

    val fake_gps_location_1 =
        Location("test").apply {
          longitude = 2.3486
          latitude = 48.8146
        }

    // Mock location response
    whenever(mockClient.getCurrentLocation(any<CurrentLocationRequest>(), anyOrNull()))
        .thenReturn(Tasks.forResult(fake_gps_location_1))

    viewModel.requestCurrentLocation(mockClient)
    advanceUntilIdle() // advances all running coroutines to completion

    // Assert _uiState is updated with correct target coordinates
    val state = viewModel.uiState.value
    assertEquals(LatLng(fake_gps_location_1.latitude, fake_gps_location_1.longitude), state.target)
    assertFalse(state.isLoading)
  }

  /**
   * Tests the `startLocationUpdates` function by simulating a user traveling along a predefined
   * route.
   *
   * This test verifies that the `MapViewModel` correctly processes a series of location updates
   * received from the `FusedLocationProviderClient`. It mocks the client to immediately send a
   * sequence of locations. The test then asserts that the `uiState` of the ViewModel is correctly
   * updated, with the final `target` being the last location in the simulated route, `isLoading`
   * being false, and `errorMsg` being null.
   */
  @Test
  fun startLocationUpdates_simulatesUserTravel() = runTest {
    val mockClient = mock<FusedLocationProviderClient>()
    val route = listOf(LatLng(47.3769, 8.5417), LatLng(47.3745, 8.5425), LatLng(47.3720, 8.5433))

    val callbackCaptor = argumentCaptor<LocationCallback>()

    // Mock requestLocationUpdates to capture callback
    doAnswer {
          // callback is the second parameter
          val callback = it.getArgument<LocationCallback>(1)
          // For test demonstration, immediately send all locations
          route.forEach { latLng ->
            val location =
                Location("test").apply {
                  latitude = latLng.latitude
                  longitude = latLng.longitude
                }
            val locationResult = LocationResult.create(listOf(location))
            callback.onLocationResult(locationResult)
          }
          null
        }
        .whenever(mockClient)
        .requestLocationUpdates(any(), callbackCaptor.capture(), anyOrNull())

    viewModel.startLocationUpdates(mockClient)

    // Wait for updates if needed; then check final state
    advanceUntilIdle()

    val finalState = viewModel.uiState.value

    // The final uiState target should be the last point of the route
    assertEquals(route.last(), finalState.target)
    assertFalse(finalState.isLoading)
    assertNull(finalState.errorMsg)
  }

  /**
   * Verifies that the ViewModel correctly handles a `SecurityException` when starting location
   * updates.
   *
   * This test simulates a scenario where location permissions have not been granted. It mocks the
   * `FusedLocationProviderClient` to throw a `SecurityException` when `requestLocationUpdates` is
   * called. It then asserts that the ViewModel's UI state is updated to reflect this error,
   * specifically by setting an appropriate error message and ensuring the loading state is false.
   */
  @Test
  fun startLocationUpdates_need_permissions() = runTest {
    val mockClient = mock<FusedLocationProviderClient>()
    val callbackCaptor = argumentCaptor<LocationCallback>()

    // Mock requestLocationUpdates to throw SecurityException
    whenever(mockClient.requestLocationUpdates(any(), callbackCaptor.capture(), anyOrNull()))
        .thenThrow(SecurityException())

    viewModel.startLocationUpdates(mockClient)
    advanceUntilIdle() // advances all running coroutines to completion

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals("Location permission not granted !", state.errorMsg)
  }

  /**
   * Tests the behavior of `startLocationUpdates` when the underlying `requestLocationUpdates` call
   * from the `FusedLocationProviderClient` throws a generic `RuntimeException`.
   *
   * This test verifies that:
   * 1. The `ViewModel` correctly catches the exception.
   * 2. The UI state is updated to reflect that loading has finished (`isLoading` is false).
   * 3. An appropriate error message, derived from the exception, is set in the UI state.
   */
  @Test
  fun startLocationUpdates_throw_exception() = runTest {
    val mockClient = mock<FusedLocationProviderClient>()
    val callbackCaptor = argumentCaptor<LocationCallback>()
    val e = RuntimeException("Something went wrong")

    // Mock requestLocationUpdates to throw Exception
    whenever(mockClient.requestLocationUpdates(any(), callbackCaptor.capture(), anyOrNull()))
        .thenThrow(e)

    viewModel.startLocationUpdates(mockClient)
    advanceUntilIdle() // advances all running coroutines to completion

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals("Location update failed: ${e.message}", state.errorMsg)
  }

  @Test
  fun testSetAndClearErrorMsg() {
    viewModel.clearErrorMsg()
    assertNull(viewModel.uiState.value.errorMsg)
  }
}
