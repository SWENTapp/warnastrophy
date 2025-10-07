package com.github.warnastrophy.core.ui.navigation

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun BottomNavigationBar(currentScreen: Screen, navigateToScreen: (String) -> Unit = {}) {
  NavigationBar {
    Screen.entries.forEach { screen ->
      NavigationBarItem(
          icon = { /* TODO: Add icon */},
          label = { Text(screen.title) },
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
