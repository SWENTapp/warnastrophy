package com.github.warnastrophy.core.ui.contact

import com.github.warnastrophy.core.data.interfaces.ContactsRepository
import com.github.warnastrophy.core.data.repository.MockContactRepository
import com.github.warnastrophy.core.model.Contact
import com.github.warnastrophy.core.ui.features.contact.EditContactViewModel
import com.github.warnastrophy.core.util.AppConfig
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertNull
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
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditContactViewModelTest {
  private lateinit var repository: ContactsRepository
  private lateinit var viewModel: EditContactViewModel

  private val contact1 = Contact("1", "Alice Johnson", "+1234567890", "Family")
  private val contact2 = Contact("2", "Bob Smith", "+19876543210", "Friend")

  private val testDispatcher = StandardTestDispatcher()

  @Before
  fun setUp() = runTest {
    Dispatchers.setMain(testDispatcher)
    repository = MockContactRepository()
    // Add some contacts to the repository
    repository.addContact(
        AppConfig.defaultUserId,
        contact1,
    )
    repository.addContact(AppConfig.defaultUserId, contact2)

    viewModel = EditContactViewModel(repository, AppConfig.defaultUserId)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  /**
   * Tests the [loadContact] function. It verifies that when a contact ID is provided, the ViewModel
   * correctly fetches the corresponding contact data from the repository and populates the
   * appropriate fields ([fullName], [phoneNumber], [relationship]) in the [uiState]. It also
   * ensures the error message is null upon successful loading.
   */
  fun `load Contact Populate UI state`() = runTest {
    viewModel.loadContact("1")
    advanceUntilIdle() // This ensures loadContact() completes and updates uiState.
    val uiState = viewModel.uiState.first()
    assertEquals(contact1.fullName, uiState.fullName)
    assertEquals(contact1.phoneNumber, uiState.phoneNumber)
    assertEquals(contact1.relationship, uiState.relationship)
    assertNull(uiState.errorMsg)
  }

  @Test
  /**
   * Tests that [editContact] correctly processes valid UI state, updates the contact in the
   * repository with the new data, and signals a successful operation by emitting the [navigateBack]
   * event.
   *
   * It verifies:
   * 1. The contact data in the repository is successfully updated.
   * 2. The transient [navigateBack] event is emitted, confirming the function completes
   *    successfully and triggers the required navigation action.
   */
  fun `Edit contact with valid contact update repository and emits navigateBack event`() = runTest {
    val navigateBackEvent = async { viewModel.navigateBack.firstOrNull() }
    viewModel.setFullName("Alice Updated")
    viewModel.setPhoneNumber("+11111111111")
    viewModel.setRelationship("Colleague")
    viewModel.editContact("1")

    advanceUntilIdle()

    val updated = repository.getContact(AppConfig.defaultUserId, "1").getOrNull()!!

    assertEquals("Alice Updated", updated.fullName)
    assertEquals("+11111111111", updated.phoneNumber)
    assertEquals("Colleague", updated.relationship)

    assertNotNull(navigateBackEvent.await())
  }

  @Test
  /**
   * Tests that [deleteContact] correctly executes the deletion logic.
   *
   * It verifies:
   * 1. The contact with the specified ID ("2") is successfully removed from the repository,
   *    resulting in a failure when attempting to retrieve it.
   * 2. The transient [navigateBack] event is emitted, confirming the success of the operation and
   *    triggering the necessary navigation action.
   */
  fun `Delete contact remove contact and emits navigateBack event`() = runTest {
    val navigateBackEvent = async { viewModel.navigateBack.firstOrNull() }
    viewModel.deleteContact("2")
    advanceUntilIdle()

    val result = repository.getContact(AppConfig.defaultUserId, "2")
    assertEquals(true, result.isFailure)

    assertNotNull(navigateBackEvent.await())
  }
}
