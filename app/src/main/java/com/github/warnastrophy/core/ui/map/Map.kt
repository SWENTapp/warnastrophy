package com.github.warnastrophy.core.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
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
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlin.compareTo

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
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED) {
      viewModel.requestCurrentLocation(locationClient) // Request location once at start (initial)
      viewModel.startLocationUpdates(locationClient) // Start continuous location updates
    } else {
      throw Exception("Location permission not granted")
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
          properties = MapProperties(isMyLocationEnabled = true)) {
            Log.d("Log", "Rendering ${hazardsList.size} hazards on the map")
            hazardsList.forEach { hazard ->
              val points = hazard.polygon?.map { Location.toLatLng(it) } ?: emptyList()

              if (points.size >= 3) {
                val color =
                    when (hazard.type) {
                      "DR" -> Color(0x80FFA500)
                      "WC" -> Color(0x800000FF)
                      "EQ" -> Color(0x80FF0000)
                      "TC" -> Color(0x80FFFF00)
                      else -> Color(0x80FFFFFF)
                    }
                Polygon(
                    points = points, strokeColor = Color.Black, strokeWidth = 4f, fillColor = color)
              } else if (points.size >= 2) {
                // If only a line, draw a polyline
                Polyline(points = points, color = Color.Black, width = 4f)
              }

              // Draw connecting polyline in addition to polygon (optional)
              if (points.size >= 2) {
                Polyline(
                    points = points + points.first(),
                    color = Color.DarkGray,
                    width = 2f,
                    geodesic = false)
              }

              val coord = hazard.coordinates ?: return@forEach
              val loc = Location.toLatLng(coord)
              Marker(
                  state = MarkerState(position = loc),
                  title = hazard.htmlDescription,
                  snippet = "Lat: ${loc.latitude}, Lng: ${loc.longitude}",
                  icon =
                      when (hazard.type) {
                        "FL" ->
                            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                        "DR" ->
                            BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_ORANGE)
                        "WC" ->
                            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                        "EQ" ->
                            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                        "TC" ->
                            BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_YELLOW)
                        else ->
                            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                      },
              )
            }
          }
  else Loading()
}
