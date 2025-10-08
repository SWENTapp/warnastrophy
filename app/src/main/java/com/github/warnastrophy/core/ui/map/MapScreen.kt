package com.github.warnastrophy.core.ui.map

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.warnastrophy.core.ui.navigation.Screen
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

class MapHaitiActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { HaitiMap() }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HaitiMap(
    viewModel: MapViewModel = viewModel(),
) {
  val uiState by viewModel.uiState.collectAsState()

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text(Screen.MAP.name) },
        )
      },
      content = { pd ->
        val cameraPositionState = rememberCameraPositionState {
          position = CameraPosition.fromLatLngZoom(uiState.target, 10f)
        }
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
