package com.github.warnastrophy.core.ui.navigation

import androidx.annotation.StringRes
import com.github.warnastrophy.R

/**
 * Screen defines the different screens in the app along with their titles.
 *
 * @param title The resource ID for the screen title.
 */
enum class Screen(@StringRes val title: Int) {
  HOME(R.string.home_screen_title),
  MAP(R.string.map_screen_title),
  PROFILE(R.string.profile_screen_title)
}
