package com.github.warnastrophy.core.ui.onboard

import com.github.warnastrophy.core.data.repository.IntroductionRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppStateManagerViewModelTest {
  private lateinit var repository: IntroductionRepository
  private lateinit var viewModel: AppStateManagerViewModel

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setup() {
    Dispatchers.setMain(testDispatcher)

    repository = mockk {
      every { isOnboardingCompleted() } returns false
      coEvery { setOnboardingCompleted() } just Runs
    }

    viewModel = AppStateManagerViewModel(repository, testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `initial onboarding state is loaded from repository`() {
    assertFalse(viewModel.isOnboardingCompleted.value)
  }

  @Test
  fun `completeOnboarding sets state to true and calls repository`() = runTest {
    viewModel.completeOnboarding()
    advanceUntilIdle()
    coVerify(exactly = 1) { repository.setOnboardingCompleted() }
    assertTrue(viewModel.isOnboardingCompleted.value)
  }
}
