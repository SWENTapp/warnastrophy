package com.github.warnastrophy.core.model.contact

/** The purpose of this class is testing contact UI */
class MockContactsRepository : ContactsRepository {
  val mockContacts =
      mutableListOf<Contact>(
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

  override suspend fun addContact(contact: Contact) {
    // 1. Check if a contact with the same ID already exists (optional, but good practice)
    if (mockContacts.none { it.id == contact.id }) {
      // 2. If not, add the new contact to the list
      mockContacts.add(contact)
    }
  }

  override fun getNewUid(): String {
    val newId = mockContacts.size + 1
    return newId.toString()
  }

  override suspend fun getAllContacts(): List<Contact> {
    return mockContacts
  }

  override suspend fun deleteContact(contactID: String) {
    mockContacts.removeIf { it.id == contactID }
  }

  override suspend fun editContact(contactID: String, newContact: Contact) {
    // 1. Find the index of the contact to be edited
    val index = mockContacts.indexOfFirst { it.id == contactID }

    if (index != -1) {
      // 2. Replace the old contact object at that index with the new contact object
      // NOTE: We MUST ensure newContact has the SAME ID as contactID to keep the list consistent.
      mockContacts[index] = newContact.copy(id = contactID)
    }
  }

  override suspend fun getContact(contactID: String): Contact {
    // 1. Find the contact by ID, or throw an exception if not found
    return mockContacts.firstOrNull { it.id == contactID }
        ?: throw NoSuchElementException("Contact with ID $contactID not found in mock data.")
  }
}
