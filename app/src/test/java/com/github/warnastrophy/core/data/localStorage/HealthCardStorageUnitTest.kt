package com.github.warnastrophy.core.data.localStorage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.github.warnastrophy.core.model.HealthCard
import junit.framework.TestCase
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
            dateOfBirthIso = "1992-05-17",
            idNumber = "securityNumber123",
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
    TestCase.assertTrue("Save should succeed", saveResult is StorageResult.Success)

    val loadResult = HealthCardStorage.loadHealthCard(context, testUserId)
    TestCase.assertTrue("Loading should succeed", loadResult is StorageResult.Success)

    val loaded = (loadResult as StorageResult.Success).data
    TestCase.assertNotNull("The loaded health card should not be null", loaded)
    TestCase.assertEquals(sampleCard.fullName, loaded!!.fullName)
    TestCase.assertEquals(sampleCard.dateOfBirthIso, loaded.dateOfBirthIso)
    TestCase.assertEquals(sampleCard.sex, loaded.sex)
    TestCase.assertEquals(sampleCard.bloodType, loaded.bloodType)
    TestCase.assertEquals(sampleCard.heightCm, loaded.heightCm)
    sampleCard.weightKg?.let {
      loaded.weightKg?.let { it1 -> TestCase.assertEquals(it, it1, 1e-6) }
    }
    TestCase.assertEquals(sampleCard.allergies, loaded.allergies)
    TestCase.assertEquals(sampleCard.chronicConditions, loaded.chronicConditions)
    TestCase.assertEquals(sampleCard.medications, loaded.medications)
    TestCase.assertEquals(sampleCard.organDonor, loaded.organDonor)
  }

  @Test
  fun `delete health card should remove data`() = runTest {
    HealthCardStorage.saveHealthCard(context, testUserId, sampleCard)

    val deleteResult = HealthCardStorage.deleteHealthCard(context, testUserId)
    TestCase.assertTrue("Deletion should succeed", deleteResult is StorageResult.Success)

    val loadResult = HealthCardStorage.loadHealthCard(context, testUserId)
    TestCase.assertTrue("Loading should succeed", loadResult is StorageResult.Success)

    val loaded = (loadResult as StorageResult.Success).data
    TestCase.assertNull("The health card should be null after deletion", loaded)
  }

  @Test
  fun `update health card should modify fields correctly`() = runTest {
    HealthCardStorage.saveHealthCard(context, testUserId, sampleCard)

    val updatedCard = sampleCard.copy(weightKg = 80.0)
    val updateResult = HealthCardStorage.updateHealthCard(context, testUserId, updatedCard)
    TestCase.assertTrue("Updating should succeed", updateResult is StorageResult.Success)

    val loadResult = HealthCardStorage.loadHealthCard(context, testUserId)
    val updated = (loadResult as StorageResult.Success).data

    TestCase.assertNotNull("The updated health card should not be null", updated)
    updated!!.weightKg?.let { TestCase.assertEquals(80.0, it, 1e-6) }
    TestCase.assertEquals(sampleCard.fullName, updated.fullName)
  }

  @Test
  fun `update health card should handle null current gracefully`() = runTest {
    val updatedCard = sampleCard.copy(fullName = "New person")
    val updateResult = HealthCardStorage.updateHealthCard(context, testUserId, updatedCard)

    TestCase.assertTrue(
        "The update should succeed even without an existing health card",
        updateResult is StorageResult.Success)

    val loadResult = HealthCardStorage.loadHealthCard(context, testUserId)
    val created = (loadResult as StorageResult.Success).data

    TestCase.assertNotNull("The health card should be created", created)
    TestCase.assertEquals("New person", created!!.fullName)
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

    TestCase.assertNotNull(loaded1)
    TestCase.assertNotNull(loaded2)
    TestCase.assertEquals("User 1", loaded1!!.fullName)
    TestCase.assertEquals("User 2", loaded2!!.fullName)
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

    TestCase.assertNull("User 1 should be deleted", loaded1)
    TestCase.assertNotNull("User 2 should still exist", loaded2)
    TestCase.assertEquals("User 2", loaded2!!.fullName)
  }

  // ------------------------------------------------------------
  // Edge cases tests
  // ------------------------------------------------------------
  @Test
  fun `load health card on empty datastore should return null`() = runTest {
    val loadResult = HealthCardStorage.loadHealthCard(context, testUserId)

    TestCase.assertTrue("The loading should succeed", loadResult is StorageResult.Success)
    val loaded = (loadResult as StorageResult.Success).data
    TestCase.assertNull("The health card should be null if the DataStore is empty", loaded)
  }

  @Test
  fun `save health card with empty lists should work`() = runTest {
    val cardWithEmptyLists =
        sampleCard.copy(
            allergies = emptyList(), medications = emptyList(), chronicConditions = emptyList())

    val saveResult = HealthCardStorage.saveHealthCard(context, testUserId, cardWithEmptyLists)
    TestCase.assertTrue(saveResult is StorageResult.Success)

    val loadResult = HealthCardStorage.loadHealthCard(context, testUserId)
    val loaded = (loadResult as StorageResult.Success).data

    TestCase.assertNotNull(loaded)
    TestCase.assertTrue(loaded!!.allergies.isEmpty())
    TestCase.assertTrue(loaded.medications.isEmpty())
    TestCase.assertTrue(loaded.chronicConditions.isEmpty())
  }

  @Test
  fun `save health card with null optional fields should work`() = runTest {
    val cardsWithNulls =
        HealthCard(
            fullName = "Test User",
            dateOfBirthIso = "1990-01-01",
            idNumber = "a social security number",
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
    TestCase.assertTrue(saveResult is StorageResult.Success)

    val loadResult = HealthCardStorage.loadHealthCard(context, testUserId)
    val loaded = (loadResult as StorageResult.Success).data

    TestCase.assertNotNull(loaded)
    TestCase.assertEquals("Test User", loaded!!.fullName)
    TestCase.assertNull(loaded.sex)
    TestCase.assertNull(loaded.bloodType)
    TestCase.assertNull(loaded.notes)
    TestCase.assertNull(loaded.heightCm)
    TestCase.assertNull(loaded.weightKg)
  }

  @Test
  fun `save health card with special characters should work`() = runTest {
    val cardWithSpecialChars =
        sampleCard.copy(
            fullName = "José María Ñoño",
            notes = "Test with emojis 🏥💊 and special characters: <>&\"'")

    val saveResult = HealthCardStorage.saveHealthCard(context, testUserId, cardWithSpecialChars)
    TestCase.assertTrue(saveResult is StorageResult.Success)

    val loadResult = HealthCardStorage.loadHealthCard(context, testUserId)
    val loaded = (loadResult as StorageResult.Success).data

    TestCase.assertNotNull(loaded)
    TestCase.assertEquals("José María Ñoño", loaded!!.fullName)
    TestCase.assertEquals("Test with emojis 🏥💊 and special characters: <>&\"'", loaded.notes)
  }

  @Test
  fun `update with large lists should work`() = runTest {
    HealthCardStorage.saveHealthCard(context, testUserId, sampleCard)

    val largeList = (1..100).map { "Medication $it" }
    val updatedCard = sampleCard.copy(medications = largeList)

    val updateResult = HealthCardStorage.updateHealthCard(context, testUserId, updatedCard)

    TestCase.assertTrue(updateResult is StorageResult.Success)

    val loadResult = HealthCardStorage.loadHealthCard(context, testUserId)
    val updated = (loadResult as StorageResult.Success).data

    TestCase.assertNotNull(updated)
    TestCase.assertEquals(100, updated!!.medications.size)
    TestCase.assertEquals("Medication 1", updated.medications.first())
    TestCase.assertEquals("Medication 100", updated.medications.last())
  }

  // ------------------------------------------------------------
  // Error Handling Tests
  // ------------------------------------------------------------
  @Test
  fun `load should return error when data is corrupted`() = runTest {
    val key = stringPreferencesKey("health_card_$testUserId")
    context.healthCardDataStore.edit { prefs -> prefs[key] = "corrupted_base64_data!!!" }

    val loadResult = HealthCardStorage.loadHealthCard(context, testUserId)

    TestCase.assertTrue("Loading should return an error", loadResult is StorageResult.Error)
    val error = (loadResult as StorageResult.Error).exception
    TestCase.assertTrue(
        "Error should be DecryptionError", error is StorageException.DecryptionError)
  }

  @Test
  fun `update should propagate error from failed load`() = runTest {
    val key = stringPreferencesKey("health_card_$testUserId")
    context.healthCardDataStore.edit { prefs -> prefs[key] = "corrupted" }

    val updatedCard = sampleCard.copy(fullName = "Updated but failed load")
    val updateResult = HealthCardStorage.updateHealthCard(context, testUserId, updatedCard)

    TestCase.assertTrue(
        "Update should return an error when load fails", updateResult is StorageResult.Error)
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
    TestCase.assertEquals("Third Name", loaded!!.fullName)
  }

  @Test
  fun `delete nonexistent card should succeed without error`() = runTest {
    val deleteResult = HealthCardStorage.deleteHealthCard(context, "nonexistent_user")
    TestCase.assertTrue(
        "Delete should succeed even for nonexistent card", deleteResult is StorageResult.Success)
  }
}
