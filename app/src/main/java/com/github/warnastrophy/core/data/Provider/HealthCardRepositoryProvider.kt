package com.github.warnastrophy.core.data.Provider

import android.content.Context
import com.github.warnastrophy.core.data.localStorage.LocalHealthCardRepository
import com.github.warnastrophy.core.data.repository.HealthCardRepository
import com.github.warnastrophy.core.data.repository.HealthCardRepositoryImpl
import com.github.warnastrophy.core.data.repository.HybridHealthCardRepository
import com.github.warnastrophy.core.data.service.DeviceIdProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object HealthCardRepositoryProvider {

  const val COLLECTION = "healthCards"

  @Volatile private var _repo: HealthCardRepository? = null

  // Public access point
  var repository: HealthCardRepository
    get() = _repo ?: error("HealthCardRepositoryProvider not initialized")
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
    repository = LocalHealthCardRepository(context.applicationContext)
  }

  /** Hybrid: local DataStore + Firestore */
  fun useHybridEncrypted(context: Context, db: FirebaseFirestore, auth: FirebaseAuth) {
    val fallbackUidProvider = { DeviceIdProvider.get(context) }

    val remote: HealthCardRepository =
        HealthCardRepositoryImpl(
            auth = null,
            db = db,
            collectionName = COLLECTION,
            fallbackUidProvider = fallbackUidProvider
        )

    val local: HealthCardRepository = LocalHealthCardRepository(context.applicationContext)

    repository = HybridHealthCardRepository(local, remote)
  }

  /** Optional helper for tests / re-init */
  fun resetForTests() {
    _repo = null
  }
}