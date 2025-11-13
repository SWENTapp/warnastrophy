package com.github.warnastrophy.core.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.github.warnastrophy.core.model.HealthCard
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ealthCardStorageTest {
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val userId = "user123"
  private val sampleCard =
      HealthCard(
          fullName = "John Doe",
          birthDate = "1990-01-01",
          sex = "M",
          socialSecurityNumber = "a social security number",
          bloodType = "O+",
          heightCm = 180,
          weightKg = 75.0,
          chronicConditions = listOf("Asthma"),
          allergies = listOf("Peanuts"),
          medications = listOf("Inhaler"),
          organDonor = true,
          notes = "No other conditions")

  @Test
  fun testSaveAndLoadHealthCard() = runBlocking {
    val saveResult = HealthCardStorage.saveHealthCard(context, userId, sampleCard)
    TestCase.assertTrue(saveResult is StorageResult.Success)

    val loadResult = HealthCardStorage.loadHealthCard(context, userId)
    TestCase.assertTrue(loadResult is StorageResult.Success)

    val loadedCard = (loadResult as StorageResult.Success).data
    TestCase.assertNotNull(loadedCard)
    TestCase.assertEquals(sampleCard, loadedCard)
  }

  @Test
  fun testDeleteHealthCard() = runBlocking {
    val saveResult = HealthCardStorage.saveHealthCard(context, userId, sampleCard)
    TestCase.assertTrue(saveResult is StorageResult.Success)

    val deleteResult = HealthCardStorage.deleteHealthCard(context, userId)
    TestCase.assertTrue(deleteResult is StorageResult.Success)

    val loadResult = HealthCardStorage.loadHealthCard(context, userId)
    TestCase.assertTrue(loadResult is StorageResult.Success)
    TestCase.assertNull((loadResult as StorageResult.Success).data)
  }

  @Test
  fun testUpdateHealthCard() = runBlocking {
    val saveResult = HealthCardStorage.saveHealthCard(context, userId, sampleCard)
    TestCase.assertTrue(saveResult is StorageResult.Success)

    val updatedCard = sampleCard.copy(fullName = "John Smith")

    val updateResult = HealthCardStorage.updateHealthCard(context, userId, updatedCard)
    TestCase.assertTrue(updateResult is StorageResult.Success)

    val loadResult = HealthCardStorage.loadHealthCard(context, userId)
    TestCase.assertTrue(loadResult is StorageResult.Success)
    val loadedCard = (loadResult as StorageResult.Success).data
    TestCase.assertNotNull(loadedCard)
    TestCase.assertEquals("John Smith", loadedCard!!.fullName)
  }
}
