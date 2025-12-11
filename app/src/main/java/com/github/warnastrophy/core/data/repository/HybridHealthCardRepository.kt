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

class HybridHealthCardRepository(
    private val local: HealthCardRepository,
    private val remote: HealthCardRepository,
) : HealthCardRepository {

  private val syncMutex = Mutex()

  private var isRemoteAvailable = true

  private val hybridRepositoryTag = "HybridHealthCardRepository"

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

  override suspend fun upsertMyHealthCard(card: HealthCard) {
    updateBothRepositories(
        localOperation = { local.upsertMyHealthCard(card) },
        remoteOperation = { remote.upsertMyHealthCard(card) })
  }

  override suspend fun deleteMyHealthCard() {
    updateBothRepositories(
        localOperation = { local.deleteMyHealthCard() },
        remoteOperation = { remote.deleteMyHealthCard() })
  }

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
      Log.d(hybridRepositoryTag, "Skipping remote update - remote marked unavailable")
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
