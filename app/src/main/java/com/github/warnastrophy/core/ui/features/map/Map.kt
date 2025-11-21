package com.github.warnastrophy.core.ui.features.map

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.core.net.toUri
import com.github.warnastrophy.core.domain.model.Location
import com.github.warnastrophy.core.domain.model.Location.Companion.toLatLng
import com.github.warnastrophy.core.permissions.PermissionResult
import com.github.warnastrophy.core.ui.components.Loading
import com.github.warnastrophy.core.ui.components.PermissionRequestCard
import com.github.warnastrophy.core.ui.components.PermissionUiTags
import com.github.warnastrophy.core.util.findActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import kotlin.text.compareTo
import kotlinx.coroutines.launch

object MapScreenTestTags {
  const val GOOGLE_MAP_SCREEN = "mapScreen"
  const val FALLBACK_ACTIVITY_ERROR = "fallbackActivityError"
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
    }
) {
  val activity = LocalContext.current.findActivity()
  val focusManager = LocalFocusManager.current

  if (activity == null) {
    Box(
        modifier = Modifier.fillMaxSize().testTag(MapScreenTestTags.FALLBACK_ACTIVITY_ERROR),
        contentAlignment = Alignment.Center) {
          Text("Error: Map screen cannot function without an Activity context.")
        }
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

          SearchBar(
              modifier = Modifier.align(Alignment.TopCenter).padding(top = 25.dp),
              viewModel = viewModel,
              cameraPositionState = cameraPositionState,
              focusManager = focusManager)

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
          Modifier.align(Alignment.BottomStart)
              .padding(16.dp)
              .testTag(MapScreenTestTags.TRACK_LOCATION_BUTTON)) {
        Icon(Icons.Outlined.LocationOn, contentDescription = "Current location")
      }
}

@Composable
fun HazardsGoogleMap(
    cameraPositionState: CameraPositionState,
    uiState: MapUIState,
) {
  val hazards = uiState.hazardState.hazards

  // Pas de pointerInput ici : on clearFocus est déjà géré par le Box parent
  GoogleMap(
      modifier = Modifier.fillMaxSize().testTag(MapScreenTestTags.GOOGLE_MAP_SCREEN),
      cameraPositionState = cameraPositionState,
      uiSettings =
          MapUiSettings(
              myLocationButtonEnabled = false,
              zoomControlsEnabled = true,
              mapToolbarEnabled = false),
      properties = MapProperties(isMyLocationEnabled = uiState.isGranted)) {
        hazards.forEach { hazard -> HazardMarker(hazard, uiState.severitiesByType) }
      }
}

// kotlin
@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    viewModel: MapViewModel,
    cameraPositionState: CameraPositionState,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
  val coroutineScope = rememberCoroutineScope()
  val focusRequester = remember { FocusRequester() }

  var text by remember { mutableStateOf("") }
  var expanded by remember { mutableStateOf(false) }
  var isTextFocused by remember { mutableStateOf(false) }

  val uiState by viewModel.uiState.collectAsState()
  val suggestions = uiState.nominatimState

  LaunchedEffect(suggestions, text, isTextFocused) {
    expanded = isTextFocused && text.isNotEmpty() && suggestions.isNotEmpty()
  }

  Box(modifier = modifier.fillMaxWidth(0.75f).testTag(MapScreenTestTags.SEARCH_BAR)) {
    Column {
      SearchTextField(
          text = text,
          onTextChange = { newText ->
            text = newText
            viewModel.searchLocations(newText)
          },
          focusRequester = focusRequester,
          onFocusChanged = { focused ->
            isTextFocused = focused
            if (!focused) expanded = false
          },
          modifier = Modifier.fillMaxWidth())

      SuggestionsDropdown(
          expanded = expanded,
          suggestions = suggestions,
          modifier = Modifier.fillMaxWidth(0.75f).background(Color.White),
          onDismiss = {
            expanded = false
            focusManager.clearFocus()
          },
          onSelect = { loc ->
            val name = loc.name ?: ""
            text = name
            expanded = false
            focusManager.clearFocus()
            coroutineScope.launch { defaultAnimate(cameraPositionState, toLatLng(loc)) }
          })
    }
  }
}

@Composable
private fun SearchTextField(
    text: String,
    onTextChange: (String) -> Unit,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
  Row(
      modifier =
          modifier
              .background(Color.White, RoundedCornerShape(16.dp))
              .padding(horizontal = 8.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = "Search Icon",
            tint = Color.Black,
            modifier = Modifier.padding(start = 8.dp))

        Spacer(Modifier.width(12.dp))

        BasicTextField(
            value = text,
            onValueChange = onTextChange,
            modifier =
                Modifier.fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { state -> onFocusChanged(state.isFocused) }
                    .testTag(MapScreenTestTags.SEARCH_BAR_TEXT_FIELD),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(color = Color.Black),
            decorationBox = { innerTextField ->
              if (text.isEmpty()) {
                Text("Search", color = Color.Black.copy(alpha = 0.6f))
              }
              innerTextField()
            })
      }
}

@Composable
private fun SuggestionsDropdown(
    expanded: Boolean,
    suggestions: List<Location>,
    onDismiss: () -> Unit,
    onSelect: (Location) -> Unit,
    modifier: Modifier = Modifier
) {
  if (!expanded || suggestions.isEmpty()) return

  DropdownMenu(
      expanded = true,
      onDismissRequest = onDismiss,
      modifier = modifier.testTag(MapScreenTestTags.SEARCH_BAR_DROPDOWN),
      properties =
          PopupProperties(
              focusable = false, dismissOnClickOutside = true, dismissOnBackPress = true)) {
        suggestions.forEachIndexed { index, item ->
          val name = item.name
          if (name != null) {
            DropdownMenuItem(
                modifier = Modifier.testTag(MapScreenTestTags.SEARCH_BAR_DROPDOWN_ITEM),
                text = { Text(name) },
                onClick = { onSelect(item) })
            if (index < suggestions.size - 1) {
              HorizontalDivider(thickness = 1.dp, color = Color(0xFFD3F4FF))
            }
          }
        }
      }
}

suspend fun defaultAnimate(cameraPositionState: CameraPositionState, position: LatLng) {
  cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(position, 12f), 1000)
}
