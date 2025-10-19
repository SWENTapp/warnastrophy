package com.github.warnastrophy.core.ui.contact

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import com.github.warnastrophy.core.model.contact.Contact
import com.github.warnastrophy.core.model.contact.ContactsRepository
import com.github.warnastrophy.core.model.contact.MockContactsRepository
import com.github.warnastrophy.core.ui.profile.contact.EditContactScreen
import com.github.warnastrophy.core.ui.profile.contact.EditContactTestTags
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class EditContactTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    val repository : ContactsRepository = MockContactsRepository()

    val contact_1 = Contact(id = "a", "Ronaldo", "+41", "Friend")
    val mock_contact_id = "e"

    @Before
    fun setUp() {
        //ContactRepositoryProvider.repository = MockContactsRepository()
        composeTestRule.setContent { EditContactScreen() }
    }

    @Test
    fun displayAllComponents() {
        //composeTestRule.setContent { EditContactScreen() }
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
        composeTestRule.onNodeWithTag(EditContactTestTags.INPUT_FULL_NAME).performTextInput(text)
        composeTestRule.onNodeWithTag(EditContactTestTags.INPUT_FULL_NAME).assertTextContains(text)
        composeTestRule
            .onNodeWithTag(EditContactTestTags.ERROR_MESSAGE, useUnmergedTree = true)
            .assertIsNotDisplayed()
    }
    @Test
    fun canEnterPhoneNumber() {
        val text = "+41189290266"
        composeTestRule.onNodeWithTag(EditContactTestTags.INPUT_PHONE_NUMBER).performTextInput(text)
        composeTestRule.onNodeWithTag(EditContactTestTags.INPUT_PHONE_NUMBER).assertTextContains(text)
        composeTestRule
            .onNodeWithTag(EditContactTestTags.ERROR_MESSAGE, useUnmergedTree = true)
            .assertIsNotDisplayed()
    }
    @Test
    fun canEnterRelationship() {
        val text = "Work"
        composeTestRule.onNodeWithTag(EditContactTestTags.INPUT_RELATIONSHIP).performTextInput(text)
        composeTestRule.onNodeWithTag(EditContactTestTags.INPUT_RELATIONSHIP).assertTextContains(text)
        composeTestRule
            .onNodeWithTag(EditContactTestTags.ERROR_MESSAGE, useUnmergedTree = true)
            .assertIsNotDisplayed()
    }
    @Test
    fun canEnterInvalidPhoneNumber() {
        val text = "+411892902"
        composeTestRule.onNodeWithTag(EditContactTestTags.INPUT_PHONE_NUMBER).performTextInput(text)
        composeTestRule.onNodeWithTag(EditContactTestTags.INPUT_PHONE_NUMBER).assertTextContains(text)
    }
    @Test
    fun enteringEmptyFullNameShowsErrorMessage() {
        val invalidText = " "
        composeTestRule.onNodeWithTag(EditContactTestTags.INPUT_FULL_NAME).performTextInput(invalidText)
        composeTestRule.onNodeWithTag(EditContactTestTags.ERROR_MESSAGE, useUnmergedTree = true).assertIsDisplayed()
    }
    @Test
    fun enteringEmptyRelationshipShowsErrorMessage() {
        val invalidText = " "
        composeTestRule
            .onNodeWithTag(EditContactTestTags.INPUT_RELATIONSHIP)
            .performTextInput(invalidText)
        composeTestRule.onNodeWithTag(EditContactTestTags.ERROR_MESSAGE, useUnmergedTree = true).assertIsDisplayed()
    }
    @Test
    fun enteringInvalidPhoneNumberShowsErrorMessage() {
        val invalidText = "+411892902"
        composeTestRule
            .onNodeWithTag(EditContactTestTags.INPUT_PHONE_NUMBER)
            .performTextInput(invalidText)
        composeTestRule.onNodeWithTag(EditContactTestTags.ERROR_MESSAGE, useUnmergedTree = true).assertIsDisplayed()
    }
}