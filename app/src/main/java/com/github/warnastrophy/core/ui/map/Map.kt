package com.github.warnastrophy.core.ui.map

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.warnastrophy.core.model.util.Location
import com.github.warnastrophy.core.ui.components.Loading
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

object MapScreenTestTags {
  const val GOOGLE_MAP_SCREEN = "mapScreen"
}

@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()
  val context = LocalContext.current
  val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
  val cameraPositionState = rememberCameraPositionState()

  LaunchedEffect(Unit) {
    if (viewModel.isLocationPermissionGranted(context)) {
      viewModel.requestCurrentLocation(locationClient) // Request location once at start (initial)
      viewModel.startLocationUpdates(locationClient) // Start continuous location updates
    }
  }

  val hazardsList = uiState.hazards ?: emptyList()

  LaunchedEffect(uiState.target) {
    cameraPositionState.animate(
        CameraUpdateFactory.newLatLngZoom(uiState.target, 12f), 1000 // 1 second animation
        )
  }

  if (!uiState.isLoading)
      GoogleMap(
          modifier = Modifier.fillMaxSize().testTag(MapScreenTestTags.GOOGLE_MAP_SCREEN),
          cameraPositionState = cameraPositionState,
          properties =
              MapProperties(isMyLocationEnabled = viewModel.isLocationPermissionGranted(context))) {
            Log.d("Log", "Rendering ${hazardsList.size} hazards on the map")
            hazardsList.forEach { hazard ->
              hazard.coordinates?.forEach { coord ->
                val loc = Location.toLatLng(coord)
                Marker(
                    state = MarkerState(position = loc),
                    title = "Marker in Haiti",
                    snippet = "Lat: ${loc.latitude}, Lng: ${loc.longitude}",
                    icon =
                        when (hazard.type) {
                          "FL" ->
                              BitmapDescriptorFactory.defaultMarker(
                                  BitmapDescriptorFactory.HUE_GREEN)
                          "DR" ->
                              BitmapDescriptorFactory.defaultMarker(
                                  BitmapDescriptorFactory.HUE_ORANGE)
                          "WC" ->
                              BitmapDescriptorFactory.defaultMarker(
                                  BitmapDescriptorFactory.HUE_BLUE)
                          "EQ" ->
                              BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                          "TC" ->
                              BitmapDescriptorFactory.defaultMarker(
                                  BitmapDescriptorFactory.HUE_YELLOW)
                          else ->
                              BitmapDescriptorFactory.defaultMarker(
                                  BitmapDescriptorFactory.HUE_AZURE)
                        },
                )
              }
            }
          }
  else Loading()
}
