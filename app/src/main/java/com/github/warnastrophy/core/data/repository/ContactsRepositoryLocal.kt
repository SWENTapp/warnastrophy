package com.github.warnastrophy.core.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.warnastrophy.core.data.local.StorageException
import com.github.warnastrophy.core.model.Contact
import com.github.warnastrophy.core.util.CryptoUtils
import com.google.gson.Gson
import java.util.UUID
import kotlin.collections.iterator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

const val CONTACTS_DATASTORE_NAME = "contacts_encrypted"

val gson = Gson()

val Context.contactDataStore: DataStore<Preferences> by
    preferencesDataStore(name = CONTACTS_DATASTORE_NAME)

class ContactsRepositoryLocal(private val dataStore: DataStore<Preferences>) : ContactsRepository {
  fun keyFor(contact: Contact) = stringPreferencesKey(contact.id)

  override fun getNewUid(): String {
    return UUID.randomUUID().toString()
  }

  override suspend fun addContact(contact: Contact): Result<Unit> {
    return runCatching {
      dataStore.edit {
        if (it.contains(keyFor(contact))) {
          throw StorageException.DataStoreError(Exception("Contact ${contact.id} already exists"))
        } else {
          val ciphered = CryptoUtils.encrypt(gson.toJson(contact))
          it[keyFor(contact)] = ciphered
        }
      }
    }
  }

  override suspend fun getAllContacts(): Result<List<Contact>> = runCatching {
    val prefs = dataStore.data.first()
    prefs.asMap().values.map { value ->
      val stringValue =
          value as? String
              ?: throw StorageException.DataStoreError(Exception("Invalid contact entry"))

      val decrypted = CryptoUtils.decrypt(stringValue)
      gson.fromJson(decrypted, Contact::class.java)
    }
  }

  override suspend fun getContact(contactID: String): Result<Contact> {
    val key = stringPreferencesKey(contactID)

    return runCatching {
      val ciphered =
          dataStore.data.map { it[key] as? String }.firstOrNull()
              ?: run {
                Log.e("ContactStorage", "Contact $contactID not found")
                throw StorageException.DataStoreError(Exception("Contact $contactID not found"))
              }

      val deciphered =
          try {
            CryptoUtils.decrypt(ciphered)
          } catch (e: Exception) {
            Log.e("ContactStorage", "Failed to decipher contact $contactID", e)
            throw StorageException.DecryptionError(e)
          }

      try {
        gson.fromJson(deciphered, Contact::class.java)
      } catch (e: Exception) {
        Log.e("ContactStorage", "Failed to parse JSON for contact $contactID", e)
        throw StorageException.DeserializationError(e)
      }
    }
  }

  override suspend fun editContact(contactID: String, newContact: Contact): Result<Unit> {
    return runCatching {
      if (contactID != newContact.id) {
        throw StorageException.DataStoreError(Exception("Contact ID mismatch"))
      }

      dataStore.edit {
        if (!it.contains(keyFor(newContact))) {
          throw StorageException.DataStoreError(
              Exception("Edit failed: contact $contactID does not exist"))
        }

        it[keyFor(newContact)] =
            try {
              val ciphered = CryptoUtils.encrypt(gson.toJson(newContact))
              ciphered
            } catch (e: Exception) {
              Log.e("ContactStorage", "Error encrypting contact", e)
              throw StorageException.EncryptionError(e)
            }
      }
    }
  }

  override suspend fun deleteContact(contactID: String): Result<Unit> {
    val key = stringPreferencesKey(contactID)

    return runCatching {
      dataStore.edit {
        if (!it.contains(key)) {
          throw StorageException.DataStoreError(
              Exception("Delete failed: contact $contactID does not exist"))
        }
        it.remove(key)
      }
    }
  }
}
