package com.github.warnastrophy.core.data.provider

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.github.warnastrophy.core.data.interfaces.UserPreferencesRepository
import com.github.warnastrophy.core.data.repository.HybridUserPreferencesRepository
import com.github.warnastrophy.core.data.repository.UserPreferencesRepositoryLocal
import com.github.warnastrophy.core.data.repository.UserPreferencesRepositoryRemote
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Provides and manages a singleton instance of [UserPreferencesRepository].
 *
 * This provider exposes a [StateFlow] that emits the currently active repository instance and
 * offers methods to initialize it in different configurations. The repository can be:
 * - **Local-only** — An instance of [UserPreferencesRepositoryLocal] backed by Jetpack DataStore.
 * - **Hybrid** — A [HybridUserPreferencesRepository] that combines local persistence via
 *   [UserPreferencesRepositoryLocal] with remote synchronization through
 *   [UserPreferencesRepositoryRemote] (Firestore).
 *
 * This object is thread-safe and intended to be initialized once during application startup. Custom
 * repositories can be injected (e.g. during tests) by directly updating the internal flow.
 *
 * ### Usage
 * Before accessing [repository], you must call either:
 * - [initLocal], or
 * - [initHybrid].
 *
 * Attempting to access [repository] before initialization will result in an
 * [IllegalStateException].
 */
object UserPreferencesRepositoryProvider {

  private val _repositoryFlow = MutableStateFlow<UserPreferencesRepository?>(null)

  /**
   * The current instance of the [UserPreferencesRepository].
   *
   * @throws IllegalStateException if the repository has not been initialized.
   */
  val repository: UserPreferencesRepository
    get() = _repositoryFlow.value ?: error("UserPreferencesRepositoryProvider not initialized")

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

    _repositoryFlow.value = HybridUserPreferencesRepository(local, remote)
  }

  /**
   * Initializes the repository using a local-only configuration backed by Jetpack DataStore.
   *
   * @param dataStore The [DataStore] instance used for local storage.
   */
  fun initLocal(dataStore: DataStore<Preferences>) {
    _repositoryFlow.value = UserPreferencesRepositoryLocal(dataStore)
  }
}
