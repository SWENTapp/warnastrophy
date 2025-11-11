package com.github.warnastrophy.core.auth

import android.os.Bundle
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DefaultSignInHelperTest {
  private lateinit var helper: DefaultSignInHelper

  @Before
  fun setup() {
    helper = DefaultSignInHelper()
  }

  @Test
  fun `toGoogleFirebaseCredential creates valid credential`() {
    val result = helper.toGoogleFirebaseCredential("google_token")
    assertNotNull(result)
  }

  @Test
  fun `toGithubFirebaseCredential creates valid credential`() {
    val result = helper.toGithubFirebaseCredential("github_token")
    assertNotNull(result)
  }

  @Test
  fun `extractAccessToken returns token from bundle`() {
    val bundle = Bundle().apply { putString("access_token", "test_token") }
    assertEquals("test_token", helper.extractAccessToken(bundle))
  }

  @Test(expected = IllegalArgumentException::class)
  fun `extractAccessToken throws when token missing`() {
    helper.extractAccessToken(Bundle())
  }

  @Test(expected = Exception::class)
  fun `extractGoogleIdTokenCredential throws with empty bundle`() {
    helper.extractGoogleIdTokenCredential(Bundle())
  }

  @Test(expected = Exception::class)
  fun `extractGoogleIdTokenCredential throws with invalid bundle`() {
    val bundle = Bundle().apply { putString("invalid", "data") }
    helper.extractGoogleIdTokenCredential(bundle)
  }
}
