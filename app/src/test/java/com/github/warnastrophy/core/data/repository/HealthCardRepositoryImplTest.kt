package com.github.warnastrophy.core.data.repository

import com.github.warnastrophy.core.model.HealthCard
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Test suite for [HealthCardRepositoryImpl].
 *
 * This class contains unit tests that verify the correct behavior of the [HealthCardRepositoryImpl]
 * class. It uses MockK to mock Firebase dependencies like [FirebaseAuth] and [FirebaseFirestore] to
 * isolate the repository logic and ensure that interactions with Firebase services are as expected.
 *
 * The tests cover the main functionalities of the repository:
 * - Fetching a user's health card ([getMyHealthCardOnce]).
 * - Creating or updating a user's health card ([upsertMyHealthCard]).
 * - Deleting a user's health card ([deleteMyHealthCard]).
 *
 * The setup for each test includes mocking the Firebase authentication and Firestore database
 * instances to provide a controlled environment.
 *
 * _Note: This test suite was mainly generated with the help of AI_
 *
 * @see HealthCardRepositoryImpl
 * @see ExperimentalCoroutinesApi
 */
@ExperimentalCoroutinesApi
class HealthCardRepositoryImplTest {
  private val mockFirebaseAuth: FirebaseAuth = mockk(relaxed = true)
  private val mockFirestore: FirebaseFirestore = mockk(relaxed = true)
  private val mockCollection: CollectionReference = mockk(relaxed = true)
  private val mockDoc: DocumentReference = mockk(relaxed = true)

  private lateinit var repository: HealthCardRepositoryImpl

  @Before
  fun setUp() {
    // --- Mock Firebase Auth ---
    val mockFirebaseUser: FirebaseUser = mockk(relaxed = true)
    every { mockFirebaseUser.uid } returns "test-user-id"
    every { mockFirebaseAuth.currentUser } returns mockFirebaseUser

    // --- Mock Firestore ---
    every { mockFirestore.collection(any()) } returns mockCollection
    every { mockCollection.document(any()) } returns mockDoc

    // --- Initialize Repository ---
    repository = HealthCardRepositoryImpl(auth = mockFirebaseAuth, db = mockFirestore)
  }

  /**
   * Verifies that `getMyHealthCardOnce` attempts to retrieve data from the cache first when the
   * `fromCacheFirst` parameter is true.
   */
  @Test
  fun `getMyHealthCardOnce returns health card from cache`() = runTest {
    // Arrange
    val healthCard = HealthCard()
    val mockSnap = mockk<DocumentSnapshot>(relaxed = true)

    every { mockSnap.exists() } returns true
    every { mockSnap.toObject(HealthCard::class.java) } returns healthCard
    coEvery { mockDoc.get(Source.CACHE) } returns Tasks.forResult(mockSnap)

    // Act
    val result = repository.getMyHealthCardOnce(fromCacheFirst = true)

    // Assert
    assertEquals(healthCard, result)
    coVerify(exactly = 1) { mockDoc.get(Source.CACHE) }
    coVerify(exactly = 0) { mockDoc.get(Source.SERVER) } // Ensure it didn't go to the server
  }

  /**
   * Verifies that `upsertMyHealthCard` correctly calls the Firestore `set` method with the merge
   * option, ensuring a non-destructive update.
   */
  @Test
  fun `upsertMyHealthCard calls set with merge`() = runTest {
    // Arrange
    val card = HealthCard()
    coEvery { mockDoc.set(card, SetOptions.merge()) } returns Tasks.forResult(null)

    // Act
    repository.upsertMyHealthCard(card)

    // Assert
    coVerify(exactly = 1) { mockDoc.set(card, SetOptions.merge()) }
  }

  /** Verifies that `deleteMyHealthCard` correctly calls the Firestore `delete` method. */
  @Test
  fun `deleteMyHealthCard calls delete`() = runTest {
    // Arrange
    // Mock the delete() method to return a successfully completed Task
    coEvery { mockDoc.delete() } returns Tasks.forResult(null)

    // Act
    repository.deleteMyHealthCard()

    // Assert
    // Verify that the delete() method was called exactly once
    coVerify(exactly = 1) { mockDoc.delete() }
  }
}
