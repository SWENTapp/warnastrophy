package com.github.warnastrophy.core.ui.map

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.github.warnastrophy.core.model.AppPermissions
import com.github.warnastrophy.core.model.HazardsDataService
import com.github.warnastrophy.core.model.Location
import com.github.warnastrophy.core.model.PermissionManager
import com.github.warnastrophy.core.model.PermissionResult
import com.github.warnastrophy.core.model.PositionService
import com.github.warnastrophy.core.ui.components.Loading
import com.github.warnastrophy.core.ui.components.PermissionRequestCard
import com.github.warnastrophy.core.ui.components.PermissionUiTags
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

object MapScreenTestTags {
  const val GOOGLE_MAP_SCREEN = "mapScreen"
  const val USER_LOCATION = "userLocation" // tiny probe to check if shown
}

data class MapScreenTestHooks(
    // Clearly indicates this forces a permission state for testing
    val forceLocationPermission: Boolean? = null,
    // Clearly indicates this provides a mocked result for the permissions request
    val mockPermissionsResult: Map<String, Boolean>? = null
)

@Composable
fun MapScreen(
    gpsService: PositionService,
    hazardsService: HazardsDataService,
    testHooks: MapScreenTestHooks = MapScreenTestHooks(),
    activity: Activity
) {
  val locationPermissions = AppPermissions.LocationFine

  val cameraPositionState = rememberCameraPositionState()
  val hazardState by hazardsService.currentHazardsState.collectAsState()
  val positionState by gpsService.positionState.collectAsState()

  var isOsRequestInFlight by remember { mutableStateOf(false) }
  val permissionsManager = remember { PermissionManager(activity) }

  // Recompute permissions whenever isOsRequestInFlight changes, e.g., after returning from a
  // permission prompt.
  val permissionsResult by
      remember(isOsRequestInFlight) {
        mutableStateOf(permissionsManager.getPermissionResult(locationPermissions))
      }
  var granted by
      remember(permissionsResult, testHooks.forceLocationPermission) {
        mutableStateOf(
            testHooks.forceLocationPermission ?: (permissionsResult is PermissionResult.Granted))
      }

  //    LaunchedEffect(Unit) {
  //        granted = testHooks.forceLocationPermission ?: (permissions is PermissionResult.Granted)
  //    }

  fun applyPermissionsResult(res: Map<String, Boolean>) {
    granted = res.values.any { it }
    permissionsManager.markPermissionsAsAsked(locationPermissions)
  }

  val launcher =
      rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res
        ->
        if (testHooks.forceLocationPermission != null) return@rememberLauncherForActivityResult
        isOsRequestInFlight = false
        applyPermissionsResult(res)
      }

  //    LaunchedEffect(testHooks.mockPermissionsResult) {
  //        testHooks.mockPermissionsResult?.let {
  //            if (testHooks.forceLocationPermission != null) return@LaunchedEffect
  //            applyPermissionsResult(it)
  //        }
  //    }

  //    LaunchedEffect(Unit) {
  //        if (testHooks.forceLocationPermission != null || testHooks.mockPermissionsResult !=
  // null)
  //            return@LaunchedEffect
  //
  //        if (!firstLaunchDone) {
  //            launcher.launch(
  //                arrayOf(
  //                    Manifest.permission.ACCESS_FINE_LOCATION,
  //                    Manifest.permission.ACCESS_COARSE_LOCATION
  //                )
  //            )
  //            firstLaunchDone = true
  //            prefs.edit().putBoolean("first_launch_done", true).apply()
  //        } else {
  //            if (!granted && status == LocPermStatus.GIVEN_TEMP) {
  //                launcher.launch(
  //                    arrayOf(
  //                        Manifest.permission.ACCESS_FINE_LOCATION,
  //                        Manifest.permission.ACCESS_COARSE_LOCATION
  //                    )
  //                )
  //            }
  //        }
  //    }

  val requestPermissions: () -> Unit = {
    val mock = testHooks.mockPermissionsResult
    if (mock != null) {
      // mock path = no OS prompt; do NOT set in-flight
      applyPermissionsResult(mock)
    } else {
      // real OS dialog path
      isOsRequestInFlight = true
      launcher.launch(locationPermissions.permissions)
    }
  }

  LaunchedEffect(granted) {
    if (granted) {
      gpsService.requestCurrentLocation()
      gpsService.startLocationUpdates()
    }
  }

  val hazardsList = hazardState

  LaunchedEffect(positionState.position, granted) {
    if (granted && !positionState.isLoading) {
      cameraPositionState.animate(
          CameraUpdateFactory.newLatLngZoom(positionState.position, 12f),
          1000) // 1 second animation)
    }
  }

  Box(Modifier.fillMaxSize()) {
    if (granted && positionState.isLoading) {
      Loading()
    } else
        GoogleMap(
            modifier = Modifier.fillMaxSize().testTag(MapScreenTestTags.GOOGLE_MAP_SCREEN),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = granted)) {
              Log.d("Log", "Rendering ${hazardsList.size} hazards on the map")
              hazardsList.forEach { hazard ->
                hazard.coordinates?.forEach { coord ->
                  val loc = Location.toLatLng(coord)
                  Marker(
                      state = MarkerState(position = loc),
                      title = "${hazard.type} in ${hazard.country}",
                      snippet = "Lat: ${loc.latitude}, Lng: ${loc.longitude}",
                      icon = BitmapDescriptorFactory.defaultMarker(markerHueFor(hazard.type)))
                }
              }
            }

    if (granted && !positionState.isLoading) {
      Box(
          modifier =
              Modifier.align(Alignment.TopStart) // anywhere; it’s just a probe
                  .size(1.dp)
                  .testTag(MapScreenTestTags.USER_LOCATION))
    }

    // Permission request card
    if (!granted) {
      if (isOsRequestInFlight) {
        Text(
            "Requesting Android location permission…",
            modifier = Modifier.testTag(PermissionUiTags.OS_PERMISSION_TEXT))
      } else {
        val (title, msg, showAllow) =
            when (permissionsResult) {
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

internal fun markerHueFor(type: String?): Float =
    when (type) {
      "FL" -> BitmapDescriptorFactory.HUE_GREEN
      "DR" -> BitmapDescriptorFactory.HUE_ORANGE
      "WC" -> BitmapDescriptorFactory.HUE_BLUE
      "EQ" -> BitmapDescriptorFactory.HUE_RED
      "TC" -> BitmapDescriptorFactory.HUE_YELLOW
      else -> BitmapDescriptorFactory.HUE_AZURE
    }
