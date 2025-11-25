package com.github.warnastrophy.core.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore

object ContactRepositoryProvider {
  @Volatile private var _repo: ContactsRepository? = null

  var repository: ContactsRepository
    get() = _repo ?: error("ContactRepositoryProvider not initialized")
    private set(value) {
      _repo = value
    }
  /**
   * Initializes the ContactsRepository with a local implementation. To be called once at
   * application startup.
   */
  fun initLocal(context: Context) {
    repository = ContactsRepositoryLocal(context.contactDataStore)
  }

  /** Initialize Hybrid (local + remote) */
  fun initHybrid(context: Context, firestore: FirebaseFirestore) {
    val local = ContactsRepositoryLocal(context.contactDataStore)
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
