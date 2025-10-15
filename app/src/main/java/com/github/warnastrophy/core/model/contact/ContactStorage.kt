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
import com.github.warnastrophy.core.model.util.StorageResult
import com.github.warnastrophy.core.ui.viewModel.Contact
import com.google.gson.Gson
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

const val CONTACTS_DATASTORE_NAME = "contacts_encrypted"

val gson = Gson()

val Context.contactDataStore: DataStore<Preferences> by
  preferencesDataStore(name = CONTACTS_DATASTORE_NAME)

/**
 * A utility class for managing contact storage operations.
 *
 * This class provides CRUD for contact information while encrypting sensitive data
 */
private class ContactStorage(
  private val dataStore: DataStore<Preferences>
) {
  fun keyFor(contact: Contact) = stringPreferencesKey(contact.id)

  /**
   * Saves a contact to the DataStore.
   * Uses the contact's ID as the key, so make sure it's unique and preserved.
   * @param contact The contact to save.
   * @return
   * - [Result.success] if the contact was saved successfully.
   * - [Result.failure] with a [StorageException] if there was an error
   */
  suspend fun saveContact(contact: Contact): Result<Unit> {
    val encryptedValue = try {
      CryptoUtils.encrypt( gson.toJson(contact))
    } catch (e: Exception) {
      Log.e("ContactStorage", "Error encrypting contact", e)
      return Result.failure(StorageException.EncryptionError(e))
    }

    try {
      dataStore.edit {
        it[keyFor(contact)] = encryptedValue
      }
    } catch (e: Exception) {
      Log.e("ContactStorage", "Error saving contact", e)
      return Result.failure(StorageException.DataStoreError(e))
    }

    return Result.success(Unit)
  }

  /**
   * Retrieves a contact by ID from the DataStore.
   *
   * @param id The ID of the contact to retrieve.
   * @return
   * - [Result.success] with the [Contact] if found and decrypted successfully.
   * - [Result.failure] with a [StorageException] if there was an error
   */
  suspend fun loadContact(id: String): Result<Contact> {
    val key = stringPreferencesKey(id)

    val ciphered = try {
      dataStore.data.map { it[key] }.first()
    } catch (e: ClassCastException) {
      Log.e("ContactStorage", "Failed to load ciphered JSON", e)
      return Result.failure(StorageException.DataStoreError(e))
    }

    if (ciphered == null) {
      Log.e("ContactStorage", "Contact not found for id: $id")
      return Result.failure(
        StorageException.DataStoreError(
          Exception("Contact not found")
        )
      )
    }

    val decrypted = try {
      CryptoUtils.decrypt(ciphered)
    } catch (e: Exception) {
      Log.e("ContactStorage", "Failed to decipher contact with id $id", e)
      return Result.failure(StorageException.DecryptionError(e))
    }

    val contact = try {
      gson.fromJson(decrypted, Contact::class.java)
    } catch (e: Exception) {
      Log.e("ContactStorage", "Failed to parse JSON for contact with id $id", e)
      return Result.failure(StorageException.DeserializationError(e))
    }

    return Result.success(contact)
  }
}