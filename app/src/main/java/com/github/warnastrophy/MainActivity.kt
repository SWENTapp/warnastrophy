package com.github.warnastrophy

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.github.warnastrophy.core.ui.repository.Hazard
import com.github.warnastrophy.core.ui.theme.MainAppTheme
import com.github.warnastrophy.core.ui.repository.HazardsRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


/**
 * `MainActivity` is the entry point of the application. It sets up the content view with the
 * `onCreate` methods. You can run the app by running the `app` configuration in Android Studio. NB:
 * Make sure you have an Android emulator running or a physical device connected.
 */
class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent { MainAppTheme { Surface(modifier = Modifier.fillMaxSize()) { MainApp() } } }
  }
}

@Composable
fun MainApp(
    context: Context = LocalContext.current,
) {
  val rep = HazardsRepository()
  LaunchedEffect(Unit) {
    val hazards = rep.getAreaHazards("POLYGON(6.0 45.8%2C6.0 47.8%2C10.5 47.8%2C10.5 45.8%2C6.0 45.8)")
  }
  Text("ntm")
}

@Preview(showBackground = true)
@Composable
fun MainAppPreview() {
  MainAppTheme { MainApp() }
}
