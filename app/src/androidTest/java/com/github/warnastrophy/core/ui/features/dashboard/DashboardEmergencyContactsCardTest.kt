package com.github.warnastrophy.core.ui.dashboard

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.github.warnastrophy.core.data.interfaces.ContactsRepository
import com.github.warnastrophy.core.data.provider.ContactRepositoryProvider
import com.github.warnastrophy.core.data.repository.MockContactRepository
import com.github.warnastrophy.core.model.Contact
import com.github.warnastrophy.core.ui.features.dashboard.DashboardEmergencyContactsCardStateful
import com.github.warnastrophy.core.ui.features.dashboard.DashboardEmergencyContactsCardStateless
import com.github.warnastrophy.core.ui.features.dashboard.DashboardEmergencyContactsTestTags
import com.github.warnastrophy.core.ui.theme.MainAppTheme
import com.github.warnastrophy.core.util.AppConfig
import com.github.warnastrophy.core.util.BaseAndroidComposeTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class DashboardEmergencyContactsCardTest : BaseAndroidComposeTest() {
  private lateinit var mockRepository: MockContactRepository
  private var originalRepository: ContactsRepository? = null

  private var userId = AppConfig.defaultUserId

  private val sampleContact1 =
      Contact(
          id = "contact_1",
          fullName = "Jane Doe",
          phoneNumber = "+1 555-123-4567",
          relationship = "Mom")

  private val sampleContact2 =
      Contact(
          id = "contact_2",
          fullName = "John Smith",
          phoneNumber = "+1 555-987-6543",
          relationship = "Dad")

  private val sampleContact3 =
      Contact(
          id = "contact_3",
          fullName = "Sarah Johnson",
          phoneNumber = "+1 555-111-2222",
          relationship = "Sister")

  @Before
  override fun setUp() {
    super.setUp()
    originalRepository =
        try {
          ContactRepositoryProvider.repository
        } catch (_: UninitializedPropertyAccessException) {
          null
        }

    mockRepository = MockContactRepository()
    ContactRepositoryProvider.repository = mockRepository
  }

  @Test
  fun dashboardEmergencyContactsCard_statefulVersion_displaysContactsFromRepository() {
    runBlocking {
      mockRepository.addContact(userId, sampleContact1)
      mockRepository.addContact(userId, sampleContact2)
    }

    composeTestRule.setContent {
      MainAppTheme { DashboardEmergencyContactsCardStateful(onManageContactsClick = {}) }
    }

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(
            "${DashboardEmergencyContactsTestTags.CONTACT_ITEM_PREFIX}0", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
        .assert(hasText("Jane Doe: +1 555-123-4567", substring = true))

    composeTestRule
        .onNodeWithTag(
            "${DashboardEmergencyContactsTestTags.CONTACT_ITEM_PREFIX}1", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
        .assert(hasText("John Smith: +1 555-987-6543", substring = true))
  }

  @Test
  fun dashboardEmergencyContactsCard_statefulVersion_displaysLoadingInitially() {
    composeTestRule.setContent {
      MainAppTheme { DashboardEmergencyContactsCardStateful(onManageContactsClick = {}) }
    }

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(DashboardEmergencyContactsTestTags.NO_CONTACTS_TEXT, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun dashboardEmergencyContactsCard_statefulVersion_displaysEmptyStateWhenNoContacts() {

    composeTestRule.setContent {
      MainAppTheme { DashboardEmergencyContactsCardStateful(onManageContactsClick = {}) }
    }

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(DashboardEmergencyContactsTestTags.NO_CONTACTS_TEXT, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
        .assertTextEquals("No emergency contacts added")
  }

  @Test
  fun dashboardEmergencyContactsCard_statefulVersion_displaysMaxTwoContacts() {
    runBlocking {
      mockRepository.addContact(userId, sampleContact1)
      mockRepository.addContact(userId, sampleContact2)
      mockRepository.addContact(userId, sampleContact3)
    }

    composeTestRule.setContent {
      MainAppTheme { DashboardEmergencyContactsCardStateful(onManageContactsClick = {}) }
    }

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(
            "${DashboardEmergencyContactsTestTags.CONTACT_ITEM_PREFIX}0", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(
            "${DashboardEmergencyContactsTestTags.CONTACT_ITEM_PREFIX}1", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(
            "${DashboardEmergencyContactsTestTags.CONTACT_ITEM_PREFIX}2", useUnmergedTree = true)
        .assertDoesNotExist()
  }

  @Test
  fun dashboardEmergencyContactsCard_manageButtonTriggersCallback() {
    var callbackTriggered = false
    runBlocking { mockRepository.addContact(userId, sampleContact1) }

    composeTestRule.setContent {
      MainAppTheme {
        DashboardEmergencyContactsCardStateful(onManageContactsClick = { callbackTriggered = true })
      }
    }

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(DashboardEmergencyContactsTestTags.CARD)
        .assertExists()
        .assertIsDisplayed()
        .assertHasClickAction()
        .performClick()

    assert(callbackTriggered) { "onManageContactsClick callback was not triggered" }
  }

  @Test
  fun dashboardEmergencyContactsCard_cardIsVisibleAndScrollable() {
    runBlocking {
      mockRepository.addContact(userId, sampleContact1)
      mockRepository.addContact(userId, sampleContact2)
    }

    composeTestRule.setContent {
      MainAppTheme { DashboardEmergencyContactsCardStateful(onManageContactsClick = {}) }
    }

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(DashboardEmergencyContactsTestTags.CARD)
        .assertExists()
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(DashboardEmergencyContactsTestTags.TITLE, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun dashboardEmergencyContactsCard_displaysCorrectContactFormat() {
    runBlocking { mockRepository.addContact(userId, sampleContact1) }

    composeTestRule.setContent {
      MainAppTheme { DashboardEmergencyContactsCardStateful(onManageContactsClick = {}) }
    }

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(
            "${DashboardEmergencyContactsTestTags.CONTACT_ITEM_PREFIX}0", useUnmergedTree = true)
        .assertExists()
        .assert(hasText("Jane Doe: +1 555-123-4567", substring = true))
  }

  @Test
  fun dashboardEmergencyContactsCard_statelessVersion_displaysProvidedContacts() {
    val contacts = listOf(sampleContact1, sampleContact2)

    composeTestRule.setContent {
      MainAppTheme {
        DashboardEmergencyContactsCardStateless(
            contacts = contacts, onManageContactsClick = {}, isLoading = false)
      }
    }

    composeTestRule
        .onNodeWithTag(
            "${DashboardEmergencyContactsTestTags.CONTACT_ITEM_PREFIX}0", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
        .assert(hasText("Jane Doe", substring = true))

    composeTestRule
        .onNodeWithTag(
            "${DashboardEmergencyContactsTestTags.CONTACT_ITEM_PREFIX}1", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
        .assert(hasText("John Smith", substring = true))
  }

  @Test
  fun dashboardEmergencyContactsCard_statelessVersion_displaysLoadingState() {
    composeTestRule.setContent {
      MainAppTheme {
        DashboardEmergencyContactsCardStateless(
            contacts = emptyList(), onManageContactsClick = {}, isLoading = true)
      }
    }

    composeTestRule.onNodeWithTag(DashboardEmergencyContactsTestTags.CARD).assertExists()

    composeTestRule
        .onNodeWithTag(
            "${DashboardEmergencyContactsTestTags.CONTACT_ITEM_PREFIX}0", useUnmergedTree = true)
        .assertDoesNotExist()
  }

  @Test
  fun dashboardEmergencyContactsCard_statelessVersion_displaysEmptyState() {
    composeTestRule.setContent {
      MainAppTheme {
        DashboardEmergencyContactsCardStateless(
            contacts = emptyList(), onManageContactsClick = {}, isLoading = false)
      }
    }

    composeTestRule
        .onNodeWithTag(DashboardEmergencyContactsTestTags.NO_CONTACTS_TEXT, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
        .assertTextEquals("No emergency contacts added")
  }

  @Test
  fun dashboardEmergencyContactsCard_handlesSingleContact() {
    runBlocking { mockRepository.addContact(userId, sampleContact1) }

    composeTestRule.setContent {
      MainAppTheme { DashboardEmergencyContactsCardStateful(onManageContactsClick = {}) }
    }

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(
            "${DashboardEmergencyContactsTestTags.CONTACT_ITEM_PREFIX}0", useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()

    composeTestRule
        .onNodeWithTag(
            "${DashboardEmergencyContactsTestTags.CONTACT_ITEM_PREFIX}1", useUnmergedTree = true)
        .assertDoesNotExist()
  }

  @Test
  fun dashboardEmergencyContactsCard_titleIsAlwaysVisible() {
    composeTestRule.setContent {
      MainAppTheme {
        DashboardEmergencyContactsCardStateless(
            contacts = emptyList(), onManageContactsClick = {}, isLoading = false)
      }
    }

    composeTestRule
        .onNodeWithTag(DashboardEmergencyContactsTestTags.TITLE, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
        .assertTextEquals("Contacts")
  }

  @Test
  fun dashboardEmergencyContactsCard_manageButtonIsAlwaysVisible() {
    // Test that manage button is visible in all states

    // Empty state
    composeTestRule.setContent {
      MainAppTheme {
        DashboardEmergencyContactsCardStateless(
            contacts = emptyList(), onManageContactsClick = {}, isLoading = false)
      }
    }

    composeTestRule
        .onNodeWithTag(DashboardEmergencyContactsTestTags.CARD)
        .assertExists()
        .assertIsDisplayed()
        .assertHasClickAction()
  }

  @Test
  fun dashboardEmergencyContactsCard_repositoryFailure_displaysEmptyState() {

    val failingRepository =
        object : ContactsRepository {
          override suspend fun getAllContacts(userId: String): Result<List<Contact>> =
              Result.failure(Exception("Repository error"))

          override suspend fun addContact(userId: String, contact: Contact): Result<Unit> =
              Result.failure(Exception("Not implemented"))

          override suspend fun getContact(userId: String, contactID: String): Result<Contact> =
              Result.failure(Exception("Not implemented"))

          override suspend fun editContact(
              userId: String,
              contactID: String,
              newContact: Contact
          ): Result<Unit> = Result.failure(Exception("Not implemented"))

          override suspend fun deleteContact(userId: String, contactID: String): Result<Unit> =
              Result.failure(Exception("Not implemented"))

          override fun getNewUid(): String = ""
        }

    ContactRepositoryProvider.repository = failingRepository

    composeTestRule.setContent {
      MainAppTheme { DashboardEmergencyContactsCardStateful(onManageContactsClick = {}) }
    }

    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithTag(DashboardEmergencyContactsTestTags.NO_CONTACTS_TEXT, useUnmergedTree = true)
        .assertExists()
        .assertIsDisplayed()
  }

  @Test
  fun dashboardEmergencyContactsCard_buttonNavigatesToManageContacts() {
    var navigateCalled = false

    composeTestRule.setContent {
      MainAppTheme {
        DashboardEmergencyContactsCardStateless(
            contacts = listOf(sampleContact1),
            onManageContactsClick = { navigateCalled = true },
            isLoading = false)
      }
    }

    composeTestRule
        .onNodeWithTag(DashboardEmergencyContactsTestTags.CARD)
        .assertExists()
        .assertIsDisplayed()
        .assertHasClickAction()
        .performClick()

    assert(navigateCalled) { "Navigation to manage contacts was not triggered." }
  }
}
