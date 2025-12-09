package com.github.warnastrophy.core.data.repository

import android.util.Log
import com.github.warnastrophy.core.data.interfaces.ContactsRepository
import com.github.warnastrophy.core.model.Contact
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A repository that combines local and remote storage for contacts. It acts as a hybrid repository,
 * first attempting to fetch data from the remote repository and then falling back to the local
 * repository if the remote operation fails.
 *
 * This class ensures synchronization between the local and remote repositories and handles the case
 * where the remote service might be unavailable. It provides methods to get, add, edit, and delete
 * contacts in both the local and remote repositories, ensuring data consistency across both storage
 * sources.
 *
 * @param local The local contacts repository to fetch, store, and modify contacts locally.
 * @param remote The remote contacts repository to fetch, store, and modify contacts remotely.
 */
class HybridContactRepository(
    private val local: ContactsRepository,
    private val remote: ContactsRepository,
) : ContactsRepository {

  private val syncMutex = Mutex()

  private var isRemoteAvailable = true

  private val hybridRepositoryTag = "HybridContactRepository"

  override fun getNewUid(): String = remote.getNewUid()

  override suspend fun getAllContacts(userId: String): Result<List<Contact>> {
    try {
      val remoteResult = remote.getAllContacts(userId)
      if (remoteResult.isSuccess) {
        val remoteContacts = remoteResult.getOrThrow()
        syncRemoteToLocal(userId, remoteContacts)
        syncMutex.withLock { isRemoteAvailable = true }
        return Result.success(remoteContacts)
      }
    } catch (e: Exception) {
      syncMutex.withLock { isRemoteAvailable = false }
      Log.w(hybridRepositoryTag, "Remote getAllContacts failed: ${e.localizedMessage}")
    }

    return local.getAllContacts(userId)
  }

  override suspend fun getContact(userId: String, contactID: String): Result<Contact> {
    try {
      val remoteResult = remote.getContact(userId, contactID)
      if (remoteResult.isSuccess) {
        val contact = remoteResult.getOrThrow()
        runCatching { local.addContact(userId, contact) }
        syncMutex.withLock { isRemoteAvailable = true }
        return Result.success(contact)
      }
    } catch (e: Exception) {
      syncMutex.withLock { isRemoteAvailable = false }
      Log.w(hybridRepositoryTag, "Remote getContact failed: ${e.localizedMessage}")
    }

    return local.getContact(userId, contactID)
  }

  override suspend fun addContact(userId: String, contact: Contact): Result<Unit> {
    return updateBothRepositories(
        localOperation = { local.addContact(userId, contact) },
        remoteOperation = { remote.addContact(userId, contact) })
  }

  override suspend fun editContact(
      userId: String,
      contactID: String,
      newContact: Contact
  ): Result<Unit> {
    return updateBothRepositories(
        localOperation = { local.editContact(userId, contactID, newContact) },
        remoteOperation = { remote.editContact(userId, contactID, newContact) })
  }

  override suspend fun deleteContact(userId: String, contactID: String): Result<Unit> {
    return updateBothRepositories(
        localOperation = { local.deleteContact(userId, contactID) },
        remoteOperation = { remote.deleteContact(userId, contactID) })
  }

  /**
   * Synchronizes the contacts from the remote repository to the local repository. This method will
   * add any contacts in the remote repository that do not exist in the local repository.
   *
   * @param userId The user ID whose contacts need to be synchronized.
   * @param remoteContacts The list of contacts fetched from the remote repository.
   */
  private suspend fun syncRemoteToLocal(userId: String, remoteContacts: List<Contact>) {
    try {
      val localContacts = local.getAllContacts(userId).getOrNull().orEmpty()
      val localIds = localContacts.map { it.id }.toSet()

      for (contact in remoteContacts) {
        if (contact.id !in localIds) {
          local.addContact(userId, contact)
        }
      }
    } catch (e: Exception) {
      Log.w(hybridRepositoryTag, "Sync to local failed: ${e.localizedMessage}")
    }
  }

  /**
   * A utility method that performs an operation on both the local and remote repositories. First,
   * the local repository is updated. If successful, it checks if the remote repository is available
   * and attempts to update it. If the remote operation fails, the local result is returned.
   *
   * @param localOperation A suspending function that performs the local operation.
   * @param remoteOperation A suspending function that performs the remote operation.
   * @return A [Result] indicating success or failure of the operation.
   */
  private suspend fun updateBothRepositories(
      localOperation: suspend () -> Result<Unit>,
      remoteOperation: suspend () -> Result<Unit>
  ): Result<Unit> {

    val localResult = localOperation()

    if (localResult.isFailure) {
      return localResult
    }

    val shouldUpdateRemote = syncMutex.withLock { isRemoteAvailable }
    if (!shouldUpdateRemote) return localResult

    try {
      remoteOperation()
      syncMutex.withLock { isRemoteAvailable = true }
    } catch (e: Exception) {
      syncMutex.withLock { isRemoteAvailable = false }
      Log.w(hybridRepositoryTag, "Remote update failed: ${e.localizedMessage}")
    }

    return localResult
  }
}
