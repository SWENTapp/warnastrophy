package com.github.warnastrophy.core.ui.features.map

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.github.warnastrophy.R
import com.github.warnastrophy.core.model.Hazard
import com.github.warnastrophy.core.model.Location.Companion.toLatLng
import com.github.warnastrophy.core.permissions.PermissionResult
import com.github.warnastrophy.core.ui.components.ActivityFallback
import com.github.warnastrophy.core.ui.components.Loading
import com.github.warnastrophy.core.ui.components.PermissionRequestCard
import com.github.warnastrophy.core.ui.components.PermissionUiTags
import com.github.warnastrophy.core.util.GeometryParser
import com.github.warnastrophy.core.util.findActivity
import com.github.warnastrophy.core.util.openAppSettings
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.MapsComposeExperimentalApi

object MapScreenTestTags {
  const val GOOGLE_MAP_SCREEN = "mapScreen"
  const val TRACK_LOCATION_BUTTON = "trackLocationButton"
  const val SEARCH_BAR = "searchBar"

  const val SEARCH_BAR_TEXT_FIELD = "searchBarTextField"

  const val SEARCH_BAR_DROPDOWN = "searchBarDropdown"

  const val SEARCH_BAR_DROPDOWN_ITEM = "searchBarDropdownItem"
}

@Composable
fun MapScreen(
    viewModel: MapViewModel,
    cameraPositionState: CameraPositionState = rememberCameraPositionState(),
    googleMap: @Composable (CameraPositionState, MapUIState) -> Unit = { cameraState, uiState ->
      HazardsGoogleMap(cameraState, uiState)
    },
    isPreview: Boolean = false,
) {
  val activity = LocalContext.current.findActivity()
  val focusManager = LocalFocusManager.current

  if (activity == null) {
    ActivityFallback()
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
    launcher.launch(viewModel.foregroundPermissions.permissions)
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
      defaultAnimate(cameraPositionState, uiState.positionState.position)
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
  Box(
      modifier =
          Modifier.fillMaxSize().pointerInput(Unit) {
            detectTapGestures { focusManager.clearFocus() }
          }) {
        if (uiState.isLoading) {
          Loading()
        } else {
          googleMap(cameraPositionState, uiState)
          if (!isPreview) {
            SearchBar(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 25.dp),
                viewModel = viewModel,
                cameraPositionState = cameraPositionState,
                focusManager = focusManager)
          }

          TrackLocationButton(uiState.isTrackingLocation) {
            viewModel.onTrackLocationClicked(cameraPositionState)
          }
        }

        // Permission request card
        if (!uiState.isGranted) {
          if (uiState.isOsRequestInFlight) {
            Text(
                "Requesting Android location permission…",
                modifier = Modifier.testTag(PermissionUiTags.OS_PERMISSION_TEXT))
          } else {
            val (title, msg, showAllow) =
                when (uiState.locationPermissionResult) {
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
                  onOpenSettingsClick = { openAppSettings(context = activity) },
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
          Modifier.align(Alignment.BottomStart)
              .padding(16.dp)
              .testTag(MapScreenTestTags.TRACK_LOCATION_BUTTON)) {
        Icon(Icons.Outlined.LocationOn, contentDescription = "Current location")
      }
}

@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun HazardsGoogleMap(
    cameraPositionState: CameraPositionState,
    uiState: MapUIState,
    context: Context = LocalContext.current,
) {
  val hazards = uiState.hazardState.hazards
  val clusterItems = hazards.map { HazardClusterItem(it) }

  GoogleMap(
      modifier = Modifier.fillMaxSize().testTag(MapScreenTestTags.GOOGLE_MAP_SCREEN),
      cameraPositionState = cameraPositionState,
      uiSettings =
          MapUiSettings(
              myLocationButtonEnabled = false,
              zoomControlsEnabled = true,
              mapToolbarEnabled = false),
      properties = MapProperties(isMyLocationEnabled = uiState.isGranted)) {
        // Draw affected zone polygons for all hazards
        hazards.forEach { hazard ->
          hazard.affectedZone?.let { zone ->
            val locations = GeometryParser.jtsGeometryToLatLngList(zone)
            if (locations!!.size > 1) {
              val polygonCoords = locations.map { location -> com.github.warnastrophy.core.model.Location.toLatLng(location) }
              PolygonWrapper(polygonCoords)
            }
          }
        }

        // Clustered hazard markers
        Clustering(
            items = clusterItems,
            onClusterClick = { cluster ->
              // Zoom in when clicking on a cluster
              false // Return false to allow default behavior (zoom in)
            },
            onClusterItemClick = { clusterItem ->
              false // Return false to show info window
            },
            clusterContent = { cluster ->
              // Custom cluster appearance
              ClusterContent(cluster.size)
            },
            clusterItemContent = { clusterItem ->
              // Individual marker content when not clustered
              HazardMarkerContent(clusterItem.hazard)
            }
        )

        uiState.selectedLocation?.let { loc ->
          val pos = toLatLng(loc)
          Marker(
              state = MarkerState(position = pos),
              title = loc.name?.substringBefore(",") ?: "",
              icon = bitmapDescriptorFromJpeg(context, R.drawable.material_symobls_pin))
        }
      }
}

@Composable
fun ClusterContent(size: Int) {
  androidx.compose.foundation.layout.Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
          .size(40.dp)
          .clip(androidx.compose.foundation.shape.CircleShape)
          .background(MaterialTheme.colorScheme.primary)
  ) {
    Text(
        text = size.toString(),
        color = MaterialTheme.colorScheme.onPrimary,
        style = MaterialTheme.typography.bodyMedium
    )
  }
}

@Composable
fun HazardMarkerContent(hazard: Hazard) {
  val severityTint = getSeverityColor(hazard)
  val icon: MapIcon = hazardTypeToMapIcon(hazard.type)
  Box(modifier = Modifier.size(64.dp)) {
    icon(tint = severityTint)
  }
}

suspend fun defaultAnimate(cameraPositionState: CameraPositionState, position: LatLng) {
  cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(position, 12f), 1000)
}
