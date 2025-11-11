package com.github.warnastrophy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.github.warnastrophy.core.data.repository.ContactRepositoryProvider
import com.github.warnastrophy.core.data.repository.HealthCardRepositoryProvider
import com.github.warnastrophy.core.ui.theme.MainAppTheme
import com.google.firebase.BuildConfig
import com.google.firebase.FirebaseApp

/**
 * `MainActivity` is the entry point of the application. It sets up the content view with the
 * `onCreate` methods. You can run the app by running the `app` configuration in Android Studio. NB:
 * Make sure you have an Android emulator running or a physical device connected.
 */
class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    FirebaseApp.initializeApp(this)
    if (BuildConfig.DEBUG) {
      HealthCardRepositoryProvider.useEmulator("10.0.2.2", 8080)
    }
    HealthCardRepositoryProvider.init(applicationContext)

    ContactRepositoryProvider.init(applicationContext)
    setContent { MainAppTheme { Surface(modifier = Modifier.fillMaxSize()) { WarnastrophyApp() } } }
  }
}
