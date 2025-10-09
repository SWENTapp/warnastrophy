package com.github.warnastrophy.core.ui.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

object MapScreenTestTags {
  const val GOOGLE_MAP_SCREEN = "mapScreen"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()

  val cameraPositionState = rememberCameraPositionState {
    position = CameraPosition.fromLatLngZoom(uiState.target, 10f)
  }

  GoogleMap(modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState) {
    uiState.locations.forEach { loc ->
      Marker(
          state = MarkerState(loc),
          title = "Marker in Haiti",
          snippet = "Lat: ${loc.latitude}, Lng: ${loc.longitude}",
      )
    }
  }
}
