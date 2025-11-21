package com.github.warnastrophy.core.auth

import androidx.credentials.Credential
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test

class GitHubAuthManagerTest {

  private lateinit var mockHelper: GitHubOAuthHelper
  private lateinit var mockCredential: Credential

  @Before
  fun setup() {
    mockHelper = mockk(relaxed = true)
    mockCredential = mockk(relaxed = true)

    GitHubAuthManager.clearHelper()
  }

  @After
  fun tearDown() {
    GitHubAuthManager.clearHelper()
  }

  @Test
  fun `setHelper stores helper correctly`() {
    GitHubAuthManager.setHelper(mockHelper)

    assertEquals(mockHelper, GitHubAuthManager.getHelper())
  }

  @Test
  fun `getHelper returns null when not set`() {
    assertNull(GitHubAuthManager.getHelper())
  }

  @Test
  fun `setCredential stores credential correctly`() {
    GitHubAuthManager.setCredential(mockCredential)

    val retrieved = GitHubAuthManager.getAndClearCredential()
    assertEquals(mockCredential, retrieved)
  }

  @Test
  fun `getAndClearCredential clears after retrieve`() {
    GitHubAuthManager.setCredential(mockCredential)

    val first = GitHubAuthManager.getAndClearCredential()
    val second = GitHubAuthManager.getAndClearCredential()

    assertEquals(mockCredential, first)
    assertNull(second)
  }

  @Test
  fun `onCredentialReady invokes callback immediately when credential already set`() {
    var callbackInvoked = false
    var receivedCredential: Credential? = null
    GitHubAuthManager.setCredential(mockCredential)

    GitHubAuthManager.onCredentialReady { credential ->
      callbackInvoked = true
      receivedCredential = credential
    }

    assertTrue(callbackInvoked)
    assertEquals(mockCredential, receivedCredential)
  }

  @Test
  fun `onCredentialReady invokes callback later when credentials set after`() {
    var callbackInvoked = false
    var receivedCredential: Credential? = null

    GitHubAuthManager.onCredentialReady { credential ->
      callbackInvoked = true
      receivedCredential = credential
    }

    assertFalse(callbackInvoked)

    GitHubAuthManager.setCredential(mockCredential)

    assertTrue(callbackInvoked)
    assertEquals(mockCredential, receivedCredential)
  }

  @Test
  fun `clearHelper clears all state`() {
    GitHubAuthManager.setHelper(mockHelper)
    GitHubAuthManager.setCredential(mockCredential)
    var callbackInvoked: Boolean
    GitHubAuthManager.onCredentialReady { callbackInvoked = true }

    GitHubAuthManager.clearHelper()
    callbackInvoked = false

    assertNull(GitHubAuthManager.getHelper())
    assertNull(GitHubAuthManager.getAndClearCredential())

    GitHubAuthManager.setCredential(mockk())
    assertFalse(callbackInvoked)

    verify { mockHelper.clearState() }
  }

  @Test
  fun `clearCallback only clears callback not helper or credential`() {
    GitHubAuthManager.setHelper(mockHelper)
    GitHubAuthManager.setCredential(mockCredential)

    var callbackInvoked = false

    GitHubAuthManager.clearCallback()

    assertEquals(mockHelper, GitHubAuthManager.getHelper())
    assertEquals(mockCredential, GitHubAuthManager.getAndClearCredential())

    GitHubAuthManager.onCredentialReady { callbackInvoked = true }

    assertFalse(callbackInvoked)
  }

  @Test
  fun `setCredential clears callback after invocation`() {
    var invocationCount = 0
    GitHubAuthManager.onCredentialReady { invocationCount++ }

    GitHubAuthManager.setCredential(mockCredential)
    GitHubAuthManager.setCredential(mockk())

    assertEquals(1, invocationCount)
  }
}
