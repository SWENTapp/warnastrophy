package com.github.warnastrophy.core.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(currentScreen: Screen, canNavigateBack: Boolean = false, navigateUp: () -> Unit = {}) {
  if (!currentScreen.hasTopBar) return

  val ctx = LocalContext.current

  TopAppBar(
      title = {
        Text(
            ctx.getString(currentScreen.title),
            modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE))
      },
      navigationIcon = {
        if (canNavigateBack) {
          IconButton(
              onClick = navigateUp,
          ) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      })
}

@Preview
@Composable
fun TopBarPreview() {
  androidx.compose.foundation.layout.Box(
      modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_PREVIEW)) {
        TopBar(Screen.HOME)
      }
}
