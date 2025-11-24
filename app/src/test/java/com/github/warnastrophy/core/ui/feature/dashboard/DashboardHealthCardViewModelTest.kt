package com.github.warnastrophy.core.ui.feature.dashboard

import android.content.Context
import com.github.warnastrophy.core.data.localStorage.HealthCardStorage
import com.github.warnastrophy.core.data.localStorage.StorageException
import com.github.warnastrophy.core.data.localStorage.StorageResult
import com.github.warnastrophy.core.model.HealthCard
import com.github.warnastrophy.core.ui.features.dashboard.DashboardHealthCardUiState
import com.github.warnastrophy.core.ui.features.dashboard.DashboardHealthCardViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import junit.framework.TestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardHealthCardViewModelTest {
  private lateinit var viewModel: DashboardHealthCardViewModel
  private lateinit var mockContext: Context
  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    mockContext = mockk(relaxed = true)
    mockkObject(HealthCardStorage)
    viewModel = DashboardHealthCardViewModel()
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  // ==== LOAD HEALTH CARD TESTS ====

  @Test
  fun `loadHealthCard emits Loading then Success with data`() = runTest {
    val healthCard = createHealthCard(bloodType = "A+", allergies = listOf("Peanuts"))
    coEvery { HealthCardStorage.loadHealthCard(mockContext, "user123") } returns
        StorageResult.Success(healthCard)

    TestCase.assertEquals(DashboardHealthCardUiState.Loading, viewModel.uiState.value)

    viewModel.loadHealthCard(mockContext, "user123")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value as DashboardHealthCardUiState.Success
    TestCase.assertEquals(healthCard, state.healthCard)
    coVerify { HealthCardStorage.loadHealthCard(mockContext, "user123") }
  }

  @Test
  fun `loadHealthCard emits Success with null when no card found`() = runTest {
    coEvery { HealthCardStorage.loadHealthCard(mockContext, "user123") } returns
        StorageResult.Success(null)

    viewModel.loadHealthCard(mockContext, "user123")
    testDispatcher.scheduler.advanceUntilIdle()

    val state = viewModel.uiState.value as DashboardHealthCardUiState.Success
    TestCase.assertEquals(null, state.healthCard)
  }

  @Test
  fun `loadHealthCard emits Error on storage failure`() = runTest {
    val errors =
        listOf(
            StorageException.DataStoreError(Exception("DB error")),
            StorageException.DecryptionError(Exception("Decrypt failed")),
            StorageException.DeserializationError(Exception("Invalid JSON")))

    errors.forEach { exception ->
      coEvery { HealthCardStorage.loadHealthCard(mockContext, "user") } returns
          StorageResult.Error(exception)

      viewModel.loadHealthCard(mockContext, "user")
      testDispatcher.scheduler.advanceUntilIdle()

      TestCase.assertTrue(viewModel.uiState.value is DashboardHealthCardUiState.Error)
    }
  }

  @Test
  fun `loadHealthCard handles multiple consecutive calls`() = runTest {
    val card1 = createHealthCard(fullName = "User 1", bloodType = "A+")
    val card2 = createHealthCard(fullName = "User 2", bloodType = "B-")

    coEvery { HealthCardStorage.loadHealthCard(mockContext, "user1") } returns
        StorageResult.Success(card1)
    coEvery { HealthCardStorage.loadHealthCard(mockContext, "user2") } returns
        StorageResult.Success(card2)

    viewModel.loadHealthCard(mockContext, "user1")
    testDispatcher.scheduler.advanceUntilIdle()
    viewModel.loadHealthCard(mockContext, "user2")
    testDispatcher.scheduler.advanceUntilIdle()

    coVerify(exactly = 1) { HealthCardStorage.loadHealthCard(mockContext, "user1") }
    coVerify(exactly = 1) { HealthCardStorage.loadHealthCard(mockContext, "user2") }
  }

  // ==== GET EMERGENCY HEALTH SUMMARY TESTS ====

  @Test
  fun `getEmergencyHealthSummary formats all information correctly`() {
    val card =
        createHealthCard(
            bloodType = "A+",
            allergies = listOf("Peanuts", "Penicillin"),
            medications = listOf("Aspirin", "Ibuprofen"),
            organDonor = true)

    val summary = viewModel.getEmergencyHealthSummary(card)

    TestCase.assertTrue(summary.contains("Blood: A+"))
    TestCase.assertTrue(summary.contains("Peanuts, Penicillin"))
    TestCase.assertTrue(summary.contains("2 meds"))
    TestCase.assertTrue(summary.contains("Organ donor"))
  }

  @Test
  fun `getEmergencyHealthSummary truncates lists with more than two items`() {
    val card =
        createHealthCard(
            bloodType = "O-", allergies = listOf("Peanuts", "Penicillin", "Shellfish", "Latex"))

    val summary = viewModel.getEmergencyHealthSummary(card)
    TestCase.assertTrue(summary.contains("Peanuts, Penicillin + 2 more"))
  }

  @Test
  fun `getEmergencyHealthSummary shows correct medication pluralization`() {
    val oneMed = createHealthCard(medications = listOf("Aspirin"))
    val twoMeds = createHealthCard(medications = listOf("Aspirin", "Ibuprofen"))

    TestCase.assertTrue(viewModel.getEmergencyHealthSummary(oneMed).contains("1 med"))
    TestCase.assertTrue(viewModel.getEmergencyHealthSummary(twoMeds).contains("2 meds"))
  }

  @Test
  fun `getEmergencyHealthSummary handles missing blood type`() {
    val withBloodType = createHealthCard(bloodType = "O+")
    val withoutBloodType = createHealthCard(bloodType = null)
    val emptyCard = createHealthCard(bloodType = null, allergies = emptyList())

    TestCase.assertTrue(viewModel.getEmergencyHealthSummary(withBloodType).contains("Blood: O+"))
    TestCase.assertTrue(viewModel.getEmergencyHealthSummary(withoutBloodType).contains("Unknown"))
    TestCase.assertTrue(
        viewModel.getEmergencyHealthSummary(emptyCard).contains("No critical info added"))
  }

  @Test
  fun `getEmergencyHealthSummary separates additional info with bullet points`() {
    val card = createHealthCard(medications = listOf("Med1", "Med2"), organDonor = true)

    val summary = viewModel.getEmergencyHealthSummary(card)
    TestCase.assertTrue(summary.contains("2 meds • Organ donor"))
  }

  // ==== HELPER ====

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
          dateOfBirthIso = birthDate,
          idNumber = socialSecurityNumber,
          bloodType = bloodType,
          allergies = allergies,
          medications = medications,
          chronicConditions = chronicConditions,
          organDonor = organDonor)
}
