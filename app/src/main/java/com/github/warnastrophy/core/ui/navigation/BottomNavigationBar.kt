package com.github.warnastrophy.core.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

private fun tagFor(screen: Screen): String =
    when (screen) {
      Screen.Dashboard -> NavigationTestTags.TAB_DASHBOARD
      Screen.Map -> NavigationTestTags.TAB_MAP
      Screen.Profile -> NavigationTestTags.TAB_PROFILE
      else -> ""
    // TODO: add test tags if needed
    }

@Composable
fun BottomNavigationBar(currentScreen: Screen, navController: NavController) {
  if (!currentScreen.hasBottomBar) return

  val ctx = LocalContext.current

  NavigationBar(modifier = Modifier.testTag(NavigationTestTags.BOTTOM_NAV)) {
    BOTTOM_NAVIGATION_BAR_SCREENS.forEach { screen ->
      NavigationBarItem(
          // FIXME: Use correctly centered labels ,
          modifier = Modifier.testTag(tagFor(screen)),
          icon = { screen.icon?.let { Icon(it, contentDescription = null) } },
          label = {
            Text(
                ctx.getString(screen.title),
                modifier = Modifier.testTag(tagFor(screen)),
            )
          },
          selected = currentScreen == screen,
          onClick = {
            navController.navigate(screen.route) {
              // Forward navigation
              popUpTo(navController.graph.startDestinationId) { saveState = true }
              // Avoid multiple copies of the same destination when spamming the same item
              launchSingleTop = true
              // Allow staying on the same screen after activity recreation (rotation, kill ?)
              restoreState = true
            }
          })
    }
  }
}

@Preview
@Composable
fun BottomNavigationBarPreview() {
  androidx.compose.foundation.layout.Box(
      modifier = Modifier.testTag(NavigationTestTags.BOTTOM_NAV_PREVIEW)) {
        BottomNavigationBar(Screen.Dashboard, rememberNavController())
      }
}
