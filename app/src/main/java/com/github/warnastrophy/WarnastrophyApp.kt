package com.github.warnastrophy

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.warnastrophy.core.data.repository.HazardRepositoryProvider
import com.github.warnastrophy.core.model.GpsService
import com.github.warnastrophy.core.model.HazardsService
import com.github.warnastrophy.core.ui.home.HomeScreen
import com.github.warnastrophy.core.ui.map.MapScreen
import com.github.warnastrophy.core.ui.navigation.BottomNavigationBar
import com.github.warnastrophy.core.ui.navigation.Screen
import com.github.warnastrophy.core.ui.navigation.Screen.HOME
import com.github.warnastrophy.core.ui.navigation.Screen.MAP
import com.github.warnastrophy.core.ui.navigation.Screen.PROFILE
import com.github.warnastrophy.core.ui.navigation.TopBar
import com.github.warnastrophy.core.ui.profile.ProfileScreen
import com.github.warnastrophy.core.ui.theme.MainAppTheme
import com.google.android.gms.location.LocationServices

@Composable
fun WarnastrophyApp() {
  val context = LocalContext.current

  val navController = rememberNavController()

  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentScreen = Screen.valueOf(backStackEntry?.destination?.route ?: HOME.name)

  val locationClient = LocationServices.getFusedLocationProviderClient(context)
  val gpsService = GpsService(locationClient)

  val hazardsRepository = HazardRepositoryProvider.repository
  val hazardsService = HazardsService(hazardsRepository, gpsService)
  Scaffold(
      bottomBar = { BottomNavigationBar(currentScreen, navController) },
      topBar = { TopBar(currentScreen) }) { innerPadding ->
        NavHost(navController, HOME.name, modifier = Modifier.padding(innerPadding)) {
          // TODO: Replace with actual screens
          // TODO: Use string resources for your titles
          composable(HOME.name) { HomeScreen() }
          composable(MAP.name) {
            MapScreen(hazardsService = hazardsService, gpsService = gpsService)
          }
          composable(PROFILE.name) { ProfileScreen() }
        }
      }
}

@Preview(showBackground = true)
@Composable
fun MainAppPreview() {
  MainAppTheme { WarnastrophyApp() }
}
