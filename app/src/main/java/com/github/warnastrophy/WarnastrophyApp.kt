package com.github.warnastrophy

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.warnastrophy.core.ui.navigation.BottomNavigationBar
import com.github.warnastrophy.core.ui.navigation.Screen
import com.github.warnastrophy.core.ui.theme.MainAppTheme

@Composable
fun WarnastrophyApp() {
  val navController = rememberNavController()

  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentScreen = Screen.valueOf(backStackEntry?.destination?.route ?: Screen.HOME.name)

  Scaffold(bottomBar = { BottomNavigationBar(currentScreen) { navController.navigate(it) } }) {
      innerPadding ->
    NavHost(navController, Screen.HOME.name, modifier = Modifier.padding(innerPadding)) {
      // TODO: Replace with actual screens
      composable(Screen.HOME.name) { Text("Home Screen") }
      composable(Screen.MAP.name) { Text("Map Screen") }
      composable(Screen.PROFILE.name) { Text("Profile Screen") }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun MainAppPreview() {
  MainAppTheme { WarnastrophyApp() }
}
