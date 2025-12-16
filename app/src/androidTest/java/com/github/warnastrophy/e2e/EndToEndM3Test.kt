package com.github.warnastrophy.e2e

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import com.github.warnastrophy.R
import com.github.warnastrophy.core.data.provider.ContactRepositoryProvider
import com.github.warnastrophy.core.data.provider.HealthCardRepositoryProvider
import com.github.warnastrophy.core.data.provider.UserPreferencesRepositoryProvider
import com.github.warnastrophy.core.data.service.SpeechRecognitionUiState
import com.github.warnastrophy.core.data.service.SpeechToTextServiceInterface
import com.github.warnastrophy.core.data.service.StateManagerService
import com.github.warnastrophy.core.data.service.TextToSpeechServiceInterface
import com.github.warnastrophy.core.data.service.TextToSpeechUiState
import com.github.warnastrophy.core.di.userPrefsDataStore
import com.github.warnastrophy.core.ui.components.CommunicationScreenTags
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

  @Test
  fun voice_confirmation_flow_tts_then_stt_then_tts_yes() {
    val context = composeTestRule.activity.applicationContext

    // MUST use EndToEndUtils.setContent() (your harness).
    setContent()

    // Force the overlay to appear (WarnastrophyComposable shows CommunicationScreen when true).
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

  /**
   * Tries to set a private field on StateManagerService (used to swap services in tests). This
   * keeps the test E2E-ish while still making STT/TTS deterministic.
   */
  private fun setStateManagerServiceField(fieldName: String, value: Any) {
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
  private fun forceShowVoiceConfirmationOverlay() {
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

  private class FakeTextToSpeechService : TextToSpeechServiceInterface {
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

  private class FakeSpeechToTextService(private val confirmationResult: Boolean) :
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
}
