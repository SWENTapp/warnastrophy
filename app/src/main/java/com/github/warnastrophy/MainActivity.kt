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
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.github.warnastrophy.core.data.provider.ContactRepositoryProvider
import com.github.warnastrophy.core.data.provider.HealthCardRepositoryProvider
import com.github.warnastrophy.core.data.service.StateManagerService
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.warnastrophy.core.data.repository.ContactRepositoryProvider
import com.github.warnastrophy.core.data.repository.HealthCardRepositoryProvider
import com.github.warnastrophy.core.data.repository.UserPreferencesRepositoryLocal
import com.github.warnastrophy.core.data.service.ServiceStateManager
import com.github.warnastrophy.core.ui.features.profile.LocalThemeViewModel
import com.github.warnastrophy.core.ui.features.profile.ThemeViewModel
import com.github.warnastrophy.core.ui.features.profile.ThemeViewModelFactory
import com.github.warnastrophy.core.ui.theme.MainAppTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

private val ComponentActivity.dataStore by preferencesDataStore(name = "user_preferences")

/**
 * `MainActivity` is the entry point of the application. It sets up the content view with the
 * `onCreate` methods. You can run the app by running the `app` configuration in Android Studio. NB:
 * Make sure you have an Android emulator running or a physical device connected.
 */
class MainActivity : ComponentActivity() {

  private fun showUI() {
    // setContent { MainAppTheme { Surface(Modifier.fillMaxSize()) { WarnastrophyComposable() } } }
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
    ContactRepositoryProvider.init(applicationContext)
    StateManagerService.init(applicationContext)

    showUI()
  }
}

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
