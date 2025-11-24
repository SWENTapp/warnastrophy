package com.github.warnastrophy.core.data.localStorage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.warnastrophy.core.model.HealthCard
import com.github.warnastrophy.core.util.CryptoUtils
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockkObject
import io.mockk.unmockkObject
import junit.framework.TestCase
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class LocalHealthCardRepositoryTest {

  private lateinit var context: Context
  private lateinit var repo: LocalHealthCardRepository

  private fun sampleCard(): HealthCard =
      HealthCard(
          fullName = "Local User",
          dateOfBirthIso = "1995-05-05",
          idNumber = "LOCAL-001",
          sex = "Female",
          bloodType = "A+",
          heightCm = 165,
          weightKg = 60.0,
          chronicConditions = listOf("condition1"),
          allergies = listOf("allergy1"),
          medications = emptyList(),
          onGoingTreatments = emptyList(),
          medicalHistory = emptyList(),
          organDonor = false,
          notes = "note")

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    repo = LocalHealthCardRepository(context)
    mockkObject(HealthCardStorage)
  }

  @After
  fun tearDown() {
    unmockkObject(HealthCardStorage)
    unmockkObject(CryptoUtils)
  }

  @Test
  fun upsertMyHealthCard_delegates_to_HealthCardStorage_and_succeeds() = runTest {
    val card = sampleCard()
    coEvery { HealthCardStorage.saveHealthCard(context, "local", card) } returns
        StorageResult.Success(Unit)

    repo.upsertMyHealthCard(card)

    coVerify(exactly = 1) { HealthCardStorage.saveHealthCard(context, "local", card) }
  }

  @Test
  fun upsertMyHealthCard_throws_when_HealthCardStorage_returns_error() = runTest {
    val card = sampleCard()
    val ex = StorageException.DataStoreError(Exception("Save failed"))
    coEvery { HealthCardStorage.saveHealthCard(context, "local", card) } returns
        StorageResult.Error(ex)

    try {
      repo.upsertMyHealthCard(card)
      TestCase.fail("Expected StorageException to be thrown")
    } catch (e: StorageException) {
      TestCase.assertEquals(ex, e)
    }
  }

  @Test
  fun getMyHealthCardOnce_returns_card_on_success() = runTest {
    val card = sampleCard()
    coEvery { HealthCardStorage.loadHealthCard(context, "local") } returns
        StorageResult.Success(card)

    val result = repo.getMyHealthCardOnce(fromCacheFirst = true)

    assertEquals(card, result)
  }

  @Test
  fun getMyHealthCardOnce_returns_null_when_storage_success_with_null_data() = runTest {
    coEvery { HealthCardStorage.loadHealthCard(context, "local") } returns
        StorageResult.Success(null)

    val result = repo.getMyHealthCardOnce(fromCacheFirst = true)

    TestCase.assertNull(result)
  }
}
