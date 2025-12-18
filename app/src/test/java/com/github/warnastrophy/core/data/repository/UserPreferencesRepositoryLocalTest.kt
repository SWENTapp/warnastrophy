package com.github.warnastrophy.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

/**
 * Unit tests for [UserPreferencesRepositoryLocal].
 *
 * The implementation of some tests were done with AI assistance
 */
@OptIn(ExperimentalCoroutinesApi::class)
class UserPreferencesRepositoryLocalTest {
  @get:Rule
  val temporaryFolder: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build()

  private val testDispatcher = UnconfinedTestDispatcher()
  private val testScope = TestScope(testDispatcher + Job())
  private val TEST_DATASTORE_NAME = "test_prefs.preferences_pb"
  private lateinit var testDataStore: DataStore<Preferences>
  private lateinit var repository: UserPreferencesRepositoryLocal

  @Before
  fun setUp() {
    // Create a test DataStore instance that writes to a temporary file
    testDataStore =
        PreferenceDataStoreFactory.create(
            scope = testScope, produceFile = { temporaryFolder.newFile(TEST_DATASTORE_NAME) })
    repository = UserPreferencesRepositoryLocal(testDataStore)
  }

  @Test
  fun getUserPreferences_whenDataStoreIsEmpty_returnsDefaultValues() = runTest {
    val preferences = repository.getUserPreferences.first()

    assertFalse(preferences.dangerModePreferences.alertMode)
    assertFalse(preferences.dangerModePreferences.inactivityDetection)
    assertFalse(preferences.dangerModePreferences.automaticSms)
    assertFalse(preferences.dangerModePreferences.automaticCalls)
    assertFalse(preferences.dangerModePreferences.autoActionsEnabled)
    assertFalse(preferences.dangerModePreferences.touchConfirmationRequired)
    assertFalse(preferences.dangerModePreferences.voiceConfirmationEnabled)
    assertFalse(preferences.themePreferences)
  }

  @Test
  fun setAlertMode_updatesPreference() = runTest {
    val testCases = listOf(true, false)

    testCases.forEach { alertModeValue ->
      repository.setAlertMode(alertModeValue)
      val preferences = repository.getUserPreferences.first()

      assertEquals(alertModeValue, preferences.dangerModePreferences.alertMode)
      assertFalse(preferences.dangerModePreferences.inactivityDetection)
      assertFalse(preferences.dangerModePreferences.automaticSms)
      assertFalse(preferences.dangerModePreferences.microphoneAccess)
    }
  }

  @Test
  fun setInactivityDetection_updatesPreference() = runTest {
    val testCases = listOf(true, false)

    testCases.forEach { inactivityDetectionValue ->
      repository.setInactivityDetection(inactivityDetectionValue)
      val preferences = repository.getUserPreferences.first()

      assertEquals(inactivityDetectionValue, preferences.dangerModePreferences.inactivityDetection)
      assertFalse(preferences.dangerModePreferences.alertMode)
      assertFalse(preferences.dangerModePreferences.automaticSms)
      assertFalse(preferences.themePreferences)
      assertFalse(preferences.dangerModePreferences.microphoneAccess)
    }
  }

  @Test
  fun setAutomaticSms_updatesPreference() = runTest {
    val testCases = listOf(true, false)

    testCases.forEach { automaticSmsValue ->
      repository.setAutomaticSms(automaticSmsValue)
      val preferences = repository.getUserPreferences.first()
      assertEquals(automaticSmsValue, preferences.dangerModePreferences.automaticSms)
      assertFalse(preferences.dangerModePreferences.alertMode)
      assertFalse(preferences.dangerModePreferences.inactivityDetection)
      assertFalse(preferences.themePreferences)
      assertFalse(preferences.dangerModePreferences.microphoneAccess)
    }
  }

  @Test
  fun setAutomaticCalls_updatesPreference() = runTest {
    val testCases = listOf(true, false)

    testCases.forEach { automaticCallsValue ->
      repository.setAutomaticCalls(automaticCallsValue)
      val preferences = repository.getUserPreferences.first()
      assertEquals(automaticCallsValue, preferences.dangerModePreferences.automaticCalls)
      assertFalse(preferences.dangerModePreferences.alertMode)
      assertFalse(preferences.dangerModePreferences.inactivityDetection)
      assertFalse(preferences.dangerModePreferences.autoActionsEnabled)
      assertFalse(preferences.dangerModePreferences.touchConfirmationRequired)
      assertFalse(preferences.dangerModePreferences.voiceConfirmationEnabled)
      assertFalse(preferences.themePreferences)
      assertFalse(preferences.dangerModePreferences.microphoneAccess)
    }
  }

  @Test
  fun setAutoActionsEnabled_updatesPreference() = runTest {
    val testCases = listOf(true, false)

    testCases.forEach { autoActionsValue ->
      repository.setAutoActionsEnabled(autoActionsValue)
      val preferences = repository.getUserPreferences.first()
      assertEquals(autoActionsValue, preferences.dangerModePreferences.autoActionsEnabled)
      assertFalse(preferences.themePreferences)
    }
  }

  @Test
  fun setTouchConfirmationRequired_updatesPreference() = runTest {
    val testCases = listOf(true, false)

    testCases.forEach { touchValue ->
      repository.setTouchConfirmationRequired(touchValue)
      val preferences = repository.getUserPreferences.first()
      assertEquals(touchValue, preferences.dangerModePreferences.touchConfirmationRequired)
    }
  }

  @Test
  fun setVoiceConfirmationEnabled_updatesPreference() = runTest {
    val testCases = listOf(true, false)

    testCases.forEach { voiceValue ->
      repository.setVoiceConfirmationEnabled(voiceValue)
      val preferences = repository.getUserPreferences.first()
      assertEquals(voiceValue, preferences.dangerModePreferences.voiceConfirmationEnabled)
    }
  }

  @Test
  fun multipleSetters_whenCalledSequentially_updateAllPreferencesCorrectly() = runTest {
    repository.setAlertMode(true)
    repository.setInactivityDetection(true)
    repository.setDarkMode(true)

    val intermediatePrefs = repository.getUserPreferences.first()

    assertTrue(intermediatePrefs.dangerModePreferences.alertMode)
    assertTrue(intermediatePrefs.dangerModePreferences.inactivityDetection)
    assertFalse(intermediatePrefs.dangerModePreferences.automaticSms)
    assertTrue(intermediatePrefs.dangerModePreferences.autoActionsEnabled)
    assertTrue(intermediatePrefs.dangerModePreferences.touchConfirmationRequired)
    assertFalse(intermediatePrefs.dangerModePreferences.voiceConfirmationEnabled)
    assertTrue(intermediatePrefs.themePreferences)
    assertFalse(intermediatePrefs.dangerModePreferences.microphoneAccess)

    repository.setAutomaticSms(true)
    repository.setAlertMode(false)
    repository.setDarkMode(false)
    repository.setMicrophoneAccess(true)
    repository.setVoiceConfirmationEnabled(true)

    val finalPrefs = repository.getUserPreferences.first()

    assertFalse(finalPrefs.dangerModePreferences.alertMode)
    assertTrue(finalPrefs.dangerModePreferences.inactivityDetection)
    assertTrue(finalPrefs.dangerModePreferences.automaticSms)
    assertTrue(finalPrefs.dangerModePreferences.microphoneAccess)
    assertTrue(finalPrefs.dangerModePreferences.autoActionsEnabled)
    assertTrue(finalPrefs.dangerModePreferences.touchConfirmationRequired)
    assertTrue(finalPrefs.dangerModePreferences.voiceConfirmationEnabled)
    assertFalse(finalPrefs.themePreferences)
  }

  @Test
  fun getUserPreferences_whenIOExceptionOccurs_returnsDefaultValues() = runTest {
    // Create a mock DataStore that throws an IOException
    val failingDataStore: DataStore<Preferences> = mock()
    whenever(failingDataStore.data).thenReturn(flow { throw IOException("Test IO Exception") })

    val failingRepository = UserPreferencesRepositoryLocal(failingDataStore)

    val preferences = failingRepository.getUserPreferences.first()

    assertFalse(preferences.dangerModePreferences.alertMode)
    assertFalse(preferences.dangerModePreferences.inactivityDetection)
    assertFalse(preferences.dangerModePreferences.automaticSms)
    assertFalse(preferences.dangerModePreferences.automaticCalls)
    assertFalse(preferences.dangerModePreferences.autoActionsEnabled)
    assertFalse(preferences.dangerModePreferences.touchConfirmationRequired)
    assertFalse(preferences.dangerModePreferences.voiceConfirmationEnabled)
    assertFalse(preferences.dangerModePreferences.microphoneAccess)

    assertFalse(preferences.themePreferences)
  }

  @Test
  fun getUserPreferences_whenOtherExceptionOccurs_rethrowsException() {
    val failingDataStore: DataStore<Preferences> = mock()
    val expectedException = IllegalStateException("Test other exception")
    whenever(failingDataStore.data).thenReturn(flow { throw expectedException })

    val failingRepository = UserPreferencesRepositoryLocal(failingDataStore)

    assertThrows(IllegalStateException::class.java) {
      runTest { failingRepository.getUserPreferences.first() }
    }
  }
}
