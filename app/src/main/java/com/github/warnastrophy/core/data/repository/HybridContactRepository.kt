package com.github.warnastrophy.core.data.repository

import com.github.warnastrophy.core.data.interfaces.ContactsRepository
import com.github.warnastrophy.core.model.Contact

class HybridContactRepository(
    private val local: ContactsRepository,
    private val remote: ContactsRepository,
) : ContactsRepository {

  override fun getNewUid(): String = remote.getNewUid()

  override suspend fun getAllContacts(userId: String): Result<List<Contact>> {
    val remoteList = remote.getAllContacts(userId)

    if (remoteList.isSuccess) {
      val remoteContacts = remoteList.getOrThrow()
      runCatching { localContactsMerge(userId, remoteContacts) }
      return Result.success(remoteContacts)
    }

    return local.getAllContacts(userId)
  }

  override suspend fun getContact(userId: String, contactID: String): Result<Contact> {

    val remoteFound = remote.getContact(userId, contactID)

    if (remoteFound.isSuccess) {
      val contact = remoteFound.getOrThrow()

      runCatching {
        // You can swap to editContact if your local storage requires "upsert" semantics
        local.addContact(userId, contact)
      }
      return Result.success(contact)
    }
    return local.getContact(userId, contactID)
  }

  override suspend fun addContact(userId: String, contact: Contact): Result<Unit> {
    val localAdd = local.addContact(userId, contact)
    if (localAdd.isSuccess) remote.addContact(userId, contact)

    return localAdd
  }

  override suspend fun editContact(
      userId: String,
      contactID: String,
      newContact: Contact
  ): Result<Unit> {
    val localEdit = local.editContact(userId, contactID, newContact)
    if (localEdit.isSuccess) remote.editContact(userId, contactID, newContact)

    return localEdit
  }

  override suspend fun deleteContact(userId: String, contactID: String): Result<Unit> {
    val localDelete = local.deleteContact(userId, contactID)
    if (localDelete.isSuccess) remote.deleteContact(userId, contactID)

    return localDelete
  }

  private suspend fun localContactsMerge(userId: String, remoteContacts: List<Contact>) {
    val localContacts = local.getAllContacts(userId).getOrNull().orEmpty()
    val localIds = localContacts.map { it.id }.toSet()

    for (c in remoteContacts) {
      if (c.id !in localIds) {
        local.addContact(userId, c)
      }
    }
  }
}
