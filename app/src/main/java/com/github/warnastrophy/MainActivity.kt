package com.github.warnastrophy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.warnastrophy.core.data.provider.ActivityRepositoryProvider
import com.github.warnastrophy.core.data.provider.ContactRepositoryProvider
import com.github.warnastrophy.core.data.provider.HealthCardRepositoryProvider
import com.github.warnastrophy.core.data.repository.UserPreferencesRepositoryLocal
import com.github.warnastrophy.core.data.service.StateManagerService
import com.github.warnastrophy.core.ui.features.profile.LocalThemeViewModel
import com.github.warnastrophy.core.ui.features.profile.ThemeViewModel
import com.github.warnastrophy.core.ui.features.profile.ThemeViewModelFactory
import com.github.warnastrophy.core.ui.theme.MainAppTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

private val ComponentActivity.dataStore by preferencesDataStore(name = "user_preferences")

/**
 * `MainActivity` is the entry point of the application. It initializes Firebase, sets up the data
 * repositories, and provides the UI for the app. It also handles the theme configuration and passes
 * the theme-related state to the composables.
 *
 * @see [setContent] - Used to set up the Compose UI.
 */
class MainActivity : ComponentActivity() {

  /**
   * Sets the content of the `MainActivity` with the application's theme and UI. This method is
   * invoked during the `onCreate` lifecycle callback to initialize the UI.
   */
  private fun showUI() {
    setContent {
      val repository = UserPreferencesRepositoryLocal(dataStore)
      val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModelFactory(repository))

      ThemedApp(themeViewModel = themeViewModel)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    FirebaseApp.initializeApp(this)

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    HealthCardRepositoryProvider.useHybridEncrypted(applicationContext, db, auth)
    ContactRepositoryProvider.initHybrid(applicationContext, db)
    ActivityRepositoryProvider.init()
    StateManagerService.init(applicationContext)

    showUI()
  }
}

/**
 * A composable function that provides the app's theme and UI content. It uses [MainAppTheme] to
 * apply the selected theme (dark or light) and sets up the environment for composables with
 * [CompositionLocalProvider].
 *
 * @param themeViewModel The [ThemeViewModel] responsible for managing theme-related state.
 */
@Composable
private fun ThemedApp(themeViewModel: ThemeViewModel) {
  val isDarkMode by themeViewModel.isDarkMode.collectAsState()
  val systemDarkTheme = isSystemInDarkTheme()

  val useDarkTheme = isDarkMode ?: systemDarkTheme

  MainAppTheme(darkTheme = useDarkTheme) {
    CompositionLocalProvider(LocalThemeViewModel provides themeViewModel) {
      Surface(Modifier.fillMaxSize()) { WarnastrophyComposable() }
    }
  }
}
