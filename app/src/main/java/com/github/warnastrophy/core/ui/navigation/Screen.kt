package com.github.warnastrophy.core.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import com.github.warnastrophy.R

object NavigationTestTags {
  const val TOP_BAR_TITLE = "topBarTitle"
  const val BOTTOM_NAV = "bottomNav"
  const val TAB_MAP = "tabMap"
  const val TAB_PROFILE = "tabProfile"
  const val TAB_HOME = "tabHome"

  const val TOP_BAR_PREVIEW = "topBarPreview"

  const val BOTTOM_NAV_PREVIEW = "bottomNavPreview"
  const val BOTTOM_BACK = "bottomBack"
  const val CONTACT_LIST = "contactList"
  const val HEALTH_CARD = "healthCard"
}

// TODO: add documentation
sealed class Screen(
    @StringRes val title: Int,
    val route: String,
    val hasBottomBar: Boolean = true,
    val hasTopBar: Boolean = true,
    val icon: ImageVector? = null,
    val isTopLevelDestination: Boolean = false
) {
  object Home :
      Screen(
          R.string.home_screen_title,
          icon = Icons.Filled.Home,
          route = "home",
          isTopLevelDestination = true)

  object Map :
      Screen(
          R.string.map_screen_title,
          icon = Icons.Filled.Place,
          route = "map",
          isTopLevelDestination = true)

  object Profile :
      Screen(
          R.string.profile_screen_title,
          icon = Icons.Filled.Person,
          route = "profile",
          isTopLevelDestination = true)

  object AddContact :
      Screen(R.string.add_contact_screen_title, icon = Icons.Filled.Add, route = "add_contact")

  object ContactList : Screen(R.string.contact_list_title, route = "contact_list")

  object HealthCard : Screen(R.string.health_card_screen_title, route = "health_card")

  data class EditContact(val contactID: String) :
      Screen(route = "edit_contact/${contactID}", title = R.string.edit_contact_screen_title) {
    companion object {
      const val route = "edit_contact/{id}"
    }
  }
}

val BOTTOM_NAVIGATION_BAR_SCREENS = setOf(Screen.Home, Screen.Map, Screen.Profile)

open class NavigationActions(private val navController: NavHostController) {
  /**
   * Navigate to the specified screen.
   *
   * @param screen The screen to navigate to
   */
  open fun navigateTo(screen: Screen) {
    if (screen.isTopLevelDestination && currentRoute() == screen.route) {
      return
    }
    navController.navigate(screen.route) {
      if (screen.isTopLevelDestination) {
        launchSingleTop = true
        popUpTo(screen.route) { inclusive = true }
      }
    }
  }

  /** Navigate back to the previous screen. */
  open fun goBack() {
    navController.popBackStack()
  }

  /**
   * Get the current route of the navigation controller.
   *
   * @return The current route
   */
  open fun currentRoute(): String {
    return navController.currentDestination?.route ?: ""
  }
}
