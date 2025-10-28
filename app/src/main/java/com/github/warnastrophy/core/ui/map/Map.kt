package com.github.warnastrophy.core.ui.map

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.net.toUri
import com.github.warnastrophy.core.model.Hazard
import com.github.warnastrophy.core.model.HazardsDataService
import com.github.warnastrophy.core.model.PositionService
import com.github.warnastrophy.core.ui.components.Loading
import com.github.warnastrophy.core.ui.components.PermissionRequestCard
import com.github.warnastrophy.core.ui.components.PermissionUiTags
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.rememberCameraPositionState

object MapScreenTestTags {
  const val GOOGLE_MAP_SCREEN = "mapScreen"
  const val USER_LOCATION = "userLocation" // tiny probe to check if shown
}

private enum class LocPermStatus {
  GRANTED,
  GIVEN_TEMP,
  DENIED_PERMANENT
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
    testHooks: MapScreenTestHooks = MapScreenTestHooks()
) {
  val context = LocalContext.current
  val cameraPositionState = rememberCameraPositionState()
  val hazardState by hazardsService.currentHazardsState.collectAsState()
  val positionState by gpsService.positionState.collectAsState()

  val activity = remember { context.findActivity() }

  //  prefs to track first launch & whether we ever asked
  val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
  var firstLaunchDone by remember { mutableStateOf(prefs.getBoolean("first_launch_done", false)) }
  var askedOnce by remember { mutableStateOf(prefs.getBoolean("loc_asked_once", false)) }

  var isOsRequestInFlight by remember { mutableStateOf(false) }

  fun hasPermission(): Boolean {
    return testHooks.forceLocationPermission
        ?: run {
          val fine =
              ContextCompat.checkSelfPermission(
                  context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                  PackageManager.PERMISSION_GRANTED
          val coarse =
              ContextCompat.checkSelfPermission(
                  context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                  PackageManager.PERMISSION_GRANTED
          return fine || coarse
        }
  }

  var granted by remember { mutableStateOf(hasPermission()) }
  var status by remember { mutableStateOf(LocPermStatus.GIVEN_TEMP) }

  fun recomputeStatus(): LocPermStatus {
    if (granted) return LocPermStatus.GRANTED
    val showRationaleFine =
        activity?.let {
          ActivityCompat.shouldShowRequestPermissionRationale(
              it, Manifest.permission.ACCESS_FINE_LOCATION)
        } ?: false
    val showRationaleCoarse =
        activity?.let {
          ActivityCompat.shouldShowRequestPermissionRationale(
              it, Manifest.permission.ACCESS_COARSE_LOCATION)
        } ?: false
    val showRationale = showRationaleFine || showRationaleCoarse
    return if (!showRationale && askedOnce) LocPermStatus.DENIED_PERMANENT
    else LocPermStatus.GIVEN_TEMP
  }

  LaunchedEffect(Unit) {
    granted = testHooks.forceLocationPermission ?: hasPermission()
    status = recomputeStatus()
  }

  fun applyPermissionsResult(res: Map<String, Boolean>) {
    granted = res.values.any { it }
    if (!askedOnce) {
      askedOnce = true
      prefs.edit { putBoolean("loc_asked_once", true) }
    }
    status = recomputeStatus()
  }
  val launcher =
      rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res
        ->
        if (testHooks.forceLocationPermission != null) return@rememberLauncherForActivityResult
        isOsRequestInFlight = false
        applyPermissionsResult(res)
      }

  LaunchedEffect(testHooks.mockPermissionsResult) {
    testHooks.mockPermissionsResult?.let {
      if (testHooks.forceLocationPermission != null) return@LaunchedEffect
      applyPermissionsResult(it)
    }
  }

  LaunchedEffect(Unit) {
    if (testHooks.forceLocationPermission != null || testHooks.mockPermissionsResult != null)
        return@LaunchedEffect

    if (!firstLaunchDone) {
      launcher.launch(
          arrayOf(
              Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
      firstLaunchDone = true
      prefs.edit().putBoolean("first_launch_done", true).apply()
    } else {
      if (!granted && status == LocPermStatus.GIVEN_TEMP) {
        launcher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION))
      }
    }
  }

  val requestPermissions: () -> Unit = {
    val mock = testHooks.mockPermissionsResult
    if (mock != null) {
      // mock path = no OS prompt; do NOT set in-flight
      applyPermissionsResult(mock)
    } else {
      // real OS dialog path
      isOsRequestInFlight = true
      launcher.launch(
          arrayOf(
              Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
  }

  LaunchedEffect(granted) {
    if (granted) {
      gpsService.requestCurrentLocation()
      gpsService.startLocationUpdates()
    }
  }

  var hazardsList = remember { emptyList<Hazard>() }

  LaunchedEffect(positionState.position, granted) {
    if (granted && !positionState.isLoading) {
      cameraPositionState.animate(
          CameraUpdateFactory.newLatLngZoom(positionState.position, 12f),
          1000) // 1 second animation)
    }
  }

  LaunchedEffect(hazardState) { hazardsList = hazardState }

  Box(Modifier.fillMaxSize()) {
    if (granted && positionState.isLoading) {
      Loading()
    } else
        GoogleMap(
            modifier = Modifier.fillMaxSize().testTag(MapScreenTestTags.GOOGLE_MAP_SCREEN),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = granted)) {
              Log.d("Log", "Rendering ${hazardsList.size} hazards on the map")
              val maxSeverities =
                  hazardsList
                      .filter { it.type != null && it.severity != null }
                      .groupBy { it.type }
                      .map {
                        (it.key ?: "Unknown") to
                            (it.value.maxOf { hazard -> hazard.severity ?: 0.0 })
                      }
                      .toMap()
              hazardsList.forEach { hazard -> HazardMarker(hazard, maxSeverities) }
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
            when (status) {
              LocPermStatus.GIVEN_TEMP ->
                  Triple(
                      "Location disabled",
                      "Limited functionality: we can’t center the map or show nearby hazards. You can allow location now or later in Android Settings.",
                      true)
              LocPermStatus.DENIED_PERMANENT ->
                  Triple(
                      "Location blocked",
                      "You selected “Don’t ask again”. To enable location, open Android Settings and grant permission.",
                      false)
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
                        "package:${context.packageName}".toUri())
                context.startActivity(intent)
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

private fun Context.findActivity(): Activity? =
    when (this) {
      is Activity -> this
      is ContextWrapper -> baseContext.findActivity()
      else -> null
    }

/** TODO: Potentially removable after switching to custom markers with MapIcons */
@JvmSynthetic // keeps it out of Java API, no-op for Kotlin
@kotlin.jvm.JvmName("markerHueForType")
internal fun markerHueFor(type: String?): Float =
    when (type) {
      "FL" -> BitmapDescriptorFactory.HUE_GREEN
      "DR" -> BitmapDescriptorFactory.HUE_ORANGE
      "WC" -> BitmapDescriptorFactory.HUE_BLUE
      "EQ" -> BitmapDescriptorFactory.HUE_RED
      "TC" -> BitmapDescriptorFactory.HUE_YELLOW
      else -> BitmapDescriptorFactory.HUE_AZURE
    }
