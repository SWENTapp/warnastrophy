package com.github.warnastrophy.core.ui.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.github.warnastrophy.core.model.util.HealthCard
import com.github.warnastrophy.core.model.util.StorageException
import com.github.warnastrophy.core.model.util.StorageResult
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HealthCardStorageUnitTest {
  private lateinit var context: Context
  private lateinit var sampleCard: HealthCard
  private val testUserId = "test_user_123"

  @Before
  fun setUp() = runTest {
    context = ApplicationProvider.getApplicationContext()
    HealthCardStorage.deleteHealthCard(context, testUserId)

    sampleCard =
        HealthCard(
            fullName = "John Doe",
            birthDate = "1992-05-17",
            sex = "M",
            bloodType = "A+",
            heightCm = 180,
            weightKg = 75.0,
            chronicConditions = listOf("Asthma"),
            allergies = listOf("Penicillin"),
            medications = listOf("Ventolin"),
            organDonor = true)
  }

  // ------------------------------------------------------------
  // Basic CRUD (Create, Update, Delete) tests
  // ------------------------------------------------------------
  @Test
  fun `save and load health card should work correctly`() = runTest {
    val saveResult = HealthCardStorage.saveHealthCard(context, testUserId, sampleCard)
    assertTrue("Save should succeed", saveResult is StorageResult.Success)

    val loadResult = HealthCardStorage.loadHealthCard(context, testUserId)
    assertTrue("Loading should succeed", loadResult is StorageResult.Success)

    val loaded = (loadResult as StorageResult.Success).data
    assertNotNull("The loaded health card should not be null", loaded)
    assertEquals(sampleCard.fullName, loaded!!.fullName)
    assertEquals(sampleCard.birthDate, loaded.birthDate)
    assertEquals(sampleCard.sex, loaded.sex)
    assertEquals(sampleCard.bloodType, loaded.bloodType)
    assertEquals(sampleCard.heightCm, loaded.heightCm)
    sampleCard.weightKg?.let { loaded.weightKg?.let { it1 -> assertEquals(it, it1, 1e-6) } }
    assertEquals(sampleCard.allergies, loaded.allergies)
    assertEquals(sampleCard.chronicConditions, loaded.chronicConditions)
    assertEquals(sampleCard.medications, loaded.medications)
    assertEquals(sampleCard.organDonor, loaded.organDonor)
  }

  @Test
  fun `delete health card should remove data`() = runTest {
    HealthCardStorage.saveHealthCard(context, testUserId, sampleCard)

    val deleteResult = HealthCardStorage.deleteHealthCard(context, testUserId)
    assertTrue("Deletion should succeed", deleteResult is StorageResult.Success)

    val loadResult = HealthCardStorage.loadHealthCard(context, testUserId)
    assertTrue("Loading should succeed", loadResult is StorageResult.Success)

    val loaded = (loadResult as StorageResult.Success).data
    assertNull("The health card should be null after deletion", loaded)
  }

  @Test
  fun `update health card should modify fields correctly`() = runTest {
    HealthCardStorage.saveHealthCard(context, testUserId, sampleCard)

    val updateResult =
        HealthCardStorage.updateHealthCard(context, testUserId) { current ->
          current!!.copy(weightKg = 80.0)
        }
    assertTrue("Updating should succeed", updateResult is StorageResult.Success)

    val loadResult = HealthCardStorage.loadHealthCard(context, testUserId)
    val updated = (loadResult as StorageResult.Success).data

    assertNotNull("The update health card should not be null", updated)
    updated!!.weightKg?.let { assertEquals(80.0, it, 1e-6) }
    assertEquals(sampleCard.fullName, updated.fullName)
  }

  @Test
  fun `update health card should handle null current gracefully`() = runTest {
    val updateResult =
        HealthCardStorage.updateHealthCard(context, testUserId) { current ->
          (current ?: sampleCard).copy(fullName = "New person")
        }
    assertTrue(
        "The update should succeed even without an existing health card",
        updateResult is StorageResult.Success)

    val loadResult = HealthCardStorage.loadHealthCard(context, testUserId)
    val created = (loadResult as StorageResult.Success).data

    assertNotNull("The health card should be created", created)
    assertEquals("New person", created!!.fullName)
  }

  // ------------------------------------------------------------
  // Multi-users tests
  // ------------------------------------------------------------
  @Test
  fun `save cards for multiple users should work independently`() = runTest {
    val user1 = "user1"
    val user2 = "user2"

    val card1 = sampleCard.copy(fullName = "User 1")
    val card2 = sampleCard.copy(fullName = "User 2")

    HealthCardStorage.saveHealthCard(context, user1, card1)
    HealthCardStorage.saveHealthCard(context, user2, card2)

    val loaded1 = (HealthCardStorage.loadHealthCard(context, user1) as StorageResult.Success).data
    val loaded2 = (HealthCardStorage.loadHealthCard(context, user2) as StorageResult.Success).data

    assertNotNull(loaded1)
    assertNotNull(loaded2)
    assertEquals("User 1", loaded1!!.fullName)
    assertEquals("User 2", loaded2!!.fullName)
  }

  @Test
  fun `delete one user should not affect other users`() = runTest {
    val user1 = "user1"
    val user2 = "user2"

    HealthCardStorage.saveHealthCard(context, user1, sampleCard.copy(fullName = "User 1"))
    HealthCardStorage.saveHealthCard(context, user2, sampleCard.copy(fullName = "User 2"))

    HealthCardStorage.deleteHealthCard(context, user1)

    val loaded1 = (HealthCardStorage.loadHealthCard(context, user1) as StorageResult.Success).data
    val loaded2 = (HealthCardStorage.loadHealthCard(context, user2) as StorageResult.Success).data

    assertNull("User 1 should be deleted", loaded1)
    assertNotNull("User 2 should still exist", loaded2)
    assertEquals("User 2", loaded2!!.fullName)
  }

  // ------------------------------------------------------------
  // Edge cases tests
  // ------------------------------------------------------------
  @Test
  fun `load health card on empty datastore should return null`() = runTest {
    val loadResult = HealthCardStorage.loadHealthCard(context, testUserId)

    assertTrue("The loading should succeed", loadResult is StorageResult.Success)
    val loaded = (loadResult as StorageResult.Success).data
    assertNull("The health card should be null if the DataStore is empty", loaded)
  }

  @Test
  fun `save health card with empty lists should work`() = runTest {
    val cardWithEmptyLists =
        sampleCard.copy(
            allergies = emptyList(), medications = emptyList(), chronicConditions = emptyList())

    val saveResult = HealthCardStorage.saveHealthCard(context, testUserId, cardWithEmptyLists)
    assertTrue(saveResult is StorageResult.Success)

    val loadResult = HealthCardStorage.loadHealthCard(context, testUserId)
    val loaded = (loadResult as StorageResult.Success).data

    assertNotNull(loaded)
    assertTrue(loaded!!.allergies.isEmpty())
    assertTrue(loaded.medications.isEmpty())
    assertTrue(loaded.chronicConditions.isEmpty())
  }

  @Test
  fun `save health card with null optional fields should work`() = runTest {
    val cardsWithNulls =
        HealthCard(
            fullName = "Test User",
            birthDate = "1990-01-01",
            sex = null,
            bloodType = null,
            heightCm = null,
            weightKg = null,
            chronicConditions = emptyList(),
            allergies = emptyList(),
            medications = emptyList(),
            organDonor = false,
            notes = null)

    val saveResult = HealthCardStorage.saveHealthCard(context, testUserId, cardsWithNulls)
    assertTrue(saveResult is StorageResult.Success)

    val loadResult = HealthCardStorage.loadHealthCard(context, testUserId)
    val loaded = (loadResult as StorageResult.Success).data

    assertNotNull(loaded)
    assertEquals("Test User", loaded!!.fullName)
    assertNull(loaded.sex)
    assertNull(loaded.bloodType)
    assertNull(loaded.notes)
    assertNull(loaded.heightCm)
    assertNull(loaded.weightKg)
  }

  @Test
  fun `save health card with special characters should work`() = runTest {
    val cardWithSpecialChars =
        sampleCard.copy(
            fullName = "José María Ñoño",
            notes = "Test with emojis 🏥💊 and special characters: <>&\"'")

    val saveResult = HealthCardStorage.saveHealthCard(context, testUserId, cardWithSpecialChars)
    assertTrue(saveResult is StorageResult.Success)

    val loadResult = HealthCardStorage.loadHealthCard(context, testUserId)
    val loaded = (loadResult as StorageResult.Success).data

    assertNotNull(loaded)
    assertEquals("José María Ñoño", loaded!!.fullName)
    assertEquals("Test with emojis 🏥💊 and special characters: <>&\"'", loaded.notes)
  }

  @Test
  fun `update with large lists should work`() = runTest {
    HealthCardStorage.saveHealthCard(context, testUserId, sampleCard)

    val largeList = (1..100).map { "Medication $it" }
    val updateResult =
        HealthCardStorage.updateHealthCard(context, testUserId) { current ->
          current!!.copy(medications = largeList)
        }

    assertTrue(updateResult is StorageResult.Success)

    val loadResult = HealthCardStorage.loadHealthCard(context, testUserId)
    val updated = (loadResult as StorageResult.Success).data

    assertNotNull(updated)
    assertEquals(100, updated!!.medications.size)
    assertEquals("Medication 1", updated.medications.first())
    assertEquals("Medication 100", updated.medications.last())
  }

  // ------------------------------------------------------------
  // Error Handling Tests
  // ------------------------------------------------------------
  @Test
  fun `load should return error when data is corrupted`() = runTest {
    val key = stringPreferencesKey("health_card_$testUserId")
    context.healthCardDataStore.edit { prefs -> prefs[key] = "corrupted_base64_data!!!" }

    val loadResult = HealthCardStorage.loadHealthCard(context, testUserId)

    assertTrue("Loading should return an error", loadResult is StorageResult.Error)
    val error = (loadResult as StorageResult.Error).exception
    assertTrue("Error should be DecryptionError", error is StorageException.DecryptionError)
  }

  @Test
  fun `update with exception in updater should return error`() = runTest {
    HealthCardStorage.saveHealthCard(context, testUserId, sampleCard)

    val updateResult =
        HealthCardStorage.updateHealthCard(context, testUserId) { _ ->
          throw IllegalStateException("Test exception in updater")
        }

    assertTrue("Update should return an error", updateResult is StorageResult.Error)
    val error = (updateResult as StorageResult.Error).exception
    assertTrue("Error should be DataStoreError", error is StorageException.DataStoreError)
  }

  @Test
  fun `update should propagate error from failed load`() = runTest {
    val key = stringPreferencesKey("health_card_$testUserId")
    context.healthCardDataStore.edit { prefs -> prefs[key] = "corrupted" }

    val updateResult =
        HealthCardStorage.updateHealthCard(context, testUserId) { current -> current ?: sampleCard }

    assertTrue("Update should return an error when load fails", updateResult is StorageResult.Error)
  }

  @Test
  fun `multiple saves should overwrite previous data`() = runTest {
    val card1 = sampleCard.copy(fullName = "First Name")
    val card2 = sampleCard.copy(fullName = "Second Name")
    val card3 = sampleCard.copy(fullName = "Third Name")

    HealthCardStorage.saveHealthCard(context, testUserId, card1)
    HealthCardStorage.saveHealthCard(context, testUserId, card2)
    HealthCardStorage.saveHealthCard(context, testUserId, card3)

    val loaded =
        (HealthCardStorage.loadHealthCard(context, testUserId) as StorageResult.Success).data
    assertEquals("Third Name", loaded!!.fullName)
  }

  @Test
  fun `delete nonexistent card should succeed without error`() = runTest {
    val deleteResult = HealthCardStorage.deleteHealthCard(context, "nonexistent_user")
    assertTrue(
        "Delete should succeed even for nonexistent card", deleteResult is StorageResult.Success)
  }
}
