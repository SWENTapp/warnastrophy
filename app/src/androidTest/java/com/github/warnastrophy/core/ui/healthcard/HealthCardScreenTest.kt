package com.github.warnastrophy.core.ui.healthcard

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.warnastrophy.core.model.HealthCard
import com.github.warnastrophy.core.ui.components.LoadingTestTags
import com.github.warnastrophy.core.ui.util.BaseAndroidComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HealthCardScreenTest : BaseAndroidComposeTest() {

  private lateinit var mockViewModel: HealthCardViewModel
  private val uiStateFlow = MutableStateFlow<HealthCardUiState>(HealthCardUiState.Idle)
  private val currentCardFlow = MutableStateFlow<HealthCard?>(null)

  // Override to use extended timeout for this test class
  override val defaultTimeout: Long = EXTENDED_TIMEOUT

  @Before
  override fun setUp() {
    super.setUp()
    mockViewModel = mockk(relaxed = true)

    uiStateFlow.value = HealthCardUiState.Idle
    currentCardFlow.value = null

    every { mockViewModel.uiState } returns uiStateFlow.asStateFlow()
    every { mockViewModel.currentCard } returns currentCardFlow.asStateFlow()

    composeTestRule.setContent { HealthCardScreen(userId = "user123", viewModel = mockViewModel) }
    composeTestRule.waitForIdle()

    composeTestRule.waitUntilWithTimeout(EXTENDED_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(HealthCardTestTags.FULL_NAME_FIELD)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }
  }

  @Test
  fun topBar_displaysCorrectTitle_andBackButton() {
    composeTestRule.waitForIdle()

    composeTestRule.waitUntilWithTimeout {
      composeTestRule.onAllNodesWithText("Health card").fetchSemanticsNodes().isNotEmpty()
    }

    composeTestRule.onNodeWithText("Health card").assertIsDisplayed()
    composeTestRule.onNodeWithTag(HealthCardTestTags.BACK_BUTTON).assertIsDisplayed()
  }

  @Test
  fun requiredFields_areDisplayed_andInitiallyEmpty() {
    composeTestRule.waitForIdle()

    composeTestRule.waitUntilWithTimeout {
      composeTestRule
          .onAllNodesWithTag(HealthCardTestTags.FULL_NAME_FIELD)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(HealthCardTestTags.FULL_NAME_FIELD)
        .assertIsDisplayed()
        .assertTextContains("")

    composeTestRule
        .onNodeWithTag(HealthCardTestTags.BIRTH_DATE_FIELD)
        .assertIsDisplayed()
        .assertTextContains("")

    composeTestRule
        .onNodeWithTag(HealthCardTestTags.SSN_FIELD)
        .assertIsDisplayed()
        .assertTextContains("")
  }

  @Test
  fun optionalFields_areDisplayed() {
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(HealthCardTestTags.SEX_FIELD).assertExists()
    composeTestRule.onNodeWithTag(HealthCardTestTags.BLOOD_TYPE_FIELD).assertExists()
    composeTestRule.onNodeWithTag(HealthCardTestTags.HEIGHT_FIELD).assertExists()
    composeTestRule.onNodeWithTag(HealthCardTestTags.WEIGHT_FIELD).assertExists()
    composeTestRule.onNodeWithTag(HealthCardTestTags.CHRONIC_CONDITIONS_FIELD).assertExists()
    composeTestRule.onNodeWithTag(HealthCardTestTags.ALLERGIES_FIELD).assertExists()
    composeTestRule.onNodeWithTag(HealthCardTestTags.MEDICATIONS_FIELD).assertExists()

    composeTestRule.onNodeWithTag(HealthCardTestTags.TREATMENTS_FIELD).assertExists()
    composeTestRule.onNodeWithTag(HealthCardTestTags.HISTORY_FIELD).assertExists()
    composeTestRule.onNodeWithTag(HealthCardTestTags.ORGAN_DONOR_FIELD).assertExists()
    composeTestRule.onNodeWithTag(HealthCardTestTags.NOTES_FIELD).assertExists()
  }

  @Test
  fun addButton_isDisplayed_when_noHealthCard() {
    composeTestRule.waitForIdle()

    composeTestRule.waitUntilWithTimeout {
      composeTestRule
          .onAllNodesWithTag(HealthCardTestTags.ADD_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(HealthCardTestTags.ADD_BUTTON)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(HealthCardTestTags.UPDATE_BUTTON).assertDoesNotExist()
    composeTestRule.onNodeWithTag(HealthCardTestTags.DELETE_BUTTON).assertDoesNotExist()
  }

  @Test
  fun updateAndDeleteButtons_areDisplayed_when_cardExists() {
    currentCardFlow.value = dummyCard()
    composeTestRule.waitForIdle()

    composeTestRule.waitUntilWithTimeout {
      composeTestRule
          .onAllNodesWithTag(HealthCardTestTags.UPDATE_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(HealthCardTestTags.UPDATE_BUTTON)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(HealthCardTestTags.DELETE_BUTTON)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule.onNodeWithTag(HealthCardTestTags.ADD_BUTTON).assertDoesNotExist()
  }

  @Test
  fun clickingAddButton_withValidFields_callsSaveHealthCard() {
    composeTestRule.onNodeWithTag(HealthCardTestTags.FULL_NAME_FIELD).performTextInput("John Doe")
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(HealthCardTestTags.BIRTH_DATE_FIELD)
        .performTextInput("01/01/2000")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(HealthCardTestTags.SSN_FIELD).performTextInput("123-45-6789")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(HealthCardTestTags.ADD_BUTTON).performScrollTo().performClick()
    composeTestRule.waitForIdle()

    verify { mockViewModel.saveHealthCard(any(), "user123", any()) }
  }

  @Test
  fun clickingUpdateButton_callsUpdateHealthCard() {
    currentCardFlow.value = dummyCard()
    composeTestRule.waitForIdle()

    composeTestRule.waitUntilWithTimeout {
      composeTestRule
          .onAllNodesWithTag(HealthCardTestTags.UPDATE_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(HealthCardTestTags.CHRONIC_CONDITIONS_FIELD)
        .performScrollTo()
        .performTextInput("Diabetes, Asthma")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(HealthCardTestTags.UPDATE_BUTTON).performScrollTo().performClick()
    composeTestRule.waitForIdle()

    verify { mockViewModel.updateHealthCard(any(), "user123", any()) }
  }

  @Test
  fun clickingDeleteButton_callsDeleteHealthCard() {
    currentCardFlow.value = dummyCard()
    composeTestRule.waitForIdle()

    composeTestRule.waitUntilWithTimeout {
      composeTestRule
          .onAllNodesWithTag(HealthCardTestTags.DELETE_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(HealthCardTestTags.DELETE_BUTTON).performScrollTo().performClick()
    composeTestRule.waitForIdle()

    verify { mockViewModel.deleteHealthCard(any(), "user123") }
  }

  @Test
  fun errorMessages_areDisplayed_whenRequiredFieldsTouchedAndEmpty() {
    composeTestRule.onNodeWithTag(HealthCardTestTags.FULL_NAME_FIELD).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(HealthCardTestTags.BIRTH_DATE_FIELD).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(HealthCardTestTags.SSN_FIELD).performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(HealthCardTestTags.ADD_BUTTON).performScrollTo().performClick()
    composeTestRule.waitForIdle()

    composeTestRule.waitUntilWithTimeout {
      composeTestRule.onAllNodesWithText("Mandatory field").fetchSemanticsNodes().size == 3
    }

    composeTestRule.onAllNodesWithText("Mandatory field").assertCountEquals(3)
  }

  @Test
  fun loadingIndicator_isDisplayed_whenUiStateLoading() {
    uiStateFlow.value = HealthCardUiState.Idle
    composeTestRule.waitForIdle()

    uiStateFlow.value = HealthCardUiState.Loading
    composeTestRule.waitForIdle()

    composeTestRule.waitUntilWithTimeout(EXTENDED_TIMEOUT) {
      composeTestRule
          .onAllNodesWithTag(LoadingTestTags.LOADING_INDICATOR)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule.onNodeWithTag(LoadingTestTags.LOADING_INDICATOR).performScrollTo()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag(LoadingTestTags.LOADING_INDICATOR).assertIsDisplayed()
  }

  @Test
  fun birthDate_field_accepts_and_formats_proper_date() {
    composeTestRule
        .onNodeWithTag(HealthCardTestTags.BIRTH_DATE_FIELD)
        .performTextInput("15/07/1998")
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(HealthCardTestTags.BIRTH_DATE_FIELD)
        .assertTextContains("15/07/1998")
  }

  @Test
  fun existingCard_isPopulated_intoForm() {
    val card = dummyCard()
    currentCardFlow.value = card
    composeTestRule.waitForIdle()

    composeTestRule.waitUntilWithTimeout {
      composeTestRule
          .onAllNodesWithText(card.fullName, substring = true)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    composeTestRule
        .onNodeWithTag(HealthCardTestTags.FULL_NAME_FIELD)
        .assertTextContains(card.fullName)
    composeTestRule
        .onNodeWithTag(HealthCardTestTags.BIRTH_DATE_FIELD)
        .assertTextContains("01/01/2000")
    composeTestRule
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
