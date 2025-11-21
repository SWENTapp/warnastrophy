package com.github.warnastrophy.core.auth

import android.app.Activity
import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.github.warnastrophy.core.ui.features.auth.SignInViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseUser
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {

  private lateinit var repository: AuthRepository
  private lateinit var viewModel: SignInViewModel
  private lateinit var context: Context
  private lateinit var credentialManager: CredentialManager
  private lateinit var activity: Activity
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    mockkStatic(FirebaseApp::class)
    val mockFirebaseApp: FirebaseApp = mockk(relaxed = true)
    every { FirebaseApp.getInstance() } returns mockFirebaseApp
    every { FirebaseApp.getApps(any()) } returns listOf(mockFirebaseApp)

    Dispatchers.setMain(testDispatcher)

    mockkObject(GitHubAuthManager)
    mockkStatic("com.github.warnastrophy.core.auth.GitHubAuthManagerKt")
    every { GitHubAuthManager.onCredentialReady(any()) } just Runs
    every { GitHubAuthManager.setHelper(any()) } just Runs
    every { GitHubAuthManager.clearHelper() } just Runs
    every { GitHubAuthManager.clearCallback() } just Runs

    repository = mockk()
    context = mockk(relaxed = true)
    credentialManager = mockk()
    activity = mockk(relaxed = true)

    viewModel = SignInViewModel(repository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkStatic(FirebaseApp::class)
    unmockkObject(GitHubAuthManager)
    unmockkStatic("com.github.warnastrophy.core.auth.GitHubAuthManagerKt")
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
    val mockUser: FirebaseUser = mockk(relaxed = true)
    val mockCredential: Credential = mockk(relaxed = true)
    val mockResponse: GetCredentialResponse = mockk { every { credential } returns mockCredential }

    coEvery { credentialManager.getCredential(any<Context>(), any<GetCredentialRequest>()) } returns
        mockResponse

    coEvery { repository.signIn(any(), AuthProvider.GOOGLE) } returns Result.success(mockUser)

    viewModel.signInWithGoogle(context, credentialManager, "test-client-id")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals(mockUser, state.user)
    assertNull(state.errorMsg)
    assertFalse(state.signedOut)
  }

  @Test
  fun `signInWithGoogle failure updates state with error`() = runTest {
    val mockCredential: Credential = mockk(relaxed = true)
    val mockResponse: GetCredentialResponse = mockk { every { credential } returns mockCredential }

    coEvery { credentialManager.getCredential(any<Context>(), any<GetCredentialRequest>()) } returns
        mockResponse

    coEvery { repository.signIn(any(), AuthProvider.GOOGLE) } returns
        Result.failure(Exception("Auth failed"))

    viewModel.signInWithGoogle(context, credentialManager, "test-client-id")
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

    viewModel.signInWithGoogle(context, credentialManager, "test-client-id")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals("Sign-in cancelled", state.errorMsg)
    assertTrue(state.signedOut)
  }

  @Test
  fun `startGitHubSignIn initiates OAuth flow and sets loading state`() = runTest {
    val mockHelper: GitHubOAuthHelper = mockk(relaxed = true)

    every { activity.startGitHubSignIn(any()) } returns mockHelper

    viewModel.startGitHubSignIn(activity)
    testDispatcher.scheduler.advanceUntilIdle()

    assertTrue(viewModel.uiState.value.isLoading)
  }

  @Test
  fun `startGitHubSignIn with custom scope initiates OAuth flow`() = runTest {
    val mockHelper: GitHubOAuthHelper = mockk(relaxed = true)
    val customScope = "user:email,repo"

    every { activity.startGitHubSignIn(customScope) } returns mockHelper

    viewModel.startGitHubSignIn(activity, customScope)
    testDispatcher.scheduler.advanceUntilIdle()

    verify { GitHubAuthManager.setHelper(mockHelper) }
    assertTrue(viewModel.uiState.value.isLoading)
  }

  @Test
  fun `startGithubSignIn handles exception and updates error state`() = runTest {
    every { activity.startGitHubSignIn(any()) } throws Exception("Failed to start")

    viewModel.startGitHubSignIn(activity)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals("Failed to start GitHub sign-in", state.errorMsg)
    assertTrue(state.signedOut)
  }

  @Test
  fun `onCredentialReady callback processes GitHub credential`() = runTest {
    val mockUser: FirebaseUser = mockk(relaxed = true)
    val mockCredential: Credential = mockk(relaxed = true)

    val callbackSlot = slot<(Credential) -> Unit>()
    every { GitHubAuthManager.onCredentialReady(capture(callbackSlot)) } just Runs

    coEvery { repository.signIn(mockCredential, AuthProvider.GITHUB) } returns
        Result.success(mockUser)

    val testViewModel = SignInViewModel(repository, testDispatcher)

    callbackSlot.captured.invoke(mockCredential)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = testViewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals(mockUser, state.user)
    assertNull(state.errorMsg)
    assertFalse(state.signedOut)
  }

  @Test
  fun `onGitHubSignInCancelled updates state and clears helper`() = runTest {
    viewModel.onGitHubSignInCancelled()
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value
    assertFalse(state.isLoading)
    assertEquals("GitHub sign-in cancelled", state.errorMsg)
    assertTrue(state.signedOut)
    assertNull(state.user)

    verify { GitHubAuthManager.clearHelper() }
  }

  @Test
  fun `startGitHubSignIn does nothing when already loading`() = runTest {
    val mockHelper: GitHubOAuthHelper = mockk(relaxed = true)

    every { activity.startGitHubSignIn(any()) } returns mockHelper

    viewModel.startGitHubSignIn(activity)
    testDispatcher.scheduler.advanceUntilIdle()

    viewModel.startGitHubSignIn(activity)
    testDispatcher.scheduler.advanceUntilIdle()

    verify(exactly = 1) { GitHubAuthManager.setHelper(any()) }
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

  @Test
  fun `GitHub credential callback handles authentication failure`() = runTest {
    val mockCredential: Credential = mockk(relaxed = true)

    val callbackSlot = slot<(Credential) -> Unit>()
    every { GitHubAuthManager.onCredentialReady(capture(callbackSlot)) } just Runs

    coEvery { repository.signIn(mockCredential, AuthProvider.GITHUB) } returns
        Result.failure(Exception("GitHub auth failed"))

    val testViewModel = SignInViewModel(repository, testDispatcher)

    callbackSlot.captured.invoke(mockCredential)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = testViewModel.uiState.value
    assertFalse(state.isLoading)
    assertNull(state.user)
    assertNotNull(state.errorMsg)
    assertTrue(state.signedOut)
  }

  @Test
  fun `GitHub credential callback handles unexpected exception`() = runTest {
    val mockCredential: Credential = mockk(relaxed = true)

    val callbackSlot = slot<(Credential) -> Unit>()
    every { GitHubAuthManager.onCredentialReady(capture(callbackSlot)) } just Runs

    coEvery { repository.signIn(mockCredential, AuthProvider.GITHUB) } throws
        RuntimeException("Unexpected error")

    val testViewModel = SignInViewModel(repository, testDispatcher)

    callbackSlot.captured.invoke(mockCredential)
    testDispatcher.scheduler.advanceUntilIdle()

    val state = testViewModel.uiState.value
    assertFalse(state.isLoading)
    assertNull(state.user)
    assertEquals("Unexpected error occurred", state.errorMsg)
    assertTrue(state.signedOut)
  }
}
