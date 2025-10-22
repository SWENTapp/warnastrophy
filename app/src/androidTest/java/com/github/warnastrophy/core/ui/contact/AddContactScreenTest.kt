package com.github.warnastrophy.core.ui.contact

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.warnastrophy.core.data.repository.ContactsRepository
import com.github.warnastrophy.core.data.repository.MockContactRepository
import com.github.warnastrophy.core.model.Contact
import com.github.warnastrophy.core.ui.profile.contact.AddContactScreen
import com.github.warnastrophy.core.ui.profile.contact.AddContactTestTags
import com.github.warnastrophy.core.ui.profile.contact.AddContactViewModel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class AddContactScreenTest {
  val UI_WAIT_TIMEOUT = 5000L
  @get:Rule val composeTestRule = createAndroidComposeRule<androidx.activity.ComponentActivity>()
  val repository: ContactsRepository = MockContactRepository()

  @Before
  fun setUp() {
    // ContactRepositoryProvider.repository = MockContactsRepository()
    val mockViewMode = AddContactViewModel(repository = repository)
    composeTestRule.setContent { AddContactScreen(addContactViewModel = mockViewMode) }
  }

  val contact_1 = Contact(id = "a", "Ronaldo", "+41", "Friend")

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
    val text = "Messi"
    composeTestRule.onNodeWithTag(AddContactTestTags.INPUT_FULL_NAME).performTextInput(text)
    composeTestRule.onNodeWithTag(AddContactTestTags.INPUT_FULL_NAME).assertTextContains(text)
    composeTestRule
        .onNodeWithTag(AddContactTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsNotDisplayed()
  }

  @Test
  fun canEnterPhoneNumber() {
    val text = "+41189290266"
    composeTestRule.onNodeWithTag(AddContactTestTags.INPUT_PHONE_NUMBER).performTextInput(text)
    composeTestRule.onNodeWithTag(AddContactTestTags.INPUT_PHONE_NUMBER).assertTextContains(text)
    composeTestRule
        .onNodeWithTag(AddContactTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsNotDisplayed()
  }

  @Test
  fun canEnterRelationship() {
    val text = "Work"
    composeTestRule.onNodeWithTag(AddContactTestTags.INPUT_RELATIONSHIP).performTextInput(text)
    composeTestRule.onNodeWithTag(AddContactTestTags.INPUT_RELATIONSHIP).assertTextContains(text)
    composeTestRule
        .onNodeWithTag(AddContactTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsNotDisplayed()
  }

  @Test
  fun canEnterInvalidPhoneNumber() {
    val text = "+411892902"
    composeTestRule.onNodeWithTag(AddContactTestTags.INPUT_PHONE_NUMBER).performTextInput(text)
    composeTestRule.onNodeWithTag(AddContactTestTags.INPUT_PHONE_NUMBER).assertTextContains(text)
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
    val numberOfContacts: Int = runBlocking {
      val result = repository.getAllContacts()
      result.getOrNull()?.size ?: 0
    }
    composeTestRule
        .onNodeWithTag(AddContactTestTags.INPUT_FULL_NAME)
        .performTextInput(contact_1.fullName)
    composeTestRule
        .onNodeWithTag(AddContactTestTags.INPUT_PHONE_NUMBER)
        .performTextInput(contact_1.phoneNumber)
    composeTestRule
        .onNodeWithTag(AddContactTestTags.INPUT_RELATIONSHIP)
        .performTextInput(contact_1.relationship)
    composeTestRule
        .onNodeWithTag(AddContactTestTags.CONTACT_SAVE)
        .assertIsDisplayed()
        .performClick()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) { true }
    composeTestRule.onNodeWithTag(AddContactTestTags.CONTACT_SAVE).assertIsDisplayed()
    runTest {
      val expectedContactSize = repository.getAllContacts().getOrThrow().size
      assertEquals(expectedContactSize, numberOfContacts)
    }
  }

  @Test
  fun savingWithEmptyFullNameShouldDoNothing() {
    val numberOfContacts: Int = runBlocking {
      val result = repository.getAllContacts()
      result.getOrNull()?.size ?: 0
    }
    composeTestRule.onNodeWithTag(AddContactTestTags.INPUT_FULL_NAME).performTextInput(" ")
    composeTestRule
        .onNodeWithTag(AddContactTestTags.INPUT_PHONE_NUMBER)
        .performTextInput(contact_1.phoneNumber)
    composeTestRule
        .onNodeWithTag(AddContactTestTags.INPUT_RELATIONSHIP)
        .performTextInput(contact_1.relationship)
    composeTestRule
        .onNodeWithTag(AddContactTestTags.CONTACT_SAVE)
        .assertIsDisplayed()
        .performClick()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) { true }
    composeTestRule.onNodeWithTag(AddContactTestTags.CONTACT_SAVE).assertIsDisplayed()
    runTest {
      val expectedContactSize = repository.getAllContacts().getOrThrow().size
      assertEquals(expectedContactSize, numberOfContacts)
    }
  }

  @Test
  fun savingWithEmptyRelationshipShouldDoNothing() {
    val numberOfContacts: Int = runBlocking {
      val result = repository.getAllContacts()
      result.getOrNull()?.size ?: 0
    }
    composeTestRule
        .onNodeWithTag(AddContactTestTags.INPUT_FULL_NAME)
        .performTextInput(contact_1.fullName)
    composeTestRule
        .onNodeWithTag(AddContactTestTags.INPUT_PHONE_NUMBER)
        .performTextInput(contact_1.phoneNumber)
    composeTestRule.onNodeWithTag(AddContactTestTags.INPUT_RELATIONSHIP).performTextInput(" ")
    composeTestRule
        .onNodeWithTag(AddContactTestTags.CONTACT_SAVE)
        .assertIsDisplayed()
        .performClick()
    composeTestRule.waitUntil(UI_WAIT_TIMEOUT) { true }
    composeTestRule.onNodeWithTag(AddContactTestTags.CONTACT_SAVE).assertIsDisplayed()
    runTest {
      val expectedContactSize = repository.getAllContacts().getOrThrow().size
      assertEquals(expectedContactSize, numberOfContacts)
    }
  }
}
