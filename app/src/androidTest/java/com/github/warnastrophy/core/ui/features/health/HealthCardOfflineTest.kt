package com.github.warnastrophy.core.ui.features.health

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.warnastrophy.core.data.Provider.HealthCardRepositoryProvider
import com.github.warnastrophy.core.model.HealthCard
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HealthCardOfflineTest {

  private lateinit var context: Context

  @Before
  fun setUp() = runBlocking {
    context = ApplicationProvider.getApplicationContext()
    HealthCardRepositoryProvider.useLocalEncrypted(context)
  }

  @After
  fun tearDown() = runBlocking {
    runCatching { HealthCardRepositoryProvider.repository.deleteMyHealthCard() }
    HealthCardRepositoryProvider.resetForTests()
  }

  @Test
  fun writeAndReadLocalHealthCard() = runTest {
    val repo = HealthCardRepositoryProvider.repository

    val card =
      HealthCard(fullName = "Offline User", dateOfBirthIso = "1999-12-31", idNumber = "OFF-1")

    repo.upsertMyHealthCard(card)

    val cached = repo.getMyHealthCardOnce(fromCacheFirst = true)
    Assert.assertNotNull(cached)
    Assert.assertEquals("Offline User", cached!!.fullName)
    Assert.assertEquals("1999-12-31", cached.dateOfBirthIso)
    Assert.assertEquals("OFF-1", cached.idNumber)
  }
}
