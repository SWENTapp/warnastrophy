package com.github.warnastrophy.core.data.repository

import com.github.warnastrophy.core.domain.model.Contact

class HybridContactRepository(
    private val local: ContactsRepository,
    private val remote: ContactsRepository,
) : ContactsRepository {

  override fun getNewUid(): String = remote.getNewUid()

  override suspend fun getAllContacts(userId: String): Result<List<Contact>> {

    val localList = local.getAllContacts(userId)
    val remoteList = remote.getAllContacts(userId)

    if (remoteList.isSuccess) {
      val remoteContacts = remoteList.getOrThrow()
      localContactsMerge(userId, remoteContacts)
    }

    return localList
  }

  override suspend fun getContact(userId: String, contactID: String): Result<Contact> {

    val localFound = local.getContact(userId, contactID)
    if (localFound.isSuccess) return localFound

    val remoteFound = remote.getContact(userId, contactID)

    if (remoteFound.isSuccess) {
      local.addContact(userId, remoteFound.getOrThrow())
    }
    return remoteFound
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
