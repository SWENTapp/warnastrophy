package com.github.warnastrophy.core.data.repository

import android.util.Log
import com.github.warnastrophy.core.model.HealthCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A repository that combines both local and remote [HealthCardRepository] sources. It provides
 * seamless access to the user's HealthCard, fetching from local storage and synchronizing with
 * remote storage when necessary.
 *
 * The repository ensures that updates are first attempted locally, followed by remote
 * synchronization when necessary. It provides error handling to switch between sources in case of
 * failures.
 *
 * This repository works by keeping track of whether the remote repository is available. In the
 * event of a failure in accessing the remote data, the repository will rely on the local data and
 * attempt to sync with the remote data when it's available again.
 *
 * @param local The local data source for the HealthCard.
 * @param remote The remote data source for the HealthCard.
 */
class HybridHealthCardRepository(
    private val local: HealthCardRepository,
    private val remote: HealthCardRepository,
) : HealthCardRepository {

  private val syncMutex = Mutex()

  private var isRemoteAvailable = true

  private val hybridRepositoryTag = "HybridHealthCardRepository"

  /**
   * Observes the HealthCard for the current user. It will first attempt to fetch data from the
   * local source and then sync with the remote if necessary. If the local observation fails, the
   * remote observation is attempted.
   *
   * @return A [Flow] emitting the current user's [HealthCard], or null if none exists.
   */
  override fun observeMyHealthCard(): Flow<HealthCard?> = flow {
    emitAll(
        local
            .observeMyHealthCard()
            .onStart { syncRemoteToLocal() }
            .catch { error ->
              Log.e(hybridRepositoryTag, "Local observation failed: ${error.localizedMessage}")
              emitAll(
                  remote.observeMyHealthCard().catch {
                    Log.e(hybridRepositoryTag, "Remote observation failed: ${it.localizedMessage}")
                    throw error
                  })
            })
  }

  /**
   * Fetches the user's HealthCard once, first attempting to retrieve from remote, then falling back
   * to local storage if the remote source is unavailable or the data doesn't exist.
   *
   * @param fromCacheFirst Flag indicating whether to try fetching from the cache first.
   * @return The user's [HealthCard] if available, or null if none exists.
   */
  override suspend fun getMyHealthCardOnce(fromCacheFirst: Boolean): HealthCard? {
    try {
      val remoteCard = remote.getMyHealthCardOnce(fromCacheFirst)
      if (remoteCard != null) {
        runCatching { local.upsertMyHealthCard(remoteCard) }
        syncMutex.withLock { isRemoteAvailable = true }
        return remoteCard
      } else {
        syncMutex.withLock { isRemoteAvailable = true }
      }
    } catch (e: Exception) {
      syncMutex.withLock { isRemoteAvailable = false }
      Log.e(hybridRepositoryTag, "Remote getMyHealthCardOnce failed: ${e.localizedMessage}")
    }

    return try {
      local.getMyHealthCardOnce(fromCacheFirst = true)
    } catch (e: Exception) {
      Log.e(hybridRepositoryTag, "Local getMyHealthCardOnce failed: ${e.localizedMessage}")
      return null
    }
  }

  /**
   * Creates or updates the current user's HealthCard in both local and remote repositories.
   *
   * @param card The [HealthCard] to upsert.
   */
  override suspend fun upsertMyHealthCard(card: HealthCard) {
    updateBothRepositories(
        localOperation = { local.upsertMyHealthCard(card) },
        remoteOperation = { remote.upsertMyHealthCard(card) })
  }

  /** Deletes the current user's HealthCard from both local and remote repositories. */
  override suspend fun deleteMyHealthCard() {
    updateBothRepositories(
        localOperation = { local.deleteMyHealthCard() },
        remoteOperation = { remote.deleteMyHealthCard() })
  }

  /**
   * Attempts to sync the remote data to the local repository. If remote data is available, it will
   * be fetched and saved in the local storage.
   */
  private suspend fun syncRemoteToLocal() {
    try {
      val remoteCard = remote.getMyHealthCardOnce(fromCacheFirst = false)
      if (remoteCard != null) {
        local.upsertMyHealthCard(remoteCard)
      }
    } catch (e: Exception) {
      syncMutex.withLock { isRemoteAvailable = false }
      Log.e(hybridRepositoryTag, "Sync from remote to local failed: ${e.localizedMessage}")
    }
  }

  /**
   * Updates both the local and remote repositories by first applying the local operation, followed
   * by the remote operation if the remote is available.
   *
   * @param localOperation A suspend function representing the local operation.
   * @param remoteOperation A suspend function representing the remote operation.
   */
  private suspend fun updateBothRepositories(
      localOperation: suspend () -> Unit,
      remoteOperation: suspend () -> Unit
  ) {
    try {
      localOperation()
    } catch (e: Exception) {
      Log.e(hybridRepositoryTag, "Local operation failed: ${e.localizedMessage}")
      throw e
    }

    val shouldUpdateRemote = syncMutex.withLock { isRemoteAvailable }
    if (!shouldUpdateRemote) {
      return
    }

    try {
      remoteOperation()
      syncMutex.withLock { isRemoteAvailable = true }
    } catch (e: Exception) {
      syncMutex.withLock { isRemoteAvailable = false }
      Log.e(hybridRepositoryTag, "Remote update failed: ${e.localizedMessage}")
      throw e
    }
  }
}
