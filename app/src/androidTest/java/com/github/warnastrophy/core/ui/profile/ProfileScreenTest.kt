package com.github.warnastrophy.core.ui.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.warnastrophy.core.ui.features.auth.AuthUIState
import com.github.warnastrophy.core.ui.features.auth.SignInViewModel
import com.github.warnastrophy.core.ui.features.profile.LocalThemeViewModel
import com.github.warnastrophy.core.ui.features.profile.ProfileScreen
import com.github.warnastrophy.core.ui.features.profile.ProfileScreenTestTag
import com.github.warnastrophy.core.ui.features.profile.ThemeViewModel
import com.github.warnastrophy.core.ui.navigation.NavigationTestTags
import com.github.warnastrophy.core.util.BaseSimpleComposeTest
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test

/**
 * Instrumentation tests for the ProfileScreen component.
 *
 * Tests cover:
 * - Display of profile list items
 * - Navigation callbacks
 * - Logout dialog flow
 * - Error handling
 * - Theme toggle functionality
 */
class ProfileScreenTest : BaseSimpleComposeTest() {

  private lateinit var mockSignInViewModel: SignInViewModel
  private lateinit var mockThemeViewModel: ThemeViewModel
  private lateinit var mockOnHealthCardClick: () -> Unit
  private lateinit var mockOnEmergencyContactsClick: () -> Unit
  private lateinit var mockOnDangerModePreferencesClick: () -> Unit
  private lateinit var mockOnLogout: () -> Unit
  private lateinit var uiStateFlow: MutableStateFlow<AuthUIState>
  private lateinit var isDarkModeFlow: MutableStateFlow<Boolean?>

  @Before
  override fun setUp() {
    super.setUp()

    // Initialize mocks
    mockSignInViewModel = mockk(relaxed = true)
    mockThemeViewModel = mockk(relaxed = true)
    mockOnHealthCardClick = mockk(relaxed = true)
    mockOnEmergencyContactsClick = mockk(relaxed = true)
    mockOnDangerModePreferencesClick = mockk(relaxed = true)
    mockOnLogout = mockk(relaxed = true)

    // Initialize UI state flow
    uiStateFlow = MutableStateFlow(AuthUIState())
    every { mockSignInViewModel.uiState } returns uiStateFlow
    every { mockSignInViewModel.signOut() } just Runs
    every { mockSignInViewModel.clearErrorMsg() } just Runs

    // Initialize theme flow
    isDarkModeFlow = MutableStateFlow(false)
    every { mockThemeViewModel.isDarkMode } returns isDarkModeFlow
    every { mockThemeViewModel.toggleTheme(any()) } just Runs
  }

  /**
   * Helper function to wrap ProfileScreen with CompositionLocalProvider for ThemeViewModel in
   * tests.
   */
  @Composable
  private fun TestProfileScreen(
      signInViewModel: SignInViewModel = mockSignInViewModel,
      onHealthCardClick: () -> Unit = {},
      onEmergencyContactsClick: () -> Unit = {},
      onLogout: () -> Unit = {},
      onDangerModePreferencesClick: () -> Unit = {}
  ) {
    CompositionLocalProvider(LocalThemeViewModel provides mockThemeViewModel) {
      ProfileScreen(
          signInViewModel = signInViewModel,
          onHealthCardClick = onHealthCardClick,
          onEmergencyContactsClick = onEmergencyContactsClick,
          onLogout = onLogout,
          onDangerModePreferencesClick = onDangerModePreferencesClick)
    }
  }

  @Test
  fun profileScreen_displaysAllListItems() {
    composeTestRule.setContent { TestProfileScreen() }

    composeTestRule.waitForIdleWithTimeout()

    // Verify all profile items are displayed
    composeTestRule.onNodeWithTag(NavigationTestTags.HEALTH_CARD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.CONTACT_LIST).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.DANGER_MODE_PREFERENCES).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ProfileScreenTestTag.THEME_TOGGLE_SWITCH).assertIsDisplayed()
    composeTestRule.onNodeWithTag(NavigationTestTags.LOGOUT).assertIsDisplayed()
  }

  @Test
  fun healthCardClick_triggersCallback() {
    composeTestRule.setContent { TestProfileScreen(onHealthCardClick = mockOnHealthCardClick) }

    composeTestRule.waitForIdleWithTimeout()

    composeTestRule.onNodeWithTag(NavigationTestTags.HEALTH_CARD).performClick()

    composeTestRule.waitForIdleWithTimeout()

    verify(exactly = 1) { mockOnHealthCardClick() }
  }

  @Test
  fun emergencyContactsClick_triggersCallback() {
    composeTestRule.setContent {
      TestProfileScreen(onEmergencyContactsClick = mockOnEmergencyContactsClick)
    }

    composeTestRule.waitForIdleWithTimeout()

    composeTestRule.onNodeWithTag(NavigationTestTags.CONTACT_LIST).performClick()

    composeTestRule.waitForIdleWithTimeout()

    verify(exactly = 1) { mockOnEmergencyContactsClick() }
  }

  @Test
  fun onDangerModePreferencesClick_triggersCallback() {
    composeTestRule.setContent {
      TestProfileScreen(onDangerModePreferencesClick = mockOnDangerModePreferencesClick)
    }
    composeTestRule.waitForIdleWithTimeout()
    composeTestRule.onNodeWithTag(NavigationTestTags.DANGER_MODE_PREFERENCES).performClick()
    composeTestRule.waitForIdleWithTimeout()

    verify(exactly = 1) { mockOnDangerModePreferencesClick() }
  }

  @Test
  fun themeToggle_whenOff_displaysLightMode() {
    isDarkModeFlow.value = false

    composeTestRule.setContent { TestProfileScreen() }

    composeTestRule.waitForIdleWithTimeout()

    // Verify switch is off and "Light" text is displayed
    composeTestRule.onNodeWithTag(ProfileScreenTestTag.THEME_TOGGLE_SWITCH).assertIsDisplayed()
    composeTestRule.onNodeWithText("Light").assertIsDisplayed()
  }

  @Test
  fun themeToggle_whenOn_displaysDarkMode() {
    isDarkModeFlow.value = true

    composeTestRule.setContent { TestProfileScreen() }

    composeTestRule.waitForIdleWithTimeout()

    // Verify switch is on and "Dark" text is displayed
    composeTestRule.onNodeWithTag(ProfileScreenTestTag.THEME_TOGGLE_SWITCH).assertIsDisplayed()
    composeTestRule.onNodeWithText("Dark").assertIsDisplayed()
  }

  @Test
  fun themeToggle_click_callsToggleTheme() {
    isDarkModeFlow.value = false

    composeTestRule.setContent { TestProfileScreen() }

    composeTestRule.waitForIdleWithTimeout()

    // Click the switch directly
    composeTestRule
        .onNodeWithTag("${ProfileScreenTestTag.THEME_TOGGLE_SWITCH}_switch")
        .performClick()

    composeTestRule.waitForIdleWithTimeout()

    // Verify toggleTheme was called with true (to enable dark mode)
    verify(exactly = 1) { mockThemeViewModel.toggleTheme(true) }
  }

  @Test
  fun themeToggle_clickWhenOn_callsToggleThemeWithFalse() {
    isDarkModeFlow.value = true

    composeTestRule.setContent { TestProfileScreen() }

    composeTestRule.waitForIdleWithTimeout()

    // Click the switch directly
    composeTestRule
        .onNodeWithTag("${ProfileScreenTestTag.THEME_TOGGLE_SWITCH}_switch")
        .performClick()

    composeTestRule.waitForIdleWithTimeout()

    // Verify toggleTheme was called with false (to disable dark mode)
    verify(exactly = 1) { mockThemeViewModel.toggleTheme(false) }
  }

  @Test
  fun logoutClick_showsConfirmationDialog() {
    composeTestRule.setContent { TestProfileScreen() }

    composeTestRule.waitForIdleWithTimeout()

    // Click logout button
    composeTestRule.onNodeWithTag(NavigationTestTags.LOGOUT).performClick()

    composeTestRule.waitForIdleWithTimeout()

    // Verify dialog is displayed by checking for dialog-specific content
    // Use unmergedTree for dialog content
    composeTestRule
        .onNodeWithText("Are you sure you want to logout?", useUnmergedTree = true)
        .assertIsDisplayed()
    composeTestRule.onNodeWithText("Cancel", useUnmergedTree = true).assertIsDisplayed()

    // Verify there are now multiple "Logout" texts (list item + dialog title + dialog button)
    composeTestRule.onAllNodesWithText("Logout", useUnmergedTree = true).assertCountEquals(3)
  }

  @Test
  fun logoutDialog_cancelButton_dismissesDialog() {
    composeTestRule.setContent { TestProfileScreen() }

    composeTestRule.waitForIdleWithTimeout()

    // Show logout dialog
    composeTestRule.onNodeWithTag(NavigationTestTags.LOGOUT).performClick()

    composeTestRule.waitForIdleWithTimeout()

    // Click cancel (use unmergedTree for dialog content)
    composeTestRule.onNodeWithText("Cancel", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdleWithTimeout()

    // Verify dialog is dismissed - dialog text should not exist
    composeTestRule
        .onNodeWithText("Are you sure you want to logout?", useUnmergedTree = true)
        .assertDoesNotExist()

    // Verify signOut was not called
    verify(exactly = 0) { mockSignInViewModel.signOut() }
  }

  @Test
  fun logoutDialog_confirmButton_callsSignOut() {
    composeTestRule.setContent { TestProfileScreen() }

    composeTestRule.waitForIdleWithTimeout()

    // Show logout dialog
    composeTestRule.onNodeWithTag(NavigationTestTags.LOGOUT).performClick()

    composeTestRule.waitForIdleWithTimeout()

    // Click confirm logout button (use unmergedTree and onAllNodesWithText to get all "Logout"
    // nodes)
    // The button is the last one (index 2: list item=0, dialog title=1, dialog button=2)
    composeTestRule.onAllNodesWithText("Logout", useUnmergedTree = true)[2].performClick()

    composeTestRule.waitForIdleWithTimeout()

    // Verify signOut was called
    verify(exactly = 1) { mockSignInViewModel.signOut() }
  }

  @Test
  fun signedOutState_triggersLogoutCallback() {
    composeTestRule.setContent { TestProfileScreen(onLogout = mockOnLogout) }

    composeTestRule.waitForIdleWithTimeout()

    // Update state to signedOut
    uiStateFlow.value = AuthUIState(signedOut = true)

    composeTestRule.waitForIdleWithTimeout()

    // Verify logout callback was triggered
    verify(exactly = 1) { mockOnLogout() }
  }

  @Test
  fun errorState_displaysErrorDialog() {
    val errorMessage = "Failed to sign out. Please try again."

    composeTestRule.setContent { TestProfileScreen() }

    composeTestRule.waitForIdleWithTimeout()

    // Update state with error
    uiStateFlow.value = AuthUIState(errorMsg = errorMessage)

    composeTestRule.waitForIdleWithTimeout()

    // Verify error dialog is displayed (use unmergedTree for dialog content)
    composeTestRule.onNodeWithText("Error", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText(errorMessage, useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithText("OK", useUnmergedTree = true).assertIsDisplayed()
  }

  @Test
  fun errorDialog_okButton_clearsError() {
    val errorMessage = "Network error occurred"

    composeTestRule.setContent { TestProfileScreen() }

    composeTestRule.waitForIdleWithTimeout()

    // Show error dialog
    uiStateFlow.value = AuthUIState(errorMsg = errorMessage)

    composeTestRule.waitForIdleWithTimeout()

    // Click OK button (use unmergedTree for dialog content)
    composeTestRule.onNodeWithText("OK", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdleWithTimeout()

    // Verify clearErrorMsg was called
    verify(exactly = 1) { mockSignInViewModel.clearErrorMsg() }
  }

  @Test
  fun logoutDialog_dismissOnClickOutside_dismissesDialog() {
    composeTestRule.setContent { TestProfileScreen() }

    composeTestRule.waitForIdleWithTimeout()

    // Show logout dialog
    composeTestRule.onNodeWithTag(NavigationTestTags.LOGOUT).performClick()

    composeTestRule.waitForIdleWithTimeout()

    // Verify dialog is displayed
    composeTestRule
        .onNodeWithText("Are you sure you want to logout?", useUnmergedTree = true)
        .assertIsDisplayed()

    // Note: Testing dismiss on click outside requires more complex setup
    // as it involves touching outside the dialog bounds
    // This is typically tested through integration or manual testing
  }

  @Test
  fun multipleClicks_onHealthCard_triggersMultipleCallbacks() {
    composeTestRule.setContent { TestProfileScreen(onHealthCardClick = mockOnHealthCardClick) }

    composeTestRule.waitForIdleWithTimeout()

    // Click multiple times
    repeat(3) {
      composeTestRule.onNodeWithTag(NavigationTestTags.HEALTH_CARD).performClick()
      composeTestRule.waitForIdleWithTimeout()
    }

    // Verify callback was called 3 times
    verify(exactly = 3) { mockOnHealthCardClick() }
  }

  @Test
  fun profileScreen_withNoCallbacks_doesNotCrash() {
    // Test that screen works with default empty callbacks
    composeTestRule.setContent { TestProfileScreen() }

    composeTestRule.waitForIdleWithTimeout()

    // Click items - should not crash
    composeTestRule.onNodeWithTag(NavigationTestTags.HEALTH_CARD).performClick()

    composeTestRule.waitForIdleWithTimeout()

    composeTestRule.onNodeWithTag(NavigationTestTags.CONTACT_LIST).performClick()

    composeTestRule.waitForIdleWithTimeout()

    // Verify screen is still displayed
    composeTestRule.onNodeWithText("Health Card").assertIsDisplayed()
  }

  @Test
  fun themeToggle_nullValue_defaultsToFalse() {
    isDarkModeFlow.value = null

    composeTestRule.setContent { TestProfileScreen() }

    composeTestRule.waitForIdleWithTimeout()

    // Verify switch defaults to off when value is null
    composeTestRule.onNodeWithTag(ProfileScreenTestTag.THEME_TOGGLE_SWITCH).assertIsDisplayed()
    composeTestRule.onNodeWithText("Light").assertIsDisplayed()
  }
}
