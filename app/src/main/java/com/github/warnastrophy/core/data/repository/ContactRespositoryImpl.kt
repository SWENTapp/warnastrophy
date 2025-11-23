package com.github.warnastrophy.core.data.repository

import android.util.Log
import com.github.warnastrophy.core.data.local.StorageException
import com.github.warnastrophy.core.domain.model.Contact
import com.github.warnastrophy.core.util.CryptoUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import java.util.UUID
import kotlinx.coroutines.tasks.await

class ContactsRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ContactsRepository {

  private val gson = Gson()

  private fun doc(userId: String, contactId: String) =
      firestore.collection("users").document(userId).collection("contacts").document(contactId)

  override fun getNewUid(): String = UUID.randomUUID().toString()

  override suspend fun addContact(userId: String, contact: Contact): Result<Unit> = runCatching {
    val cipheredJson =
        try {
          CryptoUtils.encrypt(gson.toJson(contact))
        } catch (e: Exception) {
          Log.e("FirestoreContacts", "Encryption error", e)
          throw StorageException.EncryptionError(e)
        }

    val data = mapOf("encrypted" to cipheredJson)

    try {
      doc(userId, contact.id).set(data).await()
    } catch (e: Exception) {
      Log.e("FirestoreContacts", "Failed to add contact", e)
      throw StorageException.DataStoreError(e)
    }
  }

  override suspend fun getAllContacts(userId: String): Result<List<Contact>> = runCatching {
    val snap =
        try {
          firestore.collection("users").document(userId).collection("contacts").get().await()
        } catch (e: Exception) {
          Log.e("FirestoreContacts", "Failed to get all contacts", e)
          throw StorageException.DataStoreError(e)
        }

    snap.documents.map { doc ->
      val enc =
          doc.getString("encrypted")
              ?: throw StorageException.DataStoreError(Exception("Missing encrypted field"))

      val json =
          try {
            CryptoUtils.decrypt(enc)
          } catch (e: Exception) {
            Log.e("FirestoreContacts", "Decryption error", e)
            throw StorageException.DecryptionError(e)
          }

      try {
        gson.fromJson(json, Contact::class.java)
      } catch (e: Exception) {
        Log.e("FirestoreContacts", "JSON parse error", e)
        throw StorageException.DeserializationError(e)
      }
    }
  }

  override suspend fun getContact(userId: String, contactID: String): Result<Contact> =
      runCatching {
        val docSnap =
            try {
              doc(userId, contactID).get().await()
            } catch (e: Exception) {
              Log.e("FirestoreContacts", "Failed to get contact", e)
              throw StorageException.DataStoreError(e)
            }

        if (!docSnap.exists()) {
          throw StorageException.DataStoreError(Exception("Contact not found"))
        }

        val enc =
            docSnap.getString("encrypted")
                ?: throw StorageException.DataStoreError(Exception("Invalid Firestore entry"))

        val json =
            try {
              CryptoUtils.decrypt(enc)
            } catch (e: Exception) {
              Log.e("FirestoreContacts", "Decrypt error", e)
              throw StorageException.DecryptionError(e)
            }

        try {
          gson.fromJson(json, Contact::class.java)
        } catch (e: Exception) {
          Log.e("FirestoreContacts", "JSON parse error", e)
          throw StorageException.DeserializationError(e)
        }
      }

  override suspend fun editContact(
      userId: String,
      contactID: String,
      newContact: Contact
  ): Result<Unit> = runCatching {
    if (contactID != newContact.id) throw StorageException.DataStoreError(Exception("ID mismatch"))

    val ciphered =
        try {
          CryptoUtils.encrypt(gson.toJson(newContact))
        } catch (e: Exception) {
          Log.e("FirestoreContacts", "Encryption failed", e)
          throw StorageException.EncryptionError(e)
        }

    try {
      doc(userId, contactID).set(mapOf("encrypted" to ciphered)).await()
    } catch (e: Exception) {
      Log.e("FirestoreContacts", "Failed to edit contact", e)
      throw StorageException.DataStoreError(e)
    }
  }

  override suspend fun deleteContact(userId: String, contactID: String): Result<Unit> =
      runCatching {
        try {
          doc(userId, contactID).delete().await()
        } catch (e: Exception) {
          Log.e("FirestoreContacts", "Failed to delete contact", e)
          throw StorageException.DataStoreError(e)
        }
      }
}
