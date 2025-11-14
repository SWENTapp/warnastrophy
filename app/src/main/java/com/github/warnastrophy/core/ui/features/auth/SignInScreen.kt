package com.github.warnastrophy.core.ui.features.auth

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.warnastrophy.R
import com.github.warnastrophy.core.ui.components.Loading

/** Object containing test tag constants for the SignInScreen composable UI elements. */
object SignInScreenTestTags {
  const val APP_LOGO = "appLogo"
  const val LOGIN_TITLE = "loginTitle"
  const val GOOGLE_SIGN_IN_BUTTON = "googleSignInButton"
  const val GITHUB_SIGN_IN_BUTTON = "githubSignInButton"
}

/**
 * Composable function that displays the sign-in screen.
 *
 * This screen includes the application logo, a welcome message, a loading indicator, and buttons
 * for signing in with Google or GitHub. It also displays error messages and success messages via
 * [Toast].
 *
 * @param authViewModel The [SignInViewModel] that manages the authentication state.
 * @param credentialManager The [CredentialManager] used to manage and retrieve credentials.
 * @param onSignedIn Callback to invoke when the user successfully signs in.
 */
@Composable
fun SignInScreen(
    authViewModel: SignInViewModel = viewModel(),
    credentialManager: CredentialManager = CredentialManager.create(LocalContext.current),
    onSignedIn: () -> Unit = {}
) {
  val context = LocalContext.current
  val uiState by authViewModel.uiState.collectAsState()

  val serverClientId = stringResource(id = R.string.default_web_client_id)

  LaunchedEffect(uiState.errorMsg) {
    uiState.errorMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      authViewModel.clearErrorMsg()
    }
  }

  LaunchedEffect(uiState.user) {
    uiState.user?.let {
      Toast.makeText(context, "Login successful!", Toast.LENGTH_LONG).show()
      onSignedIn()
    }
  }

  Scaffold(
      modifier = Modifier.fillMaxSize(),
      content = { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
              Text(
                  modifier = Modifier.size(250.dp).testTag(SignInScreenTestTags.APP_LOGO),
                  text =
                      "Application logo placeholder") // To replace with an image when we will have
              // a logo

              Spacer(modifier = Modifier.height(16.dp))

              Text(
                  modifier = Modifier.testTag(SignInScreenTestTags.LOGIN_TITLE),
                  text = "Welcome",
                  style =
                      MaterialTheme.typography.headlineLarge.copy(
                          fontSize = 57.sp, lineHeight = 64.sp),
                  fontWeight = FontWeight.Bold,
                  textAlign = TextAlign.Center)

              Spacer(modifier = Modifier.height(48.dp))

              if (uiState.isLoading) {
                Loading(modifier = Modifier.size(48.dp))
              } else {
                GoogleSignInButton(
                    onSignInClick = {
                      authViewModel.signInWithGoogle(context, credentialManager, serverClientId)
                    })

                Spacer(modifier = Modifier.height(16.dp))

                GitHubSignInButton(
                    onSignInClick = {
                      Toast.makeText(
                              context, "This feature is not implemented yet!", Toast.LENGTH_LONG)
                          .show()
                    })

                GuestSignInButton(onSignInClick = { onSignedIn() })
              }
            }
      })
}

/**
 * Composable function that displays the Google sign-in button.
 *
 * This button initiates the Google sign-in process when clicked.
 *
 * @param onSignInClick The callback to invoke when the button is clicked.
 */
@Composable
fun GoogleSignInButton(onSignInClick: () -> Unit) {
  Button(
      onClick = onSignInClick,
      colors = ButtonDefaults.buttonColors(containerColor = Color.White),
      shape = RoundedCornerShape(50),
      border = BorderStroke(1.dp, Color.LightGray),
      modifier =
          Modifier.padding(8.dp)
              .height(48.dp)
              .testTag(SignInScreenTestTags.GOOGLE_SIGN_IN_BUTTON)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()) {
              Image(
                  painter = painterResource(id = R.drawable.google_logo),
                  contentDescription = "Google Logo",
                  modifier = Modifier.size(30.dp).padding(end = 8.dp))

              Text(
                  text = "Sign in with Google",
                  color = Color.Gray,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Medium)
            }
      }
}

/**
 * Composable function that displays the GitHub sign-in button.
 *
 * This button is intended to initiate the GitHub sign-in process when clicked. Currently, it shows
 * a placeholder toast message indicating that the feature is not yet implemented.
 *
 * @param onSignInClick The callback to invoke when the button is clicked.
 */
@Composable
fun GitHubSignInButton(onSignInClick: () -> Unit) {
  Button(
      onClick = onSignInClick,
      colors = ButtonDefaults.buttonColors(containerColor = Color.White),
      shape = RoundedCornerShape(50),
      border = BorderStroke(1.dp, Color.LightGray),
      modifier =
          Modifier.padding(8.dp)
              .height(48.dp)
              .testTag(SignInScreenTestTags.GITHUB_SIGN_IN_BUTTON)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()) {
              Image(
                  painter = painterResource(id = R.drawable.github_logo),
                  contentDescription = "GitHub Logo",
                  modifier = Modifier.size(30.dp).padding(end = 8.dp))

              Text(
                  text = "Sign in with GitHub",
                  color = Color.Gray,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Medium)
            }
      }
}
/**
 * Composable function that displays a button for signing in as a guest.
 *
 * @param onSignInClick The callback to invoke when the button is clicked.
 * @param modifier The [Modifier] to be applied to the button.
 * @param text The text to display on the button. Default is "Se connecter en invité".
 */
@Composable
fun GuestSignInButton(
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = stringResource(R.string.sign_in_as_guest)
) {
  Button(
      onClick = onSignInClick,
      colors =
          ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.White),
      shape = RoundedCornerShape(50),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
      modifier = modifier.padding(8.dp).height(48.dp).testTag("invitedSignInButton")) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()) {
              Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
      }
}
