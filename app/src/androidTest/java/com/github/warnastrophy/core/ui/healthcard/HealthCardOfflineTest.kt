package com.github.warnastrophy.core.ui.healthcard

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.warnastrophy.core.data.repository.HealthCardRepositoryProvider
import com.github.warnastrophy.core.model.HealthCard
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HealthCardOfflineTest {

  private val auth = Firebase.auth
  private val db = Firebase.firestore

  @Before
  fun setUp() = runBlocking {
    // Route to local emulators (Android emulator host = 10.0.2.2)
    Firebase.auth.useEmulator("10.0.2.2", 9099)
    Firebase.firestore.useEmulator("10.0.2.2", 8080)
    HealthCardRepositoryProvider.useEmulator(
        host = "10.0.2.2", firestorePort = 8080, authPort = 9099)

    // Fresh test user on the **emulator**
    val email = "offline@test.dev"
    val pass = "password123"
    runCatching { auth.createUserWithEmailAndPassword(email, pass).await() }
    auth.signInWithEmailAndPassword(email, pass).await()
  }

  @After fun tearDown() = runBlocking { auth.signOut() }

  @Test
  fun writeWhileOffline_thenSyncOnline() = runTest {
    val repo = HealthCardRepositoryProvider.repository

    // Go offline
    db.disableNetwork().await()

    // Local upsert should hit cache and be observable
    val card =
        HealthCard(fullName = "Offline User", dateOfBirthIso = "1999-12-31", idNumber = "OFF-1")
    repo.upsertMyHealthCard(card)

    // Cache-first read → should return the local write
    val cached = repo.getMyHealthCardOnce() // fromCacheFirst = true
    assertNotNull(cached)
    assertEquals("Offline User", cached!!.fullName)

    // Back online → pending writes synchronize
    db.enableNetwork().await()

    // Force server read to verify sync actually happened
    val server = repo.getMyHealthCardOnce(fromCacheFirst = false)
    assertNotNull(server)
    assertEquals("Offline User", server!!.fullName)
  }
}
