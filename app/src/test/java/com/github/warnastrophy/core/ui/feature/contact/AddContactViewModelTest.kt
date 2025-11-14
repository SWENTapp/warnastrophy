package com.github.warnastrophy.core.ui.feature.contact

import com.github.warnastrophy.core.data.repository.MockContactRepository
import com.github.warnastrophy.core.domain.model.Contact
import com.github.warnastrophy.core.ui.features.profile.contact.AddContactViewModel
import junit.framework.TestCase
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddContactViewModelTest {
  private lateinit var repository: MockContactRepository
  private lateinit var viewModel: AddContactViewModel
  private val testDispatcher = StandardTestDispatcher()

  private val contact1 = Contact("1", "Alice Johnson", "+1234567890", "Family")

  @OptIn(ExperimentalCoroutinesApi::class)
  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    repository = MockContactRepository()
    viewModel = AddContactViewModel(repository = repository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  /**
   * Tests that [addContact] successfully adds a valid contact to the repository, emits
   * [navigateBack] event, and ensures that any previous error message in [uiState] is cleared upon
   * success.
   */
  fun `add contact successfully emits navigateBack and clears error`() = runTest {
    // Setup: Collect the navigateBack event flow concurrently
    val navigateBackEvent = async { viewModel.navigateBack.firstOrNull() }
    // Set valid UI state
    viewModel.setFullName(contact1.fullName)
    viewModel.setPhoneNumber(contact1.phoneNumber)
    viewModel.setRelationShip(contact1.relationship)

    viewModel.addContact()
    advanceUntilIdle()

    val added = repository.getAllContacts().getOrNull()!!
    TestCase.assertEquals(1, added.size)
    TestCase.assertEquals(contact1.fullName, added[0].fullName)

    TestCase.assertNotNull(navigateBackEvent.await())

    // Check error cleared
    TestCase.assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  /**
   * Tests that [addContact] correctly handles an invalid UI state (e.g., missing required field).
   * It should set an appropriate error message in [uiState].
   */
  fun `add contact with invalid UI state sets error message`() = runTest {
    // Setup: Collect the navigateBack event flow concurrently with a timeout
    val navigateBackEvent = async {
      withTimeoutOrNull(100.milliseconds) {
        viewModel.navigateBack.first() // first() throws if timeout reached before emission
      }
    }

    viewModel.setFullName("")
    viewModel.setPhoneNumber(contact1.phoneNumber)
    viewModel.setRelationShip(contact1.relationship)

    viewModel.addContact()
    advanceUntilIdle()

    TestCase.assertNull(navigateBackEvent.await())

    TestCase.assertEquals("At least one field is not valid!", viewModel.uiState.value.errorMsg)
  }

  @Test
  /**
   * Tests the core principle of using [MutableSharedFlow] for navigation events.
   * * It ensures that after the ViewModel successfully emits the [navigateBack] event upon contact
   *   creation (consuming the first event), no subsequent event remains buffered or is immediately
   *   re-emitted to a new collector. This validates that the event is truly **transient**
   *   (fire-and-forget) and confirms that the separate [resetNavigation] call is correctly obsolete
   *   for this pattern.
   */
  fun `MapsBack event is transient (no need for reset)`() = runTest {
    // 1. Setup: Collect the first event
    val firstEvent = async {
      withTimeoutOrNull(100.milliseconds) { viewModel.navigateBack.first() }
    }

    viewModel.setFullName(contact1.fullName)
    viewModel.setPhoneNumber(contact1.phoneNumber)
    viewModel.setRelationShip(contact1.relationship)

    viewModel.addContact()
    advanceUntilIdle()

    TestCase.assertNotNull(firstEvent.await())

    val secondEvent =
        withTimeoutOrNull(10.milliseconds) {
          viewModel.navigateBack.first() // If an event arrives in 10ms, this returns Unit
        }

    TestCase.assertNull(secondEvent)
  }
}
