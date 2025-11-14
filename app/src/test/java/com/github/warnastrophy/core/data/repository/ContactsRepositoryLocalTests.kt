package com.github.warnastrophy.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.github.warnastrophy.core.domain.model.Contact
import com.github.warnastrophy.core.util.CryptoUtils
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContactsRepositoryLocalTests {
  private lateinit var repositoryLocal: ContactsRepositoryLocal
  private lateinit var datastore: DataStore<Preferences>

  @Before
  fun etUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    ContactRepositoryProvider.init(context)
    datastore = context.contactDataStore
    repositoryLocal = ContactRepositoryProvider.repository as ContactsRepositoryLocal
  }

  @Test
  fun `identity test`() {
    val contact = Contact("1", "Alice", "1234567890", "Sister")

    runBlocking {
      assert(repositoryLocal.addContact(contact).isSuccess)
      val r = repositoryLocal.getContact("1")
      assert(r.isSuccess)
      TestCase.assertEquals(contact, r.getOrNull())
    }
  }

  @Test
  fun `saving duplicate id should fail`() {
    val contact1 = Contact("2", "Bob", "9876543210", "Friend")
    val contact2 = Contact("2", "Robert", "1112223333", "Colleague")

    runBlocking {
      assert(repositoryLocal.addContact(contact1).isSuccess)
      // Attempt to save another contact with the same id
      val result = repositoryLocal.addContact(contact2)
      assert(result.isFailure)
    }
  }

  @Test
  fun `reading inexisting contact should fail`() {
    runBlocking {
      val result = repositoryLocal.getContact("nonexistent_id")
      assert(result.isFailure)
    }
  }

  @Test
  fun `readContact should fail on corrupted data`() {
    val contact = Contact("corrupt", "Mallory", "0001112222", "Intruder")
    runBlocking {
      // Save a valid contact
      assert(repositoryLocal.addContact(contact).isSuccess)
      // Corrupt the stored data directly
      datastore.edit {
        val key = repositoryLocal.keyFor(contact)
        it[key] = "not_a_valid_ciphertext"
      }
      // Try to read the corrupted contact
      val result = repositoryLocal.getContact("corrupt")
      assert(result.isFailure)
    }
  }

  @Test
  fun `readContact should fail on invalid JSON after decryption`() {
    val contact = Contact("badjson", "Mallory", "0001112222", "Intruder")
    runBlocking {
      // Save a valid contact
      assert(repositoryLocal.addContact(contact).isSuccess)
      // Overwrite with valid ciphertext but invalid JSON
      datastore.edit {
        val key = repositoryLocal.keyFor(contact)
        it[key] = CryptoUtils.encrypt("not_a_valid_json")
      }
      // Try to read the corrupted contact
      val result = repositoryLocal.getContact("badjson")
      assert(result.isFailure)
    }
  }

  @Test
  fun `update contact`() {
    val contact = Contact("3", "Charlie", "5556667777", "Neighbor")
    val updatedContact = contact.copy(phoneNumber = "9998887777")
    runBlocking {
      assert(repositoryLocal.addContact(contact).isSuccess)
      assert(repositoryLocal.editContact(contact.id, updatedContact).isSuccess)
      val r = repositoryLocal.getContact("3")
      assert(r.isSuccess)
      TestCase.assertEquals(updatedContact, r.getOrNull())
    }
  }

  @Test
  fun `update unexisting contact should fail`() {
    val contact = Contact("4", "Diana", "4445556666", "Aunt")
    runBlocking {
      val result = repositoryLocal.editContact(contact.id, contact)
      assert(result.isFailure)
    }
  }

  @Test
  fun `delete contact`() {
    val contact = Contact("5", "Eve", "2223334444", "Cousin")
    runBlocking {
      assert(repositoryLocal.addContact(contact).isSuccess)
      assert(repositoryLocal.deleteContact("5").isSuccess)
      assert(repositoryLocal.getContact("5").isFailure)
    }
  }

  @Test
  fun `delete unexisting contact should fail`() {
    runBlocking {
      val result = repositoryLocal.deleteContact("nonexistent_id")
      assert(result.isFailure)
    }
  }

  @Test
  fun `ContactsRepository interface implementation test`() {
    val contact = Contact("6", "Frank", "7778889999", "Uncle")

    runBlocking {
      repositoryLocal.addContact(contact)
      val fetched = repositoryLocal.getContact("6")
      assert(fetched.isSuccess)
      TestCase.assertEquals(contact, fetched.getOrThrow())
    }
  }

  @Test
  fun `getContact with invalid id should fail`() {
    runBlocking { assert(repositoryLocal.getContact("invalid_id").isFailure) }
  }

  @Test
  fun `getAllContacts returns all saved contacts`() {
    val contact1 = Contact("7", "Grace", "1231231234", "Sister")
    val contact2 = Contact("8", "Heidi", "4564564567", "Friend")

    runBlocking {
      repositoryLocal.addContact(contact1)
      repositoryLocal.addContact(contact2)
      val allContactsRes = repositoryLocal.getAllContacts()
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
      repositoryLocal.addContact(contact)
      repositoryLocal.deleteContact("9")

      assert(repositoryLocal.getContact("9").isFailure)

      // Deleting again should throw
      assert(repositoryLocal.deleteContact("9").isFailure)
    }
  }

  @Test
  fun `editContact with mismatched id should fail`() {
    val contact = Contact("10", "Judy", "6546547654", "Neighbor")
    val updatedContact = contact.copy(id = "different_id")

    runBlocking {
      repositoryLocal.addContact(contact)
      assert(repositoryLocal.editContact("10", updatedContact).isFailure)
    }
  }
}
