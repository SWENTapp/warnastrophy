package com.github.warnastrophy.core.ui.contact

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.warnastrophy.core.data.repository.ContactsRepository
import com.github.warnastrophy.core.data.repository.MockContactRepository
import com.github.warnastrophy.core.model.Contact
import com.github.warnastrophy.core.ui.profile.contact.AddContactScreen
import com.github.warnastrophy.core.ui.profile.contact.AddContactTestTags
import com.github.warnastrophy.core.ui.profile.contact.AddContactViewModel
import com.github.warnastrophy.core.ui.util.BaseAndroidComposeTest
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class AddContactScreenTest : BaseAndroidComposeTest() {
  private val repository: ContactsRepository = MockContactRepository()
  private val contact1 = Contact(id = "a", "Ronaldo", "+41", "Friend")

  @Before
  override fun setUp() {
    super.setUp()
    val mockViewModel = AddContactViewModel(repository = repository)
    composeTestRule.setContent { AddContactScreen(addContactViewModel = mockViewModel) }
  }

  @Test
  fun displayAllComponents() {
    composeTestRule
        .onNodeWithTag(AddContactTestTags.CONTACT_SAVE)
        .assertTextContains("Save Contact")
    composeTestRule.onNodeWithTag(AddContactTestTags.INPUT_FULL_NAME).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AddContactTestTags.INPUT_PHONE_NUMBER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AddContactTestTags.INPUT_RELATIONSHIP).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(AddContactTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsNotDisplayed()
  }

  @Test
  fun canEnterFullName() {
    val fullName = "Messi"
    composeTestRule.onNodeWithTag(AddContactTestTags.INPUT_FULL_NAME).performTextInput(fullName)

    composeTestRule.onNodeWithTag(AddContactTestTags.INPUT_FULL_NAME).assertTextContains(fullName)

    composeTestRule
        .onNodeWithTag(AddContactTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsNotDisplayed()
  }

  @Test
  fun canEnterPhoneNumber() {
    val phoneNumber = "+41189290266"
    composeTestRule
        .onNodeWithTag(AddContactTestTags.INPUT_PHONE_NUMBER)
        .performTextInput(phoneNumber)

    composeTestRule
        .onNodeWithTag(AddContactTestTags.INPUT_PHONE_NUMBER)
        .assertTextContains(phoneNumber)

    composeTestRule
        .onNodeWithTag(AddContactTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsNotDisplayed()
  }

  @Test
  fun canEnterRelationship() {
    val relationship = "Work"
    composeTestRule
        .onNodeWithTag(AddContactTestTags.INPUT_RELATIONSHIP)
        .performTextInput(relationship)

    composeTestRule
        .onNodeWithTag(AddContactTestTags.INPUT_RELATIONSHIP)
        .assertTextContains(relationship)

    composeTestRule
        .onNodeWithTag(AddContactTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsNotDisplayed()
  }

  @Test
  fun canEnterInvalidPhoneNumber() {
    val phoneNumber = "+411892902"
    composeTestRule
        .onNodeWithTag(AddContactTestTags.INPUT_PHONE_NUMBER)
        .performTextInput(phoneNumber)

    composeTestRule
        .onNodeWithTag(AddContactTestTags.INPUT_PHONE_NUMBER)
        .assertTextContains(phoneNumber)
  }

  @Test
  fun enteringEmptyFullNameShowsErrorMessage() {
    val invalidText = " "
    composeTestRule.onNodeWithTag(AddContactTestTags.INPUT_FULL_NAME).performTextInput(invalidText)

    composeTestRule
        .onNodeWithTag(AddContactTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun enteringEmptyRelationshipShowsErrorMessage() {
    val invalidText = " "
    composeTestRule
        .onNodeWithTag(AddContactTestTags.INPUT_RELATIONSHIP)
        .performTextInput(invalidText)

    composeTestRule
        .onNodeWithTag(AddContactTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun enteringInvalidPhoneNumberShowsErrorMessage() {
    val invalidText = "+411892902"
    composeTestRule
        .onNodeWithTag(AddContactTestTags.INPUT_PHONE_NUMBER)
        .performTextInput(invalidText)

    composeTestRule
        .onNodeWithTag(AddContactTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun savingWithInvalidFullNameShouldDoNothing() {
    val numberOfContacts = runBlocking {
      val result = repository.getAllContacts()
      result.getOrNull()?.size ?: 0
    }

    composeTestRule
        .onNodeWithTag(AddContactTestTags.INPUT_FULL_NAME)
        .performTextInput(contact1.fullName)

    composeTestRule
        .onNodeWithTag(AddContactTestTags.INPUT_PHONE_NUMBER)
        .performTextInput(contact1.phoneNumber)

    composeTestRule
        .onNodeWithTag(AddContactTestTags.INPUT_RELATIONSHIP)
        .performTextInput(contact1.relationship)

    composeTestRule
        .onNodeWithTag(AddContactTestTags.CONTACT_SAVE)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdleWithTimeout(defaultTimeout)
    composeTestRule.onNodeWithTag(AddContactTestTags.CONTACT_SAVE).assertIsDisplayed()

    runTest {
      val expectedContactSize = repository.getAllContacts().getOrThrow().size
      assertEquals(expectedContactSize, numberOfContacts)
    }
  }

  @Test
  fun savingWithEmptyFullNameShouldDoNothing() {
    val numberOfContacts = runBlocking {
      val result = repository.getAllContacts()
      result.getOrNull()?.size ?: 0
    }

    composeTestRule.onNodeWithTag(AddContactTestTags.INPUT_FULL_NAME).performTextInput(" ")

    composeTestRule
        .onNodeWithTag(AddContactTestTags.INPUT_PHONE_NUMBER)
        .performTextInput(contact1.phoneNumber)

    composeTestRule
        .onNodeWithTag(AddContactTestTags.INPUT_RELATIONSHIP)
        .performTextInput(contact1.relationship)

    composeTestRule
        .onNodeWithTag(AddContactTestTags.CONTACT_SAVE)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdleWithTimeout(defaultTimeout)
    composeTestRule.onNodeWithTag(AddContactTestTags.CONTACT_SAVE).assertIsDisplayed()

    runTest {
      val expectedContactSize = repository.getAllContacts().getOrThrow().size
      assertEquals(expectedContactSize, numberOfContacts)
    }
  }

  @Test
  fun savingWithEmptyRelationshipShouldDoNothing() {
    val numberOfContacts = runBlocking {
      val result = repository.getAllContacts()
      result.getOrNull()?.size ?: 0
    }

    composeTestRule
        .onNodeWithTag(AddContactTestTags.INPUT_FULL_NAME)
        .performTextInput(contact1.fullName)

    composeTestRule
        .onNodeWithTag(AddContactTestTags.INPUT_PHONE_NUMBER)
        .performTextInput(contact1.phoneNumber)

    composeTestRule.onNodeWithTag(AddContactTestTags.INPUT_RELATIONSHIP).performTextInput(" ")

    composeTestRule
        .onNodeWithTag(AddContactTestTags.CONTACT_SAVE)
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdleWithTimeout(defaultTimeout)
    composeTestRule.onNodeWithTag(AddContactTestTags.CONTACT_SAVE).assertIsDisplayed()

    runTest {
      val expectedContactSize = repository.getAllContacts().getOrThrow().size
      assertEquals(expectedContactSize, numberOfContacts)
    }
  }
}
