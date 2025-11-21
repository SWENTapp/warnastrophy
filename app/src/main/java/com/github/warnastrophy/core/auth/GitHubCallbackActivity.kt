package com.github.warnastrophy.core.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.credentials.CustomCredential
import com.github.warnastrophy.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Enum representing various OAuth error types returned from GitHub. Each enum value contains a
 * user-friendly message that will be displayed in case of an error.
 *
 * @property message The error message that will be shown to the user.
 */
enum class AuthErrorTypes(val message: String) {
  ACCESS_DENIED("You denied access to your GitHub account"),
  UNAUTHORIZED_CLIENT("Application is not authorized"),
  UNSUPPORTED_RESPONSE_TYPE("Configuration error"),
  INVALID_SCOPE("Invalid permissions requested"),
  SERVER_ERROR("GitHub server error, please try again"),
  TEMPORARILY_UNAVAILABLE("GitHub service temporarily unavailable"),
  UNKNOWN("Authorization failed: Unknown error")
}

/**
 * The activity that handles the callback from GitHub OAuth after the user has authorized the
 * application.
 *
 * This activity is responsible for receiving the callback data from GitHub, validating it, and
 * exchanging the authorization code for an access token. If the OAuth flow succeeds, the access
 * token is passed to the [GitHubAuthManager]. If there is an error at any point, the activity
 * finishes with a result code of [ComponentActivity.RESULT_CANCELED] and displays an error message.
 *
 * This activity is typically launched as a result of a GitHub OAuth sign-in flow initiated from
 * [GitHubAuthManager] or other parts of the application.
 */
class GitHubCallbackActivity : ComponentActivity() {

  /**
   * The helper used to manage the GitHub OAuth session. This is either obtained from
   * [GitHubAuthManager] or created if not already present.
   */
  private var githubOAuthHelper: GitHubOAuthHelper? = null

  /** Coroutine scope used for launching background tasks related to the OAuth flow. */
  private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  /**
   * Called when the activity is created.
   *
   * Initializes the [GitHubOAuthHelper] and processes the intent that launched this activity. If no
   * helper is available, a new one is created, and the OAuth flow is started.
   *
   * @param savedInstanceState The saved state of the activity, if any.
   */
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    githubOAuthHelper =
        GitHubAuthManager.getHelper()
            ?: GitHubOAuthHelper(
                clientId = BuildConfig.GITHUB_CLIENT_ID,
                clientSecret = BuildConfig.GITHUB_CLIENT_SECRET,
                redirectUri = "warnastrophy://github-callback")

    handleIntent(intent)
  }

  /**
   * Handles the callback intent from GitHub OAuth.
   *
   * Validates the callback URL, checks for errors, and processes the authorization code. If valid,
   * the code is exchanged for an access token, and the user is signed in. If there are any errors,
   * the activity finishes with an error result.
   *
   * @param intent The intent that contains the callback data.
   */
  private fun handleIntent(intent: Intent?) {
    val uri = intent?.data
    val helper = githubOAuthHelper

    if (helper == null) {
      showError("OAuth session not found")
      finishWithError()
      return
    }

    if (!helper.isGitHubCallback(uri)) {
      showError("Invalid callback URL")
      finishWithError()
      return
    }

    uri?.let { callbackUri ->
      val error = helper.extractError(callbackUri)
      if (error != null) {
        try {
          val errorType = AuthErrorTypes.valueOf(error.uppercase())
          handleOAuthError(errorType)
        } catch (e: IllegalArgumentException) {
          handleOAuthError(AuthErrorTypes.UNKNOWN)
        }
        return
      }

      try {
        helper.validateState(callbackUri)
      } catch (e: SecurityException) {
        showError("Security validation failed: ${e.message}")
        finishWithError()
        return
      }

      val authCode = helper.extractAuthorizationCode(callbackUri)
      if (authCode.isNullOrBlank()) {
        showError("Authorization code not found")
        finishWithError()
        return
      }

      exchangeCodeForToken(authCode)
    }
        ?: run {
          showError("No callback data received")
          finishWithError()
        }
  }

  /**
   * Shows an error message to the user in a toast.
   *
   * @param message The error message to display.
   */
  private fun showError(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
  }

  /** Finishes the activity with an error result code [ComponentActivity.RESULT_CANCELED]. */
  private fun finishWithError() {
    setResult(RESULT_CANCELED)
    finish()
  }

  /** Finishes the activity with a success result code [ComponentActivity.RESULT_OK]. */
  private fun finishWithSuccess() {
    setResult(RESULT_OK)
    finish()
  }

  /**
   * Handles OAuth error responses from GitHub.
   *
   * Maps the error codes to user-friendly error messages (using [AuthErrorTypes]) and shows them in
   * a toast. After displaying the error, the activity finishes with an error result.
   *
   * @param error The [AuthErrorTypes] enum value that represents the error returned by GitHub.
   */
  private fun handleOAuthError(error: AuthErrorTypes) {
    val errorMessage = error.message
    showError(errorMessage)
    finishWithError()
  }

  /**
   * Exchanges the authorization code for an access token.
   *
   * If successful, the access token is stored and the user is signed in. If an error occurs, the
   * activity finishes with an error result.
   *
   * @param code The authorization code received from GitHub.
   */
  private fun exchangeCodeForToken(code: String) {
    val helper =
        githubOAuthHelper
            ?: run {
              showError("OAuth session expired")
              finishWithError()
              return
            }

    activityScope.launch {
      try {
        val accessToken = helper.exchangeCodeForAccessToken(code)

        val credentialData = Bundle().apply { putString("access_token", accessToken) }

        val credential = CustomCredential(type = CredentialTypes.TYPE_GITHUB, data = credentialData)

        GitHubAuthManager.setCredential(credential)

        finishWithSuccess()
      } catch (e: AuthenticationException) {
        showError("Authentication failed: ${e.message}")
        finishWithError()
      } catch (e: NetworkException) {
        showError("Network error: ${e.message}")
        finishWithError()
      } catch (e: Exception) {
        showError("Unexpected error: ${e.message}")
        finishWithError()
      } finally {
        helper.clearState()
        GitHubAuthManager.clearHelper()
      }
    }
  }

  /** Cancels any ongoing background tasks when the activity is destroyed. */
  override fun onDestroy() {
    super.onDestroy()
    activityScope.cancel()
  }
}
