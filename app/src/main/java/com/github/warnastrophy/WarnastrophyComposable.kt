package com.github.warnastrophy

import android.widget.Toast
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.credentials.CredentialManager
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.warnastrophy.core.data.repository.NominatimRepository
import com.github.warnastrophy.core.data.service.NominatimService
import com.github.warnastrophy.core.data.service.StateManagerService
import com.github.warnastrophy.core.ui.common.ErrorHandler
import com.github.warnastrophy.core.ui.features.auth.SignInScreen
import com.github.warnastrophy.core.ui.features.contact.AddContactScreen
import com.github.warnastrophy.core.ui.features.contact.AddContactViewModel
import com.github.warnastrophy.core.ui.features.contact.ContactListScreen
import com.github.warnastrophy.core.ui.features.contact.ContactListViewModel
import com.github.warnastrophy.core.ui.features.contact.EditContactScreen
import com.github.warnastrophy.core.ui.features.contact.EditContactViewModel
import com.github.warnastrophy.core.ui.features.dashboard.DashboardScreen
import com.github.warnastrophy.core.ui.features.health.HealthCardScreen
import com.github.warnastrophy.core.ui.features.map.MapScreen
import com.github.warnastrophy.core.ui.features.map.MapViewModel
import com.github.warnastrophy.core.ui.features.profile.ProfileScreen
import com.github.warnastrophy.core.ui.features.profile.preferences.DangerModePreferencesScreen
import com.github.warnastrophy.core.ui.features.profile.preferences.DangerModePreferencesViewModel
import com.github.warnastrophy.core.ui.navigation.BottomNavigationBar
import com.github.warnastrophy.core.ui.navigation.NavigationActions
import com.github.warnastrophy.core.ui.navigation.Screen
import com.github.warnastrophy.core.ui.navigation.Screen.Dashboard
import com.github.warnastrophy.core.ui.navigation.Screen.Map
import com.github.warnastrophy.core.ui.navigation.Screen.Profile
import com.github.warnastrophy.core.ui.navigation.Screen.SignIn
import com.github.warnastrophy.core.ui.navigation.TopBar
import com.github.warnastrophy.core.util.AppConfig
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth

/** Object containing test tags for the WarnastrophyApp. */
object WarnastrophyAppTestTags {
  const val MAIN_SCREEN = "mainScreen"
}

@Composable
fun WarnastrophyComposable(
    mockMapScreen: (@Composable () -> Unit)? = null
) {
  val context = LocalContext.current

  val credentialManager = CredentialManager.create(context)
  val firebaseAuth = remember { FirebaseAuth.getInstance() }

  var userId by remember {
    mutableStateOf(firebaseAuth.currentUser?.uid ?: AppConfig.defaultUserId)
  }

  DisposableEffect(Unit) {
    val listener =
        FirebaseAuth.AuthStateListener { auth ->
          userId = auth.currentUser?.uid ?: AppConfig.defaultUserId
        }
    firebaseAuth.addAuthStateListener(listener)
    onDispose { firebaseAuth.removeAuthStateListener(listener) }
  }

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
        SignIn.route -> SignIn
        Screen.DangerModePreferences.route -> Screen.DangerModePreferences

        // Default/Fallback: If no match, fallback to the Dashboard screen object.
        else -> Dashboard
      }

  // val startDestination = Dashboard.route
  val startDestination =
      if (FirebaseAuth.getInstance().currentUser == null) SignIn.route else Dashboard.route

  val locationClient = LocationServices.getFusedLocationProviderClient(context)

  val errorHandler = ErrorHandler()

  val gpsService = remember { StateManagerService.gpsService }
  val hazardsService = remember { StateManagerService.hazardsService }
  val permissionManager = remember { StateManagerService.permissionManager }

  val contactListViewModel = ContactListViewModel(userId = userId)
  val editContactViewModel = EditContactViewModel(userId = userId)
  val addContactViewModel = AddContactViewModel(userId = userId)

  val nominatimRepository = NominatimRepository()
  val nominatimService = NominatimService(nominatimRepository)

  val mapViewModel = MapViewModel(gpsService, hazardsService, permissionManager, nominatimService)

  Scaffold(
      modifier = Modifier.testTag(WarnastrophyAppTestTags.MAIN_SCREEN),
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
              composable(SignIn.route) {
                SignInScreen(
                    credentialManager = credentialManager,
                    onSignedIn = { navigationActions.navigateTo(Dashboard) })
              }
              composable(Dashboard.route) {
                DashboardScreen(
                    hazardsService = hazardsService,
                    mapScreen = {
                      mockMapScreen?.invoke()
                          ?: MapScreen(viewModel = mapViewModel, isPreview = true)
                    },
                    onHealthCardClick = { navigationActions.navigateTo(Screen.HealthCard) },
                    onEmergencyContactsClick = { navigationActions.navigateTo(Screen.ContactList) })
              }
              composable(Map.route) {
                mockMapScreen?.invoke() ?: MapScreen(viewModel = mapViewModel)
              }
              composable(Profile.route) {
                ProfileScreen(
                    onEmergencyContactsClick = { navigationActions.navigateTo(Screen.ContactList) },
                    onHealthCardClick = { navigationActions.navigateTo(Screen.HealthCard) },
                    onLogout = { navigationActions.navigateTo(SignIn) },
                    onDangerModePreferencesClick = {
                      navigationActions.navigateTo(Screen.DangerModePreferences)
                    })
              }
              composable(Screen.ContactList.route) {
                ContactListScreen(
                    contactListViewModel = contactListViewModel,
                    onContactClick = { navigationActions.navigateTo(Screen.EditContact(it.id)) },
                    onAddButtonClick = { navigationActions.navigateTo(Screen.AddContact) })
              }
              composable(Screen.AddContact.route) {
                AddContactScreen(
                    userId = userId,
                    addContactViewModel = addContactViewModel,
                    onDone = { navigationActions.goBack() })
              }
              composable(Screen.HealthCard.route) {
                HealthCardScreen(userId = userId, onDone = { navController.popBackStack() })
              }
              composable(route = Screen.EditContact.route) { navBackStackEntry ->
                val id = navBackStackEntry.arguments?.getString("id")

                id?.let {
                  EditContactScreen(
                      editContactViewModel = editContactViewModel,
                      onDone = { navigationActions.goBack() },
                      contactID = id)
                }
                    ?: run {
                      Toast.makeText(context, "Contact ID is null", Toast.LENGTH_SHORT).show()
                    }
              }
              composable(Screen.DangerModePreferences.route) {
                DangerModePreferencesScreen(
                    viewModel =
                        DangerModePreferencesViewModel(
                            permissionManager = permissionManager,
                            userPreferencesRepository =
                                StateManagerService.userPreferencesRepository))
              }
            }
      }
}
