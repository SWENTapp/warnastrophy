package com.github.warnastrophy.core.auth

import androidx.credentials.Credential
import com.google.firebase.auth.FirebaseUser

/**
 * Interface for handling authentication operations within the application using the Credential
 * Manager API. This interface defines methods for signing in using different authentication
 * providers such as Google and GitHub, as well as a method for signing out of the application.
 */
interface AuthRepository {

  /**
   * Signs in a user with a Google credential using the Credential Manager API.
   *
   * @param credential The Google credential containing the user's authentication information.
   * @return A [Result] containing the authenticated [FirebaseUser] if successful, or an error if
   *   failed.
   */
  suspend fun signInWithGoogle(credential: Credential): Result<FirebaseUser>

  /**
   * Signs in a user with a GitHub credential using the Credential Manager API.
   *
   * @param credential The GitHub credential containing the user's authentication information.
   * @return A [Result] containing the authenticated [FirebaseUser] if successful, or an error if
   *   failed.
   */
  suspend fun signInWithGithub(credential: Credential): Result<FirebaseUser>

  /**
   * Signs in a user with a credential from a specified authentication provider using the Credential
   * Manager API.
   *
   * @param credential The credential containing the user's authentication information.
   * @param authProvider The authentication provider (e.g., Google, GitHub).
   * @return A [Result] containing the authenticated [FirebaseUser] if successful, or an error if
   *   failed.
   */
  suspend fun signIn(credential: Credential, authProvider: AuthProvider): Result<FirebaseUser>

  /**
   * Signs out the current user.
   *
   * @return A [Result] indicating the success or failure of the sign-out operation.
   */
  fun signOut(): Result<Unit>
}
