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

data class VoiceCommunicationUiState(
    val speechState: SpeechRecognitionUiState = SpeechRecognitionUiState(),
    val textToSpeechState: TextToSpeechUiState = TextToSpeechUiState()
)

class VoiceCommunicationViewModel(
    private val speechToTextService: SpeechToTextService,
    private val textToSpeechService: TextToSpeechService
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

  fun startListening() {
    if (_uiState.value.speechState.isListening) return
    viewModelScope.launch { speechToTextService.listenForConfirmation() }
  }

  fun stopListening() {
    speechToTextService.destroy()
  }

  fun speak(text: String) {
    textToSpeechService.speak(text)
  }

  override fun onCleared() {
    speechToTextService.destroy()
    textToSpeechService.destroy()
    super.onCleared()
  }
}
