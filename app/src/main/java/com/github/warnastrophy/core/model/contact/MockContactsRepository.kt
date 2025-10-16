package com.github.warnastrophy.core.model.contact

/** The purpose of this class is testing contact UI */
class MockContactsRepository : ContactsRepository {
  val mockContacts =
      listOf(
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
    TODO("Not yet implemented")
  }

  override fun getNewUid(): String {
    TODO("Not yet implemented")
  }

  override suspend fun getAllContacts(): List<Contact> {
    return mockContacts
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
