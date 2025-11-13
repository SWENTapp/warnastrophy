package com.github.warnastrophy.core.data.repository

import android.content.Context
import com.github.warnastrophy.core.data.service.DeviceIdProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object HealthCardRepositoryProvider {

  const val COLLECTION = "healthCards"

  @Volatile private var _repo: HealthCardRepository? = null

  // Public access point
  var repository: HealthCardRepository
    get() = _repo ?: error("HealthCardRepositoryProvider not initialized")
    private set(value) { // <-- IMPORTANT: write into _repo
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

  /** Only Firestore (no hybrid), using device ID as UID */
  fun useFirestoreEncrypted(context: Context, db: FirebaseFirestore) {
    val fallbackUidProvider = { DeviceIdProvider.get(context) }

    repository =
        HealthCardRepositoryImpl(
            auth = null, // don’t depend on FirebaseAuth
            db = db,
            collectionName = COLLECTION, // "healthCards"
            fallbackUidProvider = fallbackUidProvider)
  }

  /** Hybrid: local DataStore + Firestore */
  fun useHybridEncrypted(context: Context, db: FirebaseFirestore, auth: FirebaseAuth) {
    val fallbackUidProvider = { DeviceIdProvider.get(context) }

    val remote: HealthCardRepository =
        HealthCardRepositoryImpl(
            auth = null, // still use device ID, not auth uid
            db = db,
            collectionName = COLLECTION,
            fallbackUidProvider = fallbackUidProvider)

    val local: HealthCardRepository = LocalHealthCardRepository(context.applicationContext)

    repository = HybridHealthCardRepository(local, remote, auth)
  }

  /** Optional helper for tests / re-init */
  fun resetForTests() {
    _repo = null
  }
}
