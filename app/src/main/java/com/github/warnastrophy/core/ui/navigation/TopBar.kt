package com.github.warnastrophy.core.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.github.warnastrophy.core.ui.common.ErrorHandler
import com.github.warnastrophy.core.ui.common.getScreenErrors
import com.github.warnastrophy.core.ui.features.error.ErrorScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    currentScreen: Screen,
    errorHandler: ErrorHandler = ErrorHandler(),
    canNavigateBack: Boolean = false,
    navigateUp: () -> Unit = {}
) {
  if (!currentScreen.hasTopBar) return

  val ctx = LocalContext.current
  val errorState = errorHandler.state.collectAsState()
  val errors = errorState.value.getScreenErrors(currentScreen)
  val hasErrors = errors.isNotEmpty()
  val errorMessages = errors.joinToString(separator = "\n") { ctx.getString(it.type.message) }

  var expanded by remember { mutableStateOf(false) }

  LaunchedEffect(hasErrors) { if (hasErrors) expanded = true }

  TopAppBar(
      title = {
        Text(
            ctx.getString(currentScreen.title),
            modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_TITLE))
      },
      actions = {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.testTag(NavigationTestTags.TOP_BAR_ERROR_ICON)) {
              Icon(
                  imageVector = Icons.Default.Warning,
                  contentDescription = if (hasErrors) "Errors present" else "No errors",
                  tint = if (hasErrors) Color.Red else Color.Gray)
            }

        ErrorScreen(
            message = errorMessages,
            expanded = expanded,
            onDismiss = { expanded = false },
            errors = errorState.value.errors)
      },
      navigationIcon = {
        if (canNavigateBack) {
          IconButton(
              onClick = navigateUp, modifier = Modifier.testTag(NavigationTestTags.BUTTON_BACK)) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
              }
        }
      })
}
