package com.github.warnastrophy.core.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.ui.graphics.vector.ImageVector
import com.github.warnastrophy.R

/**
 * Screen defines the different screens in the app along with their titles.
 *
 * @param title The resource ID for the screen title.
 */

object NavigationTestTags {
    const val TOP_BAR_TITLE = "topBarTitle"
    const val BOTTOM_NAV = "bottomNav"
    const val TAB_MAP = "tabMap"
    const val TAB_PROFILE = "tabProfile"
    const val TAB_HOME = "tabHome"
}
enum class Screen(
    @StringRes val title: Int,
    val hasBottomBar: Boolean = true,
    val hasTopBar: Boolean = true,
    val icon: ImageVector? = null
) {
  HOME(R.string.home_screen_title, icon = Icons.Filled.Home),
  MAP(R.string.map_screen_title, icon = Icons.Filled.Place),
  PROFILE(R.string.profile_screen_title, icon = Icons.Filled.Person),
}

val BOTTOM_NAVIGATION_BAR_SCREENS = setOf(Screen.HOME, Screen.MAP, Screen.PROFILE)
