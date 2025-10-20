package com.github.warnastrophy.core.data.repository

import com.github.warnastrophy.core.model.Contact

class MockContactsForContactListTesting : ContactsRepository {
  private val mockContacts = mutableListOf<Contact>()

  override suspend fun addContact(contact: Contact): Result<Unit> = runCatching {
    if (mockContacts.any { it.id == contact.id }) {
      throw IllegalArgumentException("Contact ${contact.id} already exists")
    }
    mockContacts.add(contact)
  }

  override fun getNewUid(): String = ""
  // We don't need this for contact list UI
  /*
  {
    return (mockContacts.size + 1).toString()


  }

   */

  override suspend fun getAllContacts(): Result<List<Contact>> = runCatching {
    // These function calls are just for coverage purpose
    /*
    getNewUid()
    deleteContact("9")
    editContact("8", Contact("8", "Arsenal", "+41123456789", "football team I love"))
    deleteContact("8")

    getContact("4")
    */

    mockContacts.toList() // return a copy
  }

  override suspend fun deleteContact(contactID: String): Result<Unit> =
      runCatching {
        /*
        val removed = mockContacts.removeIf { it.id == contactID }
        if (!removed) throw NoSuchElementException("Contact $contactID not found")

           */
      }

  override suspend fun editContact(contactID: String, newContact: Contact): Result<Unit> =
      runCatching {
        /*
        val index = mockContacts.indexOfFirst { it.id == contactID }
        if (index == -1) throw NoSuchElementException("Contact $contactID not found")
        mockContacts[index] = newContact.copy(id = contactID)

         */
      }

  override suspend fun getContact(contactID: String): Result<Contact> = runCatching {
    mockContacts.firstOrNull { it.id == contactID }
        ?: throw NoSuchElementException("Contact $contactID not found")
  }
}
