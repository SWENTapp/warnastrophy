package com.github.warnastrophy.core.ui.features.contact

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.github.warnastrophy.core.data.repository.ContactsRepository
import com.github.warnastrophy.core.util.BaseAndroidComposeTest
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest

abstract class UITest : BaseAndroidComposeTest() {
  lateinit var repository: ContactsRepository

  /**
   * Enters the provided full name into the Full Name input field on the Edit Contact screen,
   * clearing any existing text first.
   *
   * @param fullName The text to input into the full name field.
   */
  fun ComposeTestRule.enterEditFullName(fullName: String) {
    onNodeWithTag(EditContactTestTags.INPUT_FULL_NAME).performTextClearance()
    onNodeWithTag(EditContactTestTags.INPUT_FULL_NAME).performTextInput(fullName)
  }

  /**
   * Enters the provided phone number into the Phone Number input field on the Edit Contact screen,
   * clearing any existing text first.
   *
   * @param phoneNumber The text to input into the phone number field.
   */
  fun ComposeTestRule.enterEditPhoneNumber(phoneNumber: String) {
    onNodeWithTag(EditContactTestTags.INPUT_PHONE_NUMBER).performTextClearance()
    onNodeWithTag(EditContactTestTags.INPUT_PHONE_NUMBER).performTextInput(phoneNumber)
  }

  /**
   * Enters the provided relationship into the Relationship input field on the Edit Contact screen,
   * clearing any existing text first.
   *
   * @param relationship The text to input into the relationship field.
   */
  fun ComposeTestRule.enterEditRelationship(relationship: String) {
    onNodeWithTag(EditContactTestTags.INPUT_RELATIONSHIP).performTextClearance()
    onNodeWithTag(EditContactTestTags.INPUT_RELATIONSHIP).performTextInput(relationship)
  }

  /**
   * Enters the provided full name into the Full Name input field on the Add Contact screen.
   *
   * @param fullName The text to input into the full name field.
   */
  fun ComposeTestRule.enterAddFullName(fullName: String) =
      onNodeWithTag(AddContactTestTags.INPUT_FULL_NAME).performTextInput(fullName)

  /**
   * Enters the provided phone number into the Phone Number input field on the Add Contact screen.
   *
   * @param phoneNumber The text to input into the phone number field.
   */
  fun ComposeTestRule.enterAddPhoneNumber(phoneNumber: String) =
      onNodeWithTag(AddContactTestTags.INPUT_PHONE_NUMBER).performTextInput(phoneNumber)

  /**
   * Enters the provided relationship into the Relationship input field on the Add Contact screen.
   *
   * @param relationship The text to input into the relationship field.
   */
  fun ComposeTestRule.enterAddRelationship(relationship: String) =
      onNodeWithTag(AddContactTestTags.INPUT_RELATIONSHIP).performTextInput(relationship)

  /**
   * Clicks the 'Save' button on the Add Contact screen.
   *
   * It asserts the button is displayed, performs the click, and then waits for the save button to
   * disappear if [waitForRedirection] is true (indicating navigation away from the screen).
   *
   * @param waitForRedirection If true, the function waits until the save button is gone from the
   *   hierarchy.
   */
  fun ComposeTestRule.clickOnSaveContact(waitForRedirection: Boolean = false, testTag: String) {
    onNodeWithTag(testTag).assertIsDisplayed().performClick()
    waitUntil(defaultTimeout) {
      !waitForRedirection ||
          onAllNodesWithTag(
                  testTag =
                      if (testTag == AddContactTestTags.SAVE_BUTTON) AddContactTestTags.SAVE_BUTTON
                      else EditContactTestTags.SAVE_BUTTON)
              .fetchSemanticsNodes()
              .isEmpty()
    }
  }

  /**
   * Executes a given action (e.g., clicking a 'Save' button that is expected to fail validation)
   * and asserts that no new contact was added to the repository.
   *
   * This is typically used to verify that validation errors prevent data persistence.
   *
   * @param action The block of code representing the operation that should *fail* to add a contact.
   */
  fun checkNoContactWereAdded(action: () -> Unit) {
    val beforeNumberOfContacts = runBlocking {
      val result = repository.getAllContacts()
      result.getOrNull()?.size ?: 0
    }
    action()
    runTest {
      val afterNumberOfContacts = repository.getAllContacts().getOrThrow().size
      TestCase.assertEquals(beforeNumberOfContacts, afterNumberOfContacts)
    }
  }
}
