package com.github.warnastrophy.core.ui.features.health

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.warnastrophy.core.data.localStorage.LocalHealthCardRepository
import com.github.warnastrophy.core.data.provider.HealthCardRepositoryProvider
import com.github.warnastrophy.core.data.repository.HybridHealthCardRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.mockk
import junit.framework.TestCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HealthCardRepositoryProviderTest {

  private lateinit var context: Context
  private lateinit var mockDb: FirebaseFirestore
  private lateinit var mockAuth: FirebaseAuth

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    mockDb = mockk(relaxed = true)
    mockAuth = mockk(relaxed = true)
    HealthCardRepositoryProvider.resetForTests()
  }

  @After
  fun tearDown() {
    HealthCardRepositoryProvider.resetForTests()
  }

  @Test
  fun init_sets_LocalHealthCardRepository_when_repo_is_null() {
    HealthCardRepositoryProvider.init(context)
    TestCase.assertTrue(HealthCardRepositoryProvider.repository is LocalHealthCardRepository)
  }

  @Test
  fun init_does_not_override_existing_repository() {
    HealthCardRepositoryProvider.useLocalEncrypted(context)
    val first = HealthCardRepositoryProvider.repository

    HealthCardRepositoryProvider.init(context)
    TestCase.assertSame(first, HealthCardRepositoryProvider.repository)
  }

  @Test
  fun useLocalEncrypted_always_sets_LocalHealthCardRepository() {
    HealthCardRepositoryProvider.useLocalEncrypted(context)
    TestCase.assertTrue(HealthCardRepositoryProvider.repository is LocalHealthCardRepository)
  }

  @Test
  fun useHybridEncrypted_sets_HybridHealthCardRepository() {
    HealthCardRepositoryProvider.useHybridEncrypted(context, mockDb, mockAuth)
    TestCase.assertTrue(HealthCardRepositoryProvider.repository is HybridHealthCardRepository)
  }
}
