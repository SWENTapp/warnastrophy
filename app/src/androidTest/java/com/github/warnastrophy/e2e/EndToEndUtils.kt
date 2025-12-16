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
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.warnastrophy.WarnastrophyComposable
import com.github.warnastrophy.core.data.repository.UserPreferencesRepositoryLocal
import com.github.warnastrophy.core.data.service.SpeechRecognitionUiState
import com.github.warnastrophy.core.data.service.SpeechToTextServiceInterface
import com.github.warnastrophy.core.data.service.StateManagerService
import com.github.warnastrophy.core.data.service.TextToSpeechServiceInterface
import com.github.warnastrophy.core.data.service.TextToSpeechUiState
import com.github.warnastrophy.core.ui.features.UITest
import com.github.warnastrophy.core.ui.features.contact.AddContactTestTags
import com.github.warnastrophy.core.ui.features.contact.ContactListScreenTestTags
import com.github.warnastrophy.core.ui.features.contact.EditContactTestTags
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

  /**
   * Tries to set a private field on StateManagerService (used to swap services in tests). This
   * keeps the test E2E-ish while still making STT/TTS deterministic.
   */
  fun setStateManagerServiceField(fieldName: String, value: Any) {
    runCatching {
          val field = StateManagerService::class.java.getDeclaredField(fieldName)
          field.isAccessible = true
          field.set(StateManagerService, value)
        }
        .getOrElse {
          throw IllegalStateException(
              "Could not replace StateManagerService.$fieldName. " +
                  "Expose a test hook or keep the field name stable.",
              it)
        }
  }

  /**
   * Turns on the danger-mode voice confirmation overlay by mutating the orchestrator's
   * showVoiceConfirmationScreen flow via reflection.
   *
   * Expected shape (typical): val showVoiceConfirmationScreen: StateFlow<Boolean> backed by a
   * MutableStateFlow<Boolean> inside the orchestrator.
   */
  fun forceShowVoiceConfirmationOverlay() {
    val orchestrator = StateManagerService.dangerModeOrchestrator

    // 1) Directly try a MutableStateFlow field called "_showVoiceConfirmationScreen"
    val ok1 =
        runCatching {
              val f = orchestrator.javaClass.getDeclaredField("_showVoiceConfirmationScreen")
              f.isAccessible = true
              val v = f.get(orchestrator)
              (v as? MutableStateFlow<Boolean>)?.value = true
              true
            }
            .getOrDefault(false)

    if (ok1) return

    // 2) Try a MutableStateFlow field called "showVoiceConfirmationScreen"
    val ok2 =
        runCatching {
              val f = orchestrator.javaClass.getDeclaredField("showVoiceConfirmationScreen")
              f.isAccessible = true
              val v = f.get(orchestrator)
              (v as? MutableStateFlow<Boolean>)?.value = true
              true
            }
            .getOrDefault(false)

    if (ok2) return

    throw IllegalStateException(
        "Couldn't force showVoiceConfirmation overlay. " +
            "Please add a test-only method on dangerModeOrchestrator like triggerVoiceConfirmation().")
  }

  /**
   * This method check if given toggle button (which triggers an asynchronous operation) is checked.
   *
   * @param testTag test tag string of the button.
   */
  fun isSwitchOnAfterAsyncOperation(testTag: String) {
    val switchMatcher = hasTestTag(testTag) and hasClickAction()
    composeTestRule.waitUntil(timeoutMillis = DEFAULT_TIMEOUT) {
      try {
        composeTestRule.onNode(switchMatcher, useUnmergedTree = true).assertIsOn()
        true
      } catch (_: AssertionError) {
        false
      }
    }
  }

  /** This method allows to navigate to preferences mode screen. */
  fun goToDangerModePreferencesScreen() {
    composeTestRule.onNodeWithTag(NavigationTestTags.TAB_PROFILE).performClick()
    composeTestRule.onNodeWithTag(NavigationTestTags.DANGER_MODE_PREFERENCES).performClick()
  }
}

class FakeTextToSpeechService : TextToSpeechServiceInterface {
  private val _uiState = MutableStateFlow(TextToSpeechUiState())
  override val uiState: StateFlow<TextToSpeechUiState> = _uiState

  val spokenHistory = mutableListOf<String>()

  override fun speak(text: String) {
    if (text.isBlank()) return
    spokenHistory += text
    _uiState.value = _uiState.value.copy(isSpeaking = true, rms = 20f, spokenText = text)
  }

  fun finishSpeaking() {
    _uiState.value = _uiState.value.copy(isSpeaking = false, rms = 0f, spokenText = null)
  }

  override fun destroy() {
    _uiState.value = TextToSpeechUiState()
  }
}

class FakeSpeechToTextService(private val confirmationResult: Boolean) :
    SpeechToTextServiceInterface {
  private val _uiState = MutableStateFlow(SpeechRecognitionUiState())
  override val uiState: StateFlow<SpeechRecognitionUiState> = _uiState

  @Volatile var listenCalled: Boolean = false

  override suspend fun listenForConfirmation(): Boolean {
    listenCalled = true
    _uiState.value =
        SpeechRecognitionUiState(
            isListening = false,
            rmsLevel = 0f,
            recognizedText = if (confirmationResult) "yes" else "no",
            errorMessage = null,
            isConfirmed = confirmationResult)
    return confirmationResult
  }

  override fun destroy() {
    _uiState.value = SpeechRecognitionUiState()
  }
}
