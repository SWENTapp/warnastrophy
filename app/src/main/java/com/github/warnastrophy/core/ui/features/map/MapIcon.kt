package com.github.warnastrophy.core.ui.features.map

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import com.github.warnastrophy.R
import com.github.warnastrophy.core.model.Hazard
import com.github.warnastrophy.core.model.Location
import com.github.warnastrophy.core.util.GeometryParser
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberMarkerState

val Tint = SemanticsPropertyKey<Color>("Tint")
var SemanticsPropertyReceiver.tint by Tint

/**
 * Enum class representing different map icons for various hazard types. Each icon is associated
 * with a drawable resource ID and a tag for testing purposes.
 *
 * @param resId The drawable resource ID for the icon. If null, a default warning icon is used.
 * @param tag The tag used for testing the icon in UI tests.
 * - Applies the icon as a Composable function with an optional tint color.
 * - Semantics properties on the tint are for testing.
 */
enum class MapIcon(@DrawableRes val resId: Int?, val tag: String) {
  Tsunami(R.drawable.material_symbols_outlined_tsunami, "map_icon_tsunami"),
  Drought(R.drawable.material_symbols_outlined_water_voc, "map_icon_drought"),
  Earthquake(R.drawable.material_symbols_outlined_earthquake, "map_icon_earthquake"),
  Fire(R.drawable.material_symbols_outlined_local_fire_department, "map_icon_fire"),
  Flood(R.drawable.material_symbols_outlined_flood, "map_icon_flood"),
  Cyclone(R.drawable.material_symbols_outlined_storm, "map_icon_cyclone"),
  Volcano(R.drawable.material_symbols_outlined_volcano, "map_icon_volcano"),
  Unknown(null, "map_icon_unknown");

  @Composable
  operator fun invoke(tint: Color = Color.Gray) {
    val imageVector =
        if (resId != null) {
          ImageVector.vectorResource(resId)
        } else {
          Icons.Filled.Warning
        }

    Icon(
        imageVector,
        contentDescription = tag,
        modifier = Modifier.testTag(tag).semantics { this.tint = tint },
        tint = tint)
  }
}

/**
 * Composable function to display a hazard marker on the map.
 *
 * @param hazard The hazard data to be displayed.
 * @param severities A map containing the minimum and maximum severities for each hazard type.
 * @param markerContent A composable function mainly to test the marker content, because
 *   MarkerComposable is not directly testable because it's rasterized on the map.
 */
@Composable
fun HazardMarker(
    hazard: Hazard,
    severities: Map<String, Pair<Double, Double>>,
    markerContent:
        @Composable
        (
            state: MarkerState,
            title: String?,
            snippet: String?,
            content: @Composable () -> Unit) -> Unit =
        { state, title, snippet, content ->
          MarkerComposable(state = state, title = title, snippet = snippet) { content() }
        }
) {
  // The markerLocation is the centroid of the geometry.
  val markerLocation =
      hazard.centroid?.centroid?.let { point -> Location(point.y, point.x) } ?: Location(0.0, 0.0)

  val centroidLatLngs: List<Location>? =
      hazard.affectedZone?.let { nonNullGeometry ->
        // 'nonNullGeometry' inside the 'let' block is now guaranteed to be 'Geometry' (non-null)
        GeometryParser.jtsGeometryToLatLngList(nonNullGeometry)
      }

  centroidLatLngs?.let { locations ->
    if (locations.size > 1) {
      val polygonCoords = locations.map { location -> Location.toLatLng(location) }

      // Draw a polygon on the map for multi-point geometries.
      Polygon(
          points = polygonCoords,
          strokeWidth = 2f,
          strokeColor = Color.Red,
          fillColor = Color.Red.copy(alpha = 0.18f))
    } else {
      // Fallback for empty polygon.
      // This branch is unlikely if hazard.affectedZone exists.
      // Log or handle this case as an anomaly if necessary.
    }
  }

  markerContent(
      rememberMarkerState(position = Location.toLatLng(markerLocation)),
      hazard.description,
      "${hazard.severity} ${hazard.severityUnit}",
  ) {
    val icon: MapIcon =
        when (hazard.type) {
          "FL",
          "FF",
          "SS" -> MapIcon.Flood
          "DR" -> MapIcon.Drought
          "EQ",
          "AV",
          "LS",
          "MS" -> MapIcon.Earthquake
          "TC",
          "EC",
          "VW",
          "TO",
          "ST" -> MapIcon.Cyclone
          "FR",
          "WF" -> MapIcon.Fire
          "VO" -> MapIcon.Volcano
          "TS" -> MapIcon.Tsunami
          else -> MapIcon.Unknown
        }

    val tint: Color =
        hazard.severity?.let {
          val (minSev, maxSev) =
              severities[hazard.type]
                  ?: throw IllegalStateException(
                      "Max intensity not found for hazard type ${hazard.type}")
          val denom = maxSev - minSev
          val intensityRatio =
              if (denom != 0.0) {
                    (it - minSev) / denom
                  } else {
                    1.0
                  }
                  .coerceIn(0.0, 1.0)
          lerp(Color.Gray, Color.Red, intensityRatio.toFloat())
        } ?: Color.Black

    icon(tint = tint)
  }
}
