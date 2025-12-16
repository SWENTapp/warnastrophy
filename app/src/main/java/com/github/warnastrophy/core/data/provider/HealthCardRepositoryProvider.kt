package com.github.warnastrophy.core.data.provider

import android.content.Context
import com.github.warnastrophy.core.data.localStorage.LocalHealthCardRepository
import com.github.warnastrophy.core.data.repository.HealthCardRepository
import com.github.warnastrophy.core.data.repository.HealthCardRepositoryImpl
import com.github.warnastrophy.core.data.repository.HybridHealthCardRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow

object HealthCardRepositoryProvider {

  private val _repositoryFlow = MutableStateFlow<HealthCardRepository?>(null)

  const val COLLECTION = "healthCards"

  val repository: HealthCardRepository
    get() = _repositoryFlow.value ?: error("HealthCardRepositoryProvider not initialized")

  /** Default: local encrypted (DataStore) */
  fun init(context: Context) {
    if (_repositoryFlow.value == null) {
      useLocalEncrypted(context)
    }
  }

  /** Only local encrypted storage */
  fun useLocalEncrypted(context: Context) {
    _repositoryFlow.value = LocalHealthCardRepository(context.applicationContext)
  }

  /** Hybrid: local DataStore + Firestore */
  fun useHybridEncrypted(context: Context, db: FirebaseFirestore, auth: FirebaseAuth) {
    val fallbackUidProvider = { DeviceIdProvider.get(context) }

    val remote: HealthCardRepository =
        HealthCardRepositoryImpl(
            auth = auth,
            db = db,
            collectionName = COLLECTION,
            fallbackUidProvider = fallbackUidProvider)

    val local: HealthCardRepository = LocalHealthCardRepository(context.applicationContext)

    _repositoryFlow.value = HybridHealthCardRepository(local, remote)
  }

  /** Optional helper for tests / re-init */
  fun resetForTests() {
    _repositoryFlow.value = null
  }
}
