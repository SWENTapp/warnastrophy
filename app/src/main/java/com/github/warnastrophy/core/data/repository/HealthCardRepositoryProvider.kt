package com.github.warnastrophy.core.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object HealthCardRepositoryProvider {
  const val COLLECTION = "healthCards"

  @Volatile private var _repo: HealthCardRepository? = null

  val repository: HealthCardRepository
    get() = _repo ?: error("HealthCardRepositoryProvider not initialized")

  /** Default to local encrypted (on-device) if you want */
  fun init(context: Context) {
    if (_repo == null) {
      useLocalEncrypted(context)
    }
  }

  /** Keep your existing local-encrypted DataStore option */
  fun useLocalEncrypted(context: Context) {
    _repo = LocalHealthCardRepository(context.applicationContext)
  }

  /** <-- This is the function you asked for */
  fun useFirestoreEncrypted(db: FirebaseFirestore, auth: FirebaseAuth) {
    _repo = FirestoreHealthCardRepository(db, auth)
  }

  fun useHybridEncrypted(context: Context, db: FirebaseFirestore, auth: FirebaseAuth) {
    val local = LocalHealthCardRepository(context.applicationContext)
    val remote = FirestoreHealthCardRepository(db, auth)
    _repo = HybridHealthCardRepository(local, remote, auth)
  }
}
