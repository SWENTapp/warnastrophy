package com.github.warnastrophy.core.ui.contact

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.github.warnastrophy.core.data.repository.ContactsRepository
import com.github.warnastrophy.core.data.repository.MockContactRepository
import com.github.warnastrophy.core.model.Contact
import com.github.warnastrophy.core.ui.profile.contact.ContactListScreen
import com.github.warnastrophy.core.ui.profile.contact.ContactListScreenTestTags
import com.github.warnastrophy.core.ui.viewModel.ContactListViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ContactListScreenTest {

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
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()
  val repository: ContactsRepository = MockContactRepository()

  fun setContent(withInitialContacts: List<Contact> = emptyList()) {
    runTest { withInitialContacts.forEach { repository.addContact(it) } }
    val mockViewModel = ContactListViewModel(contactsRepository = repository)
    composeTestRule.setContent { ContactListScreen(contactListViewModel = mockViewModel) }
  }

  @Test
  fun testTagsCorrectlySetWhenListIsEmpty() {
    setContent()
    composeTestRule.onNodeWithTag(ContactListScreenTestTags.CONTACT_LIST).assertIsNotDisplayed()
  }

  @Test
  fun testTagsCorrectlySetWhenListIsNotEmpty() {
    setContent(withInitialContacts = mockContacts)
    composeTestRule.onNodeWithTag(ContactListScreenTestTags.CONTACT_LIST).assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ContactListScreenTestTags.getTestTagForContactItem(mockContacts[0]))
        .assertIsDisplayed()
    composeTestRule
        .onNodeWithTag(ContactListScreenTestTags.getTestTagForContactItem(mockContacts[2]))
        .assertIsDisplayed()
  }

  @Test
  fun contactListDisplaysFullName() {
    val contactList = listOf(mockContacts[3])
    setContent(withInitialContacts = contactList)
    composeTestRule
        .onNode(
            hasTestTag(ContactListScreenTestTags.getTestTagForContactItem(mockContacts[3]))
                .and(hasAnyDescendant(hasText(mockContacts[3].fullName))),
            useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun contactListDisplaysPhoneNumber() {
    val contactList = listOf(mockContacts[3])
    setContent(withInitialContacts = contactList)
    composeTestRule
        .onNode(
            hasTestTag(ContactListScreenTestTags.getTestTagForContactItem(mockContacts[3]))
                .and(hasAnyDescendant(hasText(mockContacts[3].phoneNumber))),
            useUnmergedTree = true)
        .assertIsDisplayed()
  }

  @Test
  fun contactListDisplaysRelationship() {
    val contactList = listOf(mockContacts[3])
    setContent(withInitialContacts = contactList)
    composeTestRule
        .onNode(
            hasTestTag(ContactListScreenTestTags.getTestTagForContactItem(mockContacts[3]))
                .and(hasAnyDescendant(hasText("Relationship: ${mockContacts[3].relationship}"))),
            useUnmergedTree = true)
        .assertIsDisplayed()
  }
}
