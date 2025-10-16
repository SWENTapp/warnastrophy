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
import com.github.warnastrophy.core.ui.home.HomeScreen
import com.github.warnastrophy.core.ui.map.MapScreen
import com.github.warnastrophy.core.ui.navigation.BottomNavigationBar
import com.github.warnastrophy.core.ui.navigation.Screen
import com.github.warnastrophy.core.ui.navigation.Screen.HOME
import com.github.warnastrophy.core.ui.navigation.Screen.MAP
import com.github.warnastrophy.core.ui.navigation.Screen.PROFILE
import com.github.warnastrophy.core.ui.navigation.TopBar
import com.github.warnastrophy.core.ui.profile.ProfileScreen
import com.github.warnastrophy.core.ui.profile.contact.AddContactScreen
import com.github.warnastrophy.core.ui.profile.contact.ContactListScreen
import com.github.warnastrophy.core.ui.profile.contact.EditContactScreen
import com.github.warnastrophy.core.ui.theme.MainAppTheme

@Composable
fun WarnastrophyApp() {
  val ctx = LocalContext.current

  val navController = rememberNavController()

  val backStackEntry by navController.currentBackStackEntryAsState()
  val currentScreen = Screen.valueOf(backStackEntry?.destination?.route ?: HOME.name)

  Scaffold(
      bottomBar = { BottomNavigationBar(currentScreen, navController) },
      topBar = {
        TopBar(
            currentScreen,
            canNavigateBack =
                currentScreen.title == Screen.ADD_CONTACT.title ||
                    currentScreen.title == Screen.EDIT_CONTACT.title ||
                    currentScreen.title == Screen.CONTACT_LIST.title,
            navigateUp = { navController.navigateUp() })
      }) { innerPadding ->
        NavHost(navController, HOME.name, modifier = Modifier.padding(innerPadding)) {
          // TODO: Replace with actual screens
          // TODO: Use string resources for your titles
          composable(HOME.name) { HomeScreen() }
          composable(MAP.name) { MapScreen() }
          composable(PROFILE.name) {
            ProfileScreen(onContactListClick = { navController.navigate(Screen.CONTACT_LIST.name) })
          }
          composable(Screen.CONTACT_LIST.name) {
            ContactListScreen(
                onContactClick = { navController.navigate(Screen.EDIT_CONTACT.name) },
                onAddButtonClick = { navController.navigate(Screen.ADD_CONTACT.name) })
          }
          composable(Screen.ADD_CONTACT.name) { AddContactScreen() }
          composable(Screen.EDIT_CONTACT.name) { navBackStackEntry ->
            val id = navBackStackEntry.arguments?.getString("id")

            id?.let {
              EditContactScreen(
                  onDone = { navController.navigate(Screen.PROFILE.name) }, contactID = id)
            }
                ?: run {
                  Log.e("EditContactScreen", "ToDo UID is null")
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
