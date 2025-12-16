package com.github.warnastrophy.core.data.provider

import android.content.Context
import com.github.warnastrophy.core.data.repository.IntroductionRepository
import com.github.warnastrophy.core.data.repository.OnboardingRepository

/**
 * Provides access to the [com.github.warnastrophy.core.data.repository.IntroductionRepository]
 * instance for managing onboarding state.
 */
object OnboardingRepositoryProvider {
  lateinit var repository: IntroductionRepository

  /**
   * Initializes the repository provider with the concrete
   * [com.github.warnastrophy.core.data.repository.OnboardingRepository] implementation.
   *
   * @param context The application context required to instantiate the repository.
   */
  fun init(context: Context) {
    repository = OnboardingRepository(context)
  }
}
