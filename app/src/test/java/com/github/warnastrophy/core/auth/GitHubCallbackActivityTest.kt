package com.github.warnastrophy.core.auth

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.credentials.CustomCredential
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import junit.framework.TestCase.assertNotNull
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class GitHubCallbackActivityTest {

  private lateinit var mockHelper: GitHubOAuthHelper

  @Before
  fun setup() {
    mockHelper = mockk(relaxed = true)
    GitHubAuthManager.clearHelper()
    GitHubAuthManager.setHelper(mockHelper)
  }

  @After
  fun tearDown() {
    GitHubAuthManager.clearHelper()
  }

  @Test
  fun `onCreate with valid callback exchanges code and sets credential`() = runTest {
    val uri = Uri.parse("warnastrophy://github-callback?code=test_code&state=test_state")
    val intent =
        Intent(Intent.ACTION_VIEW, uri).apply {
          setClassName(
              ApplicationProvider.getApplicationContext(), GitHubCallbackActivity::class.java.name)
        }

    every { mockHelper.isGitHubCallback(uri) } returns true
    every { mockHelper.extractError(uri) } returns null
    every { mockHelper.extractAuthorizationCode(uri) } returns "test_code"
    every { mockHelper.validateState(uri) } just Runs
    coEvery { mockHelper.exchangeCodeForAccessToken("test_code") } returns "test_token"

    var capturedCredential: CustomCredential? = null
    GitHubAuthManager.onCredentialReady { credential ->
      capturedCredential = credential as? CustomCredential
    }

    ActivityScenario.launch<GitHubCallbackActivity>(intent).use { scenario ->
      Thread.sleep(200)

      scenario.onActivity { activity -> assertTrue(activity.isFinishing) }
    }

    coVerify { mockHelper.exchangeCodeForAccessToken("test_code") }
    assertNotNull(capturedCredential)
    assertEquals(CredentialTypes.TYPE_GITHUB, capturedCredential?.type)
    assertEquals("test_token", capturedCredential?.data?.getString("access_token"))
  }

  @Test
  fun `onCreate with invalid callback finishes with error`() {
    val uri = Uri.parse("https://invalid.com/callback")
    val intent =
        Intent(Intent.ACTION_VIEW, uri).apply {
          setClassName(
              ApplicationProvider.getApplicationContext(), GitHubCallbackActivity::class.java.name)
        }

    every { mockHelper.isGitHubCallback(uri) } returns false

    val scenario = ActivityScenario.launchActivityForResult<GitHubCallbackActivity>(intent)

    assertEquals(Activity.RESULT_CANCELED, scenario.result.resultCode)
  }

  @Test
  fun `onCreate with error parameter finishes with error`() {
    val uri = Uri.parse("warnastrophy://github-callback?error=access_denied")
    val intent =
        Intent(Intent.ACTION_VIEW, uri).apply {
          setClassName(
              ApplicationProvider.getApplicationContext(), GitHubCallbackActivity::class.java.name)
        }

    every { mockHelper.isGitHubCallback(uri) } returns true
    every { mockHelper.extractError(uri) } returns "access_denied"

    val scenario = ActivityScenario.launchActivityForResult<GitHubCallbackActivity>(intent)

    assertEquals(Activity.RESULT_CANCELED, scenario.result.resultCode)
  }

  @Test
  fun `onCreate with invalid state finishes with error`() {
    val uri = Uri.parse("warnastrophy://github-callback?code=test&state=wrong")
    val intent =
        Intent(Intent.ACTION_VIEW, uri).apply {
          setClassName(
              ApplicationProvider.getApplicationContext(), GitHubCallbackActivity::class.java.name)
        }

    every { mockHelper.isGitHubCallback(uri) } returns true
    every { mockHelper.extractError(uri) } returns null
    every { mockHelper.validateState(uri) } throws SecurityException("Invalid state")

    val scenario = ActivityScenario.launchActivityForResult<GitHubCallbackActivity>(intent)

    assertEquals(Activity.RESULT_CANCELED, scenario.result.resultCode)
  }

  @Test
  fun `onCreate with missing code finishes with error`() {
    val uri = Uri.parse("warnastrophy://github-callback?state=test_state")
    val intent =
        Intent(Intent.ACTION_VIEW, uri).apply {
          setClassName(
              ApplicationProvider.getApplicationContext(), GitHubCallbackActivity::class.java.name)
        }

    every { mockHelper.isGitHubCallback(uri) } returns true
    every { mockHelper.extractError(uri) } returns null
    every { mockHelper.extractAuthorizationCode(uri) } returns null
    every { mockHelper.validateState(uri) } just Runs

    val scenario = ActivityScenario.launchActivityForResult<GitHubCallbackActivity>(intent)

    assertEquals(Activity.RESULT_CANCELED, scenario.result.resultCode)
  }

  @Test
  fun `onCreate with AuthenticationException finishes with error`() = runTest {
    val uri = Uri.parse("warnastrophy://github-callback?code=test&state=test")
    val intent =
        Intent(Intent.ACTION_VIEW, uri).apply {
          setClassName(
              ApplicationProvider.getApplicationContext(), GitHubCallbackActivity::class.java.name)
        }

    every { mockHelper.isGitHubCallback(uri) } returns true
    every { mockHelper.extractError(uri) } returns null
    every { mockHelper.extractAuthorizationCode(uri) } returns "test"
    every { mockHelper.validateState(uri) } just Runs
    coEvery { mockHelper.exchangeCodeForAccessToken("test") } throws
        AuthenticationException("Invalid credentials")

    val scenario = ActivityScenario.launchActivityForResult<GitHubCallbackActivity>(intent)

    assertEquals(Activity.RESULT_CANCELED, scenario.result.resultCode)
  }

  @Test
  fun `onCreate with NetworkException finishes with Error`() = runTest {
    val uri = Uri.parse("warnastrophy://github-callback?code=test&state=test")
    val intent =
        Intent(Intent.ACTION_VIEW, uri).apply {
          setClassName(
              ApplicationProvider.getApplicationContext(), GitHubCallbackActivity::class.java.name)
        }

    every { mockHelper.isGitHubCallback(uri) } returns true
    every { mockHelper.extractError(uri) } returns null
    every { mockHelper.extractAuthorizationCode(uri) } returns "test"
    every { mockHelper.validateState(uri) } just Runs
    coEvery { mockHelper.exchangeCodeForAccessToken("test") } throws
        NetworkException("Network error")

    val scenario = ActivityScenario.launchActivityForResult<GitHubCallbackActivity>(intent)

    assertEquals(Activity.RESULT_CANCELED, scenario.result.resultCode)
  }

  @Test
  fun `onCreate with no helper creates new helper and handles gracefully`() {
    GitHubAuthManager.clearHelper()
    val uri = Uri.parse("warnastrophy://github-callback?error=test")
    val intent =
        Intent(Intent.ACTION_VIEW, uri).apply {
          setClassName(
              ApplicationProvider.getApplicationContext(), GitHubCallbackActivity::class.java.name)
        }

    val scenario = ActivityScenario.launchActivityForResult<GitHubCallbackActivity>(intent)

    assertEquals(Activity.RESULT_CANCELED, scenario.result.resultCode)
  }

  @Test
  fun `onCreate with null uri finishes with error`() {
    val intent =
        Intent(Intent.ACTION_VIEW).apply {
          setClassName(
              ApplicationProvider.getApplicationContext(), GitHubCallbackActivity::class.java.name)
        }

    val scenario = ActivityScenario.launchActivityForResult<GitHubCallbackActivity>(intent)

    assertEquals(Activity.RESULT_CANCELED, scenario.result.resultCode)
  }
}
