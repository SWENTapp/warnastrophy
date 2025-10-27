package com.github.warnastrophy.core.ui.map

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import com.github.warnastrophy.R

/**
 * Contains map-related icons as ImageVectors. Provided by
 * [composeicons.com](https://composeicons.com/).
 */
object MapIcons {
  // Test tags for easy access from tests
  object Tags {
    const val TSUNAMI = "map_icon_tsunami"
    const val DROUGHT = "map_icon_drought"
    const val EARTHQUAKE = "map_icon_earthquake"
    const val FIRE = "map_icon_fire"
    const val FLOOD= "map_icon_flood"
    const val CYCLONE = "map_icon_cyclone"
    const val VOLCANO = "map_icon_volcano"
  }

  /**
   * Composable helper to load a vector drawable as an ImageVector. Call from a composable scope.
   */
  @Composable
  fun loadVectorFromDrawable(@DrawableRes resId: Int): ImageVector =
      ImageVector.vectorResource(resId)

    @Composable
  private fun IconWithTag(@DrawableRes resId: Int, tag: String, contentDescription: String? = null) {
    androidx.compose.material3.Icon(
      imageVector = loadVectorFromDrawable(resId),
      contentDescription = contentDescription,
      modifier = androidx.compose.ui.Modifier.testTag(tag)
    )
  }

  @Composable fun Tsunami() = IconWithTag(R.drawable.material_symbols_outlined_tsunami, Tags.TSUNAMI)
  @Composable fun Drought() = IconWithTag(R.drawable.material_symbols_outlined_water_voc, Tags.DROUGHT)
  @Composable fun Earthquake() = IconWithTag(R.drawable.material_symbols_outlined_earthquake, Tags.EARTHQUAKE)
  @Composable fun Fire() = IconWithTag(R.drawable.material_symbols_outlined_local_fire_department, Tags.FIRE)
  @Composable fun Flood() = IconWithTag(R.drawable.material_symbols_outlined_flood, Tags.FLOOD)
  @Composable fun Cyclone() = IconWithTag(R.drawable.material_symbols_outlined_storm, Tags.CYCLONE)
  @Composable fun Volcano() = IconWithTag(R.drawable.material_symbols_outlined_volcano, Tags.VOLCANO)
}
