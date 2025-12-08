package com.github.warnastrophy.e2e

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.warnastrophy.WarnastrophyComposable
import com.github.warnastrophy.core.data.provider.ContactRepositoryProvider
import com.github.warnastrophy.core.data.provider.HealthCardRepositoryProvider
import com.github.warnastrophy.core.data.repository.UserPreferencesRepositoryLocal
import com.github.warnastrophy.core.data.service.StateManagerService
import com.github.warnastrophy.core.ui.features.UITest
import com.github.warnastrophy.core.ui.features.contact.AddContactTestTags
import com.github.warnastrophy.core.ui.features.contact.ContactListScreenTestTags
import com.github.warnastrophy.core.ui.features.contact.EditContactTestTags
import com.github.warnastrophy.core.ui.features.health.HealthCardTestTags
import com.github.warnastrophy.core.ui.features.map.MapScreenTestTags
import com.github.warnastrophy.core.ui.features.profile.LocalThemeViewModel
import com.github.warnastrophy.core.ui.features.profile.ThemeViewModel
import com.github.warnastrophy.core.ui.features.profile.ThemeViewModelFactory
import com.github.warnastrophy.core.ui.navigation.NavigationTestTags
import com.github.warnastrophy.core.ui.theme.MainAppTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Before

/**
 * Provides high-level, reusable End-to-End test actions.
 *
 * This abstract class serves as a collection of common user flows. By inheriting from [UITest], it
 * gains access to the `composeTestRule` and other UI testing utilities.
 *
 * Test classes that need to perform these end-to-end actions should inherit from this class,
 * allowing them to call these utility methods directly and compose them into complex test
 * scenarios.
 */
abstract class EndToEndUtils : UITest() {

  private val testScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private var dataStoreInstance: DataStore<Preferences>? = null

  /**
   * Sets the content of the test rule to the [WarnastrophyApp], optionally using a fake map.
   *
   * This method initializes the app's UI for testing by setting up the [WarnastrophyComposable]. It
   * can optionally replace the map with a fake component to improve test stability and speed. A
   * fresh instance of [DataStore] is created for each test to ensure no conflicts between tests.
   *
   * @param useFakeMap If true, a fake map component is used in place of the real one.
   */
  fun setContent(useFakeMap: Boolean = true) {
    composeTestRule.setContent {
      val context = composeTestRule.activity.applicationContext

      if (dataStoreInstance == null) {
        val testDataStore =
            PreferenceDataStoreFactory.create(
                scope = testScope,
                produceFile = { File(context.filesDir, "test_preferences.preferences_pb") })
        dataStoreInstance = testDataStore
      }

      val repository = UserPreferencesRepositoryLocal(dataStoreInstance!!)
      val themeViewModel: ThemeViewModel = viewModel(factory = ThemeViewModelFactory(repository))

      ContactRepositoryProvider.initLocal(context)
      StateManagerService.init(context)
      contactRepository = ContactRepositoryProvider.repository
      HealthCardRepositoryProvider.useLocalEncrypted(context)

      MainAppTheme {
        CompositionLocalProvider(LocalThemeViewModel provides themeViewModel) {
          if (useFakeMap) {
            WarnastrophyComposable(mockMapScreen = { FakeMapComponent() })
          } else {
            WarnastrophyComposable()
          }
        }
      }
    }
  }

  @Before
  override fun setUp() {
    super.setUp()
    setupMockFirebaseAuth()
    setContent()
  }

  @After
  override fun tearDown() {
    super.tearDown()
    unmockkAll()
    testScope.cancel()
    dataStoreInstance = null
  }

  /**
   * Sets up a mocked Firebase authentication with a logged-in user. This ensures the app starts at
   * the Dashboard instead of the SignIn screen.
   */
  private fun setupMockFirebaseAuth() {
    mockkStatic(FirebaseApp::class)
    val mockFirebaseApp: FirebaseApp = mockk(relaxed = true)
    every { FirebaseApp.getInstance() } returns mockFirebaseApp
    every { FirebaseApp.getApps(any()) } returns listOf(mockFirebaseApp)

    mockkStatic(FirebaseAuth::class)
    val mockFirebaseAuth: FirebaseAuth = mockk(relaxed = true)
    val mockFirebaseUser: FirebaseUser =
        mockk(relaxed = true) {
          every { uid } returns "test-user-id"
          every { email } returns "test@example.com"
          every { displayName } returns "Test User"
          every { isAnonymous } returns false
        }

    every { FirebaseAuth.getInstance() } returns mockFirebaseAuth
    every { mockFirebaseAuth.currentUser } returns mockFirebaseUser
  }

  /**
   * Simulates a full user flow for adding a new contact.
   *
   * This method navigates to the add contact screen, fills out the form with the provided name, and
   * verifies that the contact appears in the contact list after being saved.
   *
   * @param name The name of the new contact to be added. Defaults to "Messi".
   */
  fun addNewContact(name: String = "Messi") {
    // Navigate to the add contact screen
    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_PROFILE).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.CONTACT_LIST).performClick()
    composeTestRule.onNodeWithTag(ContactListScreenTestTags.ADD_CONTACT_BUTTON).performClick()

    // Fill the form and save
    fillContactForm(name = name)
    composeTestRule.clickOnSaveButton(true, AddContactTestTags.SAVE_BUTTON)

    // Verify we are back on the contact list screen and the contact exists
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains("Contact List", ignoreCase = true)
    composeTestRule.onNodeWithText(name, ignoreCase = true).assertIsDisplayed()
  }

  /**
   * Simulates a full user flow for editing an existing contact.
   *
   * This method navigates to a contact, fills the form with updated details, and verifies the
   * changes are saved or canceled based on the provided `saveChanges` parameter.
   *
   * @param previousName The current name of the contact to be edited. Defaults to "Messi".
   * @param newName The new name for the contact. Defaults to "Ronaldo".
   * @param newRelationship The updated relationship value. Defaults to "Friend".
   * @param newPhoneNumber The updated phone number. Defaults to "+41765365899".
   * @param saveChanges If true, the changes are saved. If false, the changes are canceled. Defaults
   *   to true.
   */
  fun editContact(
      previousName: String = "Messi",
      newName: String = "Ronaldo",
      newRelationship: String = "Friend",
      newPhoneNumber: String = "+41765365899",
      saveChanges: Boolean = true
  ) {
    // Navigate to the contact and click on it
    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_PROFILE).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.CONTACT_LIST).performClick()
    composeTestRule
        .onNodeWithText(previousName, ignoreCase = true)
        .assertIsDisplayed()
        .performClick()

    // Fill the form with new details
    fillContactForm(newName, newRelationship, newPhoneNumber, edit = true)

    // Save or cancel the changes
    if (saveChanges) {
      composeTestRule.clickOnSaveButton(true, EditContactTestTags.SAVE_BUTTON)
    } else {
      composeTestRule.onNodeWithTag(NavigationTestTags.BUTTON_BACK).performClick()
    }

    // Verify the result on the contact list screen
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains("Contact List", ignoreCase = true)
    val expectedName = if (saveChanges) newName else previousName
    composeTestRule.onNodeWithText(expectedName, ignoreCase = true).assertIsDisplayed()
  }

  /**
   * Simulates a full user flow for deleting a contact.
   *
   * This method navigates to a contact, clicks the delete button, waits for the action to complete,
   * and verifies the contact is removed from the contact list.
   *
   * @param name The name of the contact to delete. Defaults to "Ronaldo".
   */
  fun deleteContact(name: String = "Ronaldo") {
    // Navigate to the contact and click on it
    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_PROFILE).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.CONTACT_LIST).performClick()
    composeTestRule.onNodeWithText(name, ignoreCase = true).assertIsDisplayed().performClick()

    // Click delete and wait for navigation
    composeTestRule.onNodeWithTag(EditContactTestTags.DELETE_BUTTON).performClick()
    composeTestRule.waitUntil(defaultTimeout) {
      composeTestRule
          .onAllNodesWithTag(EditContactTestTags.DELETE_BUTTON)
          .fetchSemanticsNodes()
          .isEmpty()
    }

    // Verify contact is gone
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains("Contact List", ignoreCase = true)
    composeTestRule.onNodeWithText(name, ignoreCase = true).assertDoesNotExist()
  }

  /** Simulates a full user flow for creating and saving a Health Card. */
  fun createHealthCard(
      fullName: String = "Jane Doe",
      birthDate: String = "01/01/1990",
      ssn: String = "123.456.789-00",
      sex: String = "Female",
      bloodType: String = "O+",
      height: String = "170",
      weight: String = "65.0",
      conditions: String = "Asthma",
      allergies: String = "Pollen, Peanuts",
      medications: String = "Ventolin",
      treatments: String = "None",
      history: String = "Chickenpox",
      isOrganDonor: Boolean = true,
      notes: String = "Allergic to penicillin."
  ) {
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains("Emergency Card", ignoreCase = true)

    // Fill the form
    fillHealthCardForm(
        fullName,
        birthDate,
        ssn,
        sex,
        bloodType,
        height,
        weight,
        conditions,
        allergies,
        medications,
        treatments,
        history,
        isOrganDonor,
        notes)

    composeTestRule.waitForIdle()

    // Save
    composeTestRule
        .onNodeWithTag(HealthCardTestTags.ADD_BUTTON)
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()

    // After creating, the 'Add' button becomes an 'Update' button. Wait for it to appear.
    composeTestRule.waitUntil(defaultTimeout) {
      composeTestRule
          .onAllNodesWithTag(HealthCardTestTags.UPDATE_BUTTON)
          .fetchSemanticsNodes()
          .isNotEmpty()
    }

    // Check if content saved
    checkTextFieldValue(HealthCardTestTags.FULL_NAME_FIELD, fullName)
    checkTextFieldValue(HealthCardTestTags.BIRTH_DATE_FIELD, birthDate)
    checkTextFieldValue(HealthCardTestTags.SSN_FIELD, ssn)
    checkTextFieldValue(HealthCardTestTags.SEX_FIELD, sex)
    checkTextFieldValue(HealthCardTestTags.BLOOD_TYPE_FIELD, bloodType)
    checkTextFieldValue(HealthCardTestTags.HEIGHT_FIELD, height)
    checkTextFieldValue(HealthCardTestTags.WEIGHT_FIELD, weight)
    checkTextFieldValue(HealthCardTestTags.CHRONIC_CONDITIONS_FIELD, conditions)
    checkTextFieldValue(HealthCardTestTags.ALLERGIES_FIELD, allergies)
    checkTextFieldValue(HealthCardTestTags.MEDICATIONS_FIELD, medications)
    checkTextFieldValue(HealthCardTestTags.TREATMENTS_FIELD, treatments)
    checkTextFieldValue(HealthCardTestTags.HISTORY_FIELD, history)
    checkTextFieldValue(HealthCardTestTags.NOTES_FIELD, notes)
  }

  /** Simulates editing and saving a Health Card. */
  fun editHealthCard(
      fullName: String? = null,
      birthDate: String? = null,
      ssn: String? = null,
      sex: String? = null,
      bloodType: String? = null,
      height: String? = null,
      weight: String? = null,
      conditions: String? = null,
      allergies: String? = null,
      medications: String? = null,
      treatments: String? = null,
      history: String? = null,
      isOrganDonor: Boolean = false,
      notes: String? = null
  ) {
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains("Emergency Card", ignoreCase = true)

    // Edit the form
    fillHealthCardForm(
        fullName,
        birthDate,
        ssn,
        sex,
        bloodType,
        height,
        weight,
        conditions,
        allergies,
        medications,
        treatments,
        history,
        isOrganDonor,
        notes)

    composeTestRule.waitForIdle()

    // Save
    composeTestRule
        .onNodeWithTag(HealthCardTestTags.UPDATE_BUTTON)
        .performScrollTo()
        .assertIsDisplayed()
        .performClick()

    composeTestRule.waitForIdle()

    // Check if content saved
    composeTestRule
        .onNodeWithTag(HealthCardTestTags.UPDATE_BUTTON)
        .performScrollTo()
        .assertIsDisplayed()

    composeTestRule.waitForIdle()

    checkTextFieldValue(HealthCardTestTags.FULL_NAME_FIELD, fullName)
    checkTextFieldValue(HealthCardTestTags.BIRTH_DATE_FIELD, birthDate)
    checkTextFieldValue(HealthCardTestTags.SSN_FIELD, ssn)
    checkTextFieldValue(HealthCardTestTags.SEX_FIELD, sex)
    checkTextFieldValue(HealthCardTestTags.BLOOD_TYPE_FIELD, bloodType)
    checkTextFieldValue(HealthCardTestTags.HEIGHT_FIELD, height)
    checkTextFieldValue(HealthCardTestTags.WEIGHT_FIELD, weight)
    checkTextFieldValue(HealthCardTestTags.CHRONIC_CONDITIONS_FIELD, conditions)
    checkTextFieldValue(HealthCardTestTags.ALLERGIES_FIELD, allergies)
    checkTextFieldValue(HealthCardTestTags.MEDICATIONS_FIELD, medications)
    checkTextFieldValue(HealthCardTestTags.TREATMENTS_FIELD, treatments)
    checkTextFieldValue(HealthCardTestTags.HISTORY_FIELD, history)
    checkTextFieldValue(HealthCardTestTags.NOTES_FIELD, notes)
  }

  /** Simulates deleting a Health Card. */
  fun deleteHealthCard() {
    composeTestRule
        .onNodeWithTag(NavigationTestTags.TOP_BAR_TITLE)
        .assertTextContains("Emergency Card", ignoreCase = true)

    // Delete
    composeTestRule.onNodeWithTag(HealthCardTestTags.DELETE_BUTTON).performScrollTo().performClick()

    composeTestRule.waitForIdle()

    // Check all fields empty
    checkTextFieldValue(HealthCardTestTags.FULL_NAME_FIELD, null)
    checkTextFieldValue(HealthCardTestTags.BIRTH_DATE_FIELD, null)
    checkTextFieldValue(HealthCardTestTags.SSN_FIELD, null)
    checkTextFieldValue(HealthCardTestTags.SEX_FIELD, null)
    checkTextFieldValue(HealthCardTestTags.BLOOD_TYPE_FIELD, null)
    checkTextFieldValue(HealthCardTestTags.HEIGHT_FIELD, null)
    checkTextFieldValue(HealthCardTestTags.WEIGHT_FIELD, null)
    checkTextFieldValue(HealthCardTestTags.CHRONIC_CONDITIONS_FIELD, null)
    checkTextFieldValue(HealthCardTestTags.ALLERGIES_FIELD, null)
    checkTextFieldValue(HealthCardTestTags.MEDICATIONS_FIELD, null)
    checkTextFieldValue(HealthCardTestTags.TREATMENTS_FIELD, null)
    checkTextFieldValue(HealthCardTestTags.HISTORY_FIELD, null)
    checkTextFieldValue(HealthCardTestTags.NOTES_FIELD, null)
  }

  /**
   * Simulates a full user flow for filling out the Health Card form. It enters text into all
   * available fields.
   *
   * @param fullName The full name to enter.
   * @param birthDate The birth date in dd/MM/yyyy format.
   * @param ssn The social security number.
   * @param sex The biological sex.
   * @param bloodType The blood type.
   * @param height The height in cm.
   * @param weight The weight in kg.
   * @param conditions Chronic conditions, comma-separated.
   * @param allergies Allergies, comma-separated.
   * @param medications Medications, comma-separated.
   * @param treatments Ongoing treatments, comma-separated.
   * @param history Medical history, comma-separated.
   * @param isOrganDonor Whether to toggle the organ donor switch on.
   * @param notes Additional notes.
   */
  private fun fillHealthCardForm(
      fullName: String? = "Jane Doe",
      birthDate: String? = "01/01/1990",
      ssn: String? = "123.456.789-00",
      sex: String? = "Female",
      bloodType: String? = "O+",
      height: String? = "170",
      weight: String? = "65.0",
      conditions: String? = "Asthma",
      allergies: String? = "Pollen, Peanuts",
      medications: String? = "Ventolin",
      treatments: String? = "None",
      history: String? = "Chickenpox",
      isOrganDonor: Boolean = false,
      notes: String? = "Allergic to penicillin."
  ) {
    // Helper lambda to reduce repetition for text fields
    val enterText: (String, String?) -> Unit = { tag, text ->
      if (text != null) {
        composeTestRule.onNodeWithTag(tag).performTextClearance()
        composeTestRule.onNodeWithTag(tag).performTextInput(text)
      }
    }

    enterText(HealthCardTestTags.FULL_NAME_FIELD, fullName)
    enterText(HealthCardTestTags.BIRTH_DATE_FIELD, birthDate)
    enterText(HealthCardTestTags.SSN_FIELD, ssn)
    enterText(HealthCardTestTags.SEX_FIELD, sex)
    enterText(HealthCardTestTags.BLOOD_TYPE_FIELD, bloodType)
    enterText(HealthCardTestTags.HEIGHT_FIELD, height)
    enterText(HealthCardTestTags.WEIGHT_FIELD, weight)
    enterText(HealthCardTestTags.CHRONIC_CONDITIONS_FIELD, conditions)
    enterText(HealthCardTestTags.ALLERGIES_FIELD, allergies)
    enterText(HealthCardTestTags.MEDICATIONS_FIELD, medications)
    enterText(HealthCardTestTags.TREATMENTS_FIELD, treatments)
    enterText(HealthCardTestTags.HISTORY_FIELD, history)
    enterText(HealthCardTestTags.NOTES_FIELD, notes)

    if (isOrganDonor) {
      // Assume the switch is not checked by default
      composeTestRule.onNodeWithTag(HealthCardTestTags.ORGAN_DONOR_FIELD).performClick()
      composeTestRule.onNodeWithTag(HealthCardTestTags.ORGAN_DONOR_FIELD).assertIsOn()
    }
  }

  /**
   * Checks that a text field identified by a test tag contains the expected text.
   *
   * @param fieldTag The test tag of the OutlinedTextField.
   * @param expectedText The text that the field is expected to contain.
   */
  private fun checkTextFieldValue(fieldTag: String, expectedText: String?) {
    expectedText?.let {
      composeTestRule
          .onNodeWithTag(fieldTag)
          .performScrollTo()
          .assertTextContains(it, ignoreCase = true)
    }
  }

  /**
   * Private helper to fill the contact form. It's an extension on UITest to access composeTestRule
   * and the helper methods like enterEditFullName.
   *
   * @param name The full name to enter.
   * @param relationship The relationship to enter.
   * @param phoneNumber The phone number to enter.
   * @param edit Differentiates between filling the 'add' form and the 'edit' form.
   */
  private fun fillContactForm(
      name: String = "John Doe",
      relationship: String = "Friend",
      phoneNumber: String = "+41765365899",
      edit: Boolean = false
  ) {
    if (edit) {
      composeTestRule.enterEditFullName(name)
      composeTestRule.enterEditRelationship(relationship)
      composeTestRule.enterEditPhoneNumber(phoneNumber)
    } else {
      composeTestRule.enterAddFullName(name)
      composeTestRule.enterAddRelationship(relationship)
      composeTestRule.enterAddPhoneNumber(phoneNumber)
    }
  }

  /**
   * A simple composable that stands in for the real Google Map during tests. This improves test
   * stability and speed by avoiding the complexities of a real map component.
   */
  @Composable
  private fun FakeMapComponent() {
    Box(Modifier.fillMaxSize().testTag(MapScreenTestTags.GOOGLE_MAP_SCREEN)) {
      Text("Fake map for testing", Modifier.align(Alignment.Center))
    }
  }
}
