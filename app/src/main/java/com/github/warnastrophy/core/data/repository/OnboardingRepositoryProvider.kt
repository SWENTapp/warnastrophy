package com.github.warnastrophy.core.data.repository

import android.content.Context

object OnboardingRepositoryProvider {
  lateinit var repository: IntroductionRepository

  fun init(context: Context) {
    repository = OnboardingRepository(context)
  }
}
