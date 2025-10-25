package com.github.warnastrophy.core.ui.map

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.github.warnastrophy.R

/**
 * Contains map-related icons as ImageVectors. Provided by
 * [composeicons.com](https://composeicons.com/).
 */
object MapIcons {
  /**
   * Composable helper to load a vector drawable as an ImageVector. Call from a composable scope.
   */
  @Composable
  fun loadVectorFromDrawable(@DrawableRes resId: Int): ImageVector =
      ImageVector.vectorResource(resId)

  val Tsunami: ImageVector
    @Composable get() = loadVectorFromDrawable(R.drawable.material_symbols_outlined_tsunami)

  val Drought: ImageVector
    @Composable get() = loadVectorFromDrawable(R.drawable.material_symbols_outlined_water_voc)

  val Earthquake: ImageVector
    @Composable get() = loadVectorFromDrawable(R.drawable.material_symbols_outlined_earthquake)

  val Fire: ImageVector
    @Composable
    get() = loadVectorFromDrawable(R.drawable.material_symbols_outlined_local_fire_department)

  val Flood: ImageVector
    @Composable get() = loadVectorFromDrawable(R.drawable.material_symbols_outlined_flood)

  val Cyclone: ImageVector
    @Composable get() = loadVectorFromDrawable(R.drawable.material_symbols_outlined_storm)

  val Volcano: ImageVector
    @Composable get() = loadVectorFromDrawable(R.drawable.material_symbols_outlined_volcano)
}
