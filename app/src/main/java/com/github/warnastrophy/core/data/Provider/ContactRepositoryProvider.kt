package com.github.warnastrophy.core.data.Provider

import android.content.Context
import com.github.warnastrophy.core.data.interfaces.ContactsRepository
import com.github.warnastrophy.core.data.localStorage.ContactsStorage
import com.github.warnastrophy.core.data.localStorage.contactDataStore

object ContactRepositoryProvider {
  lateinit var repository: ContactsRepository

  /**
   * Initializes the ContactsRepository with a local implementation. To be called once at
   * application startup.
   */
  fun init(context: Context) {
    repository = ContactsStorage(context.contactDataStore)
    // repository = NavigationMockContactRepository()
  }
}
