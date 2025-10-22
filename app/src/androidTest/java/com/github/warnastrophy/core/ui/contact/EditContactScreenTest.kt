package com.github.warnastrophy.core.ui.contact

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextReplacement
import com.github.warnastrophy.core.data.repository.ContactsRepository
import com.github.warnastrophy.core.data.repository.MockContactRepository
import com.github.warnastrophy.core.model.Contact
import com.github.warnastrophy.core.ui.profile.contact.EditContactScreen
import com.github.warnastrophy.core.ui.profile.contact.EditContactTestTags
import com.github.warnastrophy.core.ui.profile.contact.EditContactViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EditContactScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()
  val repository: ContactsRepository = MockContactRepository()
  private val mockContacts =
      mutableListOf(
          Contact("1", "Alice Johnson", "+1234567890", "Family"),
          Contact("2", "Dr. Robert Smith", "+9876543210", "Doctor"),
          Contact("3", "Chloé Dupont", "+41791234567", "Friend"),
          Contact("4", "Emergency Services", "911", "Critical"),
          Contact("5", "Michael Brown", "+447700900000", "Colleague"),
          Contact("6", "Grandma Sue", "+15551234567", "Family"),
          Contact("7", "Mr. Chen", "+8613800000000", "Neighbor"),
          Contact("8", "Security Guard Bob", "+18005551212", "Work"),
          Contact("9", "Zack Taylor", "+12341234123", "Friend"),
          Contact("10", "Yara Habib", "+971501112222", "Family"),
      )

  @Before
  fun setUp() {
    // ContactRepositoryProvider.repository = MockContactsRepository()
    runTest { mockContacts.forEach { repository.addContact(it) } }
    val mockViewModel = EditContactViewModel(repository)
    composeTestRule.setContent { EditContactScreen(editContactViewModel = mockViewModel) }
  }

  @Test
  fun displayAllComponents() {
    // composeTestRule.setContent { EditContactScreen() }
    composeTestRule
        .onNodeWithTag(EditContactTestTags.CONTACT_SAVE)
        .assertTextContains("Save Contact")
    composeTestRule.onNodeWithTag(EditContactTestTags.INPUT_FULL_NAME).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditContactTestTags.INPUT_PHONE_NUMBER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditContactTestTags.INPUT_RELATIONSHIP).assertIsDisplayed()
    composeTestRule.onNodeWithTag(EditContactTestTags.CONTACT_DELETE).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(EditContactTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsNotDisplayed()
  }

  @Test
  fun canEnterFullName() {
    val text = "Messi"
    composeTestRule.onNodeWithTag(EditContactTestTags.INPUT_FULL_NAME).performTextReplacement(text)
    composeTestRule.onNodeWithTag(EditContactTestTags.INPUT_FULL_NAME).assertTextContains(text)
    composeTestRule
        .onNodeWithTag(EditContactTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsNotDisplayed()
  }

  @Test
  fun canEnterPhoneNumber() {
    val text = "+41189290266"
    composeTestRule
        .onNodeWithTag(EditContactTestTags.INPUT_PHONE_NUMBER)
        .performTextReplacement(text)
    composeTestRule.onNodeWithTag(EditContactTestTags.INPUT_PHONE_NUMBER).assertTextContains(text)
    composeTestRule
        .onNodeWithTag(EditContactTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsNotDisplayed()
  }

  @Test
  fun canEnterRelationship() {
    val text = "Work"
    composeTestRule
        .onNodeWithTag(EditContactTestTags.INPUT_RELATIONSHIP)
        .performTextReplacement(text)
    composeTestRule.onNodeWithTag(EditContactTestTags.INPUT_RELATIONSHIP).assertTextContains(text)
    composeTestRule
        .onNodeWithTag(EditContactTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsNotDisplayed()
  }

  @Test
  fun canEnterInvalidPhoneNumber() {
    val text = "+411892902"
    composeTestRule
        .onNodeWithTag(EditContactTestTags.INPUT_PHONE_NUMBER)
        .performTextReplacement(text)
    composeTestRule.onNodeWithTag(EditContactTestTags.INPUT_PHONE_NUMBER).assertTextContains(text)
  }

  @Test
  fun enteringEmptyFullNameShowsErrorMessage() {
    val invalidText = " "
    composeTestRule
        .onNodeWithTag(EditContactTestTags.INPUT_FULL_NAME)
        .performTextReplacement(invalidText)
    composeTestRule
        .onNodeWithTag(EditContactTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun enteringEmptyRelationshipShowsErrorMessage() {
    val invalidText = " "
    composeTestRule
        .onNodeWithTag(EditContactTestTags.INPUT_RELATIONSHIP)
        .performTextReplacement(invalidText)
    composeTestRule
        .onNodeWithTag(EditContactTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun enteringInvalidPhoneNumberShowsErrorMessage() {
    val invalidText = "+411892902"
    composeTestRule
        .onNodeWithTag(EditContactTestTags.INPUT_PHONE_NUMBER)
        .performTextReplacement(invalidText)
    composeTestRule
        .onNodeWithTag(EditContactTestTags.ERROR_MESSAGE, useUnmergedTree = true)
        .assertIsDisplayed()
  }
}
