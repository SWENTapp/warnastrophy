package com.github.warnastrophy.core.ui.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.github.warnastrophy.core.model.contact.Contact
import com.github.warnastrophy.core.model.contact.ContactsRepositoryLocal
import com.github.warnastrophy.core.model.contact.contactDataStore
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContactsRepositoryLocalTests {
  private lateinit var repositoryLocal: ContactsRepositoryLocal
  private lateinit var datastore: DataStore<Preferences>

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    datastore = context.contactDataStore
    repositoryLocal = ContactsRepositoryLocal(datastore)
  }

  @Test
  fun `identity test`() {
    val contact = Contact("1", "Alice", "1234567890", "Sister")

    runBlocking {
      assert(repositoryLocal.saveContact(contact).isSuccess)
      val r = repositoryLocal.readContact("1")
      assert(r.isSuccess)
      assertEquals(contact, r.getOrNull())
    }
  }

  @Test
  fun `saving duplicate id should fail`() {
    val contact1 = Contact("2", "Bob", "9876543210", "Friend")
    val contact2 = Contact("2", "Robert", "1112223333", "Colleague")

    runBlocking {
      assert(repositoryLocal.saveContact(contact1).isSuccess)
      // Attempt to save another contact with the same id
      val result = repositoryLocal.saveContact(contact2)
      assert(result.isFailure)
    }
  }

  @Test
  fun `reading inexisting contact should fail`() {
    runBlocking {
      val result = repositoryLocal.readContact("nonexistent_id")
      assert(result.isFailure)
    }
  }

  @Test
  fun `readContact should fail on corrupted data`() {
    val contact = Contact("corrupt", "Mallory", "0001112222", "Intruder")
    runBlocking {
      // Save a valid contact
      assert(repositoryLocal.saveContact(contact).isSuccess)
      // Corrupt the stored data directly
      datastore.edit {
        val key = repositoryLocal.keyFor(contact)
        it[key] = "not_a_valid_ciphertext"
      }
      // Try to read the corrupted contact
      val result = repositoryLocal.readContact("corrupt")
      assert(result.isFailure)
    }
  }

  // Test: readContact should fail when deciphering succeeds but JSON is invalid
  @Test
  fun `readContact should fail on invalid JSON after decryption`() {
    val contact = Contact("badjson", "Mallory", "0001112222", "Intruder")
    runBlocking {
      // Save a valid contact
      assert(repositoryLocal.saveContact(contact).isSuccess)
      // Overwrite with valid ciphertext but invalid JSON
      datastore.edit {
        val key = repositoryLocal.keyFor(contact)
        it[key] = com.github.warnastrophy.core.model.util.CryptoUtils.encrypt("not_a_valid_json")
      }
      // Try to read the corrupted contact
      val result = repositoryLocal.readContact("badjson")
      assert(result.isFailure)
    }
  }

  @Test
  fun `update contact`() {
    val contact = Contact("3", "Charlie", "5556667777", "Neighbor")
    val updatedContact = contact.copy(phoneNumber = "9998887777")
    runBlocking {
      assert(repositoryLocal.saveContact(contact).isSuccess)
      assert(repositoryLocal.updateContact(updatedContact).isSuccess)
      val r = repositoryLocal.readContact("3")
      assert(r.isSuccess)
      assertEquals(updatedContact, r.getOrNull())
    }
  }

  @Test
  fun `update unexisting contact should fail`() {
    val contact = Contact("4", "Diana", "4445556666", "Aunt")
    runBlocking {
      val result = repositoryLocal.updateContact(contact)
      assert(result.isFailure)
    }
  }

  @Test
  fun `delete contact`() {
    val contact = Contact("5", "Eve", "2223334444", "Cousin")
    runBlocking {
      assert(repositoryLocal.saveContact(contact).isSuccess)
      assert(repositoryLocal.deleteContactInternal("5").isSuccess)
      val r = repositoryLocal.readContact("5")
      assert(r.isFailure)
    }
  }

  @Test
  fun `delete unexisting contact should fail`() {
    runBlocking {
      val result = repositoryLocal.deleteContactInternal("nonexistent_id")
      assert(result.isFailure)
    }
  }

  /* ContactsRepository interface implementation tests */
  @Test
  fun `ContactsRepository interface implementation test`() {
    val contact = Contact("6", "Frank", "7778889999", "Uncle")

    runBlocking {
      repositoryLocal.addContact(contact)
      val fetched = repositoryLocal.getContact("6")
      assertEquals(contact, fetched)
    }
  }

  @Test
  fun `getContact with invalid id should throw exception`() {
    assertThrows(Exception::class.java) { runBlocking { repositoryLocal.getContact("invalid_id") } }
  }

  @Test
  fun `getAllContacts returns all saved contacts`() {
    val contact1 = Contact("7", "Grace", "1231231234", "Sister")
    val contact2 = Contact("8", "Heidi", "4564564567", "Friend")

    runBlocking {
      repositoryLocal.addContact(contact1)
      repositoryLocal.addContact(contact2)
      val allContacts = repositoryLocal.getAllContacts()
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
      assertThrows(Exception::class.java) { runBlocking { repositoryLocal.getContact("9") } }

      // Deleting again should throw
      assertThrows(Exception::class.java) { runBlocking { repositoryLocal.deleteContact("9") } }
    }
  }

  @Test
  fun `editContact with mismatched id should throw exception`() {
    val contact = Contact("10", "Judy", "6546547654", "Neighbor")
    val updatedContact = contact.copy(id = "different_id")

    runBlocking {
      repositoryLocal.addContact(contact)
      assertThrows(Exception::class.java) {
        runBlocking { repositoryLocal.editContact("10", updatedContact) }
      }
    }
  }
}
