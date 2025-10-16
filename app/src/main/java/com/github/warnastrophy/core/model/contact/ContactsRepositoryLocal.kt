package com.github.warnastrophy.core.model.contact

class ContactsRepositoryLocal : ContactsRepository {

  override suspend fun addContact(contact: Contact) {
    TODO("Not yet implemented")
  }

  override fun getNewUid(): String {
    TODO("Not yet implemented")
  }

  override suspend fun getAllContacts(): List<Contact> {
    TODO("Not yet implemented")
  }

  override suspend fun deleteContact(contactID: String) {
    TODO("Not yet implemented")
  }

  override suspend fun editContact(contactID: String, newContact: Contact) {
    TODO("Not yet implemented")
  }

  override suspend fun getContact(contactID: String): Contact {
    TODO("Not yet implemented")
  }
}
