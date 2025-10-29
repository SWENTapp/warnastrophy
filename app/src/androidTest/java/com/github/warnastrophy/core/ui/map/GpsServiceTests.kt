package com.github.warnastrophy.core.ui.map

import android.Manifest
import android.content.Context
import android.location.Location
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.GrantPermissionRule
import com.github.warnastrophy.core.model.GpsResult
import com.github.warnastrophy.core.model.GpsService
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Tasks
import java.lang.Thread.sleep
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class GpsServiceTests {

  @get:Rule
  val permissionRule: GrantPermissionRule =
      GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION)

  private lateinit var context: Context
  private lateinit var mockClient: FusedLocationProviderClient
  private lateinit var gpsService: GpsService
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    context = ApplicationProvider.getApplicationContext()
    mockClient = Mockito.mock(FusedLocationProviderClient::class.java)
    // La permission est accordée par la rule, donc la création ne jette plus.
    gpsService = GpsService(locationClient = mockClient, context = context)
  }

  @After
  fun tearDown() {
    if (::gpsService.isInitialized) {
      gpsService.close()
    }
    Dispatchers.resetMain()
  }

  @Test
  fun requestCurrentLocation_init_location_correctly() = runTest {
    val fake_gps_location =
        Location("test").apply {
          longitude = 2.3486
          latitude = 48.8146
        }

    whenever(mockClient.getCurrentLocation(any<CurrentLocationRequest>(), anyOrNull()))
        .thenReturn(Tasks.forResult(fake_gps_location))

    gpsService.requestCurrentLocation()
    advanceUntilIdle()
    sleep(3000)

    val state = gpsService.positionState.value
    Assert.assertEquals(
        LatLng(fake_gps_location.latitude, fake_gps_location.longitude), state.position)
    Assert.assertFalse(state.isLoading)
    Assert.assertTrue(state.result is GpsResult.Success)
    Assert.assertNull(state.errorMessage)
  }

  @Test
  fun requestCurrentLocation_throw_exception() = runTest {
    val e = RuntimeException("Quelque chose s'est mal passé")
    whenever(mockClient.getCurrentLocation(any<CurrentLocationRequest>(), anyOrNull())).thenThrow(e)

    gpsService.requestCurrentLocation()
    advanceUntilIdle()
    sleep(3000)

    val state = gpsService.positionState.value
    Assert.assertFalse(state.isLoading)
    Assert.assertEquals("Location error: ${e.message}", state.errorMessage)
    Assert.assertTrue(state.result is GpsResult.Failed)
  }

  @Test
  fun startLocationUpdates_simulatesUserTravel() = runTest {
    val route = listOf(LatLng(47.3769, 8.5417), LatLng(47.3745, 8.5425), LatLng(47.3720, 8.5433))

    gpsService.startLocationUpdates()
    advanceUntilIdle()

    val field = GpsService::class.java.getDeclaredField("locationCallBack")
    field.isAccessible = true
    val callback = field.get(gpsService) as LocationCallback

    route.forEach { latLng ->
      val location =
          Location("test").apply {
            latitude = latLng.latitude
            longitude = latLng.longitude
          }
      val locationResult = LocationResult.create(listOf(location))
      callback.onLocationResult(locationResult)

      val state = gpsService.positionState.value
      assertEquals(latLng, state.position)
    }

    val finalState = gpsService.positionState.value
    assertEquals(route.last(), finalState.position)
    assertFalse(finalState.isLoading)
    assertNull(finalState.errorMessage)
  }

  @Test
  fun startLocationUpdates_handle_null_location() = runTest {
    gpsService.startLocationUpdates()
    advanceUntilIdle()

    val field = GpsService::class.java.getDeclaredField("locationCallBack")
    field.isAccessible = true
    val callback = field.get(gpsService) as LocationCallback

    val locationResult = LocationResult.create(listOf(null))
    callback.onLocationResult(locationResult)

    val state = gpsService.positionState.value
    assertFalse(state.isLoading)
    assertEquals("No location fix available", state.errorMessage)
  }

  @Test
  fun startLocationUpdates_need_permissions() = runTest {
    whenever(mockClient.requestLocationUpdates(any(), any<LocationCallback>(), anyOrNull()))
        .thenThrow(SecurityException())

    gpsService.startLocationUpdates()
    advanceUntilIdle()
    sleep(3000)

    val state = gpsService.positionState.value
    Assert.assertFalse(state.isLoading)
    Assert.assertEquals("Location permission not granted!", state.errorMessage)
    Assert.assertTrue(state.result is GpsResult.Failed)
  }

  @Test
  fun startLocationUpdates_throw_exception() = runTest {
    val e = RuntimeException("Location: Something went wrong")
    whenever(mockClient.requestLocationUpdates(any(), any<LocationCallback>(), anyOrNull()))
        .thenThrow(e)

    gpsService.startLocationUpdates()
    advanceUntilIdle()
    sleep(3000)

    val state = gpsService.positionState.value
    Assert.assertEquals("Location update failed: ${e.message}", state.errorMessage)
    Assert.assertTrue(state.result is GpsResult.Failed)
  }

  @Test
  fun testSetAndClearErrorMsg() {
    gpsService.clearErrorMsg()
    assertNull(gpsService.positionState.value.errorMessage)
  }
}
