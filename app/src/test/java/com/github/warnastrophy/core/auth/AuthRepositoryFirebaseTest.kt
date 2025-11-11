package com.github.warnastrophy.core.auth

import android.os.Bundle
import androidx.credentials.CustomCredential
import com.google.android.gms.tasks.Tasks
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.unmockkAll
import io.mockk.verify
import java.lang.Exception
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthRepositoryFirebaseTest {
  @MockK private lateinit var mockAuth: FirebaseAuth
  @MockK private lateinit var mockHelper: SignInHelper
  @MockK private lateinit var mockCredential: CustomCredential
  @MockK private lateinit var mockBundle: Bundle
  @MockK private lateinit var mockGoogleToken: GoogleIdTokenCredential
  @MockK private lateinit var mockFirebaseCred: AuthCredential
  @MockK private lateinit var mockAuthResult: AuthResult
  @MockK private lateinit var mockUser: FirebaseUser

  private lateinit var repository: AuthRepositoryFirebase

  @Before
  fun setup() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    repository = AuthRepositoryFirebase(mockAuth, mockHelper)
  }

  @After fun tearDown() = unmockkAll()

  private fun setupSuccessfulAuth(credentialType: String, token: String = "token") {
    every { mockCredential.type } returns credentialType
    every { mockCredential.data } returns mockBundle
    every { mockAuthResult.user } returns mockUser
    every { mockAuth.signInWithCredential(mockFirebaseCred) } returns
        Tasks.forResult(mockAuthResult)

    when (credentialType) {
      CredentialTypes.TYPE_GOOGLE -> {
        every { mockHelper.extractGoogleIdTokenCredential(mockBundle) } returns mockGoogleToken
        every { mockGoogleToken.idToken } returns token
        every { mockHelper.googleToFirebaseCredential(token) } returns mockFirebaseCred
      }
      else -> {
        every { mockHelper.extractAccessToken(mockBundle) } returns token
        every { mockHelper.githubToFirebaseCredential(token) } returns mockFirebaseCred
      }
    }
  }

  // ========== Google Tests ==========

  @Test
  fun `signInWithGoogle succeeds with valid credential`() = runTest {
    setupSuccessfulAuth(CredentialTypes.TYPE_GOOGLE)
    val result = repository.signInWithGoogle(mockCredential)
    assertTrue(result.isSuccess)
    assertEquals(mockUser, result.getOrNull())
  }

  @Test
  fun `signInWithGoogle fails with invalid type`() = runTest {
    every { mockCredential.type } returns "invalid"
    val result = repository.signInWithGoogle(mockCredential)
    assertTrue(result.isFailure)
    verify(exactly = 0) { mockAuth.signInWithCredential(any()) }
  }

  @Test
  fun `signInWithGoogle fails when user is null`() = runTest {
    setupSuccessfulAuth(CredentialTypes.TYPE_GOOGLE)
    every { mockAuthResult.user } returns null
    val result = repository.signInWithGoogle(mockCredential)
    assertTrue(result.isFailure)
  }

  @Test
  fun `signInWithGoogle handles Firebase exception`() = runTest {
    every { mockCredential.type } returns CredentialTypes.TYPE_GOOGLE
    every { mockCredential.data } returns mockBundle
    every { mockHelper.extractGoogleIdTokenCredential(mockBundle) } returns mockGoogleToken
    every { mockGoogleToken.idToken } returns "token"
    every { mockHelper.googleToFirebaseCredential(any()) } returns mockFirebaseCred
    every { mockAuth.signInWithCredential(any()) } returns Tasks.forException(Exception("error"))

    val result = repository.signInWithGoogle(mockCredential)
    assertTrue(result.isFailure)
  }

  // ========== GitHub Tests ==========
  @Test
  fun `signInWithGitHub succeeds with valid credentials`() = runTest {
    setupSuccessfulAuth(CredentialTypes.TYPE_GITHUB)
    val result = repository.signInWithGithub(mockCredential)
    assertTrue(result.isSuccess)
    assertEquals(mockUser, result.getOrNull())
  }

  @Test
  fun `signInWithGithub fails with invalid type`() = runTest {
    every { mockCredential.type } returns "invalid"
    val result = repository.signInWithGithub(mockCredential)
    assertTrue(result.isFailure)
    verify(exactly = 0) { mockAuth.signInWithCredential(any()) }
  }

  @Test
  fun `signInWithGithub fails when user is null`() = runTest {
    setupSuccessfulAuth(CredentialTypes.TYPE_GITHUB)
    every { mockAuthResult.user } returns null
    val result = repository.signInWithGithub(mockCredential)
    assertTrue(result.isFailure)
  }

  @Test
  fun `signInWithGithub handles Firebase exception`() = runTest {
    every { mockCredential.type } returns CredentialTypes.TYPE_GITHUB
    every { mockCredential.data } returns mockBundle
    every { mockHelper.extractAccessToken(mockBundle) } returns "token"
    every { mockHelper.githubToFirebaseCredential(any()) } returns mockFirebaseCred
    every { mockAuth.signInWithCredential(any()) } returns Tasks.forException(Exception("error"))

    val result = repository.signInWithGithub(mockCredential)
    assertTrue(result.isFailure)
  }

  // ========== Generic Sign In Tests ==========

  @Test
  fun `signIn routes to Google`() = runTest {
    setupSuccessfulAuth(CredentialTypes.TYPE_GOOGLE)
    val result = repository.signIn(mockCredential, AuthProvider.GOOGLE)
    assertTrue(result.isSuccess)
    verify(exactly = 1) { mockHelper.googleToFirebaseCredential(any()) }
  }

  @Test
  fun `signIn routes to GitHub`() = runTest {
    setupSuccessfulAuth(CredentialTypes.TYPE_GITHUB)
    val result = repository.signIn(mockCredential, AuthProvider.GITHUB)
    assertTrue(result.isSuccess)
    verify(exactly = 1) { mockHelper.githubToFirebaseCredential(any()) }
  }

  // ========== Sign Out Tests ==========

  @Test
  fun `signOut succeeds`() {
    every { mockAuth.signOut() } just Runs
    val result = repository.signOut()
    assertTrue(result.isSuccess)
    verify(exactly = 1) { mockAuth.signOut() }
  }

  @Test
  fun `signOut handles exception`() {
    every { mockAuth.signOut() } throws Exception("error")
    val result = repository.signOut()
    assertTrue(result.isFailure)
  }
}
