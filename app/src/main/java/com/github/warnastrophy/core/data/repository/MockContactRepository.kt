package com.github.warnastrophy.core.data.repository

import com.github.warnastrophy.core.domain.model.Contact

class MockContactRepository : ContactsRepository {
  private val mockContacts = mutableListOf<Contact>()

  /** If set to true, `getAllContacts()` will throw an exception to simulate a failure. */
  var shouldThrowException = false

  override suspend fun addContact(contact: Contact): Result<Unit> = runCatching {
    if (mockContacts.any { it.id == contact.id }) {
      throw IllegalArgumentException("Contact ${contact.id} already exists")
    }
    mockContacts.add(contact)
  }

  override fun getNewUid(): String = ""

  override suspend fun getAllContacts(): Result<List<Contact>> = runCatching {
    if (shouldThrowException) throw Exception()
    mockContacts.toList() // return a copy
  }

  override suspend fun deleteContact(contactID: String): Result<Unit> = runCatching {
    val index = mockContacts.indexOfFirst { it.id == contactID }
    if (index == -1) throw NoSuchElementException("Contact $contactID not found")
    mockContacts.removeAt(index)
  }

  override suspend fun editContact(contactID: String, newContact: Contact): Result<Unit> =
      runCatching {
        val index = mockContacts.indexOfFirst { it.id == contactID }
        if (index == -1) throw NoSuchElementException("Contact $contactID not found")
        mockContacts[index] = newContact
      }

  override suspend fun getContact(contactID: String): Result<Contact> = runCatching {
    mockContacts.firstOrNull { it.id == contactID }
        ?: throw NoSuchElementException("Contact $contactID not found")
  }
}
