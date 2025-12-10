package com.github.warnastrophy.core.data.provider

import android.content.Context
import com.github.warnastrophy.core.data.interfaces.ContactsRepository
import com.github.warnastrophy.core.data.localStorage.ContactsStorage
import com.github.warnastrophy.core.data.localStorage.contactDataStore
import com.github.warnastrophy.core.data.repository.ContactRepositoryImpl
import com.github.warnastrophy.core.data.repository.HybridContactRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow

object ContactRepositoryProvider {

  private val _repositoryFlow = MutableStateFlow<ContactsRepository?>(null)

  val repository: ContactsRepository
    get() = _repositoryFlow.value ?: error("ContactRepositoryProvider not initialized")
  /**
   * Initializes the ContactsRepository with a local implementation. To be called once at
   * application startup.
   */
  fun initLocal(context: Context) {
    _repositoryFlow.value = ContactsStorage(context.contactDataStore)
  }

  /** Initialize Hybrid (local + remote) */
  fun initHybrid(context: Context, firestore: FirebaseFirestore) {
    val local = ContactsStorage(context.contactDataStore)
    val remote = ContactRepositoryImpl(firestore)

    _repositoryFlow.value = HybridContactRepository(local, remote)
  }

  /** Reset for tests */
  fun resetForTests() {
    _repositoryFlow.value = null
  }

  /** Override in unit tests */
  fun setCustom(repo: ContactsRepository) {
    _repositoryFlow.value = repo
  }
}
