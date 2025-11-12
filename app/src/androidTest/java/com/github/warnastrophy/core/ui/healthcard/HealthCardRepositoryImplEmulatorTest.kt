package com.github.warnastrophy

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.warnastrophy.core.data.repository.HealthCardRepositoryProvider
import com.github.warnastrophy.core.model.HealthCard
import com.github.warnastrophy.core.ui.firebase.FirebaseTest
import com.google.firebase.auth.FirebaseAuth
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HealthCardRepositoryImplEmulatorTest {

  private lateinit var auth: FirebaseAuth

  @Before
  fun setUp() = runTest {
    FirebaseTest.initToEmulators()
    auth = FirebaseAuth.getInstance()
    // Create/sign-in test user on emulator
    // If the user exists, signIn will work; otherwise create + signIn
    val email = "alice@test.dev"
    val pass = "password123"

    runCatching { auth.createUserWithEmailAndPassword(email, pass).await() }
    auth.signInWithEmailAndPassword(email, pass).await()
  }

  @After fun tearDown() = runTest { auth.signOut() }

  @Test
  fun crudHealthCard_forCurrentUser() = runTest {
    val repo = HealthCardRepositoryProvider.repository

    // CREATE
    val card =
        HealthCard(
            fullName = "Alice A.",
            dateOfBirthIso = "2000-01-01",
            idNumber = "AA-001",
            sex = "F",
            bloodType = "O+",
            heightCm = 170,
            weightKg = 60.5,
            chronicConditions = listOf("asthma"),
            allergies = listOf("peanuts"),
            medications = listOf("med-A"),
            onGoingTreatments = listOf("therapy"),
            medicalHistory = listOf("surgery-2016"),
            organDonor = true,
            notes = "test note")
    repo.upsertMyHealthCard(card)

    // READ (cache-first one-shot)
    val read1 = repo.getMyHealthCardOnce()
    assertNotNull(read1)
    assertEquals("Alice A.", read1!!.fullName)

    // OBSERVE stream reflects existing
    val live = repo.observeMyHealthCard().firstOrNullWithTimeout()
    assertNotNull(live)

    // UPDATE
    val updated = card.copy(fullName = "Alice Updated", bloodType = "A-")
    repo.upsertMyHealthCard(updated)
    val read2 = repo.getMyHealthCardOnce(fromCacheFirst = false)
    assertEquals("Alice Updated", read2!!.fullName)
    assertEquals("A-", read2.bloodType)

    // DELETE
    repo.deleteMyHealthCard()
    val read3 = repo.getMyHealthCardOnce(fromCacheFirst = false)
    assertNull(read3)
  }
}

/** Small helper to collect exactly one emission with a timeout. */
suspend fun <T> kotlinx.coroutines.flow.Flow<T>.firstOrNullWithTimeout(ms: Long = 3_000): T? =
    withTimeoutOrNull(ms) { first() }
