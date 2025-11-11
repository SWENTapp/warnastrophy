package com.github.warnastrophy.core.auth

import android.os.Bundle
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.GithubAuthProvider
import com.google.firebase.auth.GoogleAuthProvider

/**
 * Interface for assisting with the extraction of authentication credentials from a [Bundle] and
 * conversion to Firebase [AuthCredential] for Google and GitHub sign-ins.
 */
interface SignInHelper {

  /**
   * Extracts a Google ID token credential from the provided [Bundle].
   *
   * @param bundle The [Bundle] containing the Google ID token credential.
   * @return The extracted [GoogleIdTokenCredential].
   * @throws IllegalArgumentException If the bundle does not contain the necessary information.
   */
  fun extractGoogleIdTokenCredential(bundle: Bundle): GoogleIdTokenCredential

  /**
   * Extracts the access token from the provided [Bundle].
   *
   * @param bundle The [Bundle] containing the access token.
   * @return The extracted access token as a [String].
   * @throws IllegalArgumentException If the access token is not found in the bundle.
   */
  fun extractAccessToken(bundle: Bundle): String

  /**
   * Converts a Google ID token into a Firebase [AuthCredential] for Google sign-in.
   *
   * @param idToken The Google ID token as a [String].
   * @return A [GoogleAuthProvider] Firebase [AuthCredential] for Google sign-in.
   */
  fun googleToFirebaseCredential(idToken: String): AuthCredential

  /**
   * Converts an access token into a Firebase [AuthCredential] for GitHub sign-in.
   *
   * @param accessToken The GitHub access token as a [String].
   * @return A [GithubAuthProvider] Firebase [AuthCredential] for GitHub sign-in.
   */
  fun githubToFirebaseCredential(accessToken: String): AuthCredential
}

/**
 * Default implementation of [SignInHelper] for extracting credentials from a [Bundle] and
 * converting them to Firebase [AuthCredential] for Google and GitHub sign-ins.
 */
class DefaultSignInHelper : SignInHelper {

  override fun extractGoogleIdTokenCredential(bundle: Bundle): GoogleIdTokenCredential {
    return GoogleIdTokenCredential.createFrom(bundle)
  }

  override fun extractAccessToken(bundle: Bundle): String {
    return bundle.getString("access_token")
        ?: throw IllegalArgumentException("Access token not found in bundle")
  }

  override fun googleToFirebaseCredential(idToken: String): AuthCredential {
    return GoogleAuthProvider.getCredential(idToken, null)
  }

  override fun githubToFirebaseCredential(accessToken: String): AuthCredential {
    return GithubAuthProvider.getCredential(accessToken)
  }
}
