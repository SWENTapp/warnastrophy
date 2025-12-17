package com.github.warnastrophy.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import com.github.warnastrophy.R
import com.github.warnastrophy.core.data.provider.ContactRepositoryProvider
import com.github.warnastrophy.core.data.provider.HealthCardRepositoryProvider
import com.github.warnastrophy.core.data.provider.UserPreferencesRepositoryProvider
import com.github.warnastrophy.core.data.service.StateManagerService
import com.github.warnastrophy.core.ui.components.CommunicationScreenTags
import com.github.warnastrophy.userPrefsDataStore
import org.junit.After
import org.junit.Before
import org.junit.Test

class EndToEndM3Test : EndToEndUtils() {

  private lateinit var fakeTts: FakeTextToSpeechService
  private lateinit var fakeStt: FakeSpeechToTextService

  @Before
  override fun setUp() {
    super.setUp()

    val context = composeTestRule.activity.applicationContext
    ContactRepositoryProvider.initLocal(context)
    UserPreferencesRepositoryProvider.initLocal(context.userPrefsDataStore)
    composeTestRule.runOnUiThread { StateManagerService.init(context) }
    contactRepository = ContactRepositoryProvider.repository
    HealthCardRepositoryProvider.useLocalEncrypted(context)

    // Swap real services with deterministic fakes for E2E.
    fakeTts = FakeTextToSpeechService()
    fakeStt = FakeSpeechToTextService(confirmationResult = true)

    composeTestRule.runOnUiThread {
      setStateManagerServiceField("textToSpeechService", fakeTts)
      setStateManagerServiceField("speechToTextService", fakeStt)
    }
  }

  @After
  override fun tearDown() {
    super.tearDown()
    composeTestRule.runOnUiThread { StateManagerService.shutdown() }
  }

  /**
   * Test for the voice confirmation flow:
   * 1. The screen is displayed with the necessary elements.
   * 2. The system speaks a confirmation request via Text-to-Speech (TTS).
   * 3. After the TTS finishes speaking, the system starts listening for user input via
   *    Speech-to-Text (STT).
   * 4. The system then processes the user's response ("yes") and speaks the "alert sent"
   *    confirmation via TTS.
   *
   * This test verifies that the TTS and STT interactions work as expected in the voice confirmation
   * flow:
   * - TTS should speak the confirmation request.
   * - After TTS finishes, STT should be triggered and wait for the user's input.
   * - Once the user provides "yes", the system should respond with the appropriate TTS message
   *   ("alert sent").
   *
   * @see [CommunicationScreenTags.TITLE] to verify the visibility of the title.
   * @see [CommunicationScreenTags.STATUS_CARD] to verify the visibility of the status card.
   * @see [R.string.confirmation_request] to check if the confirmation request is spoken by TTS.
   * @see [R.string.alert_sent] to verify the "alert sent" message is spoken by TTS after receiving
   *   the "yes" input.
   */
  @Test
  fun voice_confirmation_flow_tts_then_stt_then_tts_yes() {
    val context = composeTestRule.activity.applicationContext

    setContent()

    composeTestRule.runOnUiThread { forceShowVoiceConfirmationOverlay() }

    // Screen is visible.
    composeTestRule.onNodeWithTag(CommunicationScreenTags.TITLE).assertIsDisplayed()
    composeTestRule.onNodeWithTag(CommunicationScreenTags.STATUS_CARD).assertIsDisplayed()

    // VM should have spoken the confirmation request via TTS.
    composeTestRule.waitUntil(3_000) {
      fakeTts.spokenHistory.contains(context.getString(R.string.confirmation_request))
    }

    // End TTS speaking -> VM should start STT listening.
    fakeTts.finishSpeaking()

    composeTestRule.waitUntil(3_000) { fakeStt.listenCalled }

    // STT returns "yes" -> VM should speak "alert sent".
    composeTestRule.waitUntil(3_000) {
      fakeTts.spokenHistory.contains(context.getString(R.string.alert_sent))
    }
  }
}
