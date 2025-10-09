package com.github.warnastrophy.core.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.warnastrophy.core.ui.navigation.Screen
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
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

@OptIn(ExperimentalMaterial3Api::class)
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
      viewModel.updateUserLocation(locationClient)
    } else {
      println("Location permission not granted")
    }
  }

  LaunchedEffect(uiState.target) {
    cameraPositionState.animate(
        CameraUpdateFactory.newLatLngZoom(uiState.target, 12f), 1000 // 1 second animation
        )
  }

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text(Screen.MAP.name) },
        )
      },
      content = { pd ->
        GoogleMap(
            modifier = Modifier.fillMaxSize().padding(pd),
            cameraPositionState = cameraPositionState) {
              uiState.locations.forEach { loc ->
                Marker(
                    state = MarkerState(loc),
                    title = "Marker in Haiti",
                    snippet = "Lat: ${loc.latitude}, Lng: ${loc.longitude}",
                )
              }
            }
      })
}
