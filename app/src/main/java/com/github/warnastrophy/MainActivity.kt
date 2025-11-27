package com.github.warnastrophy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.github.warnastrophy.core.data.provider.HealthCardRepositoryProvider
import com.github.warnastrophy.core.data.repository.ContactRepositoryProvider
import com.github.warnastrophy.core.data.service.StateManagerService
import com.github.warnastrophy.core.ui.theme.MainAppTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * `MainActivity` is the entry point of the application. It sets up the content view with the
 * `onCreate` methods. You can run the app by running the `app` configuration in Android Studio. NB:
 * Make sure you have an Android emulator running or a physical device connected.
 */
class MainActivity : ComponentActivity() {

  private fun showUI() {
    setContent { MainAppTheme { Surface(Modifier.fillMaxSize()) { WarnastrophyComposable() } } }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    FirebaseApp.initializeApp(this)

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    HealthCardRepositoryProvider.useHybridEncrypted(applicationContext, db, auth)
    ContactRepositoryProvider.initHybrid(applicationContext, db)
    StateManagerService.init(applicationContext)

    showUI()
  }
}
