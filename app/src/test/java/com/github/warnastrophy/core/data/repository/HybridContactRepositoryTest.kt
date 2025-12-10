package com.github.warnastrophy.core.data.repository

import com.github.warnastrophy.core.data.interfaces.ContactsRepository
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
  private val contact2 = Contact("c2", "Jane", "666", "sister")

  @Before
  fun setup() {
    local = mockk(relaxed = true)
    remote = mockk(relaxed = true)

    hybrid = HybridContactRepository(local, remote)
  }

  @Test
  fun `getNewUid returns uid from remote`() = runTest {
    every { remote.getNewUid() } returns "new-uid-123"

    val result = hybrid.getNewUid()

    assertEquals("new-uid-123", result)
    verify { remote.getNewUid() }
  }

  @Test
  fun `getAllContacts returns remote data and syncs to local when remote is available`() = runTest {
    val remoteContacts = listOf(contact, contact2)
    coEvery { remote.getAllContacts(userId) } returns Result.success(remoteContacts)
    coEvery { local.getAllContacts(userId) } returns Result.success(emptyList())
    coEvery { local.addContact(userId, any()) } returns Result.success(Unit)

    val result = hybrid.getAllContacts(userId)

    assertTrue(result.isSuccess)
    assertEquals(remoteContacts, result.getOrNull())
    coVerify { remote.getAllContacts(userId) }
    coVerify { local.getAllContacts(userId) }
    coVerify { local.addContact(userId, contact) }
    coVerify { local.addContact(userId, contact2) }
  }

  @Test
  fun `getAllContacts falls back to local when remote fails`() = runTest {
    val localContacts = listOf(contact)
    coEvery { remote.getAllContacts(userId) } throws Exception("Network error")
    coEvery { local.getAllContacts(userId) } returns Result.success(localContacts)

    val result = hybrid.getAllContacts(userId)

    assertTrue(result.isSuccess)
    assertEquals(localContacts, result.getOrNull())
    coVerify { remote.getAllContacts(userId) }
    coVerify { local.getAllContacts(userId) }
  }

  @Test
  fun `getAllContacts does not sync contacts that already exist locally`() = runTest {
    val remoteContacts = listOf(contact, contact2)
    val localContacts = listOf(contact)
    coEvery { remote.getAllContacts(userId) } returns Result.success(remoteContacts)
    coEvery { local.getAllContacts(userId) } returns Result.success(localContacts)
    coEvery { local.addContact(userId, any()) } returns Result.success(Unit)

    val result = hybrid.getAllContacts(userId)

    assertTrue(result.isSuccess)
    coVerify { local.addContact(userId, contact2) }
    coVerify(exactly = 0) { local.addContact(userId, contact) }
  }

  @Test
  fun `getAllContacts always tries remote even after previous failure to enable recovery`() =
      runTest {
        val localContacts = listOf(contact)

        coEvery { remote.getAllContacts(userId) } throws Exception("Network error")
        coEvery { local.getAllContacts(userId) } returns Result.success(localContacts)

        val firstResult = hybrid.getAllContacts(userId)
        assertTrue(firstResult.isSuccess)

        coVerify(exactly = 1) { remote.getAllContacts(userId) }

        clearMocks(remote, local, recordedCalls = true, answers = false)

        coEvery { remote.getAllContacts(userId) } throws Exception("Network error")
        coEvery { local.getAllContacts(userId) } returns Result.success(localContacts)

        val result = hybrid.getAllContacts(userId)

        assertTrue(result.isSuccess)
        assertEquals(localContacts, result.getOrNull())
        coVerify(exactly = 1) { remote.getAllContacts(userId) }
        coVerify(exactly = 1) { local.getAllContacts(userId) }
      }

  @Test
  fun `getContact returns remote data and syncs to local when remote is available`() = runTest {
    coEvery { remote.getContact(userId, "c1") } returns Result.success(contact)
    coEvery { local.addContact(userId, contact) } returns Result.success(Unit)

    val result = hybrid.getContact(userId, "c1")

    assertTrue(result.isSuccess)
    assertEquals(contact, result.getOrNull())
    coVerify { remote.getContact(userId, "c1") }
    coVerify { local.addContact(userId, contact) }
  }

  @Test
  fun `getContact falls back to local when remote fails`() = runTest {
    coEvery { remote.getContact(userId, "c1") } throws Exception("Network error")
    coEvery { local.getContact(userId, "c1") } returns Result.success(contact)

    val result = hybrid.getContact(userId, "c1")

    assertTrue(result.isSuccess)
    assertEquals(contact, result.getOrNull())
    coVerify { remote.getContact(userId, "c1") }
    coVerify { local.getContact(userId, "c1") }
  }

  @Test
  fun `getContact continues even if local sync fails`() = runTest {
    coEvery { remote.getContact(userId, "c1") } returns Result.success(contact)
    coEvery { local.addContact(userId, contact) } throws Exception("Local write error")

    val result = hybrid.getContact(userId, "c1")

    assertTrue(result.isSuccess)
    assertEquals(contact, result.getOrNull())
  }

  @Test
  fun `addContact writes to local first then remote when both succeed`() = runTest {
    coEvery { local.addContact(userId, contact) } returns Result.success(Unit)
    coEvery { remote.addContact(userId, contact) } returns Result.success(Unit)

    val result = hybrid.addContact(userId, contact)

    assertTrue(result.isSuccess)
    coVerify { local.addContact(userId, contact) }
    coVerify { remote.addContact(userId, contact) }
  }

  @Test
  fun `addContact returns failure when local operation fails`() = runTest {
    coEvery { local.addContact(userId, contact) } returns Result.failure(Exception("Local error"))

    val result = hybrid.addContact(userId, contact)

    assertTrue(result.isFailure)
    coVerify { local.addContact(userId, contact) }
    coVerify(exactly = 0) { remote.addContact(userId, contact) }
  }

  @Test
  fun `addContact returns failure when remote fails`() = runTest {
    val remoteException = Exception("Remote error")
    coEvery { local.addContact(userId, contact) } returns Result.success(Unit)
    coEvery { remote.addContact(userId, contact) } returns Result.failure(remoteException)

    val result = hybrid.addContact(userId, contact)

    assertTrue(result.isFailure)
    assertEquals(remoteException, result.exceptionOrNull())
    coVerify { local.addContact(userId, contact) }
    coVerify { remote.addContact(userId, contact) }
  }

  @Test
  fun `addContact skips remote after remote becomes unavailable`() = runTest {
    val remoteException = Exception("Remote error")
    coEvery { local.addContact(userId, contact) } returns Result.success(Unit)
    coEvery { remote.addContact(userId, contact) } returns Result.failure(remoteException)

    val firstResult = hybrid.addContact(userId, contact)
    assertTrue(firstResult.isFailure)

    coVerify(exactly = 1) { remote.addContact(userId, contact) }

    clearMocks(remote, local, recordedCalls = true, answers = false)

    coEvery { local.addContact(userId, contact2) } returns Result.success(Unit)
    coEvery { remote.addContact(userId, contact2) } returns Result.success(Unit)

    val result = hybrid.addContact(userId, contact2)

    assertTrue(result.isSuccess)
    coVerify(exactly = 1) { local.addContact(userId, contact2) }
    coVerify(exactly = 0) { remote.addContact(userId, contact2) }
  }

  @Test
  fun `editContact updates local first then remote when both succeed`() = runTest {
    coEvery { local.editContact(userId, "c1", contact) } returns Result.success(Unit)
    coEvery { remote.editContact(userId, "c1", contact) } returns Result.success(Unit)

    val result = hybrid.editContact(userId, "c1", contact)

    assertTrue(result.isSuccess)
    coVerify { local.editContact(userId, "c1", contact) }
    coVerify { remote.editContact(userId, "c1", contact) }
  }

  @Test
  fun `editContact returns failure when local operation fails`() = runTest {
    coEvery { local.editContact(userId, "c1", contact) } returns
        Result.failure(Exception("Local error"))

    val result = hybrid.editContact(userId, "c1", contact)

    assertTrue(result.isFailure)
    coVerify { local.editContact(userId, "c1", contact) }
    coVerify(exactly = 0) { remote.editContact(userId, "c1", contact) }
  }

  @Test
  fun `editContact returns failure when remote fails`() = runTest {
    val remoteException = Exception("Remote error")
    coEvery { local.editContact(userId, "c1", contact) } returns Result.success(Unit)
    coEvery { remote.editContact(userId, "c1", contact) } returns Result.failure(remoteException)

    val result = hybrid.editContact(userId, "c1", contact)

    assertTrue(result.isFailure)
    assertEquals(remoteException, result.exceptionOrNull())
    coVerify { local.editContact(userId, "c1", contact) }
    coVerify { remote.editContact(userId, "c1", contact) }
  }

  @Test
  fun `deleteContact deletes from local first then remote when both succeed`() = runTest {
    coEvery { local.deleteContact(userId, "c1") } returns Result.success(Unit)
    coEvery { remote.deleteContact(userId, "c1") } returns Result.success(Unit)

    val result = hybrid.deleteContact(userId, "c1")

    assertTrue(result.isSuccess)
    coVerify { local.deleteContact(userId, "c1") }
    coVerify { remote.deleteContact(userId, "c1") }
  }

  @Test
  fun `deleteContact returns failure when local operation fails`() = runTest {
    coEvery { local.deleteContact(userId, "c1") } returns Result.failure(Exception("Local error"))

    val result = hybrid.deleteContact(userId, "c1")

    assertTrue(result.isFailure)
    coVerify { local.deleteContact(userId, "c1") }
    coVerify(exactly = 0) { remote.deleteContact(userId, "c1") }
  }

  @Test
  fun `deleteContact returns failure when remote fails`() = runTest {
    val remoteException = Exception("Remote error")
    coEvery { local.deleteContact(userId, "c1") } returns Result.success(Unit)
    coEvery { remote.deleteContact(userId, "c1") } returns Result.failure(remoteException)

    val result = hybrid.deleteContact(userId, "c1")

    assertTrue(result.isFailure)
    assertEquals(remoteException, result.exceptionOrNull())
    coVerify { local.deleteContact(userId, "c1") }
    coVerify { remote.deleteContact(userId, "c1") }
  }

  @Test
  fun `remote becomes available again after successful operation`() = runTest {
    val remoteException = Exception("Remote error")
    coEvery { local.addContact(userId, contact) } returns Result.success(Unit)
    coEvery { remote.addContact(userId, contact) } returns Result.failure(remoteException)

    val firstResult = hybrid.addContact(userId, contact)
    assertTrue(firstResult.isFailure)

    coVerify(exactly = 1) { remote.addContact(userId, contact) }

    clearMocks(remote, local, recordedCalls = true, answers = false)
    coEvery { remote.getAllContacts(userId) } returns Result.success(listOf(contact))
    coEvery { local.getAllContacts(userId) } returns Result.success(listOf(contact))

    val secondResult = hybrid.getAllContacts(userId)
    assertTrue(secondResult.isSuccess)

    coVerify(exactly = 1) { remote.getAllContacts(userId) }

    clearMocks(remote, local, recordedCalls = true, answers = false)
    coEvery { local.addContact(userId, contact2) } returns Result.success(Unit)
    coEvery { remote.addContact(userId, contact2) } returns Result.success(Unit)

    val thirdResult = hybrid.addContact(userId, contact2)

    assertTrue(thirdResult.isSuccess)
    coVerify(exactly = 1) { local.addContact(userId, contact2) }
    coVerify(exactly = 1) { remote.addContact(userId, contact2) }
  }

  @Test
  fun `write operations skip remote when marked unavailable but read operations re-enable it`() =
      runTest {
        val remoteException = Exception("Remote error")
        coEvery { local.editContact(userId, "c1", contact) } returns Result.success(Unit)
        coEvery { remote.editContact(userId, "c1", contact) } returns
            Result.failure(remoteException)

        val firstResult = hybrid.editContact(userId, "c1", contact)
        assertTrue(firstResult.isFailure)

        clearMocks(remote, local, recordedCalls = true, answers = false)
        coEvery { local.deleteContact(userId, "c1") } returns Result.success(Unit)

        val secondResult = hybrid.deleteContact(userId, "c1")
        assertTrue(secondResult.isSuccess)

        coVerify(exactly = 1) { local.deleteContact(userId, "c1") }
        coVerify(exactly = 0) { remote.deleteContact(userId, "c1") }

        clearMocks(remote, local, recordedCalls = true, answers = false)
        coEvery { remote.getContact(userId, "c2") } returns Result.success(contact2)
        coEvery { local.addContact(userId, contact2) } returns Result.success(Unit)

        val thirdResult = hybrid.getContact(userId, "c2")
        assertTrue(thirdResult.isSuccess)

        coVerify(exactly = 1) { remote.getContact(userId, "c2") }

        clearMocks(remote, local, recordedCalls = true, answers = false)
        coEvery { local.addContact(userId, contact) } returns Result.success(Unit)
        coEvery { remote.addContact(userId, contact) } returns Result.success(Unit)

        val fourthResult = hybrid.addContact(userId, contact)
        assertTrue(fourthResult.isSuccess)

        coVerify(exactly = 1) { remote.addContact(userId, contact) }
      }
}
