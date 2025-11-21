package com.github.warnastrophy.core.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.warnastrophy.core.data.local.StorageException
import com.github.warnastrophy.core.domain.model.Contact
import com.github.warnastrophy.core.util.CryptoUtils
import com.google.gson.Gson
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

const val CONTACTS_DATASTORE_NAME = "contacts_encrypted"

val gson = Gson()

val Context.contactDataStore: DataStore<Preferences> by
    preferencesDataStore(name = CONTACTS_DATASTORE_NAME)

class ContactsRepositoryLocal(private val dataStore: DataStore<Preferences>) : ContactsRepository {

  private fun keyFor(userId: String, contactId: String) =
      stringPreferencesKey("$userId/contacts/$contactId")

  fun keyFor(userId: String, contact: Contact) = keyFor(userId, contact.id)

  override fun getNewUid(): String = UUID.randomUUID().toString()

  override suspend fun addContact(userId: String, contact: Contact): Result<Unit> = runCatching {
    dataStore.edit { prefs ->
      val key = keyFor(userId, contact)
      if (prefs.contains(key)) {
        throw StorageException.DataStoreError(
            Exception("Contact ${contact.id} already exists for user $userId"))
      }
      val ciphered =
          try {
            CryptoUtils.encrypt(gson.toJson(contact))
          } catch (e: Exception) {
            Log.e("ContactStorage", "Error encrypting contact for user $userId", e)
            throw StorageException.EncryptionError(e)
          }
      prefs[key] = ciphered
    }
  }

  override suspend fun getAllContacts(userId: String): Result<List<Contact>> = runCatching {
    val prefs = dataStore.data.first()
    val prefix = "$userId/contacts/"
    prefs
        .asMap()
        .filter { (k, _) -> k.name.startsWith(prefix) }
        .map { (_, value) ->
          val stringValue =
              value as? String
                  ?: throw StorageException.DataStoreError(
                      Exception("Invalid contact entry for user $userId"))

          val decrypted =
              try {
                CryptoUtils.decrypt(stringValue)
              } catch (e: Exception) {
                Log.e("ContactStorage", "Failed to decipher a contact for user $userId", e)
                throw StorageException.DecryptionError(e)
              }

          try {
            gson.fromJson(decrypted, Contact::class.java)
          } catch (e: Exception) {
            Log.e("ContactStorage", "Failed to parse JSON for user $userId", e)
            throw StorageException.DeserializationError(e)
          }
        }
  }

  override suspend fun getContact(userId: String, contactID: String): Result<Contact> {
    val key = keyFor(userId, contactID)
    return runCatching {
      val ciphered =
          dataStore.data.map { it[key] }.firstOrNull()
              ?: run {
                Log.e("ContactStorage", "Contact $contactID not found for user $userId")
                throw StorageException.DataStoreError(
                    Exception("Contact $contactID not found for user $userId"))
              }

      val deciphered =
          try {
            CryptoUtils.decrypt(ciphered)
          } catch (e: Exception) {
            Log.e(
                "ContactStorage",
                "Failed to decipher contact $contactID for user $userId",
                e,
            )
            throw StorageException.DecryptionError(e)
          }

      try {
        gson.fromJson(deciphered, Contact::class.java)
      } catch (e: Exception) {
        Log.e(
            "ContactStorage",
            "Failed to parse JSON for contact $contactID for user $userId",
            e,
        )
        throw StorageException.DeserializationError(e)
      }
    }
  }

  override suspend fun editContact(
      userId: String,
      contactID: String,
      newContact: Contact
  ): Result<Unit> = runCatching {
    if (contactID != newContact.id) {
      throw StorageException.DataStoreError(Exception("Contact ID mismatch"))
    }

    dataStore.edit { prefs ->
      val key = keyFor(userId, newContact)
      if (!prefs.contains(key)) {
        throw StorageException.DataStoreError(
            Exception("Edit failed: contact $contactID does not exist for user $userId"))
      }

      prefs[key] =
          try {
            CryptoUtils.encrypt(gson.toJson(newContact))
          } catch (e: Exception) {
            Log.e("ContactStorage", "Error encrypting contact for user $userId", e)
            throw StorageException.EncryptionError(e)
          }
    }
  }

  override suspend fun deleteContact(userId: String, contactID: String): Result<Unit> {
    val key = keyFor(userId, contactID)
    return runCatching {
      dataStore.edit { prefs ->
        if (!prefs.contains(key)) {
          throw StorageException.DataStoreError(
              Exception("Delete failed: contact $contactID does not exist for user $userId"))
        }
        prefs.remove(key)
      }
    }
  }
}
