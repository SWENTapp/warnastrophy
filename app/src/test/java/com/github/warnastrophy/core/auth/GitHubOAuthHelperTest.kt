package com.github.warnastrophy.core.auth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class GitHubOAuthHelperTest {
  private lateinit var helper: GitHubOAuthHelper
  private lateinit var mockServer: MockWebServer
  private val testClientId = "test_client_id"
  private val testClientSecret = "test_client_secret"
  private val testRedirectUri = "warnastrophy://github-callback"

  @Before
  fun setup() {
    mockServer = MockWebServer()
    mockServer.start()
    helper =
        GitHubOAuthHelper(
            clientId = testClientId,
            clientSecret = testClientSecret,
            redirectUri = testRedirectUri,
            httpClient = OkHttpClient())
  }

  @After
  fun tearDown() {
    mockServer.shutdown()
  }

  // ==================== startOAuthFlow Tests ====================

  @Test
  fun `startOAuthFlow launches browser with PKCE and state parameters`() {
    val mockActivity = mockk<Activity>(relaxed = true)
    val intentSlot = slot<Intent>()
    every { mockActivity.startActivity(capture(intentSlot)) } just Runs

    helper.startOAuthFlow(mockActivity)

    verify { mockActivity.startActivity(any()) }
    val uri = intentSlot.captured.data
    assertEquals(Intent.ACTION_VIEW, intentSlot.captured.action)
    assertEquals("https", uri?.scheme)
    assertEquals("github.com", uri?.host)
    assertEquals(testClientId, uri?.getQueryParameter("client_id"))
    assertEquals(testRedirectUri, uri?.getQueryParameter("redirect_uri"))
    assertEquals("user:email", uri?.getQueryParameter("scope"))
    assertNotNull(uri?.getQueryParameter("state"))
    assertNotNull(uri?.getQueryParameter("code_challenge"))
    assertEquals("S256", uri?.getQueryParameter("code_challenge_method"))
  }

  @Test
  fun `startOAuthFlow uses custom scope when provided`() {
    val mockActivity = mockk<Activity>(relaxed = true)
    val intentSlot = slot<Intent>()
    every { mockActivity.startActivity(capture(intentSlot)) } just Runs

    helper.startOAuthFlow(mockActivity, "user:email repo")

    assertEquals("user:email repo", intentSlot.captured.data?.getQueryParameter("scope"))
  }

  // ==================== isGitHubCallback Tests ====================

  @Test
  fun `isGitHubCallback returns true for valid URI`() {
    assertTrue(helper.isGitHubCallback(Uri.parse("warnastrophy://github-callback?code=abc")))
  }

  @Test
  fun `isGitHubCallback returns false for invalid URIs`() {
    assertFalse(helper.isGitHubCallback(null))
    assertFalse(helper.isGitHubCallback(Uri.parse("https://github-callback?code=abc")))
    assertFalse(helper.isGitHubCallback(Uri.parse("warnastrophy://wrong-host")))
    assertFalse(helper.isGitHubCallback(Uri.parse("warnastrophy://github-wrongpath")))
  }

  // ==================== extractAuthorizationCode Tests ====================

  @Test
  fun `extractAuthorizationCode returns code when present`() {
    val uri = Uri.parse("warnastrophy://github-callback?code=test_code_123")
    assertEquals("test_code_123", helper.extractAuthorizationCode(uri))
  }

  @Test
  fun `extractAuthorizationCode returns null when missing`() {
    assertNull(helper.extractAuthorizationCode(Uri.parse("warnastrophy://github-callback")))
  }

  // ==================== extractError Tests ====================

  @Test
  fun `extractError returns error when present`() {
    val uri = Uri.parse("warnastrophy://github-callback?error=access_denied")
    assertEquals("access_denied", helper.extractError(uri))
  }

  @Test
  fun `extractError returns null when missing`() {
    assertNull(helper.extractError(Uri.parse("warnastrophy://github-callback?code=abc")))
  }

  // ==================== validateState Tests ====================

  @Test
  fun `validateState succeeds with correct state`() {
    val mockActivity = mockk<Activity>(relaxed = true)
    val intentSlot = slot<Intent>()
    every { mockActivity.startActivity(capture(intentSlot)) } just Runs

    helper.startOAuthFlow(mockActivity)
    val state = intentSlot.captured.data?.getQueryParameter("state")
    assertNotNull(state)

    val uri = Uri.parse("warnastrophy://github-callback?code=abc&state=$state")

    helper.validateState(uri)
  }

  @Test
  fun `validateState throws SecurityException with incorrect state`() {
    val mockActivity = mockk<Activity>(relaxed = true)
    every { mockActivity.startActivity(any()) } just Runs

    helper.startOAuthFlow(mockActivity)
    val uri = Uri.parse("warnastrophy://github-callback?code=abc&state=wrong_state")

    assertThrows(SecurityException::class.java) { helper.validateState(uri) }
  }

  @Test
  fun `validateState throws SecurityException with missing state`() {
    val mockActivity = mockk<Activity>(relaxed = true)
    every { mockActivity.startActivity(any()) } just Runs

    helper.startOAuthFlow(mockActivity)
    val uri = Uri.parse("warnastrophy://github-callback?code=abc")

    assertThrows(SecurityException::class.java) { helper.validateState(uri) }
  }

  // ==================== exchangeCodeForAccessToken Tests ====================

  @Test
  fun `exchangeCodeForAccessToken returns token on success with PKCE and client_secret`() =
      runTest {
        val mockActivity = mockk<Activity>(relaxed = true)
        every { mockActivity.startActivity(any()) } just Runs

        val testToken = "gho_test_token"
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("""{"access_token": "$testToken", "token_type": "bearer"}""")
                .addHeader("Content-Type", "application/json"))

        val testHelper = createHelperWithMockServer()
        testHelper.startOAuthFlow(mockActivity)

        val token = testHelper.exchangeCodeForAccessToken("test_code")

        assertEquals(testToken, token)
        val request = mockServer.takeRequest()
        val body = request.body.readUtf8()
        assertTrue(body.contains("code_verifier"))
        assertTrue(body.contains("client_secret"))
        assertTrue(body.contains("client_id"))
        assertTrue(body.contains("redirect_uri"))
      }

  @Test
  fun `exchangeCodeForAccessToken includes correct headers`() = runTest {
    val mockActivity = mockk<Activity>(relaxed = true)
    every { mockActivity.startActivity(any()) } just Runs

    mockServer.enqueue(
        MockResponse()
            .setResponseCode(200)
            .setBody("""{"access_token": "token"}""")
            .addHeader("Content-Type", "application/json"))

    val testHelper = createHelperWithMockServer()
    testHelper.startOAuthFlow(mockActivity)
    testHelper.exchangeCodeForAccessToken("test_code")

    val request = mockServer.takeRequest()
    assertEquals("application/json", request.getHeader("Accept"))
    assertEquals("Warnastrophy", request.getHeader("User-Agent"))
  }

  @Test
  fun `exchangeCodeForAccessToken throws IllegalStateException if not initialized`() = runTest {
    val testHelper = createHelperWithMockServer()

    val exception =
        try {
          testHelper.exchangeCodeForAccessToken("code")
          null
        } catch (e: Exception) {
          e
        }

    assertNotNull(exception)
    assertTrue(exception is IllegalStateException)
    assertTrue(exception.message?.contains("not initialized") == true)
  }

  @Test
  fun `exchangeCodeForAccessToken throws AuthenticationException on 4xx errors`() = runTest {
    mockServer.enqueue(MockResponse().setResponseCode(401).setBody("""{"error": "unauthorized"}"""))
    val testHelper = createHelperWithMockServer()
    initializeHelper(testHelper)

    val exception =
        try {
          testHelper.exchangeCodeForAccessToken("code")
          null
        } catch (e: Exception) {
          e
        }

    assertNotNull(exception)
    assertTrue(exception is AuthenticationException)
    assertTrue(exception.message?.contains("Authentication failed") == true)
  }

  @Test
  fun `exchangeCodeForAccessToken throws NetworkException on 5xx errors`() = runTest {
    mockServer.enqueue(MockResponse().setResponseCode(503))
    val testHelper = createHelperWithMockServer()
    initializeHelper(testHelper)

    val exception =
        try {
          testHelper.exchangeCodeForAccessToken("code")
          null
        } catch (e: Exception) {
          e
        }

    assertNotNull(exception)
    assertTrue(exception is NetworkException)
    assertTrue(exception.message?.contains("temporarily unavailable") == true)
  }

  @Test
  fun `exchangeCodeForAccessToken throws on empty response`() = runTest {
    mockServer.enqueue(MockResponse().setResponseCode(200).setBody(""))
    val testHelper = createHelperWithMockServer()
    initializeHelper(testHelper)

    val exception =
        try {
          testHelper.exchangeCodeForAccessToken("code")
          null
        } catch (e: Exception) {
          e
        }

    assertNotNull(exception)
    assertTrue(exception is NetworkException)
    assertTrue(exception.message?.contains("Empty response") == true)
  }

  @Test
  fun `exchangeCodeForAccessToken throws when token missing`() = runTest {
    mockServer.enqueue(
        MockResponse()
            .setResponseCode(200)
            .setBody("""{"token_type": "bearer"}""")
            .addHeader("Content-Type", "application/json"))

    val testHelper = createHelperWithMockServer()
    initializeHelper(testHelper)

    val exception =
        try {
          testHelper.exchangeCodeForAccessToken("code")
          null
        } catch (e: Exception) {
          e
        }

    assertNotNull(exception)
    assertTrue(exception is AuthenticationException)
    assertTrue(exception.message?.contains("Access token not found") == true)
  }

  @Test
  fun `exchangeCodeForAccessToken throws with OAuth error`() = runTest {
    mockServer.enqueue(
        MockResponse()
            .setResponseCode(200)
            .setBody("""{"error": "invalid_grant", "error_description": "Code expired"}""")
            .addHeader("Content-Type", "application/json"))

    val testHelper = createHelperWithMockServer()
    initializeHelper(testHelper)

    val exception =
        try {
          testHelper.exchangeCodeForAccessToken("code")
          null
        } catch (e: Exception) {
          e
        }

    assertNotNull(exception)
    assertTrue(exception is AuthenticationException)
    assertTrue(exception.message?.contains("Code expired") == true)
  }

  @Test
  fun `exchangeCodeForAccessToken clears sensitive data after success`() = runTest {
    mockServer.enqueue(
        MockResponse()
            .setResponseCode(200)
            .setBody("""{"access_token": "token"}""")
            .addHeader("Content-Type", "application/json"))

    val testHelper = createHelperWithMockServer()
    initializeHelper(testHelper)

    testHelper.exchangeCodeForAccessToken("code")

    val exception =
        try {
          testHelper.exchangeCodeForAccessToken("code2")
          null
        } catch (e: Exception) {
          e
        }

    assertNotNull(exception)
    assertTrue(exception is IllegalStateException)
    assertTrue(exception.message?.contains("not initialized") == true)
  }

  @Test
  fun `exchangeCodeForAccessToken clears sensitive data after failure`() = runTest {
    mockServer.enqueue(MockResponse().setResponseCode(401))
    val testHelper = createHelperWithMockServer()
    initializeHelper(testHelper)

    try {
      testHelper.exchangeCodeForAccessToken("code")
    } catch (e: Exception) {
      // Expected
    }

    val exception =
        try {
          testHelper.exchangeCodeForAccessToken("code2")
          null
        } catch (e: Exception) {
          e
        }

    assertNotNull(exception)
    assertTrue(exception is IllegalStateException)
  }

  // ==================== clearState Tests ====================

  @Test
  fun `clearState clears all sensitive data`() = runTest {
    val mockActivity = mockk<Activity>(relaxed = true)
    every { mockActivity.startActivity(any()) } just Runs

    val testHelper = createHelperWithMockServer()
    testHelper.startOAuthFlow(mockActivity)
    testHelper.clearState()

    val exception =
        try {
          testHelper.exchangeCodeForAccessToken("code")
          null
        } catch (e: Exception) {
          e
        }

    assertNotNull(exception)
    assertTrue(exception is IllegalStateException)
  }

  // ==================== Helper Methods ====================

  private fun createHelperWithMockServer() =
      GitHubOAuthHelper(
          clientId = testClientId,
          clientSecret = testClientSecret,
          redirectUri = testRedirectUri,
          tokenUrl = mockServer.url("/login/oauth/access_token").toString(),
          httpClient = OkHttpClient())

  private fun initializeHelper(helper: GitHubOAuthHelper) {
    val mockActivity = mockk<Activity>(relaxed = true)
    every { mockActivity.startActivity(any()) } just Runs
    helper.startOAuthFlow(mockActivity)
  }
}
