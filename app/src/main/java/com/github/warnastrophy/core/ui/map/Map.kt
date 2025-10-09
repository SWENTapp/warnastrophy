package com.github.warnastrophy.core.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

class MapHaitiActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { MapScreen() }
  }
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
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED) {
      viewModel.requestCurrentLocation(locationClient)
    } else {
      println("Location permission not granted")
    }
  }

  LaunchedEffect(uiState.target) {
    cameraPositionState.animate(
        CameraUpdateFactory.newLatLngZoom(uiState.target, 12f), 1000 // 1 second animation
        )
  }

  if (!uiState.isLoading)
      GoogleMap(modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState) {
        uiState.locations.forEach { loc ->
          Marker(
              state = MarkerState(loc),
              title = "Marker in Haiti",
              snippet = "Lat: ${loc.latitude}, Lng: ${loc.longitude}",
          )
        }

        Marker(
            state = MarkerState(uiState.target),
            title = "You are here",
            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
        )
      }
  else Text("Loading...")
}
