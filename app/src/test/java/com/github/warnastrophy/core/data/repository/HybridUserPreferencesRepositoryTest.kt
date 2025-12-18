package com.github.warnastrophy.core.data.repository

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class HybridUserPreferencesRepositoryTest {

  private lateinit var mockLocal: UserPreferencesRepositoryLocal
  private lateinit var mockRemote: UserPreferencesRepositoryRemote
  private lateinit var repository: HybridUserPreferencesRepository

  private val defaultPreferences = UserPreferences.default()
  private val customPreferences =
      UserPreferences(
          dangerModePreferences =
              DangerModePreferences(
                  alertMode = true,
                  inactivityDetection = true,
                  automaticSms = true,
                  automaticCalls = false,
                  microphoneAccess = false,
                  autoActionsEnabled = true,
                  touchConfirmationRequired = true,
                  voiceConfirmationEnabled = false),
          themePreferences = true)

  @Before
  fun setup() {
    mockLocal = mockk(relaxed = true)
    mockRemote = mockk(relaxed = true)

    every { mockLocal.getUserPreferences } returns flowOf(defaultPreferences)
    every { mockRemote.getUserPreferences } returns flowOf(defaultPreferences)

    coEvery { mockLocal.setAlertMode(any()) } just Runs
    coEvery { mockLocal.setInactivityDetection(any()) } just Runs
    coEvery { mockLocal.setAutomaticSms(any()) } just Runs
    coEvery { mockLocal.setAutomaticCalls(any()) } just Runs
    coEvery { mockLocal.setAutoActionsEnabled(any()) } just Runs
    coEvery { mockLocal.setTouchConfirmationRequired(any()) } just Runs
    coEvery { mockLocal.setVoiceConfirmationEnabled(any()) } just Runs
    coEvery { mockLocal.setDarkMode(any()) } just Runs
    coEvery { mockLocal.setMicrophoneAccess(any()) } just Runs

    coEvery { mockRemote.setAlertMode(any()) } just Runs
    coEvery { mockRemote.setInactivityDetection(any()) } just Runs
    coEvery { mockRemote.setAutomaticSms(any()) } just Runs
    coEvery { mockRemote.setAutomaticCalls(any()) } just Runs
    coEvery { mockRemote.setAutoActionsEnabled(any()) } just Runs
    coEvery { mockRemote.setTouchConfirmationRequired(any()) } just Runs
    coEvery { mockRemote.setVoiceConfirmationEnabled(any()) } just Runs
    coEvery { mockRemote.setDarkMode(any()) } just Runs
    coEvery { mockRemote.setMicrophoneAccess(any()) } just Runs

    repository = HybridUserPreferencesRepository(mockLocal, mockRemote)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `getUserPreferences emits local preferences immediately`() = runTest {
    every { mockLocal.getUserPreferences } returns flowOf(customPreferences)

    val result = repository.getUserPreferences.first()

    assertEquals(customPreferences, result)
    assertTrue(result.dangerModePreferences.alertMode)
    assertTrue(result.dangerModePreferences.touchConfirmationRequired)
  }

  @Test
  fun `getUserPreferences syncs remote to local on start`() = runTest {
    val remotePrefs =
        UserPreferences(
            dangerModePreferences =
                DangerModePreferences(
                    alertMode = true,
                    inactivityDetection = false,
                    automaticSms = true,
                    automaticCalls = true,
                    microphoneAccess = false,
                    autoActionsEnabled = true,
                    touchConfirmationRequired = false,
                    voiceConfirmationEnabled = true),
            themePreferences = false)

    every { mockLocal.getUserPreferences } returns flowOf(defaultPreferences)
    every { mockRemote.getUserPreferences } returns flowOf(remotePrefs)

    repository.getUserPreferences.first()

    coVerify {
      mockLocal.setAlertMode(true)
      mockLocal.setInactivityDetection(false)
      mockLocal.setAutomaticSms(true)
      mockLocal.setAutomaticCalls(true)
      mockLocal.setAutoActionsEnabled(true)
      mockLocal.setTouchConfirmationRequired(false)
      mockLocal.setVoiceConfirmationEnabled(true)
      mockLocal.setDarkMode(false)
      mockLocal.setMicrophoneAccess(false)
    }
  }

  @Test
  fun `getUserPreferences falls back to remote when local fails`() = runTest {
    every { mockLocal.getUserPreferences } returns flow { throw Exception("Local storage error") }
    every { mockRemote.getUserPreferences } returns flowOf(customPreferences)

    val result = repository.getUserPreferences.first()

    assertEquals(customPreferences, result)
  }

  @Test
  fun `getUserPreferences throws when both local and remote fail`() = runTest {
    val localError = Exception("Local error")

    every { mockLocal.getUserPreferences } returns flow { throw localError }
    every { mockRemote.getUserPreferences } returns flow { throw Exception("Remote error") }

    val exception = assertFailsWith<Exception> { repository.getUserPreferences.first() }

    assertEquals(localError, exception)
  }

  @Test
  fun `getUserPreferences attempts sync even when remote was previously unavailable`() = runTest {
    coEvery { mockRemote.setAlertMode(any()) } throws Exception("Remote unavailable")
    repository.setAlertMode(true)

    every { mockRemote.getUserPreferences } returns flowOf(customPreferences)

    repository.getUserPreferences.first()

    coVerify(atLeast = 1) { mockRemote.getUserPreferences }
  }

  @Test
  fun `setAlertMode updates both local and remote when available`() = runTest {
    repository.setAlertMode(true)

    coVerify { mockLocal.setAlertMode(true) }
    coVerify { mockRemote.setAlertMode(true) }
  }

  @Test
  fun `setAlertMode updates only local when remote fails`() = runTest {
    coEvery { mockRemote.setAlertMode(any()) } throws Exception("Remote error")

    repository.setAlertMode(false)

    coVerify { mockLocal.setAlertMode(false) }
    coVerify { mockRemote.setAlertMode(false) }
  }

  @Test
  fun `setAlertMode skips remote after first failure`() = runTest {
    coEvery { mockRemote.setAlertMode(any()) } throws Exception("Remote error")
    repository.setAlertMode(true)

    repository.setAlertMode(false)

    coVerify(exactly = 1) { mockRemote.setAlertMode(any()) }
    coVerify(exactly = 2) { mockLocal.setAlertMode(any()) }
  }

  @Test
  fun `setInactivityDetection updates both repositories when available`() = runTest {
    repository.setInactivityDetection(true)

    coVerify { mockLocal.setInactivityDetection(true) }
    coVerify { mockRemote.setInactivityDetection(true) }
  }

  @Test
  fun `setInactivityDetection updates only local when remote fails`() = runTest {
    coEvery { mockRemote.setInactivityDetection(any()) } throws Exception("Network error")

    repository.setInactivityDetection(false)

    coVerify { mockLocal.setInactivityDetection(false) }
  }

  @Test
  fun `setAutomaticSms updates both repositories when available`() = runTest {
    repository.setAutomaticSms(true)

    coVerify { mockLocal.setAutomaticSms(true) }
    coVerify { mockRemote.setAutomaticSms(true) }
  }

  @Test
  fun `setAutomaticSms continues when remote fails`() = runTest {
    coEvery { mockRemote.setAutomaticSms(any()) } throws Exception("Remote error")

    repository.setAutomaticSms(false)

    coVerify { mockLocal.setAutomaticSms(false) }
    coVerify { mockRemote.setAutomaticSms(false) }
  }

  @Test
  fun `setAutomaticCalls updates both repositories when available`() = runTest {
    repository.setAutomaticCalls(true)

    coVerify { mockLocal.setAutomaticCalls(true) }
    coVerify { mockRemote.setAutomaticCalls(true) }
  }

  @Test
  fun `setAutoActionsEnabled updates both repositories when available`() = runTest {
    repository.setAutoActionsEnabled(true)

    coVerify { mockLocal.setAutoActionsEnabled(true) }
    coVerify { mockRemote.setAutoActionsEnabled(true) }
  }

  @Test
  fun `setTouchConfirmationRequired updates both repositories when available`() = runTest {
    repository.setTouchConfirmationRequired(true)

    coVerify { mockLocal.setTouchConfirmationRequired(true) }
    coVerify { mockRemote.setTouchConfirmationRequired(true) }
  }

  @Test
  fun `setVoiceConfirmationEnabled updates both repositories when available`() = runTest {
    repository.setVoiceConfirmationEnabled(true)

    coVerify { mockLocal.setVoiceConfirmationEnabled(true) }
    coVerify { mockRemote.setVoiceConfirmationEnabled(true) }
  }

  @Test
  fun `getUserPreferences syncs remote to local on start with new preferences`() = runTest {
    val remotePrefs =
        UserPreferences(
            dangerModePreferences =
                DangerModePreferences(
                    alertMode = false,
                    inactivityDetection = true,
                    automaticSms = false,
                    automaticCalls = true,
                    microphoneAccess = false,
                    autoActionsEnabled = true,
                    touchConfirmationRequired = true,
                    voiceConfirmationEnabled = true),
            themePreferences = true)

    every { mockLocal.getUserPreferences } returns flowOf(defaultPreferences)
    every { mockRemote.getUserPreferences } returns flowOf(remotePrefs)

    repository.getUserPreferences.first()

    coVerify {
      mockLocal.setAlertMode(false)
      mockLocal.setInactivityDetection(true)
      mockLocal.setAutomaticSms(false)
      mockLocal.setAutomaticCalls(true)
      mockLocal.setAutoActionsEnabled(true)
      mockLocal.setTouchConfirmationRequired(true)
      mockLocal.setVoiceConfirmationEnabled(true)
      mockLocal.setDarkMode(true)
    }
  }

  @Test
  fun `syncRemoteToLocal marks remote available again after success`() = runTest {
    coEvery { mockRemote.setAlertMode(any()) } throws Exception("Remote write error")
    repository.setAlertMode(true)

    every { mockRemote.getUserPreferences } returns flowOf(customPreferences)
    coEvery { mockRemote.setAlertMode(any()) } just Runs
    coEvery { mockRemote.setAutoActionsEnabled(any()) } just Runs

    repository.getUserPreferences.first()

    repository.setAutoActionsEnabled(false)

    coVerify { mockRemote.setAutoActionsEnabled(false) }
  }

  @Test
  fun `remote failure disables subsequent updates for new preferences`() = runTest {
    coEvery { mockRemote.setAutoActionsEnabled(any()) } throws Exception("Remote error")

    repository.setAutoActionsEnabled(true)

    repository.setTouchConfirmationRequired(true)

    coVerify(exactly = 1) { mockRemote.setAutoActionsEnabled(any()) }
    coVerify(exactly = 0) { mockRemote.setTouchConfirmationRequired(any()) }
    coVerify(exactly = 1) { mockLocal.setAutoActionsEnabled(any()) }
    coVerify(exactly = 1) { mockLocal.setTouchConfirmationRequired(any()) }
  }

  @Test
  fun `setMicrophoneAccess updates both repositories when available`() = runTest {
    repository.setMicrophoneAccess(true)
    coVerify { mockLocal.setMicrophoneAccess(true) }
    coVerify { mockRemote.setMicrophoneAccess(true) }
  }

  @Test
  fun `multiple updates work correctly when both repositories available`() = runTest {
    repository.setAlertMode(true)
    repository.setInactivityDetection(false)
    repository.setAutomaticSms(true)
    repository.setDarkMode(false)

    coVerify {
      mockLocal.setAlertMode(true)
      mockLocal.setInactivityDetection(false)
      mockLocal.setAutomaticSms(true)
      mockLocal.setDarkMode(false)
    }

    coVerify {
      mockRemote.setAlertMode(true)
      mockRemote.setInactivityDetection(false)
      mockRemote.setAutomaticSms(true)
      mockRemote.setDarkMode(false)
    }
  }

  @Test
  fun `remote becomes unavailable after first failure`() = runTest {
    repository.setAlertMode(true)
    coVerify { mockRemote.setAlertMode(true) }

    coEvery { mockRemote.setInactivityDetection(any()) } throws Exception("Network error")
    repository.setInactivityDetection(false)

    repository.setAutomaticSms(true)

    coVerify(exactly = 1) { mockRemote.setAlertMode(any()) }
    coVerify(exactly = 1) { mockRemote.setInactivityDetection(any()) }
    coVerify(exactly = 0) { mockRemote.setAutomaticSms(any()) }
  }

  @Test
  fun `all updates continue to go to local even when remote fails`() = runTest {
    coEvery { mockRemote.setAlertMode(any()) } throws Exception("Remote error")
    repository.setAlertMode(true)

    repository.setInactivityDetection(false)
    repository.setAutomaticSms(true)
    repository.setDarkMode(false)

    coVerify(exactly = 1) { mockLocal.setAlertMode(true) }
    coVerify(exactly = 1) { mockLocal.setInactivityDetection(false) }
    coVerify(exactly = 1) { mockLocal.setAutomaticSms(true) }
    coVerify(exactly = 1) { mockLocal.setDarkMode(false) }

    coVerify(exactly = 1) { mockRemote.setAlertMode(true) }
    coVerify(exactly = 0) { mockRemote.setInactivityDetection(any()) }
    coVerify(exactly = 0) { mockRemote.setAutomaticSms(any()) }
    coVerify(exactly = 0) { mockRemote.setDarkMode(any()) }
  }

  @Test
  fun `sync copies all preferences from remote to local`() = runTest {
    val remotePrefs =
        UserPreferences(
            dangerModePreferences =
                DangerModePreferences(
                    alertMode = true,
                    inactivityDetection = false,
                    automaticSms = true,
                    automaticCalls = false,
                    microphoneAccess = false,
                    autoActionsEnabled = false,
                    touchConfirmationRequired = false,
                    voiceConfirmationEnabled = false),
            themePreferences = true)

    every { mockRemote.getUserPreferences } returns flowOf(remotePrefs)

    repository.getUserPreferences.first()

    coVerify {
      mockLocal.setAlertMode(true)
      mockLocal.setInactivityDetection(false)
      mockLocal.setAutomaticSms(true)
      mockLocal.setDarkMode(true)
    }
  }

  @Test
  fun `sync does not throw when remote fails`() = runTest {
    every { mockRemote.getUserPreferences } returns flow { throw Exception("Remote sync failed") }

    val result = repository.getUserPreferences.first()

    assertEquals(defaultPreferences, result)
  }

  @Test
  fun `sync is protected by mutex`() = runTest {
    val slowRemote = flow {
      delay(100)
      emit(customPreferences)
    }
    every { mockRemote.getUserPreferences } returns slowRemote

    val job1 = launch { repository.getUserPreferences.first() }
    val job2 = launch { repository.getUserPreferences.first() }

    job1.join()
    job2.join()

    coVerify(atLeast = 1) { mockLocal.setAlertMode(any()) }
  }

  @Test
  fun `sync restores remote availability after successful sync`() = runTest {
    coEvery { mockRemote.setAlertMode(any()) } throws Exception("Remote write error")
    repository.setAlertMode(true)

    repository.setInactivityDetection(false)
    coVerify(exactly = 0) { mockRemote.setInactivityDetection(any()) }

    every { mockRemote.getUserPreferences } returns flowOf(customPreferences)
    coEvery { mockRemote.setAlertMode(any()) } just Runs
    coEvery { mockRemote.setInactivityDetection(any()) } just Runs
    coEvery { mockRemote.setAutomaticSms(any()) } just Runs

    repository.getUserPreferences.first()

    repository.setAutomaticSms(true)

    coVerify(exactly = 1) { mockRemote.setAutomaticSms(true) }
  }

  @Test
  fun `sync marks remote unavailable if sync fails`() = runTest {
    repository.setAlertMode(true)
    coVerify(exactly = 1) { mockRemote.setAlertMode(true) }

    every { mockRemote.getUserPreferences } returns flow { throw Exception("Sync failed") }

    repository.getUserPreferences.first()

    repository.setInactivityDetection(false)
    coVerify(exactly = 0) { mockRemote.setInactivityDetection(any()) }
  }

  @Test
  fun `local update always happens even if it fails`() = runTest {
    coEvery { mockLocal.setAlertMode(any()) } throws Exception("Local error")

    assertFailsWith<Exception> { repository.setAlertMode(true) }

    coVerify { mockLocal.setAlertMode(true) }
  }

  @Test
  fun `preferences flow emits multiple values from local`() = runTest {
    val multipleValues = flow {
      emit(defaultPreferences)
      emit(customPreferences)
    }
    every { mockLocal.getUserPreferences } returns multipleValues

    val results = repository.getUserPreferences.take(2).toList()

    assertEquals(2, results.size)
    assertEquals(defaultPreferences, results[0])
    assertEquals(customPreferences, results[1])
  }

  @Test
  fun `concurrent updates are handled correctly`() = runTest {
    val jobs = List(10) { index -> launch { repository.setAlertMode(index % 2 == 0) } }

    jobs.joinAll()

    coVerify(exactly = 10) { mockLocal.setAlertMode(any()) }
    coVerify(exactly = 10) { mockRemote.setAlertMode(any()) }
  }
}
