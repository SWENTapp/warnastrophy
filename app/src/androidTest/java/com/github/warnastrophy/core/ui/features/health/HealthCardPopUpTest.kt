package com.github.warnastrophy.core.ui.features.health

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.github.warnastrophy.core.model.HealthCard
import com.github.warnastrophy.core.ui.theme.MainAppTheme
import com.github.warnastrophy.core.util.BaseAndroidComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * UI tests for the [HealthCardPopUp] composable. This class verifies the behavior and appearance of
 * the Health Card pop-up.
 */
class HealthCardPopUpTest : BaseAndroidComposeTest() {
  private lateinit var mockViewModel: HealthCardViewModel
  private val currentCardFlow = MutableStateFlow<HealthCard?>(null)
  private val fakeUserId = "user1234"

  @Before
  override fun setUp() {
    mockViewModel =
        mockk(relaxed = true) { every { currentCard } returns currentCardFlow.asStateFlow() }
    // Reset the state before each test
    currentCardFlow.value = null
  }

  @Test
  fun healthCardPopUp_displaysEmptyState_whenNoCardIsAvailable() {
    composeTestRule.setContent {
      MainAppTheme {
        HealthCardPopUp(
            onDismissRequest = {}, onClick = {}, viewModel = mockViewModel, userId = fakeUserId)
      }
    }

    // Verify that the main pop-up components are displayed
    composeTestRule.onNodeWithTag(HealthCardPopUpTestTags.ROOT_CARD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(HealthCardPopUpTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(HealthCardPopUpTestTags.EDIT_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(HealthCardPopUpTestTags.CONTENT_CARD).assertIsDisplayed()

    // Verify that the empty state text is shown
    composeTestRule.onNodeWithTag(HealthCardPopUpTestTags.EMPTY_STATE_TEXT).assertIsDisplayed()
  }

  @Test
  fun healthCardPopUp_displaysCardDetails_whenCardIsAvailable() {
    // Prepare a dummy health card
    val card =
        HealthCard(
            fullName = "Jane Doe",
            dateOfBirthIso = "1990-05-15",
            idNumber = "987654321",
            sex = "Female",
            bloodType = "O-",
            allergies = listOf("Peanuts", "Dust"),
            chronicConditions = listOf("Asthma"),
            organDonor = true,
            notes = "Regular check-ups needed.")
    currentCardFlow.value = card

    composeTestRule.setContent {
      MainAppTheme {
        HealthCardPopUp(
            onDismissRequest = {}, onClick = {}, viewModel = mockViewModel, userId = fakeUserId)
      }
    }

    // Verify that the details from the card are displayed correctly
    composeTestRule.onNodeWithText("Name").assertIsDisplayed()
    composeTestRule.onNodeWithText("Jane Doe").assertIsDisplayed()

    val expectedDate =
        LocalDate.parse("1990-05-15").format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    composeTestRule.onNodeWithText("Date of birth").assertIsDisplayed()
    composeTestRule.onNodeWithText(expectedDate).assertIsDisplayed()

    composeTestRule.onNodeWithText("Gender").assertIsDisplayed()
    composeTestRule.onNodeWithText("Female").assertIsDisplayed()

    composeTestRule.onNodeWithText("Blood Type").assertIsDisplayed()
    composeTestRule.onNodeWithText("O-").assertIsDisplayed()

    composeTestRule.onNodeWithText("Allergies").assertIsDisplayed()
    // The joinToString() will produce "Peanuts, Dust"
    composeTestRule.onNodeWithText("Peanuts, Dust").assertIsDisplayed()

    composeTestRule.onNodeWithText("Medical Conditions").assertIsDisplayed()
    composeTestRule.onNodeWithText("Asthma").assertIsDisplayed()

    composeTestRule.onNodeWithText("Organ Donor").assertIsDisplayed()
    composeTestRule.onNodeWithText("Yes").assertIsDisplayed()

    composeTestRule.onNodeWithText("Notes").assertIsDisplayed()
    composeTestRule.onNodeWithText("Regular check-ups needed.").assertIsDisplayed()

    // Ensure the empty state text is not visible
    composeTestRule.onNodeWithTag(HealthCardPopUpTestTags.EMPTY_STATE_TEXT).assertDoesNotExist()
  }

  @Test
  fun healthCardPopUp_editButtonTriggersOnClickCallback() {
    var isClicked = false

    composeTestRule.setContent {
      MainAppTheme {
        HealthCardPopUp(
            onDismissRequest = {},
            onClick = { isClicked = true },
            viewModel = mockViewModel,
            userId = fakeUserId)
      }
    }

    // Perform a click on the edit button
    composeTestRule.onNodeWithTag(HealthCardPopUpTestTags.EDIT_BUTTON).performClick()

    // Verify that the callback was triggered
    assertTrue("The onClick callback should have been triggered.", isClicked)
  }

  @Test
  fun healthCardPopUp_loadsHealthCard_onLaunch() {
    val testUserId = "testUser123"

    composeTestRule.setContent {
      MainAppTheme {
        // The LaunchedEffect inside the composable will trigger the load
        HealthCardPopUp(
            onClick = {}, onDismissRequest = {}, viewModel = mockViewModel, userId = testUserId)
      }
    }

    // The LaunchedEffect uses AppConfig.userId, which we can't control here.
    // However, we can verify that the loadHealthCard function was called with any context and the
    // expected user ID.
    // NOTE: This relies on how the Composable is implemented. If it changes to not use
    // AppConfig.userId, this test will fail.
    verify { mockViewModel.loadHealthCard(any(), testUserId) }
  }
}
