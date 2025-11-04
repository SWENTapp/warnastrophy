package com.github.warnastrophy.core.ui.map

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.github.warnastrophy.core.model.GpsPositionState
import com.github.warnastrophy.core.model.PermissionResult
import com.github.warnastrophy.core.ui.components.Loading
import com.github.warnastrophy.core.ui.components.PermissionRequestCard
import com.github.warnastrophy.core.ui.components.PermissionUiTags
import com.github.warnastrophy.core.util.findActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState

object MapScreenTestTags {
  const val GOOGLE_MAP_SCREEN = "mapScreen"
  const val FALLBACK_ACTIVITY_ERROR = "fallbackActivityError"
  const val TRACK_LOCATION_BUTTON = "trackLocationButton"
}

@Composable
fun MapScreen(
    viewModel: MapViewModel,
    cameraPositionState: CameraPositionState = rememberCameraPositionState()
) {
  val activity = LocalContext.current.findActivity()

  if (activity == null) {
    // safe fallback UI. It prevents a crash and informs the developer.
    Box(
        modifier = Modifier.fillMaxSize().testTag(MapScreenTestTags.FALLBACK_ACTIVITY_ERROR),
        contentAlignment = Alignment.Center) {
          Text("Error: Map screen cannot function without an Activity context.")
        }
    // Stop executing the rest of the composable.
    return
  }

  val uiState by viewModel.uiState.collectAsState()

  val launcher =
      rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
        viewModel.applyPermissionsResult(activity)
      }

  val requestPermissions: () -> Unit = {
    viewModel.onPermissionsRequestStart()
    launcher.launch(viewModel.locationPermissions.permissions)
  }

  /**
   * This effect starts or stops location updates based on whether the location permission has been
   * granted.
   */
  LaunchedEffect(uiState.isGranted) {
    if (uiState.isGranted) {
      viewModel.startLocationUpdate()
    } else {
      viewModel.stopLocationUpdate()
    }
  }

  /**
   * When tracking is enabled, this effect is triggered by changes in the user's location. It
   * animates the camera to center on the new position.
   */
  LaunchedEffect(uiState.isTrackingLocation, uiState.positionState.position, uiState.isGranted) {
    if (uiState.isGranted && !uiState.isLoading && uiState.isTrackingLocation) {
      defaultAnimate(cameraPositionState, uiState.positionState)
    }
  }

  /**
   * Triggered when the user starts moving the map, which stops tracking the user's location. This
   * allows the user to freely explore the map.
   */
  LaunchedEffect(cameraPositionState.isMoving, cameraPositionState.cameraMoveStartedReason) {
    if (cameraPositionState.isMoving &&
        cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE) {
      viewModel.setTracking(false)
    }
  }

  Box(Modifier.fillMaxSize()) {
    if (uiState.isLoading) {
      Loading()
    } else {
      GoogleMap(
          modifier = Modifier.fillMaxSize().testTag(MapScreenTestTags.GOOGLE_MAP_SCREEN),
          cameraPositionState = cameraPositionState,
          uiSettings =
              MapUiSettings(
                  myLocationButtonEnabled = false,
                  zoomControlsEnabled = false,
                  mapToolbarEnabled = false),
          properties = MapProperties(isMyLocationEnabled = uiState.isGranted)) {
            val severities =
                uiState.hazardState.hazards
                    .filter { it.type != null && it.severity != null }
                    .groupBy { it.type }
                    .map {
                      val minSev = it.value.minOf { hazard -> hazard.severity ?: 0.0 }
                      val maxSev = it.value.maxOf { hazard -> hazard.severity ?: 0.0 }
                      (it.key ?: "Unknown") to (Pair(minSev, maxSev))
                    }
                    .toMap()
            uiState.hazardState.hazards.forEach { hazard -> HazardMarker(hazard, severities) }
          }
      TrackLocationButton(uiState.isTrackingLocation) { viewModel.setTracking(true) }
    }

    // Permission request card
    if (!uiState.isGranted) {
      if (uiState.isOsRequestInFlight) {
        Text(
            "Requesting Android location permission…",
            modifier = Modifier.testTag(PermissionUiTags.OS_PERMISSION_TEXT))
      } else {
        val (title, msg, showAllow) =
            when (uiState.permissionResult) {
              is PermissionResult.PermanentlyDenied ->
                  Triple(
                      "Location blocked",
                      "You selected “Don’t ask again”. To enable location, " +
                          "open Android Settings and grant permission.",
                      false)
              is PermissionResult.Denied ->
                  Triple(
                      "Location disabled",
                      "Map require fine location permissions. " +
                          "You can allow location now or later in Android Settings.",
                      true)
              else -> Triple("", "", false)
            }
        if (title.isNotEmpty()) {
          PermissionRequestCard(
              title = title,
              message = msg,
              showAllowButton = showAllow,
              onAllowClick = requestPermissions,
              onOpenSettingsClick = {
                val intent =
                    Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            "package:${activity.packageName}".toUri())
                        .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                // Use the activity to start the new activity.
                activity.startActivity(intent)
              },
              modifier =
                  Modifier.align(Alignment.BottomCenter)
                      .padding(16.dp)
                      .fillMaxWidth()
                      .testTag(PermissionUiTags.CARD))
        }
      }
    }
  }
}

@Composable
fun BoxScope.TrackLocationButton(isTracking: Boolean, onClick: () -> Unit = {}) {
  val tint =
      if (isTracking) {
        MaterialTheme.colorScheme.primary
      } else {
        MaterialTheme.colorScheme.inversePrimary
      }

  FloatingActionButton(
      onClick = onClick,
      shape = MaterialTheme.shapes.extraLarge,
      containerColor = tint,
      modifier =
          Modifier.align(Alignment.BottomEnd)
              .padding(16.dp)
              .testTag(MapScreenTestTags.TRACK_LOCATION_BUTTON)) {
        Icon(Icons.Outlined.LocationOn, contentDescription = "Current location")
      }
}

private suspend fun defaultAnimate(
    cameraPositionState: CameraPositionState,
    positionState: GpsPositionState
) {
  cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(positionState.position, 12f), 1000)
}
