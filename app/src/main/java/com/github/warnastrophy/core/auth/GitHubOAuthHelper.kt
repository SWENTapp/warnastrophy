package com.github.warnastrophy.core.auth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CertificatePinner
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Helper class to handle GitHub OAuth authentication flow.
 *
 * This class manages the OAuth flow, including generating PKCE code challenge and verifier,
 * handling authorization state, and exchanging the authorization code for an access token.
 *
 * @param clientId The GitHub OAuth client ID.
 * @param redirectUri The URI to which GitHub will redirect after authorization. Default is
 *   "warnastrophy://github-callback".
 * @param tokenUrl The URL used to exchange the authorization code for an access token. Default is
 *   `TOKEN_URL`.
 * @param timeout Timeout in seconds for network requests. Default is 30 seconds.
 * @param httpClient Custom OkHttp client to be used for network requests. If not provided, a
 *   default client will be created.
 */
class GitHubOAuthHelper(
    private val clientId: String,
    private val redirectUri: String = "warnastrophy://github-callback",
    private val tokenUrl: String = TOKEN_URL,
    private val timeout: Long = DEFAULT_TIMEOUT,
    httpClient: OkHttpClient? = null
) {
  // PKCE code verifier - generated per OAuth flow
  private var codeVerifier: String? = null

  // State parameter for CSRF protection
  private var oauthState: String? = null

  private val httpClient =
      httpClient
          ?: OkHttpClient.Builder()
              .certificatePinner(
                  CertificatePinner.Builder()
                      // To get current pins: openssl s_client -servername github.com -connect
                      // github.com:443
                      // | openssl x509 -pubkey -noout | openssl pkey -pubin -outform der
                      // | openssl dgst -sha256 -binary | base64
                      .add("github.com", "sha256/uyPYgclc5Jt69vKu92vci6etcBBY5TslweRGEMlMxnc=")
                      .add("github.com", "sha256/e4wu8h9eLNeNUg6cVb5gGWM0PsiM9M3i3E32qKOkBwY=")
                      // Backup pins for redundancy
                      .add("github.com", "sha256/WoiWRyIOVNa9ihaBciRSC7XHjliYS9VwUGOIud4PB18=")
                      .build())
              .connectTimeout(timeout, TimeUnit.SECONDS)
              .readTimeout(timeout, TimeUnit.SECONDS)
              .writeTimeout(timeout, TimeUnit.SECONDS)
              .retryOnConnectionFailure(true)
              .build()

  companion object {
    private const val AUTHORIZATION_URL = "https://github.com/login/oauth/authorize"
    const val TOKEN_URL = "https://github.com/login/oauth/access_token"
    private const val DEFAULT_SCOPE = "user:email"
    private const val CODE_VERIFIER_LENGTH = 64 // RFC 7636: 43-128 characters
    const val DEFAULT_TIMEOUT = 30L
  }

  /**
   * Starts the OAuth flow by redirecting the user to GitHub's authorization page.
   *
   * @param activity The activity from which the intent to open GitHub's authorization page will be
   *   sent.
   * @param scope The scope of access being requested (default is `user:email`).
   */
  fun startOAuthFlow(activity: Activity, scope: String = DEFAULT_SCOPE) {
    codeVerifier = generateCodeVerifier()
    oauthState = generateState()

    val codeChallenge = generateCodeChallenge(codeVerifier!!)

    val authUrl =
        Uri.parse(AUTHORIZATION_URL)
            .buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("scope", scope)
            .appendQueryParameter("state", oauthState)
            .appendQueryParameter("code_challenge", codeChallenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .build()

    val intent = Intent(Intent.ACTION_VIEW, authUrl)
    activity.startActivity(intent)
  }

  /**
   * Checks if the given URI is a valid GitHub OAuth callback.
   *
   * @param uri The URI to check.
   * @return `true` if the URI matches the expected redirect URI, otherwise `false`.
   */
  fun isGitHubCallback(uri: Uri?): Boolean {
    if (uri == null) return false
    val expectedUri = Uri.parse(redirectUri)
    return uri.scheme == expectedUri.scheme && uri.host == expectedUri.host
  }

  /**
   * Extracts the authorization code from the callback URI.
   *
   * @param uri The URI received in the callback.
   * @return The authorization code if present, otherwise `null`.
   */
  fun extractAuthorizationCode(uri: Uri): String? {
    return uri.getQueryParameter("code")
  }

  /**
   * Extracts any error message from the callback URI.
   *
   * @param uri The URI received in the callback.
   * @return The error message if present, otherwise `null`.
   */
  fun extractError(uri: Uri): String? {
    return uri.getQueryParameter("error")
  }

  /**
   * Validates the state parameter from the callback URI to prevent CSRF attacks.
   *
   * @param uri The URI received in the callback.
   * @throws SecurityException if the state parameter does not match the expected state.
   */
  fun validateState(uri: Uri) {
    val receivedState = uri.getQueryParameter("state")
    if (receivedState == null || receivedState != oauthState) {
      clearSensitiveData()
      throw SecurityException("Invalid state parameter - possible CSRF attack detected")
    }
  }

  /**
   * Exchanges the authorization code for an access token.
   *
   * @param code The authorization code received from GitHub.
   * @return The access token.
   * @throws AuthenticationException if the authentication fails.
   * @throws NetworkException if the network request fails.
   */
  suspend fun exchangeCodeForAccessToken(code: String): String =
      withContext(Dispatchers.IO) {
        val currentVerifier =
            codeVerifier
                ?: throw IllegalStateException(
                    "OAuth flow not initialized - call startOAuthFlow first")

        val requestBody =
            FormBody.Builder()
                .add("client_id", clientId)
                .add("code", code)
                .add("redirect_uri", redirectUri)
                .add("code_verifier", currentVerifier) // PKCE
                .build()

        val request =
            Request.Builder()
                .url(tokenUrl)
                .post(requestBody)
                .addHeader("Accept", "application/json")
                .addHeader("User-Agent", "Warnastrophy")
                .build()

        try {
          httpClient.newCall(request).execute().use { response ->
            when {
              response.isSuccessful -> {
                val responseBody = response.body?.string()
                if (responseBody.isNullOrBlank()) {
                  throw NetworkException("Empty response from server")
                }

                parseAccessToken(responseBody)
              }
              response.code in 400..499 -> {
                throw AuthenticationException("Authentication failed - invalid credentials")
              }
              response.code in 500..599 -> {
                throw NetworkException("GitHub service temporarily unavailable")
              }
              else -> {
                throw NetworkException("Unexpected error occurred")
              }
            }
          }
        } catch (e: Exception) {
          when (e) {
            is AuthenticationException,
            is NetworkException,
            is SecurityException -> throw e
            else -> throw NetworkException("Network request failed")
          }
        } finally {
          clearSensitiveData()
        }
      }

  /** Clears the state and sensitive data. */
  fun clearState() {
    clearSensitiveData()
  }

  private fun generateCodeVerifier(): String {
    val bytes = ByteArray(CODE_VERIFIER_LENGTH)
    SecureRandom().nextBytes(bytes)
    return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
  }

  private fun generateState(): String {
    return UUID.randomUUID().toString()
  }

  private fun generateCodeChallenge(verifier: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(verifier.toByteArray(Charsets.US_ASCII))
    return Base64.encodeToString(hash, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
  }

  private fun clearSensitiveData() {
    codeVerifier = null
    oauthState = null
  }

  private fun parseAccessToken(jsonResponse: String): String {
    try {
      val json = JSONObject(jsonResponse)

      if (json.has("error")) {
        val error = json.getString("error")
        val errorDescription = json.optString("error_description", "Authentication failed")
        throw AuthenticationException("OAuth error: $error - $errorDescription")
      }

      val accessToken = json.optString("access_token", null)
      if (accessToken.isNullOrBlank()) {
        throw AuthenticationException("Access token not found in response")
      }

      return accessToken
    } catch (e: Exception) {
      when (e) {
        is AuthenticationException -> throw e
        else -> throw AuthenticationException("Failed to parse authentication response")
      }
    }
  }
}

/** Exception thrown when authentication fails. */
class AuthenticationException(message: String) : Exception(message)

/** Exception thrown when network operations fail. */
class NetworkException(message: String) : Exception(message)
