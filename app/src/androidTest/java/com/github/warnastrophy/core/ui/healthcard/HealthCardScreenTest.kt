package com.github.warnastrophy.core.ui.healthcard

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.warnastrophy.core.model.HealthCard
import com.github.warnastrophy.core.ui.components.LoadingTestTags
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HealthCardScreenTest {
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private lateinit var mockViewModel: HealthCardViewModel
  private val uiStateFlow = MutableStateFlow<HealthCardUiState>(HealthCardUiState.Idle)
  private val currentCardFlow = MutableStateFlow<HealthCard?>(null)

  @Before
  fun setUp() {
    mockViewModel = mockk<HealthCardViewModel>(relaxed = true)

    every { mockViewModel.uiState } returns uiStateFlow.asStateFlow()
    every { mockViewModel.currentCard } returns currentCardFlow.asStateFlow()

    composeRule.setContent { HealthCardScreen(userId = "user123", viewModel = mockViewModel) }
  }

  @Test
  fun topBar_displaysCorrectTitle_andBackButton() {
    composeRule.onNodeWithText("Health card").assertIsDisplayed()
    composeRule.onNodeWithTag(HealthCardTestTags.BACK_BUTTON).assertIsDisplayed()
  }

  @Test
  fun requiredFields_areDisplayed_andInitiallyEmpty() {
    composeRule
        .onNodeWithTag(HealthCardTestTags.FULL_NAME_FIELD)
        .assertIsDisplayed()
        .assertTextContains("")

    composeRule
        .onNodeWithTag(HealthCardTestTags.BIRTH_DATE_FIELD)
        .assertIsDisplayed()
        .assertTextContains("")

    composeRule
        .onNodeWithTag(HealthCardTestTags.SSN_FIELD)
        .assertIsDisplayed()
        .assertTextContains("")
  }

  @Test
  fun optionalFields_areDisplayed() {
    composeRule.onNodeWithTag(HealthCardTestTags.SEX_FIELD).assertIsDisplayed()
    composeRule.onNodeWithTag(HealthCardTestTags.BLOOD_TYPE_FIELD).assertIsDisplayed()
    composeRule.onNodeWithTag(HealthCardTestTags.HEIGHT_FIELD).assertIsDisplayed()
    composeRule.onNodeWithTag(HealthCardTestTags.WEIGHT_FIELD).assertIsDisplayed()
    composeRule.onNodeWithTag(HealthCardTestTags.CHRONIC_CONDITIONS_FIELD).assertIsDisplayed()
    composeRule.onNodeWithTag(HealthCardTestTags.ALLERGIES_FIELD).assertIsDisplayed()
    composeRule.onNodeWithTag(HealthCardTestTags.MEDICATIONS_FIELD).assertIsDisplayed()
    composeRule
        .onNodeWithTag(HealthCardTestTags.TREATMENTS_FIELD)
        .performScrollTo()
        .assertIsDisplayed()
    composeRule
        .onNodeWithTag(HealthCardTestTags.HISTORY_FIELD)
        .performScrollTo()
        .assertIsDisplayed()
    composeRule
        .onNodeWithTag(HealthCardTestTags.ORGAN_DONOR_FIELD)
        .performScrollTo()
        .assertIsDisplayed()
    composeRule.onNodeWithTag(HealthCardTestTags.NOTES_FIELD).performScrollTo().assertIsDisplayed()
  }

  @Test
  fun addButton_isDisplayed_when_noHealthCard() {
    composeRule.onNodeWithTag(HealthCardTestTags.ADD_BUTTON).performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag(HealthCardTestTags.UPDATE_BUTTON).assertDoesNotExist()
    composeRule.onNodeWithTag(HealthCardTestTags.DELETE_BUTTON).assertDoesNotExist()
  }

  @Test
  fun updateAndDeleteButtons_areDisplayed_when_cardExists() {
    currentCardFlow.value = dummyCard()
    composeRule.waitForIdle()
    composeRule
        .onNodeWithTag(HealthCardTestTags.UPDATE_BUTTON)
        .performScrollTo()
        .assertIsDisplayed()
    composeRule
        .onNodeWithTag(HealthCardTestTags.DELETE_BUTTON)
        .performScrollTo()
        .assertIsDisplayed()
    composeRule.onNodeWithTag(HealthCardTestTags.ADD_BUTTON).assertDoesNotExist()
  }

  @Test
  fun clickingAddButton_withValidFields_callsSaveHealthCard() {
    // Fill required fields
    composeRule.onNodeWithTag(HealthCardTestTags.FULL_NAME_FIELD).performTextInput("John Doe")
    composeRule.onNodeWithTag(HealthCardTestTags.BIRTH_DATE_FIELD).performTextInput("01/01/2000")
    composeRule.onNodeWithTag(HealthCardTestTags.SSN_FIELD).performTextInput("123-45-6789")

    composeRule.onNodeWithTag(HealthCardTestTags.ADD_BUTTON).performScrollTo().performClick()

    verify { mockViewModel.saveHealthCard(any(), "user123", any()) }
  }

  @Test
  fun clickingUpdateButton_callsUpdateHealthCard() {
    currentCardFlow.value = dummyCard()
    composeRule.waitForIdle()

    composeRule
        .onNodeWithTag(HealthCardTestTags.CHRONIC_CONDITIONS_FIELD)
        .performScrollTo()
        .performTextInput("Diabetes, Asthma")

    composeRule.onNodeWithTag(HealthCardTestTags.UPDATE_BUTTON).performScrollTo().performClick()

    verify { mockViewModel.updateHealthCard(any(), "user123", any()) }
  }

  @Test
  fun clickingDeleteButton_callsDeleteHealthCard() {
    currentCardFlow.value = dummyCard()
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(HealthCardTestTags.DELETE_BUTTON).performScrollTo().performClick()

    verify { mockViewModel.deleteHealthCard(any(), "user123") }
  }

  @Test
  fun errorMessages_areDisplayed_whenRequiredFieldsTouchedAndEmpty() {
    // Touch the fields but leave empty
    composeRule.onNodeWithTag(HealthCardTestTags.FULL_NAME_FIELD).performClick()
    composeRule.onNodeWithTag(HealthCardTestTags.BIRTH_DATE_FIELD).performClick()
    composeRule.onNodeWithTag(HealthCardTestTags.SSN_FIELD).performClick()

    // Force validation by trying to click Add
    composeRule.onNodeWithTag(HealthCardTestTags.ADD_BUTTON).performScrollTo().performClick()

    composeRule.onAllNodesWithText("Mandatory field").assertCountEquals(3)
  }

  @Test
  fun loadingIndicator_isDisplayed_whenUiStateLoading() {
    uiStateFlow.value = HealthCardUiState.Loading
    composeRule.waitForIdle()

    composeRule
        .onNodeWithTag(LoadingTestTags.LOADING_INDICATOR)
        .performScrollTo()
        .assertIsDisplayed()
  }

  @Test
  fun birthDate_field_accepts_and_formats_proper_date() {
    composeRule.onNodeWithTag(HealthCardTestTags.BIRTH_DATE_FIELD).performTextInput("15/07/1998")
    composeRule.onNodeWithTag(HealthCardTestTags.BIRTH_DATE_FIELD).assertTextContains("15/07/1998")
  }

  @Test
  fun existingCard_isPopulated_intoForm() {
    val card = dummyCard()
    currentCardFlow.value = card
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(HealthCardTestTags.FULL_NAME_FIELD).assertTextContains(card.fullName)
    composeRule.onNodeWithTag(HealthCardTestTags.BIRTH_DATE_FIELD).assertTextContains("01/01/2000")
    composeRule
        .onNodeWithTag(HealthCardTestTags.SSN_FIELD)
        .assertTextContains(card.socialSecurityNumber)
  }

  private fun dummyCard() =
      HealthCard(
          fullName = "John Doe",
          birthDate = "2000-01-01",
          socialSecurityNumber = "123-45-6789",
          sex = "Male",
          bloodType = "A+",
          heightCm = 180,
          weightKg = 75.0,
          chronicConditions = listOf("Diabetes"),
          allergies = listOf("Pollen"),
          medications = listOf("Metformin"),
          onGoingTreatments = listOf(),
          medicalHistory = listOf(),
          organDonor = true,
          notes = "N/A")
}
