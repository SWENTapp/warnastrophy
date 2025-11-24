package com.github.warnastrophy.core.data.repository

import com.github.warnastrophy.core.data.interfaces.ContactsRepository
import com.github.warnastrophy.core.model.Contact
import java.util.UUID
import kotlin.collections.containsKey
import kotlin.text.set

/** A mock implementation of the ContactsRepository for testing purposes. */
class MockContactRepository : ContactsRepository {

  private val contactsByUser = mutableMapOf<String, MutableMap<String, Contact>>()

  var shouldThrowException: Boolean = false

  private fun bucket(userId: String): MutableMap<String, Contact> =
      contactsByUser.getOrPut(userId) { mutableMapOf() }

  override fun getNewUid(): String = UUID.randomUUID().toString()

  override suspend fun addContact(userId: String, contact: Contact): Result<Unit> = runCatching {
    val userMap = bucket(userId)
    require(!userMap.containsKey(contact.id)) {
      "Contact ${contact.id} already exists for user $userId"
    }
    userMap[contact.id] = contact
  }

  override suspend fun getAllContacts(userId: String): Result<List<Contact>> = runCatching {
    if (shouldThrowException) throw Exception("Forced failure")
    bucket(userId).values.toList()
  }

  override suspend fun deleteContact(userId: String, contactID: String): Result<Unit> =
      runCatching {
        val userMap = bucket(userId)
        if (userMap.remove(contactID) == null) {
          throw NoSuchElementException("Contact $contactID not found for user $userId")
        }
      }

  override suspend fun editContact(
      userId: String,
      contactID: String,
      newContact: Contact
  ): Result<Unit> = runCatching {
    require(contactID == newContact.id) { "Contact ID mismatch" }
    val userMap = bucket(userId)
    if (!userMap.containsKey(contactID)) {
      throw NoSuchElementException("Contact $contactID not found for user $userId")
    }
    userMap[contactID] = newContact
  }

  override suspend fun getContact(userId: String, contactID: String): Result<Contact> =
      runCatching {
        bucket(userId)[contactID]
            ?: throw NoSuchElementException("Contact $contactID not found for user $userId")
      }
}
