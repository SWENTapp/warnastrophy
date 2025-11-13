package com.github.warnastrophy

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.warnastrophy.core.data.permissions.PermissionManager
import com.github.warnastrophy.core.data.repository.HazardRepositoryProvider
import com.github.warnastrophy.core.data.service.GpsServiceFactory
import com.github.warnastrophy.core.data.service.HazardsServiceFactory
import com.github.warnastrophy.core.ui.common.ErrorHandler
import com.github.warnastrophy.core.ui.features.contact.AddContactScreen
import com.github.warnastrophy.core.ui.features.contact.ContactListScreen
import com.github.warnastrophy.core.ui.features.contact.EditContactScreen
import com.github.warnastrophy.core.ui.features.dashboard.DashboardScreen
import com.github.warnastrophy.core.ui.features.health.HealthCardScreen
import com.github.warnastrophy.core.ui.features.map.MapScreen
import com.github.warnastrophy.core.ui.features.map.MapViewModel
import com.github.warnastrophy.core.ui.features.map.MapViewModelFactory
import com.github.warnastrophy.core.ui.features.profile.ProfileScreen
import com.github.warnastrophy.core.ui.navigation.BottomNavigationBar
import com.github.warnastrophy.core.ui.navigation.NavigationActions
import com.github.warnastrophy.core.ui.navigation.Screen
import com.github.warnastrophy.core.ui.navigation.Screen.Dashboard
import com.github.warnastrophy.core.ui.navigation.Screen.Map
import com.github.warnastrophy.core.ui.navigation.Screen.Profile
import com.github.warnastrophy.core.ui.navigation.TopBar
import com.github.warnastrophy.core.ui.theme.MainAppTheme
import com.google.android.gms.location.LocationServices

@Composable
fun WarnastrophyApp(mockMapScreen: (@Composable () -> Unit)? = null) {
  val context = LocalContext.current

  val navController = rememberNavController()
  val navigationActions = NavigationActions(navController)

  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route
  val currentScreen =
      when (currentRoute) {
        Dashboard.route -> Dashboard
        Map.route -> Map
        Profile.route -> Profile
        Screen.AddContact.route -> Screen.AddContact
        Screen.ContactList.route -> Screen.ContactList
        Screen.HealthCard.route -> Screen.HealthCard
        // For screens with arguments, use the route pattern that matches the base route.
        // The route string from backStackEntry will be 'edit_contact/{id}' if defined
        // with arguments, or null/fallback.
        Screen.EditContact.route -> Screen.EditContact(contactID = "") // Match the base route

        // Default/Fallback: If no match, fallback to the Dashboard screen object.
        else -> Dashboard
      }

  val startDestination = Dashboard.route
  val locationClient = LocationServices.getFusedLocationProviderClient(context)

  val errorHandler = ErrorHandler()

  val gpsServiceFactory = remember { GpsServiceFactory(locationClient, errorHandler) }
  val gpsService = remember { gpsServiceFactory.create() }

  val hazardsRepository = HazardRepositoryProvider.repository

  val hazardsServiceFactory = remember {
    HazardsServiceFactory(hazardsRepository, gpsService, errorHandler)
  }
  val hazardsService = remember { hazardsServiceFactory.create() }

  val permissionManager = PermissionManager(context)
  val mapViewModelFactory = remember {
    MapViewModelFactory(gpsService, hazardsService, permissionManager)
  }

  val mapScreen =
      @Composable {
        MapScreen(viewModel = MapViewModel(gpsService, hazardsService, permissionManager))
      }

  Scaffold(
      bottomBar = { BottomNavigationBar(currentScreen, navController) },
      topBar = {
        TopBar(
            currentScreen,
            canNavigateBack = !currentScreen.isTopLevelDestination,
            navigateUp = { navigationActions.goBack() },
            errorHandler = errorHandler)
      }) { innerPadding ->
        NavHost(
            navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)) {
              composable(Dashboard.route) {
                DashboardScreen(
                    hazardsService = hazardsService,
                    mapScreen = { mockMapScreen?.invoke() ?: mapScreen() },
                    onHealthCardClick = { navigationActions.navigateTo(Screen.HealthCard) },
                    onEmergencyContactsClick = { navigationActions.navigateTo(Screen.ContactList) })
              }
              composable(Map.route) {
                mockMapScreen?.invoke()
                    ?: MapScreen(
                        viewModel = MapViewModel(gpsService, hazardsService, permissionManager))
              }
              composable(Profile.route) {
                ProfileScreen(
                    onEmergencyContactsClick = { navigationActions.navigateTo(Screen.ContactList) },
                    onHealthCardClick = { navigationActions.navigateTo(Screen.HealthCard) })
              }
              composable(Screen.ContactList.route) {
                ContactListScreen(
                    onContactClick = { navigationActions.navigateTo(Screen.EditContact(it.id)) },
                    onAddButtonClick = { navigationActions.navigateTo(Screen.AddContact) })
              }
              composable(Screen.AddContact.route) {
                AddContactScreen(onDone = { navigationActions.goBack() })
              }
              composable(Screen.HealthCard.route) { HealthCardScreen() }
              composable(route = Screen.EditContact.route) { navBackStackEntry ->
                val id = navBackStackEntry.arguments?.getString("id")

                id?.let {
                  EditContactScreen(onDone = { navigationActions.goBack() }, contactID = id)
                }
                    ?: run {
                      Toast.makeText(context, "Contact ID is null", Toast.LENGTH_SHORT).show()
                    }
              }
            }
      }
}

@Preview(showBackground = true)
@Composable
fun MainAppPreview() {
  MainAppTheme { WarnastrophyApp() }
}
