package com.github.warnastrophy.core.data.local

import com.github.warnastrophy.core.data.repository.HealthCardRepository
import com.github.warnastrophy.core.data.repository.HybridHealthCardRepository
import com.github.warnastrophy.core.model.HealthCard
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HybridHealthCardRepositoryTest {

  private fun sampleCard(name: String = "John Doe"): HealthCard =
      HealthCard(
          fullName = name,
          dateOfBirthIso = "1990-01-01",
          idNumber = "123-45-6789",
          sex = "Male",
          bloodType = "O+",
          heightCm = 180,
          weightKg = 75.0,
          chronicConditions = emptyList(),
          allergies = emptyList(),
          medications = emptyList(),
          onGoingTreatments = emptyList(),
          medicalHistory = emptyList(),
          organDonor = true,
          notes = null)

  @Test
  fun upsertMyHealthCard_writes_to_both_local_and_remote() = runTest {
    val local = mockk<HealthCardRepository>(relaxed = true)
    val remote = mockk<HealthCardRepository>(relaxed = true)

    val repo = HybridHealthCardRepository(local, remote)
    val card = sampleCard()

    repo.upsertMyHealthCard(card)

    coVerify(exactly = 1) { local.upsertMyHealthCard(card) }
    coVerify(exactly = 1) { remote.upsertMyHealthCard(card) }
  }

  @Test
  fun deleteMyHealthCard_deletes_both_local_and_remote() = runTest {
    val local = mockk<HealthCardRepository>(relaxed = true)
    val remote = mockk<HealthCardRepository>(relaxed = true)

    val repo = HybridHealthCardRepository(local, remote)

    repo.deleteMyHealthCard()

    coVerify(exactly = 1) { local.deleteMyHealthCard() }
    coVerify(exactly = 1) { remote.deleteMyHealthCard() }
  }
}
