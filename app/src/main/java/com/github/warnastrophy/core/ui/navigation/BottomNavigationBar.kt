package com.github.warnastrophy.core.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun BottomNavigationBar(currentScreen: Screen, navigateToScreen: (String) -> Unit = {}) {
  val ctx = LocalContext.current

  NavigationBar {
    Screen.entries.forEach { screen ->
      NavigationBarItem(
          // FIXME: Replace with actual icons for each screen
          // FIXME: Use correctly centered labels
          icon = {
            when (screen) {
              Screen.HOME -> Icon(Icons.Filled.Home, contentDescription = null)
              Screen.MAP -> Icon(Icons.Filled.Place, contentDescription = null)
              Screen.PROFILE -> Icon(Icons.Filled.Person, contentDescription = null)
            }
          },
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
