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

  fun launch() {
    observeDataSources()
    speak(context.getString(R.string.confirmation_request))
    viewModelScope.launch {
      var previousSpeakingState = false
      textToSpeechService.uiState.collect { ttsState ->
        if (!ttsState.isSpeaking && previousSpeakingState) {
          startListening()
        }
        previousSpeakingState = ttsState.isSpeaking
      }
    }
    viewModelScope.launch {
      speechToTextService.uiState.collect { state ->
        if (state.isConfirmed == true) {
          speak(context.getString(R.string.alert_sent))
        } else if (state.isConfirmed == false) {
          speak(context.getString(R.string.alert_not_sent))
        }
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
}
