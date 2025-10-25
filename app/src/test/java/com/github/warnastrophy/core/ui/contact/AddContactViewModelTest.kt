package com.github.warnastrophy.core.ui.contact

import com.github.warnastrophy.core.data.repository.MockContactRepository
import com.github.warnastrophy.core.model.Contact
import com.github.warnastrophy.core.ui.profile.contact.AddContactViewModel
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
class AddContactViewModelTest {
  private lateinit var repository: MockContactRepository
  private lateinit var viewModel: AddContactViewModel
  private val testDispatcher = StandardTestDispatcher()

  private val contact1 = Contact("1", "Alice Johnson", "+1234567890", "Family")

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
  fun `add contact successfully sets navigateBack and clears error`() = runTest {
      // Set valid UI state
      viewModel.setFullName(contact1.fullName)
      viewModel.setPhoneNumber(contact1.phoneNumber)
      viewModel.setRelationShip(contact1.relationship)

      // Call addContact
      viewModel.addContact()
      advanceUntilIdle() // wait for coroutine to finish

      // Check repository
      val added = repository.getAllContacts().getOrNull()!!
      TestCase.assertEquals(1, added.size)
      TestCase.assertEquals(contact1.fullName, added[0].fullName)

      // Check navigateBack
      val navigateBack = viewModel.navigateBack.first()
      TestCase.assertEquals(true, navigateBack)

      // Check error cleared
      TestCase.assertNull(viewModel.uiState.value.errorMsg)
  }

  @Test
  fun `add contact with invalid UI state sets error message`() = runTest {
      // Leave fullName blank to trigger validation
      viewModel.setFullName("")
      viewModel.setPhoneNumber(contact1.phoneNumber)
      viewModel.setRelationShip(contact1.relationship)

      // Call addContact
      viewModel.addContact()
      advanceUntilIdle()

      // Check navigateBack is false
      val navigateBack = viewModel.navigateBack.first()
      TestCase.assertEquals(false, navigateBack)

      // Check error message
      TestCase.assertEquals("At least one field is not valid!", viewModel.uiState.value.errorMsg)
  }

  @Test
  fun `resetNavigation sets navigateBack to false`() = runTest {
      viewModel.setFullName(contact1.fullName)
      viewModel.setPhoneNumber(contact1.phoneNumber)
      viewModel.setRelationShip(contact1.relationship)

      viewModel.addContact()
      advanceUntilIdle()

      // navigateBack should be true now
      TestCase.assertEquals(true, viewModel.navigateBack.first())

      // Reset navigation
      viewModel.resetNavigation()
      TestCase.assertEquals(false, viewModel.navigateBack.first())
  }
}