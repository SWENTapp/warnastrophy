package com.github.warnastrophy.core.ui.features

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.github.warnastrophy.core.data.interfaces.ContactsRepository
import com.github.warnastrophy.core.data.repository.ActivityRepository
import com.github.warnastrophy.core.ui.features.contact.AddContactTestTags
import com.github.warnastrophy.core.ui.features.contact.EditContactTestTags
import com.github.warnastrophy.core.ui.features.dashboard.activity.AddActivityTestTags
import com.github.warnastrophy.core.ui.features.dashboard.activity.EditActivityTestTags
import com.github.warnastrophy.core.util.BaseAndroidComposeTest
import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest

abstract class UITest : BaseAndroidComposeTest() {
  lateinit var contactRepository: ContactsRepository
  lateinit var activityRepository: ActivityRepository

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
   * Enters the provided activity name into the Activity Name input field on the Edit Activity
   * screen, clearing any existing text first.
   *
   * @param activityName The text to input into the full name field.
   */
  fun ComposeTestRule.enterEditActivityName(activityName: String) {
    onNodeWithTag(EditActivityTestTags.INPUT_ACTIVITY_NAME).performTextClearance()
    onNodeWithTag(EditActivityTestTags.INPUT_ACTIVITY_NAME).performTextInput(activityName)
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
   * Enters the provided full name into the Activity input field on the Add Activity screen.
   *
   * @param actName The text to input into the full name field.
   */
  fun ComposeTestRule.enterAddActivityName(actName: String) =
      onNodeWithTag(AddActivityTestTags.INPUT_ACTIVITY_NAME).performTextInput(actName)
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
  fun ComposeTestRule.clickOnSaveButton(waitForRedirection: Boolean = false, testTag: String) {
    onNodeWithTag(testTag).assertIsDisplayed().performClick()
    waitUntil(defaultTimeout) {
      !waitForRedirection || onAllNodesWithTag(testTag).fetchSemanticsNodes().isEmpty()
    }
  }

  /**
   * // TODO: remove this Executes a given action (e.g., clicking a 'Save' button that is expected
   * to fail validation) and asserts that no new contact was added to the repository.
   *
   * This is typically used to verify that validation errors prevent data persistence.
   *
   * @param action The block of code representing the operation that should *fail* to add a contact.
   */
  fun checkNoContactWereAdded(action: () -> Unit, userId: String) {
    val beforeNumberOfContacts = runBlocking {
      val result = contactRepository.getAllContacts(userId)
      result.getOrNull()?.size ?: 0
    }
    action()
    runTest {
      val afterNumberOfContacts = contactRepository.getAllContacts(userId).getOrThrow().size
      TestCase.assertEquals(beforeNumberOfContacts, afterNumberOfContacts)
    }
  }

  /**
   * Executes a given action and asserts that the number of entities in the repository remains
   * unchanged.
   *
   * @param action The block of code representing the operation that should *fail* to add an entity.
   * @param userId The ID of the user whose entities are being checked.
   * @param getAllEntities A suspend function that takes the userId and returns a Result<List<T>>. T
   *   is the type of entity (e.g., Contact, Activity).
   */
  fun <T> checkNoEntityWasAdded(
      action: () -> Unit,
      userId: String,
      getAllEntities: suspend (String) -> Result<List<T>>
  ) {
    val beforeNumberOfEntities = runBlocking {
      val result = getAllEntities(userId)
      result.getOrNull()?.size ?: 0
    }
    action()

    runTest {
      val afterNumberOfEntities = getAllEntities(userId).getOrThrow().size
      TestCase.assertEquals(beforeNumberOfEntities, afterNumberOfEntities)
    }
  }
}
