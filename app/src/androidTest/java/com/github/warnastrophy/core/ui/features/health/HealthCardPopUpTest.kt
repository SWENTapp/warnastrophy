package com.github.warnastrophy.core.ui.features.health

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.warnastrophy.core.model.HealthCard
import com.github.warnastrophy.core.ui.theme.MainAppTheme
import com.github.warnastrophy.core.util.BaseAndroidComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for the [HealthCardPopUp] composable. This class verifies the behavior and appearance of
 * the Health Card pop-up.
 */
@RunWith(AndroidJUnit4::class)
class HealthCardPopUpTest : BaseAndroidComposeTest() {
  private lateinit var mockViewModel: HealthCardViewModel
  private val currentCardFlow = MutableStateFlow<HealthCard?>(null)
  private val fakeUserId = "user1234"

  @Before
  override fun setUp() {
    super.setUp()
    mockViewModel =
        mockk(relaxed = true) { every { currentCard } returns currentCardFlow.asStateFlow() }
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

    composeTestRule.onNodeWithTag(HealthCardPopUpTestTags.ROOT_CARD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(HealthCardPopUpTestTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(HealthCardPopUpTestTags.EDIT_BUTTON).assertIsDisplayed()
    composeTestRule.onNodeWithTag(HealthCardPopUpTestTags.CONTENT_CARD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(HealthCardPopUpTestTags.EMPTY_STATE_TEXT).assertIsDisplayed()
  }

  @Test
  fun healthCardPopUp_displaysCardDetails_whenCardIsAvailable() {
    val card =
        HealthCard(
            fullName = "Jane Doe",
            dateOfBirthIso = "1990-05-15",
            idNumber = "987654321",
            sex = "Female",
            bloodType = "O-",
            allergies = listOf("Peanuts", "Dust"),
            medications = listOf("Painkiller"),
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

    composeTestRule
        .onNodeWithTag(HealthCardPopUpTestTags.FULL_NAME_VALUE)
        .assertIsDisplayed()
        .assertTextContains("Jane Doe")

    composeTestRule
        .onNodeWithTag(HealthCardPopUpTestTags.BIRTH_DATE_VALUE)
        .assertIsDisplayed()
        .assertTextContains("15/05/1990")

    composeTestRule
        .onNodeWithTag(HealthCardPopUpTestTags.SEX_VALUE)
        .assertIsDisplayed()
        .assertTextContains("Female")

    composeTestRule
        .onNodeWithTag(HealthCardPopUpTestTags.BLOOD_TYPE_VALUE)
        .assertIsDisplayed()
        .assertTextContains("O-")

    composeTestRule
        .onNodeWithTag(HealthCardPopUpTestTags.ALLERGIES_VALUE)
        .assertIsDisplayed()
        .assertTextContains("Peanuts, Dust")
    composeTestRule
        .onNodeWithTag(HealthCardPopUpTestTags.MEDICATIONS_VALUE)
        .assertIsDisplayed()
        .assertTextContains("Painkiller")

    composeTestRule
        .onNodeWithTag(HealthCardPopUpTestTags.ORGAN_DONOR_VALUE)
        .assertIsDisplayed()
        .assertTextContains("Yes")

    composeTestRule
        .onNodeWithTag(HealthCardPopUpTestTags.NOTES_VALUE)
        .assertIsDisplayed()
        .assertTextContains("Regular check-ups needed.")

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

    composeTestRule.onNodeWithTag(HealthCardPopUpTestTags.EDIT_BUTTON).performClick()

    assertTrue(isClicked)
  }

  @Test
  fun healthCardPopUp_loadsHealthCard_onLaunch() {
    val testUserId = "testUser123"

    composeTestRule.setContent {
      MainAppTheme {
        // LaunchedEffect will trigger loadHealthCard
        HealthCardPopUp(
            onClick = {}, onDismissRequest = {}, viewModel = mockViewModel, userId = testUserId)
      }
    }

    composeTestRule.waitForIdle()

    verify(exactly = 1) { mockViewModel.loadHealthCard(any(), testUserId) }
  }
}
