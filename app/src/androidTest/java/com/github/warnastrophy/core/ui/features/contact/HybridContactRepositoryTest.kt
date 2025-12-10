package com.github.warnastrophy.core.ui.features.contact

import com.github.warnastrophy.core.data.interfaces.ContactsRepository
import com.github.warnastrophy.core.data.repository.HybridContactRepository
import com.github.warnastrophy.core.model.Contact
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

  @Test
  fun getNewUid() = runTest {
    // Simulate remote returning a new UID
    coEvery { remote.getNewUid() } returns "new-uid"

    val result = hybrid.getNewUid()

    // Assert that the returned UID matches the expected value
    assertTrue(result == "new-uid")
  }

  @Test
  fun get_new_uid_should_return_uid_from_remote() = runTest {
    coEvery { remote.getNewUid() } returns "new-uid"

    val result = hybrid.getNewUid()

    assertTrue(result == "new-uid")
  }

  // Test for addContact
  @Test
  fun add_contact_should_add_contact_to_local_and_remote() = runTest {
    coEvery { local.addContact(userId, contact) } returns Result.success(Unit)

    val result = hybrid.addContact(userId, contact)

    assertTrue(result.isSuccess)
    coVerify { local.addContact(userId, contact) }
    coVerify(exactly = 1) { remote.addContact(userId, contact) }
  }

  // Test for editContact
  @Test
  fun edit_contact_should_update_local_and_remote_repositories() = runTest {
    coEvery { local.editContact(userId, "c1", contact) } returns Result.success(Unit)

    val result = hybrid.editContact(userId, "c1", contact)

    assertTrue(result.isSuccess)
    coVerify { local.editContact(userId, "c1", contact) }
    coVerify { remote.editContact(userId, "c1", contact) }
  }

  // Test for deleteContact
  @Test
  fun delete_contact_should_delete_contact_from_local_and_remote() = runTest {
    coEvery { local.deleteContact(userId, "c1") } returns Result.success(Unit)

    val result = hybrid.deleteContact(userId, "c1")

    assertTrue(result.isSuccess)
    coVerify { local.deleteContact(userId, "c1") }
    coVerify { remote.deleteContact(userId, "c1") }
  }
}
