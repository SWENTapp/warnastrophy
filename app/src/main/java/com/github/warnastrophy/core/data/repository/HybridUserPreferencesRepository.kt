package com.github.warnastrophy.core.data.repository

import android.util.Log
import com.github.warnastrophy.core.data.interfaces.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A repository that combines local and remote data sources to manage user preferences.
 *
 * This repository uses the [UserPreferencesRepositoryLocal] for local storage via Jetpack DataStore
 * and [UserPreferencesRepositoryRemote] for remote storage via Firestore. It synchronizes
 * preferences between the local and remote repositories, ensuring that both are updated
 * consistently. The class handles synchronization between the two data sources and manages fallback
 * mechanisms when the remote source is unavailable.
 *
 * @param local The [UserPreferencesRepositoryLocal] used for local user preference storage.
 * @param remote The [UserPreferencesRepositoryRemote] used for remote user preference storage.
 */
class HybridUserPreferencesRepository(
    private val local: UserPreferencesRepositoryLocal,
    private val remote: UserPreferencesRepositoryRemote
) : UserPreferencesRepository {

  /** Mutex used to ensure safe concurrent access to remote preferences synchronization. */
  private val syncMutex = Mutex()

  /** Flag indicating whether the remote repository is available. */
  private var isRemoteAvailable = true

  private val hybridRepositoryTag = "HybridUserPreferencesRepository"

  /**
   * A [Flow] that emits the current user preferences, first attempting to load from the local
   * repository and then from the remote repository if an error occurs during local retrieval.
   *
   * The remote data is synchronized to the local repository on the first access attempt.
   */
  override val getUserPreferences: Flow<UserPreferences> = flow {
    emitAll(
        local.getUserPreferences
            .onStart { syncRemoteToLocal() }
            .catch { error -> emitAll(remote.getUserPreferences.catch { throw error }) })
  }

  /**
   * Updates both local and remote repositories to set the alert mode preference.
   *
   * @param enabled A Boolean indicating whether the alert mode should be enabled or not.
   */
  override suspend fun setAlertMode(enabled: Boolean) {
    updateBothRepositories { setAlertMode(enabled) }
  }

  /**
   * Updates both local and remote repositories to set the inactivity detection preference.
   *
   * @param enabled A Boolean indicating whether inactivity detection should be enabled or not.
   */
  override suspend fun setInactivityDetection(enabled: Boolean) {
    updateBothRepositories { setInactivityDetection(enabled) }
  }

  /**
   * Updates both local and remote repositories to set the automatic SMS preference.
   *
   * @param enabled A Boolean indicating whether automatic SMS should be enabled or not.
   */
  override suspend fun setAutomaticSms(enabled: Boolean) {
    updateBothRepositories { setAutomaticSms(enabled) }
  }

  override suspend fun setAutomaticCalls(enabled: Boolean) {
    updateBothRepositories { setAutomaticCalls(enabled) }
  }

  /**
   * Updates both local and remote repositories to set the dark mode preference.
   *
   * @param isDark A Boolean indicating whether dark mode should be enabled or not.
   */
  override suspend fun setDarkMode(isDark: Boolean) {
    updateBothRepositories { setDarkMode(isDark) }
  }

  /**
   * Updates both local and remote repositories by applying the provided update function to both.
   *
   * This function attempts to update the local repository and, if the remote repository is
   * available, updates it as well. If the remote update fails, it logs the error and prevents
   * further updates until the remote becomes available again.
   *
   * @param update A suspend function that performs the update on the [UserPreferencesRepository].
   */
  private suspend fun updateBothRepositories(update: suspend UserPreferencesRepository.() -> Unit) {
    local.update()

    val shouldUpdateRemote = syncMutex.withLock { isRemoteAvailable }
    if (!shouldUpdateRemote) return

    if (isRemoteAvailable) {
      try {
        remote.update()
      } catch (e: Exception) {
        syncMutex.withLock { isRemoteAvailable = false }
        Log.w(hybridRepositoryTag, "Remote update failed: ${e.localizedMessage}")
      }
    }
  }

  /**
   * Synchronizes the remote preferences to the local repository.
   *
   * This function fetches the user's preferences from the remote repository and updates the local
   * repository accordingly. The synchronization is protected by a [Mutex] to prevent concurrent
   * access. If the remote repository is unavailable, it logs a warning and marks the remote
   * repository as unavailable.
   */
  private suspend fun syncRemoteToLocal() {
    syncMutex.withLock {
      try {
        remote.getUserPreferences.first().let { remotePrefs ->
          with(local) {
            setAlertMode(remotePrefs.dangerModePreferences.alertMode)
            setInactivityDetection(remotePrefs.dangerModePreferences.inactivityDetection)
            setAutomaticSms(remotePrefs.dangerModePreferences.automaticSms)
            setDarkMode(remotePrefs.themePreferences)
          }
        }
        isRemoteAvailable = true
      } catch (e: Exception) {
        isRemoteAvailable = false
        Log.w(hybridRepositoryTag, "Remote update failed: ${e.localizedMessage}")
      }
    }
  }
}
