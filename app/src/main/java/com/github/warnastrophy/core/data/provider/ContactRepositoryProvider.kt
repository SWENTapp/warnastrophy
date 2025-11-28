package com.github.warnastrophy.core.data.repository

import android.content.Context
import com.github.warnastrophy.core.data.interfaces.ContactsRepository
import com.github.warnastrophy.core.data.localStorage.ContactsStorage
import com.github.warnastrophy.core.data.localStorage.contactDataStore
import com.google.firebase.firestore.FirebaseFirestore

object ContactRepositoryProvider {
  @Volatile private var _repo: ContactsRepository? = null

  var repository: ContactsRepository
    get() = _repo ?: error("ContactRepositoryProvider not initialized")
    set(value) {
      _repo = value
    }
  /**
   * Initializes the ContactsRepository with a local implementation. To be called once at
   * application startup.
   */
  fun initLocal(context: Context) {
    repository = ContactsStorage(context.contactDataStore)
  }

  /** Initialize Hybrid (local + remote) */
  fun initHybrid(context: Context, firestore: FirebaseFirestore) {
    val local = ContactsStorage(context.contactDataStore)
    val remote = ContactsRepositoryImpl(firestore)

    repository = HybridContactRepository(local, remote)
  }

  /** Reset for tests */
  fun resetForTests() {
    _repo = null
  }

  /** Override in unit tests */
  fun setCustom(repo: ContactsRepository) {
    _repo = repo
  }
}
