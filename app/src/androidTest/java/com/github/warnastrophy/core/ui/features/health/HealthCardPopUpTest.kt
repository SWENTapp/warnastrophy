package com.github.warnastrophy.core.ui.features.health

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.warnastrophy.core.model.HealthCard
import com.github.warnastrophy.core.ui.theme.MainAppTheme
import com.github.warnastrophy.core.util.BaseAndroidComposeTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    currentCardFlow.value = dummyCard()
    composeTestRule.setContent {
      MainAppTheme {
        HealthCardPopUp(
            onDismissRequest = {}, onClick = {}, viewModel = mockViewModel, userId = fakeUserId)
      }
    }

    checkHealthCardTitles()
    checkHealthCardFields(dummyCard())
    composeTestRule.onNodeWithTag(HealthCardPopUpTestTags.EMPTY_STATE_TEXT).assertDoesNotExist()
  }

  @Test
  fun healthCardPopUp_displaysCardWithEmptyFields() {
    val cardWithNulls =
        dummyCard(
            chronicConditions = emptyList(),
            allergies = emptyList(),
            medications = emptyList(),
            onGoingTreatments = emptyList(),
            medicalHistory = emptyList(),
        )
    currentCardFlow.value = cardWithNulls

    composeTestRule.setContent {
      MainAppTheme {
        HealthCardPopUp(
            onDismissRequest = {}, onClick = {}, viewModel = mockViewModel, userId = fakeUserId)
      }
    }

    checkHealthCardTitles()
    checkHealthCardFields(cardWithNulls)
    composeTestRule.onNodeWithTag(HealthCardPopUpTestTags.EMPTY_STATE_TEXT).assertDoesNotExist()
  }

  @Test
  fun healthCardPopUp_displaysCardWithNullFields() {
    val cardWithNulls = dummyCard(sex = null, bloodType = null, organDonor = null, notes = null)
    currentCardFlow.value = cardWithNulls

    composeTestRule.setContent {
      MainAppTheme {
        HealthCardPopUp(
            onDismissRequest = {}, onClick = {}, viewModel = mockViewModel, userId = fakeUserId)
      }
    }

    checkHealthCardTitles()
    checkHealthCardFields(cardWithNulls)
    composeTestRule.onNodeWithTag(HealthCardPopUpTestTags.EMPTY_STATE_TEXT).assertDoesNotExist()
  }

  @Test
  fun healthCardPopUp_displaysCardWithEmptyStringsFields() {
    val cardWithNulls = dummyCard(sex = "", bloodType = "", notes = "")
    currentCardFlow.value = cardWithNulls

    composeTestRule.setContent {
      MainAppTheme {
        HealthCardPopUp(
            onDismissRequest = {}, onClick = {}, viewModel = mockViewModel, userId = fakeUserId)
      }
    }

    checkHealthCardTitles()
    checkHealthCardFields(cardWithNulls)
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

    composeTestRule.setContent {
      MainAppTheme {
        // LaunchedEffect will trigger loadHealthCard
        HealthCardPopUp(
            onClick = {}, onDismissRequest = {}, viewModel = mockViewModel, userId = fakeUserId)
      }
    }

    composeTestRule.waitForIdle()

    verify(exactly = 1) { mockViewModel.loadHealthCard(any(), fakeUserId) }
  }

  private fun dummyCard(
      fullName: String = "John Doe",
      dateOfBirthIso: String = "2000-01-01",
      idNumber: String = "123-45-6789",
      sex: String? = "Male",
      bloodType: String? = "A+",
      heightCm: Int? = 180,
      weightKg: Double? = 75.0,
      chronicConditions: List<String> = listOf("Diabetes"),
      allergies: List<String> = listOf("Pollen"),
      medications: List<String> = listOf("Metformin"),
      onGoingTreatments: List<String> = listOf(),
      medicalHistory: List<String> = listOf(),
      organDonor: Boolean? = true,
      notes: String? = "N/A"
  ) =
      HealthCard(
          fullName = fullName,
          dateOfBirthIso = dateOfBirthIso,
          idNumber = idNumber,
          sex = sex,
          bloodType = bloodType,
          heightCm = heightCm,
          weightKg = weightKg,
          chronicConditions = chronicConditions,
          allergies = allergies,
          medications = medications,
          onGoingTreatments = onGoingTreatments,
          medicalHistory = medicalHistory,
          organDonor = organDonor,
          notes = notes)

  private fun checkHealthCardFields(card: HealthCard) {
    val uiDf = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)
    val date = card.dateOfBirthIso.let { LocalDate.parse(it).format(uiDf) } ?: "-"

    composeTestRule
        .onNodeWithTag(HealthCardPopUpTestTags.FULL_NAME_VALUE)
        .performScrollTo()
        .assertIsDisplayed()
        .assertTextContains(card.fullName)

    composeTestRule
        .onNodeWithTag(HealthCardPopUpTestTags.BIRTH_DATE_VALUE)
        .performScrollTo()
        .assertIsDisplayed()
        .assertTextContains(date)

    composeTestRule
        .onNodeWithTag(HealthCardPopUpTestTags.SEX_VALUE)
        .performScrollTo()
        .assertIsDisplayed()
        .assertTextContains(card.sex.takeIf { !it.isNullOrBlank() } ?: "-")

    composeTestRule
        .onNodeWithTag(HealthCardPopUpTestTags.BLOOD_TYPE_VALUE)
        .performScrollTo()
        .assertIsDisplayed()
        .assertTextContains(card.bloodType.takeIf { !it.isNullOrBlank() } ?: "-")

    composeTestRule
        .onNodeWithTag(HealthCardPopUpTestTags.ALLERGIES_VALUE)
        .performScrollTo()
        .assertIsDisplayed()
        .assertTextContains(card.allergies.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "-")
    composeTestRule
        .onNodeWithTag(HealthCardPopUpTestTags.MEDICATIONS_VALUE)
        .performScrollTo()
        .assertIsDisplayed()
        .assertTextContains(card.medications.takeIf { it.isNotEmpty() }?.joinToString(", ") ?: "-")

    composeTestRule
        .onNodeWithTag(HealthCardPopUpTestTags.ORGAN_DONOR_VALUE)
        .performScrollTo()
        .assertIsDisplayed()
        .assertTextContains(if (card.organDonor == true) "Yes" else "No")

    composeTestRule
        .onNodeWithTag(HealthCardPopUpTestTags.NOTES_VALUE)
        .performScrollTo()
        .assertIsDisplayed()
        .assertTextContains(card.notes.takeIf { !it.isNullOrBlank() } ?: "-")
  }

  private fun checkHealthCardTitles() {
    composeTestRule
        .onNodeWithTag(HealthCardPopUpTestTags.FULL_NAME_TITLE)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(HealthCardPopUpTestTags.BIRTH_DATE_TITLE)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(HealthCardPopUpTestTags.SEX_TITLE)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(HealthCardPopUpTestTags.BLOOD_TYPE_TITLE)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(HealthCardPopUpTestTags.ALLERGIES_TITLE)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(HealthCardPopUpTestTags.MEDICATIONS_TITLE)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(HealthCardPopUpTestTags.ORGAN_DONOR_TITLE)
        .performScrollTo()
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(HealthCardPopUpTestTags.NOTES_TITLE)
        .performScrollTo()
        .assertIsDisplayed()
  }
}
