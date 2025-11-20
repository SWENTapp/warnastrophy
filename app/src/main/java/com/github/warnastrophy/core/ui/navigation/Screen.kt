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
  const val TOP_BAR_ERROR_ICON = "TopBar_ErrorIcon"
  const val BOTTOM_NAV = "bottomNav"
  const val TAB_MAP = "tabMap"
  const val TAB_PROFILE = "tabProfile"
  const val TAB_DASHBOARD = "tabDashboard"
  const val TOP_BAR_PREVIEW = "topBarPreview"
  const val BOTTOM_NAV_PREVIEW = "bottomNavPreview"
  const val BUTTON_BACK = "buttonBack"
  const val CONTACT_LIST = "contactList"
  const val HEALTH_CARD = "healthCard"
  const val LOGOUT = "logout"
  const val SIGN_IN = "signIn"
}

/** Object for holding navigation routes. */
object NavRoutes {
  const val DASHBOARD = "dashboard"
  const val MAP = "map"
  const val PROFILE = "profile"
  const val ADD_CONTACT = "add_contact"
  const val CONTACT_LIST = "contact_list"
  const val HEALTH_CARD = "health_card"
  const val EDIT_CONTACT = "edit_contact"
  const val SIGN_IN = "sign-in"
  const val DANGER_MODE_PREFERENCES = "danger_mode_preferences"
}

/**
 * A sealed class representing all possible navigation destinations within the application.
 *
 * Each object or data class within this sealed class defines a specific screen, encapsulating its
 * properties such as the title, route, and UI element visibility. This centralized definition helps
 * in managing navigation logic and ensures type safety.
 *
 * @property title The string resource ID for the screen's title, typically shown in the top bar.
 * @property route The unique string identifier for the navigation route. This is used by the
 *   `NavController`.
 * @property hasBottomBar A boolean flag indicating whether the bottom navigation bar should be
 *   visible on this screen. Defaults to `true`.
 * @property hasTopBar A boolean flag indicating whether the top app bar should be visible on this
 *   screen. Defaults to `true`.
 * @property icon The `ImageVector` to be displayed for this screen, usually in the bottom
 *   navigation bar. Can be `null`.
 * @property isTopLevelDestination A boolean flag indicating if this screen is a top-level
 *   destination in the navigation graph. Top-level destinations are typically accessible from the
 *   bottom navigation bar and have special back stack handling. Defaults to `false`.
 */
sealed class Screen(
    @StringRes val title: Int,
    val route: String,
    val hasBottomBar: Boolean = true,
    val hasTopBar: Boolean = true,
    val icon: ImageVector? = null,
    val isTopLevelDestination: Boolean = false
) {
  object Dashboard :
      Screen(
          R.string.dashboard_screen_title,
          icon = Icons.Filled.Home,
          route = NavRoutes.DASHBOARD,
          isTopLevelDestination = true)

  object Map :
      Screen(
          R.string.map_screen_title,
          icon = Icons.Filled.Place,
          route = NavRoutes.MAP,
          isTopLevelDestination = true)

  object Profile :
      Screen(
          R.string.profile_screen_title,
          icon = Icons.Filled.Person,
          route = NavRoutes.PROFILE,
          isTopLevelDestination = true)

  object AddContact :
      Screen(
          R.string.add_contact_screen_title,
          icon = Icons.Filled.Add,
          route = NavRoutes.ADD_CONTACT)

  object ContactList : Screen(R.string.contact_list_title, route = NavRoutes.CONTACT_LIST)

  object HealthCard : Screen(R.string.health_card_screen_title, route = NavRoutes.HEALTH_CARD)

  data class EditContact(val contactID: String) :
      Screen(
          route = "${NavRoutes.EDIT_CONTACT}/$contactID",
          title = R.string.edit_contact_screen_title) {
    companion object {
      val route = "${NavRoutes.EDIT_CONTACT}/{id}"
    }
  }

  object SignIn :
      Screen(
          R.string.sign_in_title,
          route = NavRoutes.SIGN_IN,
          hasBottomBar = false,
          hasTopBar = false)
}

val BOTTOM_NAVIGATION_BAR_SCREENS = setOf(Screen.Dashboard, Screen.Map, Screen.Profile)

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
