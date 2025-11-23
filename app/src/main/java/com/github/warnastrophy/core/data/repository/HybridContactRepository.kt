package com.github.warnastrophy.core.data.repository

import android.util.Log
import com.github.warnastrophy.core.domain.model.Contact
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HybridContactRepository(
    private val local: ContactsRepositoryLocal,
    private val remote: ContactsRepositoryImpl,
) : ContactsRepository {

  private val ioScope = CoroutineScope(Dispatchers.IO)

  override fun getNewUid(): String = remote.getNewUid()

  override suspend fun getAllContacts(userId: String): Result<List<Contact>> {

    val localResult = local.getAllContacts(userId)

    ioScope.launch { syncFromRemote(userId) }

    return localResult
  }

  override suspend fun getContact(userId: String, contactID: String): Result<Contact> {

    val localFound = local.getContact(userId, contactID)
    if (localFound.isSuccess) return localFound

    val remoteFound = remote.getContact(userId, contactID)

    remoteFound.onSuccess { remoteContact ->
      ioScope.launch { local.addContact(userId, remoteContact) }
    }

    return remoteFound
  }

  override suspend fun addContact(userId: String, contact: Contact): Result<Unit> {
    val localAdd = local.addContact(userId, contact)
    if (localAdd.isFailure) return localAdd

    ioScope.launch {
      remote.addContact(userId, contact).onFailure { Log.e("Hybrid", "Failed remote add", it) }
    }

    return localAdd
  }

  override suspend fun editContact(
      userId: String,
      contactID: String,
      newContact: Contact
  ): Result<Unit> {
    val localEdit = local.editContact(userId, contactID, newContact)
    if (localEdit.isFailure) return localEdit

    ioScope.launch {
      remote.editContact(userId, contactID, newContact).onFailure {
        Log.e("Hybrid", "Failed remote edit", it)
      }
    }

    return localEdit
  }

  override suspend fun deleteContact(userId: String, contactID: String): Result<Unit> {
    val localDelete = local.deleteContact(userId, contactID)
    if (localDelete.isFailure) return localDelete

    ioScope.launch {
      remote.deleteContact(userId, contactID).onFailure {
        Log.e("Hybrid", "Failed remote delete", it)
      }
    }

    return localDelete
  }

  private suspend fun syncFromRemote(userId: String) {
    val remoteResult = remote.getAllContacts(userId)
    if (remoteResult.isFailure) return

    val remoteContacts = remoteResult.getOrThrow()
    val localContacts = local.getAllContacts(userId).getOrDefault(emptyList())

    val localMap = localContacts.associateBy { it.id }

    for (remoteContact in remoteContacts) {
      if (remoteContact.id !in localMap) {
        local.addContact(userId, remoteContact)
      }
    }
  }
}
