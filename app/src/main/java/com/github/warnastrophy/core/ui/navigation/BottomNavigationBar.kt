package com.github.warnastrophy.core.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@Composable
fun BottomNavigationBar(currentScreen: Screen, navController: NavController) {
  if (!currentScreen.hasBottomBar) return

  val ctx = LocalContext.current

  NavigationBar {
    BOTTOM_NAVIGATION_BAR_SCREENS.forEach { screen ->
      NavigationBarItem(
          // FIXME: Use correctly centered labels
          icon = { screen.icon?.let { Icon(it, contentDescription = null) } },
          label = { Text(ctx.getString(screen.title)) },
          selected = currentScreen == screen,
          onClick = {
            navController.navigate(screen.name) {
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
  BottomNavigationBar(Screen.HOME, rememberNavController())
}
