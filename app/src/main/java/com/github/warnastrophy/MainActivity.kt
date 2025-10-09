package com.github.warnastrophy

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.github.warnastrophy.core.ui.repository.Hazard
import com.github.warnastrophy.core.ui.repository.HazardsRepository
import com.github.warnastrophy.core.ui.theme.MainAppTheme
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * `MainActivity` is the entry point of the application. It sets up the content view with the
 * `onCreate` methods. You can run the app by running the `app` configuration in Android Studio. NB:
 * Make sure you have an Android emulator running or a physical device connected.
 */
class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    val rep = HazardsRepository()
    var resp = emptyList<Hazard>()
    GlobalScope.launch {
      resp = rep.getAreaHazards("POLYGON((6.0 45.8%2C6.0 47.8%2C10.5 47.8%2C10.5 45.8%2C6.0 45.8))")
    }
    Log.d("HasardsRepository :", resp.toString())
    super.onCreate(savedInstanceState)
    setContent { MainAppTheme { Surface(modifier = Modifier.fillMaxSize()) { WarnastrophyApp() } } }
  }
}
