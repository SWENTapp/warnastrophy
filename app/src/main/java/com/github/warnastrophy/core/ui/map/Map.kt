package com.github.warnastrophy.core.ui.map

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
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.warnastrophy.core.ui.map.MapScreenTestTags.GOOGLE_MAP_SCREEN
import com.github.warnastrophy.core.ui.map.MapScreenTestTags.getTestTagForLocationMarker
import com.github.warnastrophy.core.ui.navigation.Screen
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

object MapScreenTestTags {
  const val GOOGLE_MAP_SCREEN = "mapScreen"

  fun getTestTagForLocationMarker(index: Int): String = "locationMarker_$index"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
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
            modifier = Modifier.fillMaxSize().padding(pd).testTag(GOOGLE_MAP_SCREEN),
            cameraPositionState = cameraPositionState) {
              uiState.locations.forEachIndexed { i, loc ->
                Marker(
                    tag = Modifier.testTag(getTestTagForLocationMarker(i)),
                    state = MarkerState(loc),
                    title = "Marker in Haiti",
                    snippet = "Lat: ${loc.latitude}, Lng: ${loc.longitude}",
                )
              }
            }
      })
}
