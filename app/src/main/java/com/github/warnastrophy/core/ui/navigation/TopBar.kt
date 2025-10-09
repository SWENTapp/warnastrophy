package com.github.warnastrophy.core.ui.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.github.warnastrophy.core.ui.theme.MainAppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(currentScreen: Screen) {
  if (!currentScreen.hasTopBar) return

  val ctx = LocalContext.current

  TopAppBar(title = { Text(ctx.getString(currentScreen.title)) })
}

@Preview
@Composable
fun TopBarPreview() {
  MainAppTheme { TopBar(Screen.HOME) }
}
