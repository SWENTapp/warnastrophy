package com.github.warnastrophy.core.auth

import androidx.credentials.Credential
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await

/** Defines constant types for credential types used in authentication. */
object CredentialTypes {

  /** The credential type for Google ID tokens. */
  const val TYPE_GOOGLE = TYPE_GOOGLE_ID_TOKEN_CREDENTIAL

  /** The credential type for GitHub access tokens. */
  const val TYPE_GITHUB = "com.github.warnastrophy.TYPE_GITHUB_CREDENTIAL"
}

/**
 * Implementation of [AuthRepository] for Firebase authentication. This repository provides methods
 * for signing in with Google and GitHub credentials using Firebase Authentication services.
 *
 * @param auth The [FirebaseAuth] instance used to authenticate users.
 * @param helper A [SignInHelper] that assists with credential extraction and conversion to Firebase
 *   credentials.
 */
class AuthRepositoryFirebase(
    private val auth: FirebaseAuth = Firebase.auth,
    private val helper: SignInHelper = DefaultSignInHelper()
) : AuthRepository {

  companion object {
    const val UNEXPECTED_ERROR_MESSAGE = "Unexpected error."
  }

  /**
   * Builds a [GetSignInWithGoogleOption] for signing in with Google, using the provided server
   * client ID.
   *
   * @param serverClientId The server client ID used for Google sign-in.
   * @return A [GetSignInWithGoogleOption] configured for Google sign-in.
   */
  fun getGoogleSignInOption(serverClientId: String) =
      GetSignInWithGoogleOption.Builder(serverClientId = serverClientId).build()

  /**
   * Signs in with a Google credential by extracting the Google ID token from the provided
   * [Credential], converting it to a Firebase credential, and authenticating the user with
   * Firebase.
   *
   * @param credential The [Credential] containing the Google ID token.
   * @return A [Result] containing the authenticated [FirebaseUser] if successful, or an error if
   *   failed.
   */
  override suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser> {
    return try {
      if (credential is CustomCredential && credential.type == CredentialTypes.TYPE_GOOGLE) {
        val idToken = helper.extractGoogleIdTokenCredential(credential.data).idToken
        val firebaseCred = helper.toGoogleFirebaseCredential(idToken)

        val user =
            auth.signInWithCredential(firebaseCred).await().user
                ?: return Result.failure(
                    IllegalStateException("Login failed: Could not retrieve user information"))
        Result.success(user)
      } else {
        Result.failure(IllegalStateException("Login failed: Credential is not of type Google ID"))
      }
    } catch (e: Exception) {
      Result.failure(
          IllegalStateException(
              "Google login failed: ${e.localizedMessage ?: UNEXPECTED_ERROR_MESSAGE}"))
    }
  }

  /**
   * Signs in with a GitHub credential by extracting the access token from the provided
   * [Credential], converting it to a Firebase credential, and authenticating the user with
   * Firebase.
   *
   * @param credential The [Credential] containing the GitHub access token.
   * @return A [Result] containing the authenticated [FirebaseUser] if successful, or an error if
   *   failed.
   */
  override suspend fun signInWithGithub(credential: Credential): Result<FirebaseUser> {
    return try {
      if (credential is CustomCredential && credential.type == CredentialTypes.TYPE_GITHUB) {
        val accessToken = helper.extractAccessToken(credential.data)
        val firebaseCred = helper.toGithubFirebaseCredential(accessToken)

        val user =
            auth.signInWithCredential(firebaseCred).await().user
                ?: return Result.failure(
                    IllegalStateException("Login failed: Could not retrieve user information"))
        return Result.success(user)
      } else {
        return Result.failure(
            IllegalStateException("Login failed: Credential is not of type GitHub"))
      }
    } catch (e: Exception) {
      Result.failure(
          IllegalStateException(
              "GitHub login failed: ${e.localizedMessage ?: UNEXPECTED_ERROR_MESSAGE}"))
    }
  }

  /**
   * Signs in the user with the specified credential and authentication provider. The authentication
   * provider determines whether the login is handled by Google or GitHub.
   *
   * @param credential The [Credential] containing the user's authentication information.
   * @param authProvider The authentication provider (e.g., Google, GitHub).
   * @return A [Result] containing the authenticated [FirebaseUser] if successful, or an error if
   *   failed.
   */
  override suspend fun signIn(
      credential: Credential,
      authProvider: AuthProvider
  ): Result<FirebaseUser> {
    return when (authProvider) {
      AuthProvider.GOOGLE -> signInWithGoogle(credential)
      AuthProvider.GITHUB -> signInWithGithub(credential)
    }
  }

  /**
   * Signs out the current user.
   *
   * @return A [Result] indicating the success or failure of the sign-out operation.
   */
  override fun signOut(): Result<Unit> {
    return try {
      auth.signOut()
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(
          IllegalStateException("Logout failed: ${e.localizedMessage ?: UNEXPECTED_ERROR_MESSAGE}"))
    }
  }
}
