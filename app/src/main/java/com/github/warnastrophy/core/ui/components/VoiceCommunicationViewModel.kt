import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.warnastrophy.R
import com.github.warnastrophy.core.data.service.SpeechRecognitionUiState
import com.github.warnastrophy.core.data.service.SpeechToTextServiceInterface
import com.github.warnastrophy.core.data.service.TextToSpeechServiceInterface
import com.github.warnastrophy.core.data.service.TextToSpeechUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

interface VoiceCommunicationViewModelInterface {
  val uiState: StateFlow<VoiceCommunicationUiState>

  fun resetConfirmation()
}
/**
 * Represents the combined UI state for voice communication, including speech recognition and
 * text-to-speech states.
 *
 * @property speechState The current state of speech recognition.
 * @property textToSpeechState The current state of text-to-speech.
 */
data class VoiceCommunicationUiState(
    val speechState: SpeechRecognitionUiState = SpeechRecognitionUiState(),
    val textToSpeechState: TextToSpeechUiState = TextToSpeechUiState()
)

/**
 * ViewModel for managing voice communication, including speech recognition and text-to-speech
 * functionality. It combines the states from both services and provides methods to control
 * listening and speaking.
 *
 * @param speechToTextService The service for speech-to-text operations.
 * @param textToSpeechService The service for text-to-speech operations.
 */
class VoiceCommunicationViewModel(
    private val speechToTextService: SpeechToTextServiceInterface,
    private val textToSpeechService: TextToSpeechServiceInterface,
    private val context: Context
) : ViewModel(), VoiceCommunicationViewModelInterface {

  private val _uiState =
      MutableStateFlow(
          VoiceCommunicationUiState(
              speechState = speechToTextService.uiState.value,
              textToSpeechState = textToSpeechService.uiState.value))
  override val uiState: StateFlow<VoiceCommunicationUiState> = _uiState.asStateFlow()

  private var isLaunched = false
  private var ttsListenerJob: kotlinx.coroutines.Job? = null
  private var speechListenerJob: kotlinx.coroutines.Job? = null
  private var hasSpokenFinalMessage = false

  fun launch() {
    // Prevent multiple launches
    if (isLaunched) return
    isLaunched = true
    hasSpokenFinalMessage = false

    observeDataSources()
    speak(context.getString(R.string.confirmation_request))

    ttsListenerJob =
        viewModelScope.launch {
          var previousSpeakingState = false
          textToSpeechService.uiState.collect { ttsState ->
            // Only start listening if TTS finished and we haven't spoken the final message yet
            if (!ttsState.isSpeaking && previousSpeakingState && !hasSpokenFinalMessage) {
              startListening()
            }
            previousSpeakingState = ttsState.isSpeaking
          }
        }

    speechListenerJob =
        viewModelScope.launch {
          var previousConfirmed: Boolean? = null
          speechToTextService.uiState.collect { state ->
            // Only speak the message once when isConfirmed changes from null to a value
            if (state.isConfirmed != null && previousConfirmed == null) {
              hasSpokenFinalMessage = true
              if (state.isConfirmed == true) {
                speak(context.getString(R.string.alert_sent))
              } else {
                speak(context.getString(R.string.alert_not_sent))
              }
            }
            previousConfirmed = state.isConfirmed
          }
        }
  }

  /**
   * est-ce Observes changes in the speech-to-text and text-to-speech service states and updates the
   * combined UI state.
   */
  private fun observeDataSources() {
    combine(speechToTextService.uiState, textToSpeechService.uiState) {
            speechState,
            textToSpeechState ->
          VoiceCommunicationUiState(
              speechState = speechState, textToSpeechState = textToSpeechState)
        }
        .onEach { _uiState.value = it }
        .launchIn(viewModelScope)
  }

  /** Starts listening for speech input if not already listening. */
  suspend fun startListening() {
    speechToTextService.listenForConfirmation()
  }

  /** Stops listenin g for speech input. */
  fun stopListening() {
    speechToTextService.destroy()
  }

  /**
   * Speaks the provided text using the text-to-speech service.
   *
   * @param text The text to speak.
   */
  fun speak(text: String) {
    textToSpeechService.speak(text)
  }

  /** Cleans up resources when the ViewModel is cleared. */
  fun clear() = { onCleared() }

  /** Resets the confirmation state to null for the next voice confirmation flow. */
  override fun resetConfirmation() {
    // Cancel ongoing jobs
    ttsListenerJob?.cancel()
    speechListenerJob?.cancel()
    ttsListenerJob = null
    speechListenerJob = null
    isLaunched = false
    hasSpokenFinalMessage = false

    // Stop any ongoing speech/listening
    stopListening()

    _uiState.value = VoiceCommunicationUiState()
  }

  override fun onCleared() {
    speechToTextService.destroy()
    textToSpeechService.destroy()
    super.onCleared()
  }
}

class MockVoiceCommunicationViewModel(initialState: VoiceCommunicationUiState) :
    VoiceCommunicationViewModelInterface {
  private val _uiState = MutableStateFlow(initialState)
  override val uiState: StateFlow<VoiceCommunicationUiState> = _uiState

  fun updateState(state: VoiceCommunicationUiState) {
    _uiState.value = state
  }

  override fun resetConfirmation() {
    _uiState.value =
        _uiState.value.copy(speechState = _uiState.value.speechState.copy(isConfirmed = null))
  }
}
