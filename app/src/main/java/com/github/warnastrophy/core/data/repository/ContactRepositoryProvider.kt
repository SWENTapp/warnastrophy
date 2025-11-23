package com.github.warnastrophy.core.data.repository

import android.content.Context

object ContactRepositoryProvider {
  lateinit var repository: ContactsRepository

  /**
   * Initializes the ContactsRepository with a local implementation. To be called once at
   * application startup.
   */
  fun init(context: Context) {
    val localRepo = ContactsRepositoryLocal(context.contactDataStore)
    val remoteRepo = ContactsRepositoryImpl()

    repository = HybridContactRepository(local = localRepo, remote = remoteRepo)
    // repository = NavigationMockContactRepository()
  }
}
