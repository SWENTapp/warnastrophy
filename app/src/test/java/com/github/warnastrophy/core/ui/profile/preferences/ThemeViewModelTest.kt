package com.github.warnastrophy.core.ui.profile.preferences

import com.github.warnastrophy.core.data.repository.DangerModePreferences
import com.github.warnastrophy.core.data.repository.UserPreferences
import com.github.warnastrophy.core.data.repository.UserPreferencesRepository
import com.github.warnastrophy.core.ui.features.profile.ThemeViewModel
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var repository: UserPreferencesRepository
  private lateinit var viewModel: ThemeViewModel

  private val defaultDangerModePreferences =
      DangerModePreferences(alertMode = false, inactivityDetection = false, automaticSms = false)

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    repository = mockk(relaxed = true)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkAll()
  }

  @Test
  fun `isDarkMode when repository emits true emits true`() = runTest {
    val userPreferences =
        UserPreferences(
            dangerModePreferences = defaultDangerModePreferences, themePreferences = true)

    coEvery { repository.getUserPreferences } returns flowOf(userPreferences)

    viewModel = ThemeViewModel(repository)

    val job = backgroundScope.launch { viewModel.isDarkMode.collect {} }
    advanceUntilIdle()

    assertTrue(viewModel.isDarkMode.value ?: false)
    job.cancel()
  }

  @Test
  fun `isDarkMode when repository emits false emits false`() = runTest {
    val userPreferences =
        UserPreferences(
            dangerModePreferences = defaultDangerModePreferences, themePreferences = false)

    coEvery { repository.getUserPreferences } returns flowOf(userPreferences)

    viewModel = ThemeViewModel(repository)

    val job = backgroundScope.launch { viewModel.isDarkMode.collect {} }
    advanceUntilIdle()

    assertFalse(viewModel.isDarkMode.value ?: true)
    job.cancel()
  }

  @Test
  fun `isDarkMode initial value is null`() = runTest {
    coEvery { repository.getUserPreferences } returns flowOf()

    viewModel = ThemeViewModel(repository)

    assertNull(viewModel.isDarkMode.value)
  }

  @Test
  fun `toggleTheme with true calls setDarkMode with true`() = runTest {
    val userPreferences =
        UserPreferences(
            dangerModePreferences = defaultDangerModePreferences, themePreferences = false)

    coEvery { repository.getUserPreferences } returns flowOf(userPreferences)
    coEvery { repository.setDarkMode(any()) } just Runs

    viewModel = ThemeViewModel(repository)
    viewModel.toggleTheme(true)
    advanceUntilIdle()

    coVerify(exactly = 1) { repository.setDarkMode(true) }
  }

  @Test
  fun `toggleTheme with false calls setDarkMode with false`() = runTest {
    val userPreferences =
        UserPreferences(
            dangerModePreferences = defaultDangerModePreferences, themePreferences = true)

    coEvery { repository.getUserPreferences } returns flowOf(userPreferences)
    coEvery { repository.setDarkMode(any()) } just Runs

    viewModel = ThemeViewModel(repository)
    viewModel.toggleTheme(false)
    advanceUntilIdle()

    coVerify(exactly = 1) { repository.setDarkMode(false) }
  }

  @Test
  fun `toggleTheme called multiple times calls repository correctly`() = runTest {
    val userPreferences =
        UserPreferences(
            dangerModePreferences = defaultDangerModePreferences, themePreferences = false)

    coEvery { repository.getUserPreferences } returns flowOf(userPreferences)
    coEvery { repository.setAlertMode(any()) } just Runs

    viewModel = ThemeViewModel(repository)

    viewModel.toggleTheme(true)
    viewModel.toggleTheme(false)
    viewModel.toggleTheme(true)
    advanceUntilIdle()

    coVerify(exactly = 2) { repository.setDarkMode(true) }
    coVerify(exactly = 1) { repository.setDarkMode(false) }
  }

  @Test
  fun `isDarkMode when preferences change emits new value`() = runTest {
    val lightPreferences =
        UserPreferences(
            dangerModePreferences = defaultDangerModePreferences, themePreferences = false)
    val darkPreferences =
        UserPreferences(
            dangerModePreferences = defaultDangerModePreferences, themePreferences = true)

    coEvery { repository.getUserPreferences } returns flowOf(lightPreferences, darkPreferences)

    viewModel = ThemeViewModel(repository)

    val job = backgroundScope.launch { viewModel.isDarkMode.collect {} }
    advanceUntilIdle()

    assertTrue(viewModel.isDarkMode.value ?: false)
    job.cancel()
  }
}
