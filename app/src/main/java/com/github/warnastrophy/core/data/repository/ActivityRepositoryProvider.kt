package com.github.warnastrophy.core.data.repository

object ActivityRepositoryProvider {
  lateinit var repository: ActivityRepository

  /**
   * Initializes the ContactsRepository with a local implementation. To be called once at
   * application startup.
   */
}
