package com.github.warnastrophy.core.ui.healthcard

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.github.warnastrophy.core.model.util.HealthCard
import com.github.warnastrophy.core.model.util.StorageResult
import com.github.warnastrophy.core.ui.repository.HealthCardStorage
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Test

class HealthCardStorageTest {
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val userId = "user123"
  private val sampleCard =
      HealthCard(
          fullName = "John Doe",
          birthDate = "1990-01-01",
          sex = "M",
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

    val updateResult =
        HealthCardStorage.updateHealthCard(context, userId) { current ->
          current!!.copy(fullName = "John Smith")
        }
    assertTrue(updateResult is StorageResult.Success)

    val loadResult = HealthCardStorage.loadHealthCard(context, userId)
    assertTrue(loadResult is StorageResult.Success)
    val loadedCard = (loadResult as StorageResult.Success).data
    assertEquals("John Smith", loadedCard!!.fullName)
  }
}
