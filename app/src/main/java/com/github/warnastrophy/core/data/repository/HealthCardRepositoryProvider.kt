package com.github.warnastrophy.core.data.repository

import android.content.Context
import com.github.warnastrophy.core.data.service.DeviceIdProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Simple Service Locator for a singleton HealthCardRepository. Mirrors your
 * HazardRepositoryProvider pattern.
 */
object HealthCardRepositoryProvider {

  /** Firestore collection holding per-user documents. */
  const val COLLECTION: String = "healthCards"

  private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
  private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

  private val defaultRepo: HealthCardRepository by lazy {
    HealthCardRepositoryImpl(auth, db, COLLECTION)
  }

  /** The active repository. Override in tests if needed. */
  @Volatile var repository: HealthCardRepository = defaultRepo

  fun init(context: Context) {
    val auth: FirebaseAuth? = null // no auth
    val db = FirebaseFirestore.getInstance()
    val fallback = { DeviceIdProvider.get(context) }
    repository = HealthCardRepositoryImpl(auth, db, COLLECTION, fallback)
  }
  /**
   * Point both Auth and Firestore to local emulators (call *before* first use ideally). On Android
   * emulator, host is usually "10.0.2.2".
   */
  @Synchronized
  fun useEmulator(host: String = "10.0.2.2", firestorePort: Int = 8080, authPort: Int = 9099) {
    db.useEmulator(host, firestorePort)
    auth.useEmulator(host, authPort)
  }

  /** Swap a fake/double for tests. */
  @Synchronized
  fun setRepositoryForTests(fake: HealthCardRepository) {
    repository = fake
  }

  /** Restore the default Firestore-backed repo. */
  @Synchronized
  fun resetToDefault() {
    repository = defaultRepo
  }
}
