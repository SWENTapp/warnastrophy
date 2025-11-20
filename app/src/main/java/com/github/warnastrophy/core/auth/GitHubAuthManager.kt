package com.github.warnastrophy.core.auth

import android.app.Activity
import androidx.credentials.Credential
import com.github.warnastrophy.BuildConfig

/**
 * Starts the GitHub OAuth sign-in flow.
 *
 * This function initializes a [GitHubOAuthHelper] with the GitHub client ID and redirect URI and
 * then begins the OAuth flow to allow the user to sign in with GitHub. It also takes an optional
 * `scope` parameter which defaults to `user:email`. The scope determines the level of access that
 * the application requests from the GitHub account.
 *
 * @param scope The scope of access requested from the GitHub account. The default value is
 *   `user:email`.
 * @return An instance of [GitHubOAuthHelper] which manages the OAuth process.
 */
fun Activity.startGitHubSignIn(scope: String = "user:email"): GitHubOAuthHelper {
  val helper =
      GitHubOAuthHelper(
          clientId = BuildConfig.GITHUB_CLIENT_ID, redirectUri = "warnastrophy://github-callback")
  helper.startOAuthFlow(this, scope)
  return helper
}

/**
 * Manages the GitHub OAuth authentication process.
 *
 * This singleton object handles the management of the current GitHub OAuth helper and credential
 * flow. It provides methods to set and retrieve the [GitHubOAuthHelper], manage authentication
 * credentials, and register a callback to notify when the credentials are ready.
 */
object GitHubAuthManager {
  /**
   * The current active [GitHubOAuthHelper] instance used for OAuth flows. This helper is used to
   * initiate the OAuth flow and handle the response.
   */
  private var currentHelper: GitHubOAuthHelper? = null

  /**
   * Stores the credential pending retrieval. This credential is typically set after a successful
   * OAuth flow.
   */
  private var pendingCredential: Credential? = null

  /**
   * A callback that gets invoked when the credential is ready. It allows consumers of this class to
   * handle the credential once it is available.
   */
  private var credentialCallback: ((Credential) -> Unit)? = null

  /**
   * Sets the [GitHubOAuthHelper] instance to be used for the OAuth flow.
   *
   * @param helper The [GitHubOAuthHelper] instance to set.
   */
  fun setHelper(helper: GitHubOAuthHelper) {
    currentHelper = helper
  }

  /**
   * Retrieves the current [GitHubOAuthHelper] instance, if available.
   *
   * @return The current [GitHubOAuthHelper] instance, or `null` if none is set.
   */
  fun getHelper(): GitHubOAuthHelper? = currentHelper

  /**
   * Sets the credential once it is obtained from the OAuth flow. It also triggers the callback if
   * it was set previously.
   *
   * @param credential The credential received from GitHub OAuth.
   */
  fun setCredential(credential: Credential) {
    pendingCredential = credential
    credentialCallback?.invoke(credential)
    credentialCallback = null
  }

  /**
   * Retrieves and clears the pending credential.
   *
   * This method allows retrieval of the credential and ensures it is cleared afterward.
   *
   * @return The pending [Credential], or `null` if no credential is pending.
   */
  fun getAndClearCredential(): Credential? {
    val credential = pendingCredential
    pendingCredential = null
    return credential
  }

  /**
   * Registers a callback to be notified when the credential is ready.
   *
   * If the credential is already available, the callback is immediately invoked. Otherwise, it will
   * be invoked when the credential is set via [setCredential].
   *
   * @param callback A function that takes a [Credential] and is invoked when the credential is
   *   available.
   */
  fun onCredentialReady(callback: (Credential) -> Unit) {
    credentialCallback = callback
    pendingCredential?.let {
      callback(it)
      pendingCredential = null
      credentialCallback = null
    }
  }

  /**
   * Clears the current helper and resets the state, including the pending credential and callback.
   */
  fun clearHelper() {
    currentHelper?.clearState()
    currentHelper = null
    pendingCredential = null
    credentialCallback = null
  }

  /**
   * Clears the callback that was registered via [onCredentialReady]. This prevents any future
   * callbacks from being invoked.
   */
  fun clearCallback() {
    credentialCallback = null
  }
}
