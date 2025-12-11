package com.github.warnastrophy.core.data.repository

import com.github.warnastrophy.core.model.HealthCard
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import junit.framework.TestCase.assertNull
import junit.framework.TestCase.assertTrue
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class HybridHealthCardRepositoryTest {

  private lateinit var local: HealthCardRepository
  private lateinit var remote: HealthCardRepository
  private lateinit var hybrid: HybridHealthCardRepository

  private val healthCard =
      HealthCard(
          bloodType = "O+",
          allergies = listOf("Peanuts"),
          medications = listOf("Aspirin"),
          chronicConditions = listOf("Diabetes"))

  private val healthCard2 =
      HealthCard(
          bloodType = "A-",
          allergies = listOf("Penicillin"),
          medications = emptyList(),
          chronicConditions = emptyList())

  @Before
  fun setup() {
    local = mockk(relaxed = true)
    remote = mockk(relaxed = true)

    hybrid = HybridHealthCardRepository(local, remote)
  }

  @Test
  fun `observeMyHealthCard emits local data first then syncs with remote`() = runTest {
    every { local.observeMyHealthCard() } returns flowOf(healthCard)
    coEvery { remote.getMyHealthCardOnce(false) } returns healthCard2
    coEvery { local.upsertMyHealthCard(healthCard2) } just Runs

    val result = hybrid.observeMyHealthCard().first()

    assertEquals(healthCard, result)

    coVerify { remote.getMyHealthCardOnce(false) }
    coVerify { local.upsertMyHealthCard(healthCard2) }
  }

  @Test
  fun `observeMyHealthCard falls back to remote when local fails`() = runTest {
    every { local.observeMyHealthCard() } returns flow { throw Exception("Local Error") }
    every { remote.observeMyHealthCard() } returns flowOf(healthCard)

    val result = hybrid.observeMyHealthCard().first()

    assertEquals(healthCard, result)
  }

  @Test
  fun `observeMyHealthCard throws when both local and remote fail`() = runTest {
    val localException = Exception("Local error")
    every { local.observeMyHealthCard() } returns flow { throw localException }
    every { remote.observeMyHealthCard() } returns flow { throw Exception("Remote error") }

    try {
      hybrid.observeMyHealthCard().first()
      assertTrue("Should have thrown exception", false)
    } catch (e: Exception) {
      assertEquals(localException, e)
    }
  }

  @Test
  fun `observeMyHealthCard continues with local even if sync fails`() = runTest {
    every { local.observeMyHealthCard() } returns flowOf(healthCard)
    coEvery { remote.getMyHealthCardOnce(false) } throws Exception("Remote error")

    val result = hybrid.observeMyHealthCard().first()
    assertEquals(healthCard, result)

    coVerify { remote.getMyHealthCardOnce(false) }
  }

  @Test
  fun `getMyHealthCardOnce returns remote data and syncs to local when remote is available`() =
      runTest {
        coEvery { remote.getMyHealthCardOnce(true) } returns healthCard
        coEvery { local.upsertMyHealthCard(healthCard) } just Runs

        val result = hybrid.getMyHealthCardOnce(fromCacheFirst = true)

        assertEquals(healthCard, result)
        coVerify { remote.getMyHealthCardOnce(true) }
        coVerify { local.upsertMyHealthCard(healthCard) }
      }

  @Test
  fun `getMyHealthCardOnce returns null when remote returns null successfully`() = runTest {
    coEvery { remote.getMyHealthCardOnce(false) } returns null
    coEvery { local.getMyHealthCardOnce(true) } returns null

    val result = hybrid.getMyHealthCardOnce(fromCacheFirst = false)

    assertNull(result)
    coVerify { remote.getMyHealthCardOnce(false) }
    coVerify { local.getMyHealthCardOnce(true) }
  }

  @Test
  fun `getMyHealthCardOnce falls back to local when remote fails`() = runTest {
    coEvery { remote.getMyHealthCardOnce(true) } throws Exception("Network error")
    coEvery { local.getMyHealthCardOnce(true) } returns healthCard

    val result = hybrid.getMyHealthCardOnce(fromCacheFirst = true)

    assertEquals(healthCard, result)
    coVerify { remote.getMyHealthCardOnce(true) }
    coVerify { local.getMyHealthCardOnce(true) }
  }

  @Test
  fun `getMyHealthCardOnce returns null when both remote and local fail`() = runTest {
    coEvery { remote.getMyHealthCardOnce(false) } throws Exception("Network error")
    coEvery { local.getMyHealthCardOnce(true) } throws Exception("Local error")

    val result = hybrid.getMyHealthCardOnce(fromCacheFirst = false)

    assertNull(result)
  }

  @Test
  fun `getMyHealthCardOnce continues even if local sync fails`() = runTest {
    coEvery { remote.getMyHealthCardOnce(true) } returns healthCard
    coEvery { local.upsertMyHealthCard(healthCard) } throws Exception("Local write error")

    val result = hybrid.getMyHealthCardOnce(fromCacheFirst = true)

    assertEquals(healthCard, result)
  }

  @Test
  fun `getMyHealthCardOnce always tries remote even after previous failure to enable recovery`() =
      runTest {
        coEvery { remote.getMyHealthCardOnce(true) } throws Exception("Network error")
        coEvery { local.getMyHealthCardOnce(true) } returns healthCard

        val firstResult = hybrid.getMyHealthCardOnce(fromCacheFirst = true)
        assertEquals(healthCard, firstResult)

        coVerify(exactly = 1) { remote.getMyHealthCardOnce(true) }

        clearMocks(remote, local, recordedCalls = true, answers = false)

        coEvery { remote.getMyHealthCardOnce(true) } throws Exception("Network error")
        coEvery { local.getMyHealthCardOnce(true) } returns healthCard

        val result = hybrid.getMyHealthCardOnce(fromCacheFirst = true)

        assertEquals(healthCard, result)
        coVerify(exactly = 1) { remote.getMyHealthCardOnce(true) }
        coVerify(exactly = 1) { local.getMyHealthCardOnce(true) }
      }

  @Test
  fun `upsertMyHealthCard writes to local first then remote when both succeed`() = runTest {
    coEvery { local.upsertMyHealthCard(healthCard) } just Runs
    coEvery { remote.upsertMyHealthCard(healthCard) } just Runs

    hybrid.upsertMyHealthCard(healthCard)

    coVerify { local.upsertMyHealthCard(healthCard) }
    coVerify { remote.upsertMyHealthCard(healthCard) }
  }

  @Test
  fun `upsertMyHealthCard throws when local operation fails`() = runTest {
    val localException = Exception("Local error")
    coEvery { local.upsertMyHealthCard(healthCard) } throws localException

    try {
      hybrid.upsertMyHealthCard(healthCard)
      assertTrue("Should have thrown exception", false)
    } catch (e: Exception) {
      assertEquals(localException, e)
    }

    coVerify { local.upsertMyHealthCard(healthCard) }
    coVerify(exactly = 0) { remote.upsertMyHealthCard(healthCard) }
  }

  @Test
  fun `upsertMyHealthCard throws when remote fails`() = runTest {
    val remoteException = Exception("Remote error")
    coEvery { local.upsertMyHealthCard(healthCard) } just Runs
    coEvery { remote.upsertMyHealthCard(healthCard) } throws remoteException

    try {
      hybrid.upsertMyHealthCard(healthCard)
      assertTrue("Should have thrown exception", false)
    } catch (e: Exception) {
      assertEquals(remoteException, e)
    }

    coVerify { local.upsertMyHealthCard(healthCard) }
    coVerify { remote.upsertMyHealthCard(healthCard) }
  }

  @Test
  fun `upsertMyHealthCard skips remote after remote becomes unavailable`() = runTest {
    val remoteException = Exception("Remote error")
    coEvery { local.upsertMyHealthCard(healthCard) } just Runs
    coEvery { remote.upsertMyHealthCard(healthCard) } throws remoteException

    try {
      hybrid.upsertMyHealthCard(healthCard)
      assertTrue("Should have thrown exception", false)
    } catch (e: Exception) {
      assertEquals(remoteException, e)
    }

    coVerify(exactly = 1) { remote.upsertMyHealthCard(healthCard) }

    clearMocks(remote, local, recordedCalls = true, answers = false)

    coEvery { local.upsertMyHealthCard(healthCard2) } just Runs
    coEvery { remote.upsertMyHealthCard(healthCard2) } just Runs

    hybrid.upsertMyHealthCard(healthCard2)

    coVerify(exactly = 1) { local.upsertMyHealthCard(healthCard2) }
    coVerify(exactly = 0) { remote.upsertMyHealthCard(healthCard2) }
  }

  @Test
  fun `deleteMyHealthCard deletes from local first then remote when both succeed`() = runTest {
    coEvery { local.deleteMyHealthCard() } just Runs
    coEvery { remote.deleteMyHealthCard() } just Runs

    hybrid.deleteMyHealthCard()

    coVerify { local.deleteMyHealthCard() }
    coVerify { remote.deleteMyHealthCard() }
  }

  @Test
  fun `deleteMyHealthCard throws when local operation fails`() = runTest {
    val localException = Exception("Local error")
    coEvery { local.deleteMyHealthCard() } throws localException

    try {
      hybrid.deleteMyHealthCard()
      assertTrue("Should have thrown exception", false)
    } catch (e: Exception) {
      assertEquals(localException, e)
    }

    coVerify { local.deleteMyHealthCard() }
    coVerify(exactly = 0) { remote.deleteMyHealthCard() }
  }

  @Test
  fun `deleteMyHealthCard throws when remote fails`() = runTest {
    val remoteException = Exception("Remote error")
    coEvery { local.deleteMyHealthCard() } just Runs
    coEvery { remote.deleteMyHealthCard() } throws remoteException

    try {
      hybrid.deleteMyHealthCard()
      assertTrue("Should have thrown exception", false)
    } catch (e: Exception) {
      assertEquals(remoteException, e)
    }

    coVerify { local.deleteMyHealthCard() }
    coVerify { remote.deleteMyHealthCard() }
  }

  @Test
  fun `deleteMyHealthCard skips remote after remote becomes unavailable`() = runTest {
    val remoteException = Exception("Remote error")
    coEvery { local.deleteMyHealthCard() } just Runs
    coEvery { remote.deleteMyHealthCard() } throws remoteException

    try {
      hybrid.deleteMyHealthCard()
      assertTrue("Should have thrown exception", false)
    } catch (e: Exception) {
      assertEquals(remoteException, e)
    }

    coVerify(exactly = 1) { remote.deleteMyHealthCard() }

    clearMocks(remote, local, recordedCalls = true, answers = false)

    coEvery { local.deleteMyHealthCard() } just Runs
    coEvery { remote.deleteMyHealthCard() } just Runs

    hybrid.deleteMyHealthCard()

    coVerify(exactly = 1) { local.deleteMyHealthCard() }
    coVerify(exactly = 0) { remote.deleteMyHealthCard() }
  }

  @Test
  fun `remote becomes available again after successful read operation`() = runTest {
    val remoteException = Exception("Remote error")
    coEvery { local.upsertMyHealthCard(healthCard) } just Runs
    coEvery { remote.upsertMyHealthCard(healthCard) } throws remoteException

    try {
      hybrid.upsertMyHealthCard(healthCard)
      assertTrue("Should have thrown exception", false)
    } catch (e: Exception) {
      assertEquals(remoteException, e)
    }

    coVerify(exactly = 1) { remote.upsertMyHealthCard(healthCard) }

    clearMocks(remote, local, recordedCalls = true, answers = false)
    coEvery { remote.getMyHealthCardOnce(true) } returns healthCard
    coEvery { local.upsertMyHealthCard(healthCard) } just Runs

    val result = hybrid.getMyHealthCardOnce(fromCacheFirst = true)
    assertEquals(healthCard, result)

    coVerify(exactly = 1) { remote.getMyHealthCardOnce(true) }

    clearMocks(remote, local, recordedCalls = true, answers = false)
    coEvery { local.upsertMyHealthCard(healthCard2) } just Runs
    coEvery { remote.upsertMyHealthCard(healthCard2) } just Runs

    hybrid.upsertMyHealthCard(healthCard2)

    coVerify(exactly = 1) { local.upsertMyHealthCard(healthCard2) }
    coVerify(exactly = 1) { remote.upsertMyHealthCard(healthCard2) }
  }

  @Test
  fun `write operations skip remote when marked unavailable but read operations re-enable it`() =
      runTest {
        val remoteException = Exception("Remote error")
        coEvery { local.upsertMyHealthCard(healthCard) } just Runs
        coEvery { remote.upsertMyHealthCard(healthCard) } throws remoteException

        try {
          hybrid.upsertMyHealthCard(healthCard)
          assertTrue("Should have thrown exception", false)
        } catch (e: Exception) {
          assertEquals(remoteException, e)
        }

        clearMocks(remote, local, recordedCalls = true, answers = false)
        coEvery { local.deleteMyHealthCard() } just Runs

        hybrid.deleteMyHealthCard()

        coVerify(exactly = 1) { local.deleteMyHealthCard() }
        coVerify(exactly = 0) { remote.deleteMyHealthCard() }

        clearMocks(remote, local, recordedCalls = true, answers = false)
        coEvery { remote.getMyHealthCardOnce(false) } returns healthCard2
        coEvery { local.upsertMyHealthCard(healthCard2) } just Runs

        val result = hybrid.getMyHealthCardOnce(fromCacheFirst = false)
        assertEquals(healthCard2, result)

        coVerify(exactly = 1) { remote.getMyHealthCardOnce(false) }

        clearMocks(remote, local, recordedCalls = true, answers = false)
        coEvery { local.upsertMyHealthCard(healthCard) } just Runs
        coEvery { remote.upsertMyHealthCard(healthCard) } just Runs

        hybrid.upsertMyHealthCard(healthCard)

        coVerify(exactly = 1) { remote.upsertMyHealthCard(healthCard) }
      }

  @Test
  fun `syncRemoteToLocal does not affect remote availability when sync succeeds`() = runTest {
    every { local.observeMyHealthCard() } returns flowOf(null)
    coEvery { remote.getMyHealthCardOnce(false) } returns healthCard
    coEvery { local.upsertMyHealthCard(healthCard) } just Runs

    val result = hybrid.observeMyHealthCard().first()
    assertNull(result)

    coVerify { remote.getMyHealthCardOnce(false) }
    coVerify { local.upsertMyHealthCard(healthCard) }

    clearMocks(remote, local, recordedCalls = true, answers = false)
    coEvery { local.upsertMyHealthCard(healthCard2) } just Runs
    coEvery { remote.upsertMyHealthCard(healthCard2) } just Runs

    hybrid.upsertMyHealthCard(healthCard2)

    coVerify(exactly = 1) { remote.upsertMyHealthCard(healthCard2) }
  }

  @Test
  fun `syncRemoteToLocal marks remote unavailable when sync fails`() = runTest {
    every { local.observeMyHealthCard() } returns flowOf(null)
    coEvery { remote.getMyHealthCardOnce(false) } throws Exception("Network error")

    val result = hybrid.observeMyHealthCard().first()
    assertNull(result)

    coVerify { remote.getMyHealthCardOnce(false) }

    clearMocks(remote, local, recordedCalls = true, answers = false)
    coEvery { local.upsertMyHealthCard(healthCard) } just Runs

    hybrid.upsertMyHealthCard(healthCard)

    coVerify(exactly = 1) { local.upsertMyHealthCard(healthCard) }
    coVerify(exactly = 0) { remote.upsertMyHealthCard(healthCard) }
  }
}
