package com.github.warnastrophy.core.ui.features.map

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.warnastrophy.R
import com.github.warnastrophy.core.model.Location.Companion.toLatLng
import com.github.warnastrophy.core.permissions.PermissionResult
import com.github.warnastrophy.core.ui.components.ActivityFallback
import com.github.warnastrophy.core.ui.components.Loading
import com.github.warnastrophy.core.ui.components.PermissionRequestCard
import com.github.warnastrophy.core.ui.components.PermissionUiTags
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
import com.google.maps.android.compose.rememberCameraPositionState

object MapScreenTestTags {
  const val GOOGLE_MAP_SCREEN = "mapScreen"
  const val TRACK_LOCATION_BUTTON = "trackLocationButton"
  const val SEARCH_BAR = "searchBar"

  const val SEARCH_BAR_TEXT_FIELD = "searchBarTextField"

  const val SEARCH_BAR_DROPDOWN = "searchBarDropdown"

  const val SEARCH_BAR_DROPDOWN_ITEM = "searchBarDropdownItem"

  const val SEARCH_BAR_CLEAR_BUTTON = "searchBarClearButton"
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
  val uiState by viewModel.uiState.collectAsState()

  if (activity == null) {
    ActivityFallback()
    return
  }

  HandleLocationUpdates(uiState, viewModel)
  HandleCameraMovement(uiState, cameraPositionState, viewModel)

  Box(
      modifier =
          Modifier.fillMaxSize().pointerInput(Unit) {
            detectTapGestures { focusManager.clearFocus() }
          }) {
        if (uiState.isLoading) {
          Loading()
        } else {
          MapContent(
              cameraPositionState,
              uiState,
              viewModel,
              isPreview,
              googleMap,
          )
        }

        LocationPermissionHandler(
            uiState = uiState,
            viewModel = viewModel,
            activity = activity,
        )
      }
}

/**
 * Composable that displays the main content of the map screen, including the map itself and UI
 * overlays.
 *
 * This function lays out the core map view and conditionally adds interactive elements like the
 * [SearchBar] and [TrackLocationButton] based on the current state.
 *
 * @param cameraPositionState The state of the map's camera, controlling its position and zoom.
 * @param uiState The current state of the UI, containing information like location tracking status.
 * @param viewModel The ViewModel providing the logic and data for the map screen.
 * @param isPreview A flag to indicate if the composable is being rendered in a preview context,
 *   which disables certain interactive elements like the [SearchBar].
 * @param googleMap A composable lambda that renders the actual map implementation, such as
 *   [HazardsGoogleMap].
 */
@Composable
private fun BoxScope.MapContent(
    cameraPositionState: CameraPositionState,
    uiState: MapUIState,
    viewModel: MapViewModel,
    isPreview: Boolean,
    googleMap: @Composable (CameraPositionState, MapUIState) -> Unit
) {
  val focusManager = LocalFocusManager.current
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

/**
 * A composable that handles the UI for requesting location permissions.
 *
 * This function observes the [MapUIState] to determine the current state of location permissions.
 *
 * @param uiState The current state of the map UI, which includes permission status.
 * @param viewModel The view model responsible for handling permission logic and state updates.
 * @param activity The current activity, required to launch the permission request and to open app
 *   settings.
 */
@Composable
private fun BoxScope.LocationPermissionHandler(
    uiState: MapUIState,
    viewModel: MapViewModel,
    activity: Activity
) {
  val launcher =
      rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
        viewModel.applyPermissionsResult(activity)
      }

  val requestPermissions: () -> Unit = {
    viewModel.onPermissionsRequestStart()
    launcher.launch(viewModel.locationPermissions.permissions)
  }

  if (uiState.isGranted) return

  if (uiState.isOsRequestInFlight) {
    Text(
        stringResource(id = R.string.map_requesting_location_permission),
        modifier = Modifier.testTag(PermissionUiTags.OS_PERMISSION_TEXT))
  } else {
    val (title, msg, showAllow) =
        when (uiState.locationPermissionResult) {
          is PermissionResult.PermanentlyDenied ->
              Triple(
                  stringResource(id = R.string.map_location_blocked_title),
                  stringResource(id = R.string.map_location_blocked_message),
                  false)
          is PermissionResult.Denied ->
              Triple(
                  stringResource(id = R.string.map_location_disabled_title),
                  stringResource(id = R.string.map_location_disabled_message),
                  true)
          else -> return
        }
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

/**
 * A side effect that starts or stops location updates based on the permission status.
 *
 * This composable uses a [LaunchedEffect] that triggers whenever the `uiState.isGranted` value
 * changes. If location permission is granted, it calls the view model to start receiving location
 * updates. If the permission is revoked or not granted, it stops the updates.
 *
 * @param uiState The current state of the map UI, containing the location permission status.
 * @param viewModel The view model responsible for handling location update logic.
 */
@Composable
private fun HandleLocationUpdates(uiState: MapUIState, viewModel: MapViewModel) {
  LaunchedEffect(uiState.isGranted) {
    if (uiState.isGranted) {
      viewModel.startLocationUpdate()
    } else {
      viewModel.stopLocationUpdate()
    }
  }
}

/**
 * A composable that observes the UI state and camera position to manage camera movements.
 *
 * This function contains two main effects:
 * 1. It animates the camera to the user's current location when location tracking is enabled,
 *    permissions are granted, the app is not in a loading state, and the user's position changes.
 * 2. It automatically disables location tracking if the user manually moves the map via a gesture
 *    (e.g., panning or zooming). This allows the user to break out of tracking mode to explore the
 *    map freely.
 *
 * @param uiState The current state of the map UI, containing information about location tracking,
 *   permissions, and user position.
 * @param cameraPositionState The state object that controls the map's camera position and movement.
 * @param viewModel The view model responsible for managing the map's business logic, including
 *   enabling or disabling location tracking.
 */
@Composable
private fun HandleCameraMovement(
    uiState: MapUIState,
    cameraPositionState: CameraPositionState,
    viewModel: MapViewModel
) {
  LaunchedEffect(uiState.isTrackingLocation, uiState.positionState.position, uiState.isGranted) {
    if (uiState.isGranted && !uiState.isLoading && uiState.isTrackingLocation) {
      defaultAnimate(cameraPositionState, uiState.positionState.position)
    }
  }

  LaunchedEffect(cameraPositionState.isMoving, cameraPositionState.cameraMoveStartedReason) {
    if (cameraPositionState.isMoving &&
        cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE) {
      viewModel.setTracking(false)
    }
  }
}

/**
 * A floating action button that allows the user to toggle location tracking on the map.
 *
 * @param isTracking A boolean indicating whether location tracking is currently active.
 * @param onClick A lambda function to be invoked when the button is clicked.
 */
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
        Icon(
            Icons.Outlined.LocationOn,
            contentDescription = stringResource(id = R.string.map_track_location_button_cd))
      }
}

/**
 * A composable that displays a Google Map with hazard markers and a marker for a selected location.
 *
 * This function integrates with the Google Maps Compose library to render the map. It displays
 * markers for each hazard present in the `uiState`. It also allows for one marker to be "selected"
 * at a time, which can change its appearance (handled by `HazardMarker`). Additionally, if a
 * location is selected (e.g., from a search), a distinct pin marker is displayed at that location.
 *
 * Map UI settings are configured to disable the default "My Location" button (as a custom one is
 * used), enable zoom controls, and disable the map toolbar. The user's location is shown on the map
 * if location permissions have been granted.
 *
 * @param cameraPositionState The state object that controls the map's camera position.
 * @param uiState The current state of the map UI, containing hazard data, selected location, and
 *   permission status.
 * @param context The Android `Context`, used here to load bitmap descriptors for markers. Defaults
 *   to the `LocalContext`.
 */
@Composable
fun HazardsGoogleMap(
    cameraPositionState: CameraPositionState,
    uiState: MapUIState,
    context: Context = LocalContext.current,
) {
  val hazards = uiState.hazardState.hazards
  var selectedMarkerId by remember { mutableStateOf<Int?>(null) }

  GoogleMap(
      modifier = Modifier.fillMaxSize().testTag(MapScreenTestTags.GOOGLE_MAP_SCREEN),
      cameraPositionState = cameraPositionState,
      onMapClick = {},
      uiSettings =
          MapUiSettings(
              myLocationButtonEnabled = false,
              zoomControlsEnabled = true,
              mapToolbarEnabled = false),
      properties = MapProperties(isMyLocationEnabled = uiState.isGranted)) {
        hazards.forEach { hazard ->
          HazardMarker(
              hazard = hazard,
              selectedMarkerId = selectedMarkerId,
              onMarkerSelected = { selectedMarkerId = it })
        }

        uiState.selectedLocation?.let { loc ->
          val pos = toLatLng(loc)
          Marker(
              state = MarkerState(position = pos),
              title = loc.name?.substringBefore(",") ?: "",
              icon = bitmapDescriptorFromJpeg(context, R.drawable.material_symobls_pin))
        }
      }
}

/**
 * A default animation for the camera movement.
 *
 * @param cameraPositionState The state of the camera to animate.
 * @param position The [LatLng] to animate the camera to.
 */
suspend fun defaultAnimate(cameraPositionState: CameraPositionState, position: LatLng) {
  cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(position, 12f), 1000)
}
