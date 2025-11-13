package com.github.warnastrophy.core.ui.features.contact

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import com.github.warnastrophy.core.data.repository.MockContactRepository
import com.github.warnastrophy.core.model.Contact
import org.junit.Before
import org.junit.Test

class EditContactScreenTest : UITest() {
  val contact_1 = Contact(id = "a", "Ronaldo", "+41", "Friend")

  @Before
  override fun setUp() {
    super.setUp()
    repository = MockContactRepository()
    val mockViewModel = EditContactViewModel(repository)
    composeTestRule.setContent { EditContactScreen(editContactViewModel = mockViewModel) }
  }

  @Test
  fun displayAllComponents() {
    composeTestRule
        .onNodeWithTag(EditContactTestTags.SAVE_BUTTON)
        .assertTextContains("Save Contact")
    composeTestRule.onNodeWithTag(EditContactTestTags.INPUT_FULL_NAME).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditContactTestTags.INPUT_PHONE_NUMBER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditContactTestTags.INPUT_RELATIONSHIP).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditContactTestTags.DELETE_BUTTON).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EditContactTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsNotDisplayed()
  }

  @Test
  fun canEnterFullName() {
    val text = "Messi"
    composeTestRule.enterEditFullName(text)
    composeTestRule.onNodeWithTag(EditContactTestTags.INPUT_FULL_NAME).assertTextContains(text)
    composeTestRule
        .onNodeWithTag(EditContactTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsNotDisplayed()
  }

  @Test
  fun canEnterPhoneNumber() {
    val text = "+41189290266"
    composeTestRule.enterEditPhoneNumber(text)
    composeTestRule.onNodeWithTag(EditContactTestTags.INPUT_PHONE_NUMBER).assertTextContains(text)
    composeTestRule
        .onNodeWithTag(EditContactTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsNotDisplayed()
  }

  @Test
  fun canEnterRelationship() {
    val text = "Work"
    composeTestRule.enterEditRelationship(text)
    composeTestRule.onNodeWithTag(EditContactTestTags.INPUT_RELATIONSHIP).assertTextContains(text)
    composeTestRule
        .onNodeWithTag(EditContactTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsNotDisplayed()
  }

  @Test
  fun canEnterInvalidPhoneNumber() {
    val text = "+411892902"
    composeTestRule.enterEditPhoneNumber(text)
    composeTestRule.onNodeWithTag(EditContactTestTags.INPUT_PHONE_NUMBER).assertTextContains(text)
  }

  @Test
  fun enteringEmptyFullNameShowsErrorMessage() {
    val invalidText = " "
    composeTestRule.enterEditFullName(invalidText)
    composeTestRule
        .onNodeWithTag(EditContactTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun enteringEmptyRelationshipShowsErrorMessage() {
    val invalidText = " "
    composeTestRule.enterEditRelationship(invalidText)
    composeTestRule
        .onNodeWithTag(EditContactTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun enteringInvalidPhoneNumberShowsErrorMessage() {
    val invalidText = "+411892902"
    composeTestRule.enterEditPhoneNumber(invalidText)
    composeTestRule
        .onNodeWithTag(EditContactTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun savingWithEmptyFullNameShouldDoNothing() {
    composeTestRule.enterEditFullName(" ")
    composeTestRule.enterEditPhoneNumber("+41654186477")
    composeTestRule.enterEditRelationship(contact_1.relationship)
    composeTestRule.clickOnSaveContact(testTag = EditContactTestTags.SAVE_BUTTON)
    composeTestRule.onNodeWithTag(AddContactTestTags.SAVE_BUTTON).assertIsDisplayed()
  }

  @Test
  fun savingWithEmptyRelationshipShouldDoNothing() {
    composeTestRule.enterEditFullName(contact_1.fullName)
    composeTestRule.enterEditPhoneNumber("+41654186477")
    composeTestRule.enterEditRelationship(" ")
    composeTestRule.clickOnSaveContact(testTag = EditContactTestTags.SAVE_BUTTON)
    composeTestRule.onNodeWithTag(AddContactTestTags.SAVE_BUTTON).assertIsDisplayed()
  }

  @Test
  fun savingWithInvalidPhoneNumberShouldDoNothing() {
    composeTestRule.enterEditFullName(contact_1.fullName)
    composeTestRule.enterEditPhoneNumber(contact_1.phoneNumber)
    composeTestRule.enterEditRelationship(contact_1.relationship)
    composeTestRule.clickOnSaveContact(testTag = EditContactTestTags.SAVE_BUTTON)
    composeTestRule.onNodeWithTag(AddContactTestTags.SAVE_BUTTON).assertIsDisplayed()
  }
}
