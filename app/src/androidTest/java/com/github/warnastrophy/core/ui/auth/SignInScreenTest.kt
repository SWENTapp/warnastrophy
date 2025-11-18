package com.github.warnastrophy.core.ui.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.credentials.CredentialManager
import com.github.warnastrophy.core.ui.components.LoadingTestTags
import com.github.warnastrophy.core.ui.features.auth.AuthUIState
import com.github.warnastrophy.core.ui.features.auth.GitHubSignInButton
import com.github.warnastrophy.core.ui.features.auth.GoogleSignInButton
import com.github.warnastrophy.core.ui.features.auth.SignInScreen
import com.github.warnastrophy.core.ui.features.auth.SignInScreenTestTags
import com.github.warnastrophy.core.ui.features.auth.SignInViewModel
import com.github.warnastrophy.core.ui.theme.MainAppTheme
import com.github.warnastrophy.core.ui.util.BaseAndroidComposeTest
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Test

@HiltAndroidTest
class SignInScreenTest : BaseAndroidComposeTest() {

  private lateinit var mockCredentialManager: CredentialManager
  private lateinit var mockViewModel: SignInViewModel
  private lateinit var onSignedInCallback: () -> Unit

  @Before
  override fun setUp() {
    super.setUp()
    mockCredentialManager = mockk(relaxed = true)
    onSignedInCallback = mockk(relaxed = true)
    mockViewModel = mockk(relaxed = true)

    every { mockViewModel.uiState } returns MutableStateFlow(AuthUIState())
  }

  @After
  override fun tearDown() {
    super.tearDown()
    unmockkAll()
  }

  private fun setUpSignInScreen() {
    composeTestRule.setContent {
      MainAppTheme {
        SignInScreen(
            authViewModel = mockViewModel,
            credentialManager = mockCredentialManager,
            onSignedIn = onSignedInCallback)
      }
    }
  }

  @Test
  fun signInScreen_displaysAllUIElements() {
    setUpSignInScreen()
    composeTestRule.waitForIdleWithTimeout()

    composeTestRule.onNodeWithTag(SignInScreenTestTags.APP_LOGO).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.LOGIN_TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.GOOGLE_SIGN_IN_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.GITHUB_SIGN_IN_BUTTON).assertIsDisplayed()
  }

  @Test
  fun signInScreen_googleButtonClick_triggersSignIn() {
    var signInCalled = false
    every { mockViewModel.signInWithGoogle(any(), any(), any()) } answers { signInCalled = true }

    setUpSignInScreen()
    composeTestRule.waitForIdleWithTimeout()

    composeTestRule.onNodeWithTag(SignInScreenTestTags.GOOGLE_SIGN_IN_BUTTON).performClick()
    composeTestRule.waitForIdleWithTimeout()

    assert(signInCalled)
    verify { mockViewModel.signInWithGoogle(any(), any(), any()) }
  }

  @Test
  fun signInScreen_githubButtonClick_showsNotImplementedMessage() {
    setUpSignInScreen()
    composeTestRule.waitForIdleWithTimeout()

    composeTestRule.onNodeWithTag(SignInScreenTestTags.GITHUB_SIGN_IN_BUTTON).performClick()
    composeTestRule.waitForIdleWithTimeout()

    verify(exactly = 0) { mockViewModel.signInWithGithub(any()) }
  }

  @Test
  fun signInScreen_whenLoading_showsLoadingIndicator() {
    every { mockViewModel.uiState } returns MutableStateFlow(AuthUIState(isLoading = true))

    setUpSignInScreen()
    composeTestRule.waitForIdleWithTimeout()

    composeTestRule.onNodeWithTag(LoadingTestTags.LOADING_INDICATOR).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.GOOGLE_SIGN_IN_BUTTON).assertDoesNotExist()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.GITHUB_SIGN_IN_BUTTON).assertDoesNotExist()
  }

  @Test
  fun signInScreen_whenNotLoading_showsSignInButtons() {
    every { mockViewModel.uiState } returns MutableStateFlow(AuthUIState(isLoading = false))

    setUpSignInScreen()
    composeTestRule.waitForIdleWithTimeout()

    composeTestRule.onNodeWithTag(SignInScreenTestTags.GOOGLE_SIGN_IN_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.GITHUB_SIGN_IN_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(LoadingTestTags.LOADING_INDICATOR).assertDoesNotExist()
  }

  @Test
  fun signInScreen_onSuccessLogin_triggersCallback() {
    val mockUser: FirebaseUser = mockk(relaxed = true)
    val stateFlow = MutableStateFlow(AuthUIState())

    every { mockViewModel.uiState } returns stateFlow

    setUpSignInScreen()
    composeTestRule.waitForIdleWithTimeout()

    stateFlow.value = AuthUIState(user = mockUser)
    composeTestRule.waitForIdleWithTimeout()

    verify(timeout = 1000) { onSignedInCallback() }
  }

  @Test
  fun signInScreen_onError_clearsErrorMessage() {
    val stateFlow = MutableStateFlow(AuthUIState())
    every { mockViewModel.uiState } returns stateFlow
    every { mockViewModel.clearErrorMsg() } just runs

    setUpSignInScreen()
    composeTestRule.waitForIdleWithTimeout()

    stateFlow.value = AuthUIState(errorMsg = "Login failed")
    composeTestRule.waitForIdleWithTimeout()

    verify(timeout = 1000) { mockViewModel.clearErrorMsg() }
  }

  @Test
  fun googleSignInButton_isClickableAndDisplaysCorrectText() {
    var clicked = false

    composeTestRule.setContent {
      MainAppTheme { GoogleSignInButton(onSignInClick = { clicked = true }) }
    }

    composeTestRule.waitForIdleWithTimeout()

    composeTestRule
        .onNodeWithTag(SignInScreenTestTags.GOOGLE_SIGN_IN_BUTTON)
        .assertIsDisplayed()
        .assertTextContains("Sign in with Google")
        .performClick()

    composeTestRule.waitForIdleWithTimeout()
    assert(clicked)
  }

  @Test
  fun githubSignInButton_isClickableAndDisplaysCorrectText() {
    var clicked = false

    composeTestRule.setContent {
      MainAppTheme { GitHubSignInButton(onSignInClick = { clicked = true }) }
    }

    composeTestRule.waitForIdleWithTimeout()

    composeTestRule
        .onNodeWithTag(SignInScreenTestTags.GITHUB_SIGN_IN_BUTTON)
        .assertIsDisplayed()
        .assertTextContains("Sign in with GitHub")
        .performClick()

    composeTestRule.waitForIdleWithTimeout()
    assert(clicked)
  }

  @Test
  fun signInScreen_stateTransitions_updateUICorrectly() {
    val stateFlow = MutableStateFlow(AuthUIState())
    every { mockViewModel.uiState } returns stateFlow

    setUpSignInScreen()
    composeTestRule.waitForIdleWithTimeout()

    composeTestRule.onNodeWithTag(SignInScreenTestTags.GOOGLE_SIGN_IN_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.GITHUB_SIGN_IN_BUTTON).assertIsDisplayed()

    stateFlow.value = AuthUIState(isLoading = true)
    composeTestRule.waitForIdleWithTimeout()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.GOOGLE_SIGN_IN_BUTTON).assertDoesNotExist()
    composeTestRule.onNodeWithTag(LoadingTestTags.LOADING_INDICATOR).assertIsDisplayed()

    stateFlow.value = AuthUIState(isLoading = false)
    composeTestRule.waitForIdleWithTimeout()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.GOOGLE_SIGN_IN_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(SignInScreenTestTags.GITHUB_SIGN_IN_BUTTON).assertIsDisplayed()
  }
}
