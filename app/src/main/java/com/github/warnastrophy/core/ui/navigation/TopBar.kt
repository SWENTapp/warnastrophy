package com.github.warnastrophy.core.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import com.github.warnastrophy.core.model.ErrorHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    currentScreen: Screen,
    errorHandler: ErrorHandler = ErrorHandler(),
    onErrorClick: () -> Unit = {}
) {
  if (!currentScreen.hasTopBar) return

  val ctx = LocalContext.current
  val errors = errorHandler.getScreenErrors(currentScreen) // get current screen errors
  val hasErrors = errors.isNotEmpty()

  TopAppBar(
      title = {
        Text(
            ctx.getString(currentScreen.title),
            modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE))
      },
      actions = {
        IconButton(onClick = onErrorClick) {
          Icon(
              imageVector = Icons.Default.Warning,
              contentDescription = if (hasErrors) "Errors present" else "No errors",
              tint = if (hasErrors) Color.Red else Color.Gray)
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
