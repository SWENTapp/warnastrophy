package com.github.warnastrophy.core.ui.healthcard

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.warnastrophy.core.data.repository.HealthCardRepositoryProvider
import com.github.warnastrophy.core.domain.model.HealthCard
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    assertNotNull(cached)
    assertEquals("Offline User", cached!!.fullName)
    assertEquals("1999-12-31", cached.dateOfBirthIso)
    assertEquals("OFF-1", cached.idNumber)
  }
}
