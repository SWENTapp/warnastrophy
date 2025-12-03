package com.github.warnastrophy.core.data.provider

import android.content.Context
import com.github.warnastrophy.core.data.localStorage.LocalActivityRepository
import com.github.warnastrophy.core.data.repository.ActivityRepository
import com.github.warnastrophy.core.data.repository.MockActivityRepository

object ActivityRepositoryProvider {

  @Volatile private var _repo: ActivityRepository? = null

  // Public access point
  var repository: ActivityRepository
    get() = _repo ?: error("ActivityRepositoryProvider not initialized")
    private set(value) {
      _repo = value
    }

  /** Default: local encrypted (DataStore) */
  fun init(context: Context) {
    if (_repo == null) {
      useLocalEncrypted(context)
    }
  }

  /** Only local encrypted storage */
  fun useLocalEncrypted(context: Context) {
    repository = LocalActivityRepository(context.applicationContext)
  }

  /** Use mock repository for testing */
  fun useMock() {
    repository = MockActivityRepository()
  }
}
