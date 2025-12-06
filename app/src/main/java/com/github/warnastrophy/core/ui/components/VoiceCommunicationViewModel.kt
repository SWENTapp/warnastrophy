import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.warnastrophy.core.data.service.SpeechRecognitionUiState
import com.github.warnastrophy.core.data.service.SpeechToTextService
import com.github.warnastrophy.core.data.service.TextToSpeechService
import com.github.warnastrophy.core.data.service.TextToSpeechUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

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
    private val speechToTextService: SpeechToTextService,
    private val textToSpeechService: TextToSpeechService,
) : ViewModel() {

  private val _uiState =
      MutableStateFlow(
          VoiceCommunicationUiState(
              speechState = speechToTextService.uiState.value,
              textToSpeechState = textToSpeechService.uiState.value))
  val uiState: StateFlow<VoiceCommunicationUiState> = _uiState.asStateFlow()

  init {
    observeDataSources()
  }

  /**
   * Observes changes in the speech-to-text and text-to-speech service states and updates the
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
  fun startListening() {
    if (_uiState.value.speechState.isListening) return
    viewModelScope.launch { speechToTextService.listenForConfirmation() }
  }

  /** Stops listening for speech input. */
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
