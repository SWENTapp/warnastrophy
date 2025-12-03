package com.github.warnastrophy.core.data.repository

import android.content.Context

/** Provides access to the [IntroductionRepository] instance for managing onboarding state. */
object OnboardingRepositoryProvider {
  lateinit var repository: IntroductionRepository

  /**
   * Initializes the repository provider with the concrete [OnboardingRepository] implementation.
   *
   * @param context The application context required to instantiate the repository.
   */
  fun init(context: Context) {
    repository = OnboardingRepository(context)
  }
}
