package com.github.warnastrophy.core.ui.features.map

import android.R.attr.type
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import coil.compose.AsyncImage
import com.github.warnastrophy.R
import com.github.warnastrophy.core.model.Hazard
import com.github.warnastrophy.core.model.Location
import com.github.warnastrophy.core.ui.components.StandardDashboardCard
import com.github.warnastrophy.core.ui.features.dashboard.getImageForEvent
import com.github.warnastrophy.core.util.GeometryParser
import com.github.warnastrophy.core.util.formatDate
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.MarkerInfoWindow
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberMarkerState
import java.util.Locale

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

/*
  Computes the severity tint color for a hazard based on its severity and type.
  The color transitions from gray (low severity) to red (high severity).
*/
fun computeSeverityTint(hazard: Hazard, severities: Map<String, Pair<Double, Double>>): Color {
  val severity = hazard.severity ?: return Color.Black
  val type = hazard.type ?: return Color.Black
  val (minSev, maxSev) =
      try {
        severities[type]
            ?: throw IllegalStateException("Max intensity not found for hazard type $type")
      } catch (e: Exception) {
        Log.e("computeSeverityTint", "Error retrieving severities for type $type", e)
        return Color.Black
      }
  val denom = maxSev - minSev
  val ratio =
      if (denom != 0.0) {
            (severity - minSev) / denom
          } else {
            1.0
          }
          .coerceIn(0.0, 1.0)

  return lerp(Color.Gray, Color.Red, ratio.toFloat())
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
        { state, title, snippet, _ /* we ignore content here for default impl */ ->
          val ctx = LocalContext.current
          val severityTint = computeSeverityTint(hazard, severities)
          val iconRes = hazardTypeToDrawableRes(hazard.type)
          val markerIcon: BitmapDescriptor? =
              iconRes?.let {
                bitmapDescriptorFromVector(
                    context = ctx, vectorResId = it, sizeDp = 32f, tintColor = severityTint)
              }

          MarkerInfoWindow(
              state = state,
              onClick = { false }, // keep default behaviour
              icon = markerIcon,
              onInfoWindowClick = { openHazardArticle(ctx, hazard.articleUrl) }) {
                HazardInfoWindowContent(hazard = hazard, title = title, snippet = snippet)
              }
        }
) {
  // The markerLocation is the centroid of the geometry.
  val markerLocation =
      hazard.centroid?.centroid?.let { point -> Location(point.y, point.x) } ?: Location(0.0, 0.0)

  val affectedZone: List<Location>? =
      hazard.affectedZone?.let { nonNullGeometry ->
        // 'nonNullGeometry' inside the 'let' block is now guaranteed to be 'Geometry' (non-null)
        GeometryParser.jtsGeometryToLatLngList(nonNullGeometry)
      }
  val snippet: String? = formatSeveritySnippet(hazard)

  affectedZone?.let { locations ->
    if (locations.size > 1) {
      val polygonCoords = locations.map { location -> Location.toLatLng(location) }

      PolygonWrapper(polygonCoords)
    } else {
      // Fallback for empty polygon.
      // This branch is unlikely if hazard.affectedZone exists.
      // Log or handle this case as an anomaly if necessary.
    }
  }
  val severityTint = computeSeverityTint(hazard, severities)

  markerContent(
      rememberMarkerState(position = Location.toLatLng(markerLocation)),
      hazard.description,
      snippet,
  ) {
    val icon: MapIcon = hazardTypeToMapIcon(hazard.type)
    icon(tint = severityTint)
  }
}

/*
  This function maps hazard types to drawable resource IDs.
  Returns null if the type is unrecognized.
*/
fun hazardTypeToDrawableRes(type: String?): Int? =
    when (type) {
      "FL",
      "FF",
      "SS" -> R.drawable.material_symbols_outlined_flood
      "DR" -> R.drawable.material_symbols_outlined_water_voc
      "EQ",
      "AV",
      "LS",
      "MS" -> R.drawable.material_symbols_outlined_earthquake
      "TC",
      "EC",
      "VW",
      "TO",
      "ST" -> R.drawable.material_symbols_outlined_storm
      "FR",
      "WF" -> R.drawable.material_symbols_outlined_local_fire_department
      "VO" -> R.drawable.material_symbols_outlined_volcano
      "TS" -> R.drawable.material_symbols_outlined_tsunami
      else -> null
    }

/*
   This function maps hazard types to MapIcon enum values.
*/
fun hazardTypeToMapIcon(type: String?): MapIcon =
    when (type) {
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

/*
   Formats the severity of a hazard into a readable string.
   Returns null if severity is null or zero.
*/
fun formatSeveritySnippet(hazard: Hazard): String? {
  val value = hazard.severity ?: return null
  if (value == 0.0) return null

  val rounded =
      if (value == kotlin.math.floor(value)) value.toInt().toString()
      else String.format(Locale.US, "%.1f", value)

  val unit = hazard.severityUnit.orEmpty().trim()
  return if (unit.isNotEmpty()) "$rounded $unit" else rounded
}

/*
This function opens a hazard article URL in the device's default web browser.
  If the URL is null, the function does nothing.
  @param context The context used to start the activity.
  @param articleUrl The URL of the hazard article to open.
 */
fun openHazardArticle(context: Context, articleUrl: String?) {
  articleUrl ?: return
  val intent = Intent(Intent.ACTION_VIEW, Uri.parse(articleUrl))
  ContextCompat.startActivity(context, intent, null)
}

@Composable
@GoogleMapComposable
fun PolygonWrapper(polygonCoords: List<LatLng>) {
  Polygon(
      points = polygonCoords,
      strokeWidth = 2f,
      strokeColor = MaterialTheme.colorScheme.error,
      fillColor = MaterialTheme.colorScheme.error.copy(alpha = 0.18f))
}

/*
  This function creates a BitmapDescriptor from a JPEG resource.
*/
fun bitmapDescriptorFromJpeg(context: Context, jpegResId: Int): BitmapDescriptor {
  val bitmap: Bitmap = BitmapFactory.decodeResource(context.resources, jpegResId)
  return BitmapDescriptorFactory.fromBitmap(bitmap)
}

/*
  This function creates a BitmapDescriptor from a vector drawable resource.
  It allows specifying the size in dp and an optional tint color.
*/
fun bitmapDescriptorFromVector(
    context: Context,
    @DrawableRes vectorResId: Int,
    sizeDp: Float = 32f,
    tintColor: Color? = null,
): BitmapDescriptor {
  val drawable =
      AppCompatResources.getDrawable(context, vectorResId)
          ?: return BitmapDescriptorFactory.defaultMarker()

  val density = context.resources.displayMetrics.density
  val sizePx = (sizeDp * density).toInt()

  val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bitmap)

  drawable.setBounds(0, 0, canvas.width, canvas.height)

  tintColor?.let { color -> DrawableCompat.setTint(drawable, color.toArgb()) }

  drawable.draw(canvas)
  return BitmapDescriptorFactory.fromBitmap(bitmap)
}

/*
   This Composable displays an image for a hazard.
   It uses the articleUrl if it points to a valid image format; otherwise, it falls back to a default image.
*/
@Composable
fun HazardNewsImage(hazard: Hazard, modifier: Modifier = Modifier) {
  val fallbackRes = getImageForEvent(hazard.type ?: "default")
  val imageUrl = hazard.articleUrl

  val clippedModifier = modifier.clip(RoundedCornerShape(8.dp))

  if (!imageUrl.isNullOrBlank() &&
      (imageUrl.endsWith(".jpg", true) ||
          imageUrl.endsWith(".jpeg", true) ||
          imageUrl.endsWith(".png", true))) {

    // Coil will show nothing / placeholder if there’s no connection
    AsyncImage(
        model = imageUrl,
        contentDescription = hazard.description ?: "Hazard image",
        contentScale = ContentScale.Crop,
        modifier = clippedModifier,
        placeholder = painterResource(fallbackRes),
        error = painterResource(fallbackRes))
  } else {
    Image(
        painter = painterResource(fallbackRes),
        contentDescription = hazard.description ?: "Hazard image",
        contentScale = ContentScale.Crop,
        modifier = clippedModifier)
  }
}

/*
   This Composable displays the content of a hazard info window.
   It includes the hazard image, title, severity snippet, severity text, date, and a hint to open the article.
*/
@Composable
fun HazardInfoWindowContent(
    hazard: Hazard,
    title: String?,
    snippet: String?,
) {
  StandardDashboardCard(
      modifier = Modifier.widthIn(max = 260.dp),
      backgroundColor = MaterialTheme.colorScheme.surface,
      borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)) {
        Column(
            modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              // Top row: small image + title
              Row(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically) {
                    HazardNewsImage(hazard = hazard, modifier = Modifier.size(48.dp))

                    Text(
                        text = hazard.description ?: (title ?: "Hazard"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                  }

              // Numeric severity snippet (e.g., "13590 ha")
              snippet
                  ?.takeIf { it.isNotBlank() }
                  ?.let { sevLine ->
                    Text(
                        text = sevLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.DarkGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                  }

              // Optional: severityText from API
              hazard.severityText
                  ?.takeIf { it.isNotBlank() }
                  ?.let { sevText ->
                    Text(
                        text = sevText,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                  }

              // Optional: date
              hazard.date
                  ?.takeIf { it.isNotBlank() }
                  ?.let { dateStr ->
                    Text(
                        text = formatDate(dateStr),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                  }

              // Hint that tapping opens the full article
              if (hazard.articleUrl != null) {
                Text(
                    text = stringResource(id = R.string.open_article_link),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold)
              }
            }
      }
}
