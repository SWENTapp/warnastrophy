package com.github.warnastrophy.core.ui.contact

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import com.github.warnastrophy.core.domain.model.Contact
import com.github.warnastrophy.core.ui.features.profile.contact.AddContactScreen
import com.github.warnastrophy.core.ui.features.profile.contact.AddContactTestTags
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test

@HiltAndroidTest
class AddContactScreenTest : UITest() {
  private val contact1 = Contact(id = "a", "Ronaldo", "+41", "Friend")

  @Before
  override fun setUp() {
    hiltRule.inject()
    composeTestRule.setContent { AddContactScreen() }
  }

  @After
  override fun tearDown() = runBlocking {
    val contacts = repository.getAllContacts().getOrNull() ?: emptyList()
    contacts.forEach { contact -> repository.deleteContact(contact.id) }

    @Test
    fun displayAllComponents() {
      composeTestRule
          .onNodeWithTag(AddContactTestTags.SAVE_BUTTON)
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
      composeTestRule.enterAddFullName(fullName)
      composeTestRule.onNodeWithTag(AddContactTestTags.INPUT_FULL_NAME).assertTextContains(fullName)
      composeTestRule
          .onNodeWithTag(AddContactTestTags.ERROR_MESSAGE, useUnmergedTree = true)
          .assertIsNotDisplayed()
    }

    @Test
    fun canEnterPhoneNumber() {
      val phoneNumber = "+41189290266"
      composeTestRule.enterAddPhoneNumber(phoneNumber)
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
      composeTestRule.enterAddRelationship(relationship)

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
      composeTestRule.enterAddPhoneNumber(phoneNumber)

      composeTestRule
          .onNodeWithTag(AddContactTestTags.INPUT_PHONE_NUMBER)
          .assertTextContains(phoneNumber)
    }

    @Test
    fun enteringEmptyFullNameShowsErrorMessage() {
      val invalidText = " "
      composeTestRule.enterAddFullName(invalidText)
      composeTestRule
          .onNodeWithTag(AddContactTestTags.ERROR_MESSAGE, useUnmergedTree = true)
          .assertIsDisplayed()
    }

    @Test
    fun enteringEmptyRelationshipShowsErrorMessage() {
      val invalidText = " "
      composeTestRule.enterAddRelationship(invalidText)
      composeTestRule
          .onNodeWithTag(AddContactTestTags.ERROR_MESSAGE, useUnmergedTree = true)
          .assertIsDisplayed()
    }

    @Test
    fun enteringInvalidPhoneNumberShowsErrorMessage() {
      val invalidText = "+411892902"
      composeTestRule.enterAddPhoneNumber(invalidText)
      composeTestRule
          .onNodeWithTag(AddContactTestTags.ERROR_MESSAGE, useUnmergedTree = true)
          .assertIsDisplayed()
    }

    @Test
    fun savingWithEmptyFullNameShouldDoNothing() = checkNoContactWereAdded {
      composeTestRule.enterAddFullName(" ")

      composeTestRule.enterAddPhoneNumber("+41678234566")

      composeTestRule.enterAddRelationship(contact1.relationship)

      composeTestRule.clickOnSaveContact(testTag = AddContactTestTags.SAVE_BUTTON)

      composeTestRule.onNodeWithTag(AddContactTestTags.SAVE_BUTTON).assertIsDisplayed()
    }

    @Test
    fun savingWithEmptyRelationshipShouldDoNothing() {
      composeTestRule.enterAddFullName(contact1.fullName)

      composeTestRule.enterAddPhoneNumber("+41587289288")

      composeTestRule.enterAddRelationship(" ")

      composeTestRule.clickOnSaveContact(testTag = AddContactTestTags.SAVE_BUTTON)

      composeTestRule.onNodeWithTag(AddContactTestTags.SAVE_BUTTON).assertIsDisplayed()
    }

    @Test
    fun savingWithInvalidPhoneNumberShouldDoNothing() {
      composeTestRule.enterEditFullName(contact1.fullName)
      composeTestRule.enterEditPhoneNumber(contact1.fullName)
      composeTestRule.enterEditRelationship(contact1.relationship)
      composeTestRule.clickOnSaveContact(testTag = AddContactTestTags.SAVE_BUTTON)
      composeTestRule.onNodeWithTag(AddContactTestTags.SAVE_BUTTON).assertIsDisplayed()
    }
  }
}
