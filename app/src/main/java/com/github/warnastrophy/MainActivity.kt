package com.github.warnastrophy

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.github.warnastrophy.core.ui.theme.MainAppTheme

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
  Text("Hello Warnastrophy!")
}

@Preview(showBackground = true)
@Composable
fun MainAppPreview() {
  MainAppTheme { MainApp() }
}
