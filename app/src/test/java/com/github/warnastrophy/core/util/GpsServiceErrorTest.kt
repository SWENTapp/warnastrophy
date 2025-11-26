package com.github.warnastrophy.core.util

import android.location.Location
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.warnastrophy.core.domain.model.GpsService
import com.github.warnastrophy.core.ui.common.ErrorHandler
import com.github.warnastrophy.core.ui.common.ErrorType
import com.github.warnastrophy.core.ui.common.getScreenErrors
import com.github.warnastrophy.core.ui.navigation.Screen
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class GpsServiceErrorTest {

  private lateinit var locationClient: FusedLocationProviderClient
  private lateinit var errorHandler: ErrorHandler
  private lateinit var gpsService: GpsService

  @Before
  fun setup() {
    // mock the location client (instrumented test - Android classes available)
    locationClient = mock()
    errorHandler = ErrorHandler()
    gpsService = GpsService(locationClient, errorHandler)
    // ensure Looper is prepared (device/emulator already has main looper)
    Looper.getMainLooper()
  }

  @Test
  fun startLocationUpdates_whenSecurityException_addsPermissionError() {
    // requestLocationUpdates throws SecurityException -> LOCATION_NOT_GRANTED_ERROR expected
    doThrow(SecurityException())
        .whenever(locationClient)
        .requestLocationUpdates(any<LocationRequest>(), any<LocationCallback>(), any())

    gpsService.startLocationUpdates()

    assertTrue(
        errorHandler.state.value.errors.any {
          it.type == ErrorType.LOCATION_NOT_GRANTED_ERROR && it.screenType == Screen.Map
        })
  }

  @Test
  fun startLocationUpdates_whenGenericException_addsUpdateError() {
    // requestLocationUpdates throws a generic exception -> LOCATION_UPDATE_ERROR expected
    doThrow(RuntimeException("boom"))
        .whenever(locationClient)
        .requestLocationUpdates(any<LocationRequest>(), any<LocationCallback>(), any())

    gpsService.startLocationUpdates()

    assertTrue(
        errorHandler.state.value.errors.any {
          it.type == ErrorType.LOCATION_UPDATE_ERROR && it.screenType == Screen.Map
        })
  }

  @Test
  fun startLocationUpdates_success_clearsPreviousErrors() {
    // pre-populate handler with both error types for the Map screen
    errorHandler.addError(ErrorType.LOCATION_NOT_GRANTED_ERROR, Screen.Map)
    errorHandler.addError(ErrorType.LOCATION_UPDATE_ERROR, Screen.Map)

    doAnswer { invocation ->
          val callback = invocation.getArgument<LocationCallback>(1)
          val loc =
              Location("mock").apply {
                latitude = 12.34
                longitude = 56.78
              }
          val lr = LocationResult.create(listOf(loc))
          callback.onLocationResult(lr)
          null
        }
        .whenever(locationClient)
        .requestLocationUpdates(any<LocationRequest>(), any<LocationCallback>(), any())

    gpsService.startLocationUpdates()

    verify(locationClient)
        .requestLocationUpdates(any<LocationRequest>(), any<LocationCallback>(), any())

    gpsService.requestCurrentLocation()

    assertTrue(errorHandler.state.value.errors.none { it.screenType == Screen.Map })
  }

  @Test
  fun close_removesLocationUpdates_and_clearsErrors() {
    // pre-populate an error
    errorHandler.addError(ErrorType.LOCATION_ERROR, Screen.Map)

    gpsService.close()

    // removeLocationUpdates should be called with any LocationCallback
    verify(locationClient).removeLocationUpdates(any<LocationCallback>())

    // errors for Screen.Map should be cleared
    assertTrue(errorHandler.state.value.getScreenErrors(Screen.Map).isEmpty())
  }
}
