package com.github.warnastrophy.core.data.repository

import com.github.warnastrophy.core.model.Contact

class NavigationMockContactRepository : ContactsRepository {
  private val mockContacts =
      mutableListOf(
          Contact("1", "Alice Johnson", "+1234567890", "Family"),
          Contact("2", "Dr. Robert Smith", "+9876543210", "Doctor"),
          Contact("3", "Chloé Dupont", "+41791234567", "Friend"),
          Contact("4", "Emergency Services", "911", "Critical"),
          Contact("5", "Michael Brown", "+447700900000", "Colleague"),
          Contact("6", "Grandma Sue", "+15551234567", "Family"),
          Contact("7", "Mr. Chen", "+8613800000000", "Neighbor"),
          Contact("8", "Security Guard Bob", "+18005551212", "Work"),
          Contact("9", "Zack Taylor", "+12341234123", "Friend"),
          Contact("10", "Yara Habib", "+971501112222", "Family"),
      )

  override suspend fun addContact(contact: Contact): Result<Unit> = runCatching {
    if (mockContacts.any { it.id == contact.id }) {
      throw IllegalArgumentException("Contact ${contact.id} already exists")
    }
    mockContacts.add(contact)
  }

  override fun getNewUid(): String = ""

  override suspend fun getAllContacts(): Result<List<Contact>> = runCatching {
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
