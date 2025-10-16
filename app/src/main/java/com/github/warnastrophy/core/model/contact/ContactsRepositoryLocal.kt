package com.github.warnastrophy.core.model.contact

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.warnastrophy.core.model.util.CryptoUtils
import com.github.warnastrophy.core.model.util.StorageException
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

const val CONTACTS_DATASTORE_NAME = "contacts_encrypted"

val gson = Gson()

val Context.contactDataStore: DataStore<Preferences> by
    preferencesDataStore(name = CONTACTS_DATASTORE_NAME)

/*
  TODO: This needs refactoring to only have one public method per operation
   ideally returning Result<T> instead of throwing exceptions.
   This will make the errors easier to handle in UI.
*/

/**
 * A utility class for managing contact storage operations.
 *
 * This class provides CRUD for contact information while encrypting sensitive data.
 */
class ContactsRepositoryLocal(private val dataStore: DataStore<Preferences>) : ContactsRepository {
  fun keyFor(contact: Contact) = stringPreferencesKey(contact.id)

  /**
   * Adds a new contact to the DataStore.
   *
   * @param contact The contact to add.
   * @throws Exception if saving fails or the contact already exists.
   */
  override suspend fun addContact(contact: Contact) {
    val result = saveContact(contact)
    if (result.isFailure) throw result.exceptionOrNull()!!
  }

  /**
   * Generates a new unique identifier for a contact.
   *
   * @return A new UUID string.
   */
  override fun getNewUid(): String {
    return java.util.UUID.randomUUID().toString()
  }

  /**
   * Retrieves all contacts from the DataStore.
   *
   * @return A list of all stored contacts.
   */
  override suspend fun getAllContacts(): List<Contact> {
    val prefs = dataStore.data.first()
    val contacts = mutableListOf<Contact>()
    for ((_, value) in prefs.asMap()) {
      try {
        val decrypted = CryptoUtils.decrypt(value as String)
        val contact = gson.fromJson(decrypted, Contact::class.java)
        contacts.add(contact)
      } catch (e: Exception) {
        throw StorageException.DataStoreError(e)
      }
    }
    return contacts
  }

  /**
   * Retrieves a contact by its ID.
   *
   * @param contactID The ID of the contact to retrieve.
   * @return The contact with the specified ID.
   * @throws Exception if the contact is not found or cannot be read.
   */
  override suspend fun getContact(contactID: String): Contact {
    val result = readContact(contactID)
    if (result.isSuccess) return result.getOrNull()!!
    throw result.exceptionOrNull()!!
  }

  /**
   * Edits an existing contact.
   *
   * @param contactID The ID of the contact to edit.
   * @param newContact The new contact data.
   * @throws Exception if the contact ID does not match or update fails.
   */
  override suspend fun editContact(contactID: String, newContact: Contact) {
    if (contactID != newContact.id) {
      throw StorageException.DataStoreError(Exception("Contact ID mismatch"))
    }
    val result = updateContact(newContact)
    if (result.isFailure) throw result.exceptionOrNull()!!
  }

  /**
   * Deletes a contact by its ID.
   *
   * @param contactID The ID of the contact to delete.
   * @throws Exception if the contact does not exist or deletion fails.
   */
  override suspend fun deleteContact(contactID: String) {
    val result = deleteContactInternal(contactID)
    if (result.isFailure) throw result.exceptionOrNull()!!
  }

  /* Helper methods for CRUD operations with error handling */

  /**
   * Helper method to delete a contact from the DataStore.
   *
   * @param id The ID of the contact to delete.
   * @return Result of the operation.
   */
  suspend fun deleteContactInternal(id: String): Result<Unit> {
    val key = stringPreferencesKey(id)
    var r = Result.success(Unit)
    dataStore.edit {
      if (!it.contains(key)) {
        r =
            Result.failure(
                StorageException.DataStoreError(
                    Exception("Delete failed: contact $id does not exist")))
        return@edit
      }
      it.remove(key)
      r = Result.success(Unit)
    }
    return r
  }

  /**
   * Saves a new contact to the DataStore.
   *
   * @param contact The contact to save.
   * @return Result of the operation.
   */
  suspend fun saveContact(contact: Contact): Result<Unit> {
    var r = Result.success(Unit)
    try {
      dataStore.edit {
        if (it.contains(keyFor(contact))) {
          r =
              Result.failure(
                  StorageException.DataStoreError(
                      Exception("Contact ${contact.id} already exists")))
          return@edit
        }
        val ciphered = CryptoUtils.encrypt(gson.toJson(contact))
        it[keyFor(contact)] = ciphered
        r = Result.success(Unit)
      }
    } catch (e: Exception) {
      Log.e("ContactStorage", "Error saving contact ${contact.id}", e)
      r = Result.failure(StorageException.DataStoreError(e))
    }
    return r
  }

  /**
   * Reads a contact from the DataStore by its ID.
   *
   * @param id The ID of the contact to read.
   * @return Result containing the contact or an error.
   */
  suspend fun readContact(id: String): Result<Contact> {
    val key = stringPreferencesKey(id)
    val ciphered =
        try {
          dataStore.data.map { it[key] }.first()
        } catch (e: ClassCastException) {
          Log.e("ContactStorage", "Failed to load ciphered JSON for $id", e)
          return Result.failure(StorageException.DataStoreError(e))
        }
    if (ciphered == null) {
      Log.e("ContactStorage", "Contact $id not found")
      return Result.failure(StorageException.DataStoreError(Exception("Contact $id not found")))
    }
    val deciphered =
        try {
          CryptoUtils.decrypt(ciphered)
        } catch (e: Exception) {
          Log.e("ContactStorage", "Failed to decipher contact $id", e)
          return Result.failure(StorageException.DecryptionError(e))
        }
    val contact =
        try {
          gson.fromJson(deciphered, Contact::class.java)
        } catch (e: Exception) {
          Log.e("ContactStorage", "Failed to parse JSON for contact $id", e)
          return Result.failure(StorageException.DeserializationError(e))
        }
    return Result.success(contact)
  }

  /**
   * Updates an existing contact in the DataStore.
   *
   * @param contact The contact with updated data.
   * @return Result of the operation.
   */
  suspend fun updateContact(contact: Contact): Result<Unit> {
    var r: Result<Unit> = Result.success(Unit)
    dataStore.edit {
      if (!it.contains(keyFor(contact))) {
        r =
            Result.failure(
                StorageException.DataStoreError(
                    Exception("Edit failed: contact ${contact.id} does not exist")))
        return@edit
      }
      it[keyFor(contact)] =
          try {
            val ciphered = CryptoUtils.encrypt(gson.toJson(contact))
            r = Result.success(Unit)
            ciphered
          } catch (e: Exception) {
            Log.e("ContactStorage", "Error encrypting contact", e)
            r = Result.failure(StorageException.EncryptionError(e))
            return@edit
          }
    }
    return r
  }
}
