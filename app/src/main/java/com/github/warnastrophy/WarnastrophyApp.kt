package com.github.warnastrophy

import android.util.Log
import android.widget.Toast
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
import com.github.warnastrophy.core.data.repository.HazardsRepository
import com.github.warnastrophy.core.model.GpsService
import com.github.warnastrophy.core.model.HazardsService
import com.github.warnastrophy.core.ui.healthcard.HealthCardScreen
import com.github.warnastrophy.core.ui.home.HomeScreen
import com.github.warnastrophy.core.ui.map.MapScreen
import com.github.warnastrophy.core.ui.navigation.BottomNavigationBar
import com.github.warnastrophy.core.ui.navigation.NavigationActions
import com.github.warnastrophy.core.ui.navigation.Screen
import com.github.warnastrophy.core.ui.navigation.Screen.Home
import com.github.warnastrophy.core.ui.navigation.Screen.Map
import com.github.warnastrophy.core.ui.navigation.Screen.Profile
import com.github.warnastrophy.core.ui.navigation.TopBar
import com.github.warnastrophy.core.ui.profile.ProfileScreen
import com.github.warnastrophy.core.ui.profile.contact.AddContactScreen
import com.github.warnastrophy.core.ui.profile.contact.ContactListScreen
import com.github.warnastrophy.core.ui.profile.contact.EditContactScreen
import com.github.warnastrophy.core.ui.theme.MainAppTheme
import com.google.android.gms.location.LocationServices

@Composable
fun WarnastrophyApp() {
  val ctx = LocalContext.current

  val navController = rememberNavController()
  val navigationActions = NavigationActions(navController)

  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = backStackEntry?.destination?.route
  val currentScreen =
      when (currentRoute) {
        Home.route -> Home
        Map.route -> Map
        Profile.route -> Profile
        Screen.AddContact.route -> Screen.AddContact
        Screen.ContactList.route -> Screen.ContactList
        Screen.HealthCard.route -> Screen.HealthCard

        // For screens with arguments, use the route pattern that matches the base route.
        // The route string from backStackEntry will be 'edit_contact/{id}' if defined
        // with arguments, or null/fallback.
        Screen.EditContact.route -> Screen.EditContact(contactID = "") // Match the base route

        // Default/Fallback: If no match, fallback to the Home screen object.
        else -> Home
      }

  val startDestination = Home.route
  val locationClient = LocationServices.getFusedLocationProviderClient(LocalContext.current)
  val gpsService = GpsService(locationClient)

  val hazardsRepository = HazardsRepository()
  val hazardsService = HazardsService(hazardsRepository, gpsService)
  Scaffold(
      bottomBar = { BottomNavigationBar(currentScreen, navController) },
      topBar = {
        TopBar(
            currentScreen,
            canNavigateBack =
                currentScreen is Screen.AddContact ||
                    currentScreen is Screen.EditContact ||
                    currentScreen is Screen.ContactList ||
                    currentScreen is Screen.HealthCard,
            navigateUp = { navigationActions.goBack() })
      }) { innerPadding ->
        NavHost(
            navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)) {
              composable(Home.route) { HomeScreen() }
              composable(Map.route) {
                MapScreen(hazardsService = hazardsService, gpsService = gpsService)
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
                      Log.e("EditContactScreen", "Contact ID is null")
                      Toast.makeText(ctx, "Contact ID is null", Toast.LENGTH_SHORT).show()
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
