package com.github.warnastrophy.core.ui.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import com.github.warnastrophy.core.model.Hazard
import com.github.warnastrophy.core.model.HazardsDataService
import com.github.warnastrophy.core.model.Location
import com.github.warnastrophy.core.model.PositionService
import com.github.warnastrophy.core.ui.components.Loading
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

object MapScreenTestTags {
  const val GOOGLE_MAP_SCREEN = "mapScreen"
}

@Composable
fun MapScreen(
    gpsService: PositionService,
    hazardsService: HazardsDataService,
) {
  val context = LocalContext.current
  val cameraPositionState = rememberCameraPositionState()
  val hazardState by hazardsService.currentHazardsState.collectAsState()
  val positionState by gpsService.positionState.collectAsState()

  LaunchedEffect(Unit) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED) {
      gpsService.requestCurrentLocation()
      gpsService.startLocationUpdates()
    } else {
      throw Exception("Location permission not granted")
    }
  }

  LaunchedEffect(positionState) {
    cameraPositionState.animate(
        CameraUpdateFactory.newLatLngZoom(positionState.position, 12f), 1000 // 1 second animation
        )
  }

  if (!positionState.isLoading)
      GoogleMap(
          modifier = Modifier.fillMaxSize().testTag(MapScreenTestTags.GOOGLE_MAP_SCREEN),
          cameraPositionState = cameraPositionState,
          properties = MapProperties(isMyLocationEnabled = true)) {
            hazardState.forEach { hazard -> HazardMarker(hazard) }
          }
  else Loading()
}

@Composable
private fun HazardMarker(hazard: Hazard) {
  var markerLocation = Location(0.0, 0.0)
  hazard.coordinates?.let {
    // The marker location is the average of all coordinates

    if (it.size > 1) {
      val polygonCoords =
          it.map { coord ->
            markerLocation =
                Location(
                    markerLocation.latitude + coord.latitude,
                    markerLocation.longitude + coord.longitude)
            Location.toLatLng(coord)
          }

      markerLocation =
          Location(markerLocation.latitude / it.size, markerLocation.longitude / it.size)

      Polygon(
          points = polygonCoords,
          strokeWidth = 2f,
      )
    } else {
      markerLocation = it[0]
    }
  }

  MarkerComposable(
      state = rememberMarkerState(position = Location.toLatLng(markerLocation)),
      title = hazard.description,
      snippet = "${hazard.severity} ${hazard.severityUnit}",
  ) {
    val imageVector =
        when (hazard.type) {
          "FL",
          "FF",
          "SS" -> MapIcons.Flood
          "DR" -> MapIcons.Drought
          "EQ",
          "AV",
          "LS",
          "MS" -> MapIcons.Earthquake
          "TC",
          "EC",
          "VW",
          "TO",
          "ST" -> MapIcons.Cyclone
          "FR",
          "WF" -> MapIcons.Fire
          "VO" -> MapIcons.Volcano
          "TS" -> MapIcons.Tsunami
          else -> Icons.Filled.Warning // Default icon
        }
    Icon(imageVector, contentDescription = "Hazard", tint = Color.Black)
  }
}
