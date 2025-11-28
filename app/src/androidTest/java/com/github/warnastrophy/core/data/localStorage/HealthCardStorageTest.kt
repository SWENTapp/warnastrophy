package com.github.warnastrophy.core.ui.healthcard

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.github.warnastrophy.core.data.localStorage.HealthCardStorage
import com.github.warnastrophy.core.data.localStorage.StorageException
import com.github.warnastrophy.core.data.localStorage.StorageResult
import com.github.warnastrophy.core.data.localStorage.healthCardDataStore
import com.github.warnastrophy.core.model.HealthCard
import com.github.warnastrophy.core.util.CryptoUtils
import com.google.gson.Gson
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class HealthCardStorageTest {
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val userId = "user123"
  private val sampleCard =
      HealthCard(
          fullName = "John Doe",
          dateOfBirthIso = "1990-01-01",
          sex = "M",
          idNumber = "ID123456",
          bloodType = "O+",
          heightCm = 180,
          weightKg = 75.0,
          chronicConditions = listOf("Asthma"),
          allergies = listOf("Peanuts"),
          medications = listOf("Inhaler"),
          organDonor = true,
          notes = "No other conditions")

  @Before
  fun setUp() {
    mockkObject(CryptoUtils)
  }

  @After
  fun tearDown() {
    unmockkObject(CryptoUtils)
    clearAllMocks()
  }

  @Test
  fun testSaveAndLoadHealthCard() = runBlocking {
    val saveResult = HealthCardStorage.saveHealthCard(context, userId, sampleCard)
    assertTrue(saveResult is StorageResult.Success)

    val loadResult = HealthCardStorage.loadHealthCard(context, userId)
    assertTrue(loadResult is StorageResult.Success)

    val loadedCard = (loadResult as StorageResult.Success).data
    assertNotNull(loadedCard)
    assertEquals(sampleCard, loadedCard)
  }

  @Test
  fun testDeleteHealthCard() = runBlocking {
    val saveResult = HealthCardStorage.saveHealthCard(context, userId, sampleCard)
    assertTrue(saveResult is StorageResult.Success)

    val deleteResult = HealthCardStorage.deleteHealthCard(context, userId)
    assertTrue(deleteResult is StorageResult.Success)

    val loadResult = HealthCardStorage.loadHealthCard(context, userId)
    assertTrue(loadResult is StorageResult.Success)
    assertNull((loadResult as StorageResult.Success).data)
  }

  @Test
  fun testUpdateHealthCard() = runBlocking {
    val saveResult = HealthCardStorage.saveHealthCard(context, userId, sampleCard)
    assertTrue(saveResult is StorageResult.Success)

    val updatedCard = sampleCard.copy(fullName = "John Smith")

    val updateResult = HealthCardStorage.updateHealthCard(context, userId, updatedCard)
    assertTrue(updateResult is StorageResult.Success)

    val loadResult = HealthCardStorage.loadHealthCard(context, userId)
    assertTrue(loadResult is StorageResult.Success)
    val loadedCard = (loadResult as StorageResult.Success).data
    assertNotNull(loadedCard)
    assertEquals("John Smith", loadedCard!!.fullName)
  }

  @Test
  fun saveHealthCardReturnsErrorOnEncryptionFailure() = runTest {
    every { CryptoUtils.encrypt(any()) } throws RuntimeException("Encryption failed!")

    val result = HealthCardStorage.saveHealthCard(context, userId, sampleCard)

    assertTrue(result is StorageResult.Error)
    val exception = (result as StorageResult.Error).exception
    assertTrue(exception is StorageException.DataStoreError)
  }

  @Test
  fun loadHealthCardReturnsErrorOnJSONDeserializationFailure() = runTest {
    // Save validly encrypted but malformed JSON data.
    val malformedJson = "{\"fullName\":\"John Doe\", \"birthDate\": \"missing-quote}"
    val encryptedMalformedJson = "encrypted-bad-json"
    context.healthCardDataStore.edit { preferences ->
      preferences[stringPreferencesKey("health_card_$userId")] = encryptedMalformedJson
    }
    every { CryptoUtils.decrypt(encryptedMalformedJson) } returns malformedJson

    val result = HealthCardStorage.loadHealthCard(context, userId)

    assertTrue(result is StorageResult.Error)
    val exception = (result as StorageResult.Error).exception
    assertTrue(exception is StorageException.DeserializationError)
  }

  @Test
  fun updateHealthCardReturnErrorWhenSaveFails() = runTest {
    // Initial load succeed
    val originalJson = "{\"fullName\":\"Original Name\"}"
    val originalEncrypted = "encrypted-original"
    context.healthCardDataStore.edit { preferences ->
      preferences[stringPreferencesKey("health_card_$userId")] = originalEncrypted
    }
    every { CryptoUtils.decrypt(originalEncrypted) } returns originalJson

    // Save fail
    val updatedCard = sampleCard.copy(fullName = "John Smith")
    val updatedJson = Gson().toJson(updatedCard)
    val encryptionException = RuntimeException("Encryption failed on save!")
    every { CryptoUtils.encrypt(updatedJson) } throws encryptionException

    val result = HealthCardStorage.updateHealthCard(context, userId, updatedCard)

    assertTrue("Result should be an error", result is StorageResult.Error)
    val exception = (result as StorageResult.Error).exception
    assertTrue(
        "Exception should be of type DataStoreError from save",
        exception is StorageException.DataStoreError)
    assertEquals(
        "The cause should be the original encryption exception",
        encryptionException,
        exception.cause)
  }
}
