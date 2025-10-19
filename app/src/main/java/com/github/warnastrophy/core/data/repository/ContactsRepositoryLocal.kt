package com.github.warnastrophy.core.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.github.warnastrophy.core.model.Contact
import com.github.warnastrophy.core.util.CryptoUtils
import com.github.warnastrophy.core.data.local.StorageException
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import kotlin.collections.iterator

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

  override suspend fun getAllContacts(): Result<List<Contact>> {
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
    return Result.success(contacts)
  }

  override suspend fun getContact(contactID: String): Result<Contact> {
    val key = stringPreferencesKey(contactID)

    val ciphered =
      try {
        dataStore.data.map { it[key] }.first()
      } catch (e: ClassCastException) {
        Log.e("ContactStorage", "Failed to load ciphered JSON for $contactID", e)
        return Result.failure(StorageException.DataStoreError(e))
      }

    if (ciphered == null) {
      Log.e("ContactStorage", "Contact $contactID not found")
      return Result.failure(
        StorageException.DataStoreError(
          Exception("Contact $contactID not found")
        )
      )
    }

    val deciphered =
      try {
        CryptoUtils.decrypt(ciphered)
      } catch (e: Exception) {
        Log.e("ContactStorage", "Failed to decipher contact $contactID", e)
        return Result.failure(StorageException.DecryptionError(e))
      }

    val contact =
      try {
        gson.fromJson(deciphered, Contact::class.java)
      } catch (e: Exception) {
        Log.e("ContactStorage", "Failed to parse JSON for contact $contactID", e)
        return Result.failure(StorageException.DeserializationError(e))
      }

    return Result.success(contact)
  }

  override suspend fun editContact(contactID: String, newContact: Contact): Result<Unit> {
    var r: Result<Unit> = Result.success(Unit)

    if (contactID != newContact.id) {
      return Result.failure(
        StorageException.DataStoreError(
          Exception("Contact ID mismatch")
        )
      )
    }

    dataStore.edit {
      if (!it.contains(keyFor(newContact))) {
        r =
          Result.failure(
            StorageException.DataStoreError(
              Exception("Edit failed: contact $contactID does not exist")))
        return@edit
      }

      it[keyFor(newContact)] =
        try {
          val ciphered = CryptoUtils.encrypt(gson.toJson(newContact))
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

  override suspend fun deleteContact(contactID: String): Result<Unit> {
    val key = stringPreferencesKey(contactID)
    var r = Result.success(Unit)

    dataStore.edit {
      if (!it.contains(key)) {
        r =
          Result.failure(
            StorageException.DataStoreError(
              Exception("Delete failed: contact $contactID does not exist")))
        return@edit
      }

      it.remove(key)
      r = Result.success(Unit)
    }

    return r
  }
}
