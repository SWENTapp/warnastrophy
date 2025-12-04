package com.github.warnastrophy.core.data.provider

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.github.warnastrophy.core.data.repository.HybridUserPreferencesRepository
import com.github.warnastrophy.core.data.repository.UserPreferencesRepository
import com.github.warnastrophy.core.data.repository.UserPreferencesRepositoryLocal
import com.github.warnastrophy.core.data.repository.UserPreferencesRepositoryRemote
import com.google.firebase.firestore.FirebaseFirestore

/**
 * A singleton provider for accessing and initializing [UserPreferencesRepository] instances. This
 * object provides methods to initialize and manage different types of repositories that manage user
 * preferences, including local and hybrid (local + remote) repositories.
 *
 * The repository can either be:
 * - **Local**: backed by [UserPreferencesRepositoryLocal] using Jetpack DataStore for local
 *   storage.
 * - **Hybrid**: backed by both [UserPreferencesRepositoryLocal] and
 *   [UserPreferencesRepositoryRemote] using Firestore for remote storage.
 *
 * This object is thread-safe and allows for different configurations depending on the needs of the
 * application. It also supports resetting and setting custom repositories, which is useful for
 * testing or advanced use cases.
 */
object UserPreferencesRepositoryProvider {
  @Volatile private var _repo: UserPreferencesRepository? = null

  /**
   * The current instance of the [UserPreferencesRepository].
   *
   * @throws IllegalStateException if the repository has not been initialized.
   */
  var repository: UserPreferencesRepository
    get() = _repo ?: error("UserPreferencesRepositoryProvider not initialized")
    private set(value) {
      _repo = value
    }

  /**
   * Initializes the repository with a hybrid approach using both local and remote repositories.
   * This method sets up the repository to use a combination of [UserPreferencesRepositoryLocal] and
   * [UserPreferencesRepositoryRemote] with Firestore as the remote data source.
   *
   * @param dataStore The [DataStore] instance that will be used for local storage.
   * @param firestore The [FirebaseFirestore] instance that will be used for remote storage.
   */
  fun initHybrid(dataStore: DataStore<Preferences>, firestore: FirebaseFirestore) {
    val local = UserPreferencesRepositoryLocal(dataStore)
    val remote = UserPreferencesRepositoryRemote(firestore)

    repository = HybridUserPreferencesRepository(local, remote)
  }
}
