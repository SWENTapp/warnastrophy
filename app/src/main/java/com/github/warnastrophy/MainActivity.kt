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


  private fun showUI() {
    setContent {
      MainAppTheme { Surface(Modifier.fillMaxSize()) { WarnastrophyApp() } }
    }
  }
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    FirebaseApp.initializeApp(this)
    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
    val db   = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    if (BuildConfig.DEBUG) {
      auth.useEmulator("10.0.2.2", 9099)
      db.useEmulator("10.0.2.2", 8080)
    }

    // Route HealthCards to Firestore + app-layer encryption
    HealthCardRepositoryProvider.useHybridEncrypted(applicationContext, db, auth)

    showUI()

    ContactRepositoryProvider.init(applicationContext)
  }
}
