package com.github.warnastrophy.core.auth

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.github.warnastrophy.core.ui.features.auth.SignInViewModel
import com.google.firebase.auth.FirebaseUser
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {
  private lateinit var repository: AuthRepository
  private lateinit var viewModel: SignInViewModel
  private lateinit var context: Context
  private lateinit var credentialManager: CredentialManager
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)
    repository = mockk()
    context = mockk(relaxed = true)
    credentialManager = mockk()
    viewModel = SignInViewModel(repository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun `clearErrorMsg clears error message`() {
    viewModel.clearErrorMsg()
    testDispatcher.scheduler.advanceUntilIdle()
    assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun `signInWithGoogle success updates state correctly`() = runTest {
    val mockUser: FirebaseUser = mockk()
    val mockCredential: Credential = mockk()
    val mockResponse: GetCredentialResponse = mockk { every { credential } returns mockCredential }

    coEvery { credentialManager.getCredential(any<Context>(), any<GetCredentialRequest>()) } returns
        mockResponse

    coEvery { repository.signIn(any(), AuthProvider.GOOGLE) } returns Result.success(mockUser)

    viewModel.signInWithGoogle(context, credentialManager, "test-server-id")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals(mockUser, state.user)
    assertNull(state.errorMsg)
    assertFalse(state.signedOut)
  }

  @Test
  fun `signInWithGoogle failure updates state with error`() = runTest {
    val mockCredential: Credential = mockk()
    val mockResponse: GetCredentialResponse = mockk { every { credential } returns mockCredential }

    coEvery { credentialManager.getCredential(any<Context>(), any<GetCredentialRequest>()) } returns
        mockResponse
    coEvery { repository.signIn(any(), AuthProvider.GOOGLE) } returns
        Result.failure(Exception("Auth failed"))

    viewModel.signInWithGoogle(context, credentialManager, "test-server-id")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertNull(state.user)
    assertNotNull(state.errorMsg)
    assertTrue(state.signedOut)
  }

  @Test
  fun `signInWithGoogle cancellation updates state correctly`() = runTest {
    coEvery { credentialManager.getCredential(any<Context>(), any<GetCredentialRequest>()) } throws
        GetCredentialCancellationException("Cancelled")

    viewModel.signInWithGoogle(context, credentialManager, "test-server-id")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals("Sign-in cancelled", state.errorMsg)
    assertTrue(state.signedOut)
  }

  @Test
  fun `signInWithGithub success updates state correctly`() = runTest {
    val mockUser: FirebaseUser = mockk()
    val mockCredential: Credential = mockk()

    coEvery { repository.signIn(mockCredential, AuthProvider.GITHUB) } returns
        Result.success(mockUser)

    viewModel.signInWithGithub(mockCredential)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals(mockUser, state.user)
    assertNull(state.errorMsg)
    assertFalse(state.signedOut)
  }

  @Test
  fun `signInWithGithub failure updates state with error`() = runTest {
    val mockCredential: Credential = mockk()

    coEvery { repository.signIn(mockCredential, AuthProvider.GITHUB) } returns
        Result.failure(Exception("Github auth failed"))

    viewModel.signInWithGithub(mockCredential)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertNull(state.user)
    assertNotNull(state.errorMsg)
    assertTrue(state.signedOut)
  }

  @Test
  fun `signOut success clears user state`() = runTest {
    coEvery { repository.signOut() } returns Result.success(Unit)

    viewModel.signOut()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNull(state.user)
    assertTrue(state.signedOut)
    assertNull(state.errorMsg)
  }

  @Test
  fun `signOut failure updates error message`() = runTest {
    coEvery { repository.signOut() } returns Result.failure(Exception("Sign-out failed"))

    viewModel.signOut()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertNotNull(state.errorMsg)
  }
}
