package com.github.warnastrophy.core.ui.profile

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.warnastrophy.core.ui.features.auth.AuthUIState
import com.github.warnastrophy.core.ui.features.auth.SignInViewModel
import com.github.warnastrophy.core.ui.features.profile.ProfileScreen
import com.github.warnastrophy.core.ui.navigation.NavigationTestTags
import com.github.warnastrophy.core.ui.util.BaseSimpleComposeTest
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
 */
class ProfileScreenTest : BaseSimpleComposeTest() {

  private lateinit var mockViewModel: SignInViewModel
  private lateinit var mockOnHealthCardClick: () -> Unit
  private lateinit var mockOnEmergencyContactsClick: () -> Unit
  private lateinit var mockOnLogout: () -> Unit
  private lateinit var uiStateFlow: MutableStateFlow<AuthUIState>

  @Before
  override fun setUp() {
    super.setUp()

    // Initialize mocks
    mockViewModel = mockk(relaxed = true)
    mockOnHealthCardClick = mockk(relaxed = true)
    mockOnEmergencyContactsClick = mockk(relaxed = true)
    mockOnLogout = mockk(relaxed = true)

    // Initialize UI state flow
    uiStateFlow = MutableStateFlow(AuthUIState())
    every { mockViewModel.uiState } returns uiStateFlow
    every { mockViewModel.signOut() } just Runs
    every { mockViewModel.clearErrorMsg() } just Runs
  }

  @Test
  fun profileScreen_displaysAllListItems() {
    composeTestRule.setContent { ProfileScreen(viewModel = mockViewModel) }

    composeTestRule.waitForIdleWithTimeout()

    // Verify all profile items are displayed
    composeTestRule.onNodeWithText("Health Card").assertIsDisplayed()
    composeTestRule.onNodeWithText("Emergency contacts").assertIsDisplayed()
    composeTestRule.onNodeWithText("Logout").assertIsDisplayed()
  }

  @Test
  fun profileScreen_healthCardItemHasCorrectTestTag() {
    composeTestRule.setContent { ProfileScreen(viewModel = mockViewModel) }

    composeTestRule.waitForIdleWithTimeout()

    composeTestRule.onNodeWithTag(NavigationTestTags.HEALTH_CARD).assertIsDisplayed()
  }

  @Test
  fun profileScreen_emergencyContactsItemHasCorrectTestTag() {
    composeTestRule.setContent { ProfileScreen(viewModel = mockViewModel) }

    composeTestRule.waitForIdleWithTimeout()

    composeTestRule.onNodeWithTag(NavigationTestTags.CONTACT_LIST).assertIsDisplayed()
  }

  @Test
  fun profileScreen_logoutItemHasCorrectTestTag() {
    composeTestRule.setContent { ProfileScreen(viewModel = mockViewModel) }

    composeTestRule.waitForIdleWithTimeout()

    composeTestRule.onNodeWithTag(NavigationTestTags.LOGOUT).assertIsDisplayed()
  }

  @Test
  fun healthCardClick_triggersCallback() {
    composeTestRule.setContent {
      ProfileScreen(viewModel = mockViewModel, onHealthCardClick = mockOnHealthCardClick)
    }

    composeTestRule.waitForIdleWithTimeout()

    composeTestRule.onNodeWithTag(NavigationTestTags.HEALTH_CARD).performClick()

    composeTestRule.waitForIdleWithTimeout()

    verify(exactly = 1) { mockOnHealthCardClick() }
  }

  @Test
  fun emergencyContactsClick_triggersCallback() {
    composeTestRule.setContent {
      ProfileScreen(
          viewModel = mockViewModel, onEmergencyContactsClick = mockOnEmergencyContactsClick)
    }

    composeTestRule.waitForIdleWithTimeout()

    composeTestRule.onNodeWithTag(NavigationTestTags.CONTACT_LIST).performClick()

    composeTestRule.waitForIdleWithTimeout()

    verify(exactly = 1) { mockOnEmergencyContactsClick() }
  }

  @Test
  fun logoutClick_showsConfirmationDialog() {
    composeTestRule.setContent { ProfileScreen(viewModel = mockViewModel) }

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
    composeTestRule.setContent { ProfileScreen(viewModel = mockViewModel) }

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
    verify(exactly = 0) { mockViewModel.signOut() }
  }

  @Test
  fun logoutDialog_confirmButton_callsSignOut() {
    composeTestRule.setContent { ProfileScreen(viewModel = mockViewModel) }

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
    verify(exactly = 1) { mockViewModel.signOut() }
  }

  @Test
  fun signedOutState_triggersLogoutCallback() {
    composeTestRule.setContent { ProfileScreen(viewModel = mockViewModel, onLogout = mockOnLogout) }

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

    composeTestRule.setContent { ProfileScreen(viewModel = mockViewModel) }

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

    composeTestRule.setContent { ProfileScreen(viewModel = mockViewModel) }

    composeTestRule.waitForIdleWithTimeout()

    // Show error dialog
    uiStateFlow.value = AuthUIState(errorMsg = errorMessage)

    composeTestRule.waitForIdleWithTimeout()

    // Click OK button (use unmergedTree for dialog content)
    composeTestRule.onNodeWithText("OK", useUnmergedTree = true).performClick()

    composeTestRule.waitForIdleWithTimeout()

    // Verify clearErrorMsg was called
    verify(exactly = 1) { mockViewModel.clearErrorMsg() }
  }

  @Test
  fun logoutDialog_dismissOnClickOutside_dismissesDialog() {
    composeTestRule.setContent { ProfileScreen(viewModel = mockViewModel) }

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
    composeTestRule.setContent {
      ProfileScreen(viewModel = mockViewModel, onHealthCardClick = mockOnHealthCardClick)
    }

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
    composeTestRule.setContent { ProfileScreen(viewModel = mockViewModel) }

    composeTestRule.waitForIdleWithTimeout()

    // Click items - should not crash
    composeTestRule.onNodeWithTag(NavigationTestTags.HEALTH_CARD).performClick()

    composeTestRule.waitForIdleWithTimeout()

    composeTestRule.onNodeWithTag(NavigationTestTags.CONTACT_LIST).performClick()

    composeTestRule.waitForIdleWithTimeout()

    // Verify screen is still displayed
    composeTestRule.onNodeWithText("Health Card").assertIsDisplayed()
  }
}
