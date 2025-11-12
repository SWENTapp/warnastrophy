package com.github.warnastrophy.core.ui.dashboard

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.warnastrophy.core.data.local.HealthCardStorage
import com.github.warnastrophy.core.data.local.StorageException
import com.github.warnastrophy.core.data.local.StorageResult
import com.github.warnastrophy.core.model.HealthCard
import com.github.warnastrophy.core.ui.components.LoadingTestTags
import com.github.warnastrophy.core.ui.features.dashboard.DashboardHealthCardViewModel
import com.github.warnastrophy.core.ui.theme.MainAppTheme
import com.github.warnastrophy.core.ui.util.BaseSimpleComposeTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Before
import org.junit.Test

class DashboardHealthCardTest : BaseSimpleComposeTest() {
  private lateinit var mockContext: Context
  private lateinit var viewModel: DashboardHealthCardViewModel

  @Before
  override fun setUp() {
    super.setUp()
    mockContext = mockk(relaxed = true)
    mockkObject(HealthCardStorage)
    viewModel = DashboardHealthCardViewModel()
  }

  @After
  override fun tearDown() {
    super.tearDown()
    unmockkAll()
  }

  // ==== HELPER METHODS ====

  private fun setStatelessContent(
      healthCard: HealthCard? = null,
      onHealthCardClick: () -> Unit = {},
      isLoading: Boolean = false,
      summaryText: String? = null
  ) {
    composeTestRule.setContent {
      MainAppTheme {
        DashboardHealthCardStateless(
            healthCard = healthCard,
            onHealthCardClick = onHealthCardClick,
            isLoading = isLoading,
            summaryText = summaryText)
      }
    }
  }

  private fun setStatefulContent(onHealthCardClick: () -> Unit = {}, userId: String = "user") {
    composeTestRule.setContent {
      MainAppTheme {
        DashboardHealthCardStateful(
            onHealthCardClick = onHealthCardClick,
            userId = userId,
            context = mockContext,
            viewModel = viewModel)
      }
    }
  }

  private fun createHealthCard(
      fullName: String = "Test User",
      birthDate: String = "1990-01-01",
      socialSecurityNumber: String = "123456789",
      bloodType: String? = "A+",
      allergies: List<String> = emptyList(),
      medications: List<String> = emptyList(),
      chronicConditions: List<String> = emptyList(),
      organDonor: Boolean = false
  ) =
      HealthCard(
          fullName = fullName,
          dateOfBirthIso =birthDate,
            idNumber = socialSecurityNumber,
          bloodType = bloodType,
          allergies = allergies,
          medications = medications,
          chronicConditions = chronicConditions,
          organDonor = organDonor)

  // ==== STATELESS TESTS ====

  @Test
  fun stateless_displaysLoading_whenIsLoadingIsTrue() {
    setStatelessContent(isLoading = true)

    composeTestRule.onNodeWithTag(DashboardHealthCardTestTags.CARD).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(DashboardHealthCardTestTags.TITLE, useUnmergedTree = true)
        .assertTextEquals("Health")
    composeTestRule
        .onNodeWithTag(LoadingTestTags.LOADING_INDICATOR, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun stateless_displaysEmptyState_whenNoHealthCard() {
    setStatelessContent()

    composeTestRule
        .onNodeWithTag(DashboardHealthCardTestTags.SUBTITLE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun stateless_displaysHealthData_withAllInformation() {
    val healthCard =
        createHealthCard(
            fullName = "John Doe",
            birthDate = "1990-05-15",
            bloodType = "A+",
            allergies = listOf("Peanuts", "Penicillin"),
            medications = listOf("Aspirin", "Ibuprofen"),
            chronicConditions = listOf("Asthma", "Diabetes"),
            organDonor = true)
    val summaryText = viewModel.getEmergencyHealthSummary(healthCard)

    setStatelessContent(healthCard = healthCard, summaryText = summaryText)

    composeTestRule.onNodeWithTag(DashboardHealthCardTestTags.CARD).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(DashboardHealthCardTestTags.SUBTITLE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun stateless_displaysAllergies_withMoreThanTwo() {
    val healthCard =
        createHealthCard(
            bloodType = "O-", allergies = listOf("Peanuts", "Penicillin", "Shellfish", "Latex"))
    val summaryText = viewModel.getEmergencyHealthSummary(healthCard)

    setStatelessContent(healthCard = healthCard, summaryText = summaryText)

    composeTestRule
        .onNodeWithTag(DashboardHealthCardTestTags.SUBTITLE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun stateless_displaysChronicConditions_withMoreThanTwo() {
    val healthCard =
        createHealthCard(
            bloodType = "AB+",
            chronicConditions = listOf("Diabetes", "Asthma", "Hypertension", "Arthritis"))
    val summaryText = viewModel.getEmergencyHealthSummary(healthCard)

    setStatelessContent(healthCard = healthCard, summaryText = summaryText)

    composeTestRule
        .onNodeWithTag(DashboardHealthCardTestTags.SUBTITLE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun stateless_displaysMedications_singular() {
    val healthCard = createHealthCard(bloodType = "B-", medications = listOf("Aspirin"))
    val summaryText = viewModel.getEmergencyHealthSummary(healthCard)

    setStatelessContent(healthCard = healthCard, summaryText = summaryText)

    composeTestRule
        .onNodeWithTag(DashboardHealthCardTestTags.SUBTITLE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun stateless_displaysNoCriticalInfo_whenOnlyBloodTypeMissing() {
    val healthCard = createHealthCard(bloodType = null)
    val summaryText = viewModel.getEmergencyHealthSummary(healthCard)

    setStatelessContent(healthCard = healthCard, summaryText = summaryText)

    composeTestRule
        .onNodeWithTag(DashboardHealthCardTestTags.SUBTITLE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun stateless_triggersCallback_whenCardClicked() {
    var clicked = false

    setStatelessContent(onHealthCardClick = { clicked = true })

    composeTestRule.onNodeWithTag(DashboardHealthCardTestTags.CARD).performClick()
    assert(clicked) { "onHealthCardClick callback was not triggered" }
  }

  // ===== STATEFUL TESTS =====

  @Test
  fun stateful_showsLoading_initially() {
    coEvery { HealthCardStorage.loadHealthCard(any(), any()) } coAnswers
        {
          kotlinx.coroutines.delay(1000)
          StorageResult.Success(null)
        }

    setStatefulContent()

    composeTestRule
        .onNodeWithTag(LoadingTestTags.LOADING_INDICATOR, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun stateful_displaysHealthCard_whenLoadSucceeds() {
    val healthCard =
        createHealthCard(
            fullName = "John Doe",
            allergies = listOf("Peanuts"),
            medications = listOf("Aspirin"),
            organDonor = true)
    coEvery { HealthCardStorage.loadHealthCard(mockContext, "user") } returns
        StorageResult.Success(healthCard)

    setStatefulContent()

    composeTestRule.waitForIdleWithTimeout()
    composeTestRule.onNodeWithTag(DashboardHealthCardTestTags.CARD).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(DashboardHealthCardTestTags.SUBTITLE, useUnmergedTree = true)
        .assertIsDisplayed()
    coVerify { HealthCardStorage.loadHealthCard(mockContext, "user") }
  }

  @Test
  fun stateful_displaysEmptyState_whenNoHealthCardFound() {
    coEvery { HealthCardStorage.loadHealthCard(mockContext, "user") } returns
        StorageResult.Success(null)

    setStatefulContent()

    composeTestRule.waitForIdleWithTimeout()
    composeTestRule
        .onNodeWithTag(DashboardHealthCardTestTags.SUBTITLE, useUnmergedTree = true)
        .assertIsDisplayed()
    coVerify { HealthCardStorage.loadHealthCard(mockContext, "user") }
  }

  @Test
  fun stateful_displaysEmptyState_onDataStoreError() {
    coEvery { HealthCardStorage.loadHealthCard(mockContext, "user") } returns
        StorageResult.Error(StorageException.DataStoreError(Exception("DB error")))

    setStatefulContent()

    composeTestRule.waitForIdleWithTimeout()
    composeTestRule
        .onNodeWithTag(DashboardHealthCardTestTags.SUBTITLE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun stateful_displaysEmptyState_onDecryptionError() {
    coEvery { HealthCardStorage.loadHealthCard(mockContext, "user") } returns
        StorageResult.Error(StorageException.DecryptionError(Exception("Decryption failed")))

    setStatefulContent()

    composeTestRule.waitForIdleWithTimeout()
    composeTestRule
        .onNodeWithTag(DashboardHealthCardTestTags.SUBTITLE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun stateful_triggersCallback_whenCardClicked() {
    var clicked = false
    val healthCard = createHealthCard(bloodType = "B+")
    coEvery { HealthCardStorage.loadHealthCard(mockContext, "user") } returns
        StorageResult.Success(healthCard)

    setStatefulContent(onHealthCardClick = { clicked = true })

    composeTestRule.waitForIdleWithTimeout()
    composeTestRule.onNodeWithTag(DashboardHealthCardTestTags.CARD).performClick()
    assert(clicked) { "Callback not triggered" }
  }

  @Test
  fun stateful_usesDefaultUserId_whenNotProvided() {
    coEvery { HealthCardStorage.loadHealthCard(any(), "John Doe") } returns
        StorageResult.Success(null)

    composeTestRule.setContent {
      MainAppTheme {
        DashboardHealthCardStateful(
            onHealthCardClick = {}, context = mockContext, viewModel = viewModel)
      }
    }

    composeTestRule.waitForIdleWithTimeout()
    coVerify { HealthCardStorage.loadHealthCard(any(), "John Doe") }
  }
}
