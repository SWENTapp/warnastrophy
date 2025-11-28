package com.github.warnastrophy.core.data.provider

import com.github.warnastrophy.core.data.repository.ActivityRepository
import com.github.warnastrophy.core.data.repository.MockActivityRepository

object ActivityRepositoryProvider {
  lateinit var repository: ActivityRepository

  fun init() {
    repository = MockActivityRepository()
  }
}
