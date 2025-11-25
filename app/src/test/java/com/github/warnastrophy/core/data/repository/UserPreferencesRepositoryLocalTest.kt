package com.github.warnastrophy.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

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
  }

  @Test
  fun setAlertMode_whenSetToTrue_updatesPreference() = runTest {
    repository.setAlertMode(true)

    val preferences = repository.getUserPreferences.first()

    assertTrue(preferences.dangerModePreferences.alertMode)
    assertFalse(preferences.dangerModePreferences.inactivityDetection)
    assertFalse(preferences.dangerModePreferences.automaticSms)
  }

  @Test
  fun setInactivityDetection_whenSetToTrue_updatesPreference() = runTest {
    repository.setInactivityDetection(true)

    val preferences = repository.getUserPreferences.first()

    assertTrue(preferences.dangerModePreferences.inactivityDetection)
    assertFalse(preferences.dangerModePreferences.alertMode)
    assertFalse(preferences.dangerModePreferences.automaticSms)
  }

  @Test
  fun setAutomaticSms_whenSetToTrue_updatesPreference() = runTest {
    repository.setAutomaticSms(true)

    val preferences = repository.getUserPreferences.first()

    assertTrue(preferences.dangerModePreferences.automaticSms)
    assertFalse(preferences.dangerModePreferences.alertMode)
    assertFalse(preferences.dangerModePreferences.inactivityDetection)
  }

  @Test
  fun multipleSetters_whenCalledSequentially_updateAllPreferencesCorrectly() = runTest {
    repository.setAlertMode(true)
    repository.setInactivityDetection(true)

    val intermediatePrefs = repository.getUserPreferences.first()

    assertTrue(intermediatePrefs.dangerModePreferences.alertMode)
    assertTrue(intermediatePrefs.dangerModePreferences.inactivityDetection)
    assertFalse(intermediatePrefs.dangerModePreferences.automaticSms)

    repository.setAutomaticSms(true)
    repository.setAlertMode(false)

    val finalPrefs = repository.getUserPreferences.first()

    assertFalse(finalPrefs.dangerModePreferences.alertMode)
    assertTrue(finalPrefs.dangerModePreferences.inactivityDetection)
    assertTrue(finalPrefs.dangerModePreferences.automaticSms)
  }
}
