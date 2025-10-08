package com.github.warnastrophy

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.warnastrophy.core.ui.map.MapScreen
import com.github.warnastrophy.core.ui.navigation.BottomNavigationBar
import com.github.warnastrophy.core.ui.navigation.Screen
import com.github.warnastrophy.core.ui.navigation.Screen.HOME
import com.github.warnastrophy.core.ui.navigation.Screen.MAP
import com.github.warnastrophy.core.ui.navigation.Screen.PROFILE
import com.github.warnastrophy.core.ui.theme.MainAppTheme

@Composable
fun WarnastrophyApp() {
  val ctx = LocalContext.current

  val navController = rememberNavController()

  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentScreen = Screen.valueOf(backStackEntry?.destination?.route ?: HOME.name)

  Scaffold(bottomBar = { BottomNavigationBar(currentScreen) { navController.navigate(it) } }) {
      innerPadding ->
    NavHost(navController, HOME.name, modifier = Modifier.padding(innerPadding)) {
      // TODO: Replace with actual screens
      // TODO: Use string resources for your titles
      composable(HOME.name) { Text(ctx.getString(HOME.title)) }
      composable(MAP.name) { MapScreen() }
      composable(PROFILE.name) { Text(ctx.getString(PROFILE.title)) }
    }
  }
}

@Preview(showBackground = true)
@Composable
fun MainAppPreview() {
  MainAppTheme { WarnastrophyApp() }
}
