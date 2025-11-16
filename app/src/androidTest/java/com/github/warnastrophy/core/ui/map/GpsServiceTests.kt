package com.github.warnastrophy.core.ui.map

import android.location.Location
import com.github.warnastrophy.core.domain.error.ErrorDisplayManager
import com.github.warnastrophy.core.domain.model.GpsResult
import com.github.warnastrophy.core.domain.model.GpsService
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Tasks
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.lang.Thread.sleep
import javax.inject.Inject
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.isActive
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
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever

@HiltAndroidTest
@ExperimentalCoroutinesApi
class GpsServiceTests {
  // private lateinit var context: Context

  @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
  @Inject lateinit var mockClient: FusedLocationProviderClient
  private lateinit var gpsService: GpsService

  @Inject lateinit var errorDisplayManager: ErrorDisplayManager
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    hiltRule.inject()
    Dispatchers.setMain(testDispatcher)
    // mockErrorDisplayManager = mockk<ErrorDisplayManager>()
    // context = ApplicationProvider.getApplicationContext()
    // mockClient = Mockito.mock<FusedLocationProviderClient>()
    gpsService = GpsService(locationClient = mockClient, errorDisplayManager)
  }

  @After
  fun tearDown() {
    gpsService.close()
    Dispatchers.resetMain()
  }

  @Test
  fun requestCurrentLocation_init_location_correctly() = runTest {
    val fake_gps_location =
        Location("test").apply {
          longitude = 2.3486
          latitude = 48.8146
        }

    // Mock location response
    whenever(mockClient.getCurrentLocation(any<CurrentLocationRequest>(), anyOrNull()))
        .thenReturn(Tasks.forResult(fake_gps_location))

    gpsService.requestCurrentLocation()
    advanceUntilIdle() // Fait progresser toutes les coroutines en cours d'exécution
    sleep(3000)
    // Vérifier que l'état est correctement mis à jour
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

    // Simuler une exception
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

    // Récupère le callback privé via la réflexion
    val field = GpsService::class.java.getDeclaredField("locationCallBack")
    field.isAccessible = true
    val callback = field.get(gpsService) as LocationCallback

    // Simule chaque point du trajet
    route.forEach { latLng ->
      val location =
          Location("test").apply {
            latitude = latLng.latitude
            longitude = latLng.longitude
          }
      val locationResult = LocationResult.create(listOf(location))
      callback.onLocationResult(locationResult)

      // Vérifie l'état intermédiaire
      val state = gpsService.positionState.value
      assertEquals(latLng, state.position)
    }

    // Vérifie l'état final
    val finalState = gpsService.positionState.value
    assertEquals(route.last(), finalState.position)
    assertFalse(finalState.isLoading)
    assertNull(finalState.errorMessage)
  }

  @Test
  fun startLocationUpdates_handle_null_location() = runTest {
    // On utilise le mock déjà injecté dans gpsService
    // Lance la demande de mise à jour de la position
    gpsService.startLocationUpdates()
    advanceUntilIdle()

    // Récupère le callback privé via la réflexion
    val field = GpsService::class.java.getDeclaredField("locationCallBack")
    field.isAccessible = true
    val callback = field.get(gpsService) as LocationCallback

    // Simule un résultat de localisation null
    val locationResult = LocationResult.create(listOf(null))
    callback.onLocationResult(locationResult)

    // Vérifie l’état
    val state = gpsService.positionState.value
    assertFalse(state.isLoading)
    assertEquals("No location fix available", state.errorMessage)
  }

  @Test
  fun startLocationUpdates_need_permissions() = runTest {
    // Simuler une exception de sécurité
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

    // Simuler une exception générique
    whenever(mockClient.requestLocationUpdates(any(), any<LocationCallback>(), anyOrNull()))
        .thenThrow(e)

    gpsService.startLocationUpdates()
    advanceUntilIdle()
    sleep(3000)
    val state = gpsService.positionState.value
    // Assert.assertFalse(state.isLoading)
    Assert.assertEquals("Location update failed: ${e.message}", state.errorMessage)
    Assert.assertTrue(state.result is GpsResult.Failed)
  }

  @Test
  fun testSetAndClearErrorMsg() {
    gpsService.clearErrorMsg()
    assertNull(gpsService.positionState.value.errorMessage)
  }

  @Test
  fun testStopLocationUpdate() {
    gpsService.startLocationUpdates()

    val field = GpsService::class.java.getDeclaredField("serviceScope")
    field.isAccessible = true
    val callback = field.get(gpsService) as CoroutineScope

    gpsService.stopLocationUpdates()

    assertFalse("serviceScope should be active before cancellation", callback.isActive)

    val state = gpsService.positionState.value
    assertFalse(state.isLoading)
  }
}
