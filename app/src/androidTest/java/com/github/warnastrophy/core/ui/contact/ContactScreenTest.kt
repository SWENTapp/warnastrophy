package com.github.warnastrophy.core.ui.contact

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.github.warnastrophy.core.data.repository.ContactsRepository
import com.github.warnastrophy.core.data.repository.MockContactRepository
import com.github.warnastrophy.core.ui.profile.contact.AddContactTestTags
import com.github.warnastrophy.core.ui.profile.contact.EditContactTestTags
import com.github.warnastrophy.core.ui.util.BaseAndroidComposeTest
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest

abstract class ContactScreenTest : BaseAndroidComposeTest() {
  val repository: ContactsRepository = MockContactRepository()

  // The setup function must be implemented in subclasses
  // to provide the specific type of repo they need

  fun ComposeTestRule.enterEditFullName(fullName: String) {
    onNodeWithTag(EditContactTestTags.INPUT_FULL_NAME).performTextClearance()
    onNodeWithTag(EditContactTestTags.INPUT_FULL_NAME).performTextInput(fullName)
  }

  fun ComposeTestRule.enterEditPhoneNumber(phoneNumber: String) {
    onNodeWithTag(EditContactTestTags.INPUT_PHONE_NUMBER).performTextClearance()
    onNodeWithTag(EditContactTestTags.INPUT_PHONE_NUMBER).performTextInput(phoneNumber)
  }

  fun ComposeTestRule.enterEditRelationship(relationship: String) {
    onNodeWithTag(EditContactTestTags.INPUT_RELATIONSHIP).performTextClearance()
    onNodeWithTag(EditContactTestTags.INPUT_RELATIONSHIP).performTextInput(relationship)
  }

  fun ComposeTestRule.enterAddFullName(fullName: String) =
      onNodeWithTag(AddContactTestTags.INPUT_FULL_NAME).performTextInput(fullName)

  fun ComposeTestRule.enterAddPhoneNumber(phoneNumber: String) =
      onNodeWithTag(AddContactTestTags.INPUT_PHONE_NUMBER).performTextInput(phoneNumber)

  fun ComposeTestRule.enterAddRelationship(relationship: String) =
      onNodeWithTag(AddContactTestTags.INPUT_RELATIONSHIP).performTextInput(relationship)

  fun ComposeTestRule.clickOnSaveForAddContact(waitForRedirection: Boolean = false) {
    onNodeWithTag(AddContactTestTags.CONTACT_SAVE).assertIsDisplayed().performClick()
    waitUntil(defaultTimeout) {
      !waitForRedirection ||
          onAllNodesWithTag(AddContactTestTags.CONTACT_SAVE).fetchSemanticsNodes().isEmpty()
    }
  }

  fun checkNoContactWereAdded(action: () -> Unit) {
    val beforeNumberOfContacts = runBlocking {
      val result = repository.getAllContacts()
      result.getOrNull()?.size ?: 0
    }
    action()
    runTest {
      val afterNumberOfContacts = repository.getAllContacts().getOrThrow().size
      assertEquals(beforeNumberOfContacts, afterNumberOfContacts)
    }
  }
}
