package com.github.warnastrophy.core.ui.contact

import com.github.warnastrophy.core.data.interfaces.ContactsRepository
import com.github.warnastrophy.core.data.repository.HybridContactRepository
import com.github.warnastrophy.core.model.Contact
import io.mockk.*
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class HybridContactRepositoryTest {

  private lateinit var local: ContactsRepository
  private lateinit var remote: ContactsRepository
  private lateinit var hybrid: HybridContactRepository

  private val userId = "user123"
  private val contact = Contact("c1", "John", "555", "brother")

  @Before
  fun setup() {
    local = mockk(relaxed = true)
    remote = mockk(relaxed = true)

    hybrid = HybridContactRepository(local, remote)
  }

  @Test
  fun getAllContacts_falls_back_to_local_when_remote_fails() = runTest {
    val localContacts = listOf(contact)

    coEvery { remote.getAllContacts(userId) } returns Result.failure(Exception("network"))
    coEvery { local.getAllContacts(userId) } returns Result.success(localContacts)

    val result = hybrid.getAllContacts(userId)

    assertTrue(result.isSuccess)
    // we can safely inspect the list via getOrNull without weird casts
    assertEquals(localContacts, result.getOrNull())

    coVerify(exactly = 1) { remote.getAllContacts(userId) }
    coVerify(exactly = 1) { local.getAllContacts(userId) }
    // no merge in this path
    coVerify(exactly = 0) { local.addContact(any(), any()) }
  }

  @Test
  fun getContact_uses_remote_when_successful_and_syncs_to_local() = runTest {
    coEvery { remote.getContact(userId, "c1") } returns Result.success(contact)

    val result = hybrid.getContact(userId, "c1")

    assertTrue(result.isSuccess)
    assertEquals(contact, result.getOrNull())

    coVerify(exactly = 1) { remote.getContact(userId, "c1") }
    // remote contact should be synced locally
    coVerify(exactly = 1) { local.addContact(userId, contact) }
    // no local fallback
    coVerify(exactly = 0) { local.getContact(any(), any()) }
  }

  @Test
  fun addContact_writes_local_first() = runTest {
    coEvery { local.addContact(userId, contact) } returns Result.success(Unit)

    val result = hybrid.addContact(userId, contact)

    assertTrue(result.isSuccess)
    coVerify { local.addContact(userId, contact) }
    coVerify(exactly = 1) { remote.addContact(userId, contact) }
  }

  @Test
  fun editContact_updates_local_then_remote() = runTest {
    coEvery { local.editContact(userId, "c1", contact) } returns Result.success(Unit)

    val result = hybrid.editContact(userId, "c1", contact)

    assertTrue(result.isSuccess)
    coVerify { local.editContact(userId, "c1", contact) }
    coVerify { remote.editContact(userId, "c1", contact) }
  }

  @Test
  fun deleteContact_deletes_locally_then_remote() = runTest {
    coEvery { local.deleteContact(userId, "c1") } returns Result.success(Unit)

    val result = hybrid.deleteContact(userId, "c1")

    assertTrue(result.isSuccess)
    coVerify { local.deleteContact(userId, "c1") }
    coVerify { remote.deleteContact(userId, "c1") }
  }
}
