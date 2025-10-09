package com.github.warnastrophy.core.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun BottomNavigationBar(currentScreen: Screen, navigateToScreen: (String) -> Unit = {}) {
  if (!currentScreen.hasBottomBar) return

  val ctx = LocalContext.current

  NavigationBar {
    BOTTOM_NAVIGATION_BAR_SCREENS.forEach { screen ->
      NavigationBarItem(
          // FIXME: Use correctly centered labels
          icon = { screen.icon?.let { Icon(it, contentDescription = null) } },
          label = { Text(ctx.getString(screen.title)) },
          selected = currentScreen == screen,
          onClick = { navigateToScreen(screen.name) })
    }
  }
}

@Preview
@Composable
fun BottomNavigationBarPreview() {
  BottomNavigationBar(Screen.HOME)
}
