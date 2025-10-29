package com.github.warnastrophy.core.ui.contact

import com.github.warnastrophy.core.data.repository.ContactsRepository
import com.github.warnastrophy.core.data.repository.MockContactRepository
import com.github.warnastrophy.core.model.Contact
import com.github.warnastrophy.core.ui.profile.contact.EditContactViewModel
import junit.framework.TestCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditContactViewModeTest {
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
      repository.addContact(contact1)
      repository.addContact(contact2)

      viewModel = EditContactViewModel(repository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `load Contact Populate UI state`() = runTest {
      viewModel.loadContact("1")
      advanceUntilIdle() // This ensures loadContact() completes and updates uiState.
      val uiState = viewModel.uiState.first()
      TestCase.assertEquals(contact1.fullName, uiState.fullName)
      TestCase.assertEquals(contact1.phoneNumber, uiState.phoneNumber)
      TestCase.assertEquals(contact1.relationship, uiState.relationship)
      TestCase.assertNull(uiState.errorMsg)
  }

  @Test
  fun `Edit contact with valid contact update repository and set navigateBack`() = runTest {
      viewModel.setFullName("Alice Updated")

      viewModel.setPhoneNumber("+11111111111")

      viewModel.setRelationShip("Colleague")

      viewModel.editContact("1")
      // wait for coroutine to finish

      advanceUntilIdle()
      val updated = repository.getContact("1").getOrNull()!!
      advanceUntilIdle()
      TestCase.assertEquals("Alice Updated", updated.fullName)
      TestCase.assertEquals("+11111111111", updated.phoneNumber)
      TestCase.assertEquals("Colleague", updated.relationship)

      val navigateBack = viewModel.navigateBack.first()
      TestCase.assertEquals(true, navigateBack)
  }

  @Test
  fun `Delete contact remove contact and set navigateBack`() = runTest {
      viewModel.deleteContact("2")
      advanceUntilIdle()

      val result = repository.getContact("2")
      advanceUntilIdle()
      TestCase.assertEquals(result.isFailure, true)

      val navigateBack = viewModel.navigateBack.first()
      TestCase.assertEquals(true, navigateBack)
  }
}