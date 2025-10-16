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
 * This class provides CRUD for contact information while encrypting sensitive data.
 */
class ContactStorage(
  private val dataStore: DataStore<Preferences>
) {
  fun keyFor(contact: Contact) = stringPreferencesKey(contact.id)

  /**
   * Saves a contact to the DataStore.
   * Uses the contact's ID as the key, so make sure it's unique and preserved.
   * Fails if a contact with the same ID already exists (expliciuse [updateContact] to modify).
   * @param contact The contact to save.
   * @return
   * - [Result.success] if the contact was saved successfully.
   * - [Result.failure] with a [StorageException] if there was an error.
   */
  suspend fun saveContact(contact: Contact): Result<Unit> {
    var r = Result.success(Unit)
    try {
      dataStore.edit {
        if (it.contains(keyFor(contact))) {
          r = Result.failure(
            StorageException.DataStoreError(
              Exception("Contact ${contact.id} already exists")
            )
          )

          return@edit
        }
        val ciphered = CryptoUtils.encrypt( gson.toJson(contact))

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
   * Retrieves a contact by ID from the DataStore.
   *
   * @param id The ID of the contact to retrieve.
   * @return
   * - [Result.success] with the [Contact] if found and decrypted successfully.
   * - [Result.failure] with a [StorageException] if there was an error.
   */
  suspend fun readContact(id: String): Result<Contact> {
    val key = stringPreferencesKey(id)

    val ciphered = try {
      dataStore.data.map { it[key] }.first()
    } catch (e: ClassCastException) {
      Log.e("ContactStorage", "Failed to load ciphered JSON for $id", e)
      return Result.failure(StorageException.DataStoreError(e))
    }

    if (ciphered == null) {
      Log.e("ContactStorage", "Contact $id not found")
      return Result.failure(
        StorageException.DataStoreError(
          Exception("Contact $id not found")
        )
      )
    }

    val deciphered = try {
      CryptoUtils.decrypt(ciphered)
    } catch (e: Exception) {
      Log.e("ContactStorage", "Failed to decipher contact $id", e)
      return Result.failure(StorageException.DecryptionError(e))
    }

    val contact = try {
      gson.fromJson(deciphered, Contact::class.java)
    } catch (e: Exception) {
      Log.e("ContactStorage", "Failed to parse JSON for contact $id", e)
      return Result.failure(StorageException.DeserializationError(e))
    }

    return Result.success(contact)
  }

  /**
   * Updates an existing contact in the DataStore.
   * Fails if the contact does not exist.
   * @param contact The contact to update.
   * @return
   * - [Result.success] if the contact was updated successfully.
   * - [Result.failure] with a [StorageException] if there was an error.
   */
  suspend fun updateContact(contact: Contact): Result<Unit> {
    var r: Result<Unit> = Result.success(Unit)

    dataStore.edit {
      if (!it.contains(keyFor(contact))) {
        r = Result.failure(
          StorageException.DataStoreError(
            Exception("Edit failed: contact ${contact.id} does not exist")
          )
        )

        return@edit
      }

      it [keyFor(contact)] = try {
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

  /**
   * Deletes a contact by ID from the DataStore.
   * Fails if the contact does not exist.
   * @param id The ID of the contact to delete.
   * @return
   * - [Result.success] if the contact was deleted successfully.
   * - [Result.failure] with a [StorageException] if there was an error.
   */
  suspend fun deleteContact(id: String): Result<Unit> {
    val key = stringPreferencesKey(id)

    var r = Result.success(Unit)

    dataStore.edit {
      if (!it.contains(key)) { // Do not rely on remove's return value, it's undocumented
        r = Result.failure(
          StorageException.DataStoreError(
            Exception("Delete failed: contact $id does not exist")
          )
        )
        return@edit
      }

      it.remove(key)
      r = Result.success(Unit)
    }

    return r
  }
}