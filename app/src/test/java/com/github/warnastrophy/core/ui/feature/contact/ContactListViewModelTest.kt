package com.github.warnastrophy.core.ui.contact

import com.github.warnastrophy.core.data.repository.MockContactRepository
import com.github.warnastrophy.core.ui.features.profile.contact.ContactListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ContactListViewModelTest {
  private lateinit var repository: MockContactRepository
  private lateinit var viewModel: ContactListViewModel
  private val testDispatcher = StandardTestDispatcher()
  private val testScope = TestScope(testDispatcher)

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    repository = MockContactRepository()
    viewModel = ContactListViewModel(repository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  /**
   * Tests that the ViewModel correctly handles a failure from the repository during initialization.
   */
  @Test
  fun init_handles_repository_failure() =
      testScope.runTest {
        repository.shouldThrowException = true

        viewModel.refreshUIState()

        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.value
        Assert.assertTrue(uiState.contacts.isEmpty())
        Assert.assertNotNull(uiState.errorMsg)
      }

  /** Tests that calling `clearErrorMsg` clears the error message in the UI state. */
  @Test
  fun clearErrorMsg_clears_error_message_in_UI_state() {
    viewModel.clearErrorMsg()

    viewModel.refreshUIState()

    viewModel.clearErrorMsg()

    val uiState = viewModel.uiState.value
    Assert.assertNull(uiState.errorMsg)
  }
}
