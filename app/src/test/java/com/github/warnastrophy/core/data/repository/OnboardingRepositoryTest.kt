package com.github.warnastrophy.core.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OnboardingRepositoryTest {
  private lateinit var context: Context
  private lateinit var repository: OnboardingRepository

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext()
    repository = OnboardingRepository(context)

    context.getSharedPreferences("onboarding", Context.MODE_PRIVATE).edit().clear().commit()
  }

  @Test
  fun `default value is false when nothing is saved`() {
    val result = repository.isOnboardingCompleted()
    assertFalse(result)
  }

  @Test
  fun `setOnboardingCompleted sets completed to true`() {
    repository.setOnboardingCompleted()

    val prefsValue =
        context
            .getSharedPreferences("onboarding", Context.MODE_PRIVATE)
            .getBoolean("completed", false)

    assertTrue(prefsValue)
  }
}
