package com.github.warnastrophy.core.ui.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.github.warnastrophy.core.data.localStorage.ContactsStorage
import com.github.warnastrophy.core.data.localStorage.StorageException
import com.github.warnastrophy.core.data.localStorage.contactDataStore
import com.github.warnastrophy.core.model.Contact
import com.github.warnastrophy.core.util.CryptoUtils
import io.mockk.coEvery
import io.mockk.mockkObject
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContactsRepositoryLocalTests {
  companion object {
    private const val TEST_USER_ID = "test_user"
  }

  private lateinit var repositoryLocal: ContactsStorage
  private lateinit var datastore: DataStore<Preferences>

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    datastore = context.contactDataStore
    repositoryLocal = ContactsStorage(datastore)
    mockkObject(CryptoUtils)
    coEvery { CryptoUtils.encrypt(any()) } answers { "ENC(${firstArg<String>()})" }
    coEvery { CryptoUtils.decrypt(any()) } answers
        {
          val txt = firstArg<String>()
          if (!txt.startsWith("ENC(")) throw Exception("bad cipher")
          txt.removePrefix("ENC(").removeSuffix(")")
        }
  }

  @Test
  fun `identity test`() {
    val contact = Contact("1", "Alice", "1234567890", "Sister")

    runBlocking {
      assert(repositoryLocal.addContact(userId = TEST_USER_ID, contact = contact).isSuccess)
      val r = repositoryLocal.getContact(userId = TEST_USER_ID, contactID = "1")
      assert(r.isSuccess)
      assertEquals(contact, r.getOrNull())
    }
  }

  @Test
  fun `saving duplicate id should fail`() {
    val contact1 = Contact("2", "Bob", "9876543210", "Friend")
    val contact2 = Contact("2", "Robert", "1112223333", "Colleague")

    runBlocking {
      assert(repositoryLocal.addContact(userId = TEST_USER_ID, contact = contact1).isSuccess)
      // Attempt to save another contact with the same id
      val result = repositoryLocal.addContact(userId = TEST_USER_ID, contact = contact2)
      assert(result.isFailure)
    }
  }

  @Test
  fun `reading inexisting contact should fail`() {
    runBlocking {
      val result = repositoryLocal.getContact(userId = TEST_USER_ID, contactID = "nonexistent_id")
      assert(result.isFailure)
    }
  }

  @Test
  fun `readContact should fail on corrupted data`() {
    val contact = Contact("corrupt", "Mallory", "0001112222", "Intruder")
    runBlocking {
      // Save a valid contact
      repositoryLocal.addContact(TEST_USER_ID, contact)

      // Force invalid ciphertext
      datastore.edit {
        val key = repositoryLocal.keyFor(TEST_USER_ID, contact)
        it[key] = "NOT_A_VALID_CIPHER"
      }

      // Try to read the corrupted contact
      val result = repositoryLocal.getContact(userId = TEST_USER_ID, contactID = "corrupt")
      assert(result.isFailure)
      assert(result.exceptionOrNull() is StorageException.DecryptionError)
    }
  }

  @Test
  fun `readContact should fail on invalid JSON after decryption`() {
    val contact = Contact("badjson", "Mallory", "0001112222", "Intruder")
    runBlocking {
      // Save a valid contact
      repositoryLocal.addContact(TEST_USER_ID, contact)
      // Overwrite with valid ciphertext but invalid JSON
      datastore.edit {
        val key = repositoryLocal.keyFor(userId = TEST_USER_ID, contact = contact)
        it[key] = CryptoUtils.encrypt("not_a_valid_json")
      }
      // Try to read the corrupted contact
      val result = repositoryLocal.getContact(userId = TEST_USER_ID, contactID = "badjson")
      assert(result.isFailure)
      assert(result.exceptionOrNull() is StorageException.DeserializationError)
    }
  }

  @Test
  fun `update contact`() {
    val contact = Contact("3", "Charlie", "5556667777", "Neighbor")
    val updatedContact = contact.copy(phoneNumber = "9998887777")
    runBlocking {
      assert(repositoryLocal.addContact(userId = TEST_USER_ID, contact = contact).isSuccess)
      assert(
          repositoryLocal
              .editContact(
                  userId = TEST_USER_ID, contactID = contact.id, newContact = updatedContact)
              .isSuccess)
      val r = repositoryLocal.getContact(userId = TEST_USER_ID, contactID = "3")
      assert(r.isSuccess)
      assertEquals(updatedContact, r.getOrNull())
    }
  }

  @Test
  fun `update unexisting contact should fail`() {
    val contact = Contact("4", "Diana", "4445556666", "Aunt")
    runBlocking {
      val result =
          repositoryLocal.editContact(
              userId = TEST_USER_ID, contactID = contact.id, newContact = contact)
      assert(result.isFailure)
    }
  }

  @Test
  fun `delete contact`() {
    val contact = Contact("5", "Eve", "2223334444", "Cousin")
    runBlocking {
      assert(repositoryLocal.addContact(userId = TEST_USER_ID, contact = contact).isSuccess)
      assert(repositoryLocal.deleteContact(userId = TEST_USER_ID, contactID = "5").isSuccess)
      assert(repositoryLocal.getContact(userId = TEST_USER_ID, contactID = "5").isFailure)
    }
  }

  @Test
  fun `delete unexisting contact should fail`() {
    runBlocking {
      val result =
          repositoryLocal.deleteContact(userId = TEST_USER_ID, contactID = "nonexistent_id")
      assert(result.isFailure)
    }
  }

  @Test
  fun `ContactsRepository interface implementation test`() {
    val contact = Contact("6", "Frank", "7778889999", "Uncle")

    runBlocking {
      repositoryLocal.addContact(userId = TEST_USER_ID, contact = contact)
      val fetched = repositoryLocal.getContact(userId = TEST_USER_ID, contactID = "6")
      assert(fetched.isSuccess)
      assertEquals(contact, fetched.getOrThrow())
    }
  }

  @Test
  fun `getContact with invalid id should fail`() {
    runBlocking {
      assert(repositoryLocal.getContact(userId = TEST_USER_ID, contactID = "invalid_id").isFailure)
    }
  }

  @Test
  fun `getAllContacts returns all saved contacts`() {
    val contact1 = Contact("7", "Grace", "1231231234", "Sister")
    val contact2 = Contact("8", "Heidi", "4564564567", "Friend")

    runBlocking {
      repositoryLocal.addContact(userId = TEST_USER_ID, contact = contact1)
      repositoryLocal.addContact(userId = TEST_USER_ID, contact = contact2)
      val allContactsRes = repositoryLocal.getAllContacts(userId = TEST_USER_ID)
      assert(allContactsRes.isSuccess)
      val allContacts = allContactsRes.getOrNull()!!
      assert(allContacts.contains(contact1))
      assert(allContacts.contains(contact2))
    }
  }

  @Test
  fun `deleteContact deletes only existing contact`() {
    val contact = Contact("9", "Ivan", "3213214321", "Colleague")

    runBlocking {
      repositoryLocal.addContact(userId = TEST_USER_ID, contact = contact)
      repositoryLocal.deleteContact(userId = TEST_USER_ID, contactID = "9")

      assert(repositoryLocal.getContact(userId = TEST_USER_ID, contactID = "9").isFailure)

      // Deleting again should throw
      assert(repositoryLocal.deleteContact(userId = TEST_USER_ID, contactID = "9").isFailure)
    }
  }

  @Test
  fun `editContact with mismatched id should fail`() {
    val contact = Contact("10", "Judy", "6546547654", "Neighbor")
    val updatedContact = contact.copy(id = "different_id")

    runBlocking {
      repositoryLocal.addContact(userId = TEST_USER_ID, contact = contact)
      assert(
          repositoryLocal
              .editContact(userId = TEST_USER_ID, contactID = "10", newContact = updatedContact)
              .isFailure)
    }
  }
}
