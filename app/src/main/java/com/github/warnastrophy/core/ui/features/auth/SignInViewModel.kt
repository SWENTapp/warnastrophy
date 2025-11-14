package com.github.warnastrophy.core.ui.features.auth

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.warnastrophy.core.auth.AuthProvider
import com.github.warnastrophy.core.auth.AuthRepository
import com.github.warnastrophy.core.auth.AuthRepositoryFirebase
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Data class representing the UI state of the sign-in screen.
 *
 * @property isLoading Boolean indicating if the sign-in process is ongoing.
 * @property user The authenticated Firebase user, or null if no user is signed in.
 * @property errorMsg The error message to be displayed, or null if no error.
 * @property signedOut Boolean indicating if the user has been signed out.
 */
data class AuthUIState(
    val isLoading: Boolean = false,
    val user: FirebaseUser? = null,
    val errorMsg: String? = null,
    val signedOut: Boolean = false
)

/**
 * ViewModel for handling authentication logic, including signing in and signing out with Google or
 * GitHub.
 *
 * @param repository The authentication repository used for signing in and signing out.
 * @param dispatcher The CoroutineDispatcher used for launching coroutines (defaults to
 *   [Dispatchers.Main]).
 */
class SignInViewModel(
    private val repository: AuthRepository = AuthRepositoryFirebase(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {
  private val _uiState = MutableStateFlow(AuthUIState())
  val uiState: StateFlow<AuthUIState> = _uiState.asStateFlow()

  /** Clears the error message in the UI state. */
  fun clearErrorMsg() {
    _uiState.update { it.copy(errorMsg = null) }
  }

  /**
   * Signs in the user with Google credentials using the provided [CredentialManager].
   *
   * @param context The context used for signing in.
   * @param credentialManager The CredentialManager used to request credentials.
   * @param serverClientId The server client ID for the Google sign-in.
   */
  fun signInWithGoogle(
      context: Context,
      credentialManager: CredentialManager,
      serverClientId: String
  ) {
    if (_uiState.value.isLoading) return

    viewModelScope.launch(dispatcher) {
      _uiState.update { it.copy(isLoading = true, errorMsg = null) }

      try {
        val signInOptions =
            if (repository is AuthRepositoryFirebase) {
              repository.getGoogleSignInOption(serverClientId)
            } else {
              GetSignInWithGoogleOption.Builder(serverClientId = serverClientId).build()
            }

        val signInRequest =
            GetCredentialRequest.Builder().addCredentialOption(signInOptions).build()

        val credential = credentialManager.getCredential(context, signInRequest).credential

        performSignIn(credential, AuthProvider.GOOGLE)
      } catch (e: GetCredentialCancellationException) {
        _uiState.update {
          it.copy(isLoading = false, errorMsg = "Sign-in cancelled", signedOut = true, user = null)
        }
      } catch (e: GetCredentialException) {
        _uiState.update {
          it.copy(
              isLoading = false,
              errorMsg = "Failed to get credentials",
              signedOut = true,
              user = null)
        }
      } catch (e: Exception) {
        _uiState.update {
          it.copy(
              isLoading = false,
              errorMsg = "Unexpected error occurred",
              signedOut = true,
              user = null)
        }
      }
    }
  }

  /**
   * Signs in the user with GitHub credentials.
   *
   * @param credential The GitHub credential used for signing in.
   */
  fun signInWithGithub(credential: Credential) {
    if (_uiState.value.isLoading) return

    viewModelScope.launch(dispatcher) {
      _uiState.update { it.copy(isLoading = true, errorMsg = null) }
      performSignIn(credential, AuthProvider.GITHUB)
    }
  }

  /**
   * Performs the sign-in operation with the provided credential and authentication provider. This
   * method should only be called from within a coroutine scope with loading state already set.
   *
   * @param credential The credential used for signing in.
   * @param authProvider The authentication provider (Google or GitHub).
   */
  private suspend fun performSignIn(credential: Credential, authProvider: AuthProvider) {
    try {
      repository
          .signIn(credential, authProvider)
          .fold(
              onSuccess = { user ->
                _uiState.update {
                  it.copy(isLoading = false, user = user, errorMsg = null, signedOut = false)
                }
              },
              onFailure = {
                _uiState.update {
                  it.copy(
                      isLoading = false, errorMsg = "Sign-in failed", signedOut = true, user = null)
                }
              })
    } catch (e: Exception) {
      _uiState.update {
        it.copy(
            isLoading = false,
            errorMsg = "Unexpected error occurred",
            signedOut = true,
            user = null)
      }
    }
  }

  /** Signs out the current user. */
  fun signOut() {
    viewModelScope.launch(dispatcher) {
      repository
          .signOut()
          .fold(
              onSuccess = {
                _uiState.update { it.copy(user = null, signedOut = true, errorMsg = null) }
              },
              onFailure = { _uiState.update { it.copy(errorMsg = "Sign-out failed") } })
    }
  }
}
