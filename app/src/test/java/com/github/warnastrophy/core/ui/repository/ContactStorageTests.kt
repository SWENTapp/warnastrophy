package com.github.warnastrophy.core.ui.repository

import androidx.test.core.app.ApplicationProvider
import com.github.warnastrophy.core.model.contact.Contact
import com.github.warnastrophy.core.model.contact.ContactsRepositoryLocal
import com.github.warnastrophy.core.model.contact.contactDataStore
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ContactStorageTests {
  private lateinit var contactStorage: ContactsRepositoryLocal

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    contactStorage = ContactsRepositoryLocal(context.contactDataStore)
  }

  @Test
  fun `identity test`() {
    val contact = Contact("1", "Alice", "1234567890", "Sister")

    runBlocking {
      assert(contactStorage.saveContact(contact).isSuccess)
      val r = contactStorage.readContact("1")
      assert(r.isSuccess)
      assertEquals(contact, r.getOrNull())
    }
  }

  @Test
  fun `saving duplicate id should fail`() {
    val contact1 = Contact("2", "Bob", "9876543210", "Friend")
    val contact2 = Contact("2", "Robert", "1112223333", "Colleague")

    runBlocking {
      assert(contactStorage.saveContact(contact1).isSuccess)
      // Attempt to save another contact with the same id
      val result = contactStorage.saveContact(contact2)
      assert(result.isFailure)
    }
  }

  @Test
  fun `reading inexisting contact should fail`() {
    runBlocking {
      val result = contactStorage.readContact("nonexistent_id")
      assert(result.isFailure)
    }
  }

  @Test
  fun `update contact`() {
    val contact = Contact("3", "Charlie", "5556667777", "Neighbor")
    val updatedContact = contact.copy(phoneNumber = "9998887777")
    runBlocking {
      assert(contactStorage.saveContact(contact).isSuccess)
      assert(contactStorage.updateContact(updatedContact).isSuccess)
      val r = contactStorage.readContact("3")
      assert(r.isSuccess)
      assertEquals(updatedContact, r.getOrNull())
    }
  }

  @Test
  fun `update unexisting contact should fail`() {
    val contact = Contact("4", "Diana", "4445556666", "Aunt")
    runBlocking {
      val result = contactStorage.updateContact(contact)
      assert(result.isFailure)
    }
  }
}
