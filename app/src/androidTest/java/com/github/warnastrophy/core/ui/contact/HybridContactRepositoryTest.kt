package com.github.warnastrophy.core.ui.contact

import com.github.warnastrophy.core.data.repository.ContactsRepository
import com.github.warnastrophy.core.data.repository.HybridContactRepository
import com.github.warnastrophy.core.domain.model.Contact
import io.mockk.*
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

  // ----------------------------------------------------
  // GET CONTACT local hit
  // ----------------------------------------------------
  @Test
  fun getContact_returns_local_contact_if_exists() = runTest {
    coEvery { local.getContact(userId, "c1") } returns Result.success(contact)

    val result = hybrid.getContact(userId, "c1")

    assertTrue(result.isSuccess)
    coVerify(exactly = 0) { remote.getContact(any(), any()) }
  }

  @Test
  fun addContact_writes_local_first() = runTest {
    coEvery { local.addContact(userId, contact) } returns Result.success(Unit)

    val result = hybrid.addContact(userId, contact)

    assertTrue(result.isSuccess)
    coVerify { local.addContact(userId, contact) }
    coVerify(exactly = 1) { remote.addContact(userId, contact) }
  }

  // ----------------------------------------------------
  // EDIT CONTACT
  // ----------------------------------------------------
  @Test
  fun editContact_updates_local_then_remote() = runTest {
    coEvery { local.editContact(userId, "c1", contact) } returns Result.success(Unit)

    val result = hybrid.editContact(userId, "c1", contact)

    assertTrue(result.isSuccess)
    coVerify { local.editContact(userId, "c1", contact) }
    coVerify { remote.editContact(userId, "c1", contact) }
  }

  // ----------------------------------------------------
  // DELETE CONTACT
  // ----------------------------------------------------
  @Test
  fun deleteContact_deletes_locally_then_remote() = runTest {
    coEvery { local.deleteContact(userId, "c1") } returns Result.success(Unit)

    val result = hybrid.deleteContact(userId, "c1")

    assertTrue(result.isSuccess)
    coVerify { local.deleteContact(userId, "c1") }
    coVerify { remote.deleteContact(userId, "c1") }
  }
}
