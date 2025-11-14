package com.github.warnastrophy.core.ui.dashboard

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.github.warnastrophy.core.data.local.HealthCardStorage
import com.github.warnastrophy.core.data.local.StorageException
import com.github.warnastrophy.core.data.local.StorageResult
import com.github.warnastrophy.core.data.repository.HealthCardRepository
import com.github.warnastrophy.core.domain.model.HealthCard
import com.github.warnastrophy.core.ui.features.health.HealthCardUiState
import com.github.warnastrophy.core.ui.features.health.HealthCardViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import junit.framework.TestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class HealthCardViewModelTest {
  @get:Rule val instantExecutorRule = InstantTaskExecutorRule()

  private val testDispatcher = StandardTestDispatcher()

  private lateinit var viewModel: HealthCardViewModel
  private lateinit var mockContext: Context
  private lateinit var mockRepo: HealthCardRepository

  private val testUserId = "test_user_123"
  private val testHealthCard =
      HealthCard(
          fullName = "John Doe",
          dateOfBirthIso = "1990-01-01",
          idNumber = "123-45-6789",
          sex = "Male",
          bloodType = "O+",
          heightCm = 180,
          weightKg = 75.0,
          chronicConditions = listOf("Asthma"),
          allergies = listOf("Peanuts"),
          medications = listOf("Albuterol"),
          onGoingTreatments = emptyList(),
          medicalHistory = listOf("Broken arm (2015)"),
          organDonor = true,
          notes = "Regular checkups needed")

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    mockRepo = mockk(relaxed = true)
    viewModel = HealthCardViewModel(repo = mockRepo, dispatcher = testDispatcher)
    mockContext = mockk(relaxed = true)
    mockkObject(HealthCardStorage)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkObject(HealthCardStorage)
  }

  @Test
  fun `initial state is Idle and currentCard is null`() {
    TestCase.assertEquals(HealthCardUiState.Idle, viewModel.uiState.value)
    TestCase.assertNull(viewModel.currentCard.value)
  }

  @Test
  fun `loadHealthCard success updates state and currentCard`() = runTest {
    // Given
    coEvery { HealthCardStorage.loadHealthCard(mockContext, testUserId) } returns
        StorageResult.Success(testHealthCard)

    // When
    viewModel.loadHealthCard(mockContext, testUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    TestCase.assertTrue(viewModel.uiState.value is HealthCardUiState.Success)
    TestCase.assertEquals(
        "Health card successfully loaded",
        (viewModel.uiState.value as HealthCardUiState.Success).message)
    TestCase.assertEquals(testHealthCard, viewModel.currentCard.value)

    coVerify { HealthCardStorage.loadHealthCard(mockContext, testUserId) }
  }

  @Test
  fun `loadHealthCard with null data returns success with null card`() = runTest {
    // Given
    coEvery { HealthCardStorage.loadHealthCard(mockContext, testUserId) } returns
        StorageResult.Success(null)

    // When
    viewModel.loadHealthCard(mockContext, testUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    TestCase.assertTrue(viewModel.uiState.value is HealthCardUiState.Success)
    TestCase.assertNull(viewModel.currentCard.value)
  }

  @Test
  fun `loadHealthCard failure updates state to Error`() = runTest {
    // Given
    val errorMessage = "Decryption failed"
    val exception = StorageException.DecryptionError(Exception(errorMessage))
    coEvery { HealthCardStorage.loadHealthCard(mockContext, testUserId) } returns
        StorageResult.Error(exception)

    // When
    viewModel.loadHealthCard(mockContext, testUserId)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    TestCase.assertTrue(viewModel.uiState.value is HealthCardUiState.Error)
    TestCase.assertTrue(
        (viewModel.uiState.value as HealthCardUiState.Error)
            .message
            .contains("Decryption", ignoreCase = true))
    TestCase.assertNull(viewModel.currentCard.value)
  }

  @Test
  fun `loadHealthCard sets Loading state during operation`() = runTest {
    // Given
    coEvery { HealthCardStorage.loadHealthCard(mockContext, testUserId) } coAnswers
        {
          delay(100)
          StorageResult.Success(testHealthCard)
        }

    // When
    viewModel.loadHealthCard(mockContext, testUserId)
    testScheduler.runCurrent()

    // Then - check Loading state
    TestCase.assertEquals(HealthCardUiState.Loading, viewModel.uiState.value)

    // Advance to completion
    testScheduler.advanceUntilIdle()

    // Then - after completion
    TestCase.assertTrue(viewModel.uiState.value is HealthCardUiState.Success)
  }

  @Test
  fun `saveHealthCard success updates state and currentCard`() = runTest {
    // Given
    coEvery { HealthCardStorage.saveHealthCard(mockContext, testUserId, testHealthCard) } returns
        StorageResult.Success(Unit)

    // When
    viewModel.saveHealthCard(mockContext, testUserId, testHealthCard)
    testScheduler.advanceUntilIdle()

    // Then
    TestCase.assertTrue(viewModel.uiState.value is HealthCardUiState.Success)
    TestCase.assertEquals(
        "Health card saved successfully",
        (viewModel.uiState.value as HealthCardUiState.Success).message)
    TestCase.assertEquals(testHealthCard, viewModel.currentCard.value)
    coVerify { HealthCardStorage.saveHealthCard(mockContext, testUserId, testHealthCard) }
  }

  @Test
  fun `saveHealthCard failure updates state to Error`() = runTest {
    // Given
    val exception = StorageException.DataStoreError(Exception("Save failed"))
    coEvery { HealthCardStorage.saveHealthCard(mockContext, testUserId, testHealthCard) } returns
        StorageResult.Error(exception)

    // When
    viewModel.saveHealthCard(mockContext, testUserId, testHealthCard)
    testScheduler.advanceUntilIdle()

    // Then
    TestCase.assertTrue(viewModel.uiState.value is HealthCardUiState.Error)
    TestCase.assertNull(viewModel.currentCard.value)
  }

  @Test
  fun `saveHealthCard sets Loading state during operation`() = runTest {
    // Given
    coEvery { HealthCardStorage.saveHealthCard(mockContext, testUserId, testHealthCard) } coAnswers
        {
          delay(50)
          StorageResult.Success(Unit)
        }

    // When
    viewModel.saveHealthCard(mockContext, testUserId, testHealthCard)
    testScheduler.runCurrent()

    // Then - Check Loading state
    TestCase.assertEquals(HealthCardUiState.Loading, viewModel.uiState.value)

    testScheduler.advanceUntilIdle()

    // Then - after completion
    TestCase.assertTrue(viewModel.uiState.value is HealthCardUiState.Success)
  }

  @Test
  fun `updateHealthCard success updates state and currentCard`() = runTest {
    // Given
    val updatedCard = testHealthCard.copy(weightKg = 80.0)
    coEvery { HealthCardStorage.updateHealthCard(mockContext, testUserId, updatedCard) } returns
        StorageResult.Success(Unit)

    // When
    viewModel.updateHealthCard(mockContext, testUserId, updatedCard)
    testScheduler.advanceUntilIdle()

    // Then
    TestCase.assertTrue(viewModel.uiState.value is HealthCardUiState.Success)
    TestCase.assertEquals(
        "Health card updated successfully",
        (viewModel.uiState.value as HealthCardUiState.Success).message)
    TestCase.assertEquals(updatedCard, viewModel.currentCard.value)
    TestCase.assertEquals(80.0, viewModel.currentCard.value?.weightKg)
  }

  @Test
  fun `updateHealthCard failure updates state to Error`() = runTest {
    // Given
    val updatedCard = testHealthCard.copy(weightKg = 80.0)
    val exception = StorageException.DataStoreError(Exception("Update failed"))
    coEvery { HealthCardStorage.updateHealthCard(mockContext, testUserId, updatedCard) } returns
        StorageResult.Error(exception)

    // When
    viewModel.updateHealthCard(mockContext, testUserId, updatedCard)
    testScheduler.advanceUntilIdle()

    // Then
    TestCase.assertTrue(viewModel.uiState.value is HealthCardUiState.Error)
  }

  @Test
  fun `deleteHealthCard success clears currentCard and updates state`() = runTest {
    // Given - first save a card
    coEvery { HealthCardStorage.saveHealthCard(mockContext, testUserId, testHealthCard) } returns
        StorageResult.Success(Unit)

    viewModel.saveHealthCard(mockContext, testUserId, testHealthCard)
    testScheduler.advanceUntilIdle()

    // Setup delete
    coEvery { HealthCardStorage.deleteHealthCard(mockContext, testUserId) } returns
        StorageResult.Success(Unit)

    // When
    viewModel.deleteHealthCard(mockContext, testUserId)
    testScheduler.advanceUntilIdle()

    // Then
    TestCase.assertTrue(viewModel.uiState.value is HealthCardUiState.Success)
    TestCase.assertEquals(
        "Health card deleted successfully",
        (viewModel.uiState.value as HealthCardUiState.Success).message)
    TestCase.assertNull(viewModel.currentCard.value)

    coVerify { HealthCardStorage.deleteHealthCard(mockContext, testUserId) }
  }

  @Test
  fun `deleteHealthCard failure updates state to Error`() = runTest {
    // Given
    val exception = StorageException.DataStoreError(Exception("Delete failed"))
    coEvery { HealthCardStorage.deleteHealthCard(mockContext, testUserId) } returns
        StorageResult.Error(exception)

    // When
    viewModel.deleteHealthCard(mockContext, testUserId)
    testScheduler.advanceUntilIdle()

    // Then
    TestCase.assertTrue(viewModel.uiState.value is HealthCardUiState.Error)
  }

  @Test
  fun `resetUiState sets state back to Idle`() = runTest {
    // Given - set state to Success
    coEvery { HealthCardStorage.saveHealthCard(mockContext, testUserId, testHealthCard) } returns
        StorageResult.Success(Unit)

    viewModel.saveHealthCard(mockContext, testUserId, testHealthCard)
    testScheduler.advanceUntilIdle()
    TestCase.assertTrue(viewModel.uiState.value is HealthCardUiState.Success)

    // When
    viewModel.resetUiState()

    // Then
    TestCase.assertEquals(HealthCardUiState.Idle, viewModel.uiState.value)
  }

  @Test
  fun `resetUiState from Error state sets state to Idle`() = runTest {
    // Given - set state to Error
    val exception = StorageException.DataStoreError(Exception("Error"))
    coEvery { HealthCardStorage.loadHealthCard(mockContext, testUserId) } returns
        StorageResult.Error(exception)

    viewModel.loadHealthCard(mockContext, testUserId)
    testScheduler.advanceUntilIdle()
    TestCase.assertTrue(viewModel.uiState.value is HealthCardUiState.Error)

    // When
    viewModel.resetUiState()

    // Then
    TestCase.assertEquals(HealthCardUiState.Idle, viewModel.uiState.value)
  }

  @Test
  fun `multiple operations in sequence maintain correct state`() = runTest {
    // Save
    coEvery { HealthCardStorage.saveHealthCard(mockContext, testUserId, testHealthCard) } returns
        StorageResult.Success(Unit)

    viewModel.saveHealthCard(mockContext, testUserId, testHealthCard)
    testScheduler.advanceUntilIdle()
    TestCase.assertEquals(testHealthCard, viewModel.currentCard.value)

    // Update
    val updatedCard = testHealthCard.copy(weightKg = 80.0)
    coEvery { HealthCardStorage.updateHealthCard(mockContext, testUserId, updatedCard) } returns
        StorageResult.Success(Unit)

    viewModel.updateHealthCard(mockContext, testUserId, updatedCard)
    testScheduler.advanceUntilIdle()
    TestCase.assertEquals(80.0, viewModel.currentCard.value?.weightKg)

    // Delete
    coEvery { HealthCardStorage.deleteHealthCard(mockContext, testUserId) } returns
        StorageResult.Success(Unit)

    viewModel.deleteHealthCard(mockContext, testUserId)
    testScheduler.advanceUntilIdle()
    TestCase.assertNull(viewModel.currentCard.value)
  }
}
