package com.github.warnastrophy.core.ui.features.contact

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.warnastrophy.core.data.repository.MockContactRepository
import com.github.warnastrophy.core.model.Contact
import com.github.warnastrophy.core.ui.theme.MainAppTheme
import com.github.warnastrophy.core.util.BaseAndroidComposeTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class ContactPopUpTest : BaseAndroidComposeTest() {
  private lateinit var mockViewModel: ContactListViewModel
  private lateinit var repository: MockContactRepository

  private val mockOnDismissRequest: () -> Unit = mock()
  private val mockOnClick: () -> Unit = mock()
  private val mockOnContactClick: (Contact) -> Unit = mock()
  private val fakeUserId = "user1234"
  private val sampleContact1 =
      Contact(id = "1", fullName = "John Doe", phoneNumber = "111-222-333", relationship = "Father")
  private val sampleContact2 =
      Contact(
          id = "2", fullName = "Jane Smith", phoneNumber = "444-555-666", relationship = "Mother")
  private val sampleContacts = listOf(sampleContact1, sampleContact2)

  @Before
  override fun setUp() {
    repository = MockContactRepository()
    mockViewModel = ContactListViewModel(contactsRepository = repository, userId = fakeUserId)
  }

  private fun setContent(withInitialContacts: List<Contact> = emptyList()) {
    runTest {
      withInitialContacts.forEach { repository.addContact(userId = fakeUserId, contact = it) }
    }
    composeTestRule.setContent {
      MainAppTheme {
        ContactPopUp(
            userId = fakeUserId,
            viewModel = mockViewModel,
            onDismissRequest = mockOnDismissRequest,
            onClick = mockOnClick,
            onContactClick = mockOnContactClick)
      }
    }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag(ContactPopUpTestTags.ROOT_CARD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ContactPopUpTestTags.TITLE).assertIsDisplayed()
  }

  private fun assertContactDisplayed(contact: Contact) {
    composeTestRule.onNodeWithTag(ContactPopUpTestTags.contactItem(contact.id)).assertIsDisplayed()
    composeTestRule.onNodeWithText(contact.fullName).assertIsDisplayed()
    composeTestRule
        .onNodeWithText("${contact.relationship} - ${contact.phoneNumber}")
        .assertIsDisplayed()
  }

  @Test
  fun whenDataIsAvailable_displaysContactList() = runTest {
    setContent(sampleContacts)

    composeTestRule.onNodeWithTag(ContactPopUpTestTags.CONTACT_LIST).assertIsDisplayed()
    assertContactDisplayed(sampleContact1)
    assertContactDisplayed(sampleContact2)
  }

  @Test
  fun whenContactsListIsEmpty_displaysEmptyState() {
    setContent()

    composeTestRule.onNodeWithTag(ContactPopUpTestTags.EMPTY_STATE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ContactPopUpTestTags.CONTACT_LIST).assertDoesNotExist()
  }

  @Test
  fun whenErrorOccurs_displaysErrorState() = runTest {
    repository.shouldThrowException = true

    setContent()

    composeTestRule.onNodeWithTag(ContactPopUpTestTags.ERROR_STATE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(ContactPopUpTestTags.CONTACT_LIST).assertDoesNotExist()
    composeTestRule.onNodeWithTag(ContactPopUpTestTags.EMPTY_STATE).assertDoesNotExist()
  }

  @Test
  fun whenForwardArrowIsClicked_invokesOnClickLambda() = runTest {
    setContent()

    composeTestRule.onNodeWithTag(ContactPopUpTestTags.VIEW_ALL_BUTTON).performClick()
    composeTestRule.waitForIdle()

    verify(mockOnClick).invoke()
  }

  @Test
  fun whenContactItemIsClicked_invokesOnContactClickLambda() = runTest {
    setContent(listOf(sampleContact1))

    assertContactDisplayed(sampleContact1)
    composeTestRule
        .onNodeWithTag(ContactPopUpTestTags.contactItem(sampleContact1.id))
        .performClick()
    composeTestRule.waitForIdle()

    verify(mockOnContactClick).invoke(sampleContact1)
  }
}
