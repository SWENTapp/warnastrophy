package com.github.warnastrophy.core.data.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.speech.tts.UtteranceProgressListener
import com.github.warnastrophy.core.ui.common.ErrorHandler
import com.github.warnastrophy.core.ui.common.ErrorType
import com.github.warnastrophy.core.ui.navigation.Screen
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Represents the UI state for text-to-speech functionality.
 *
 * @property errorMessage The error message if any occurred during speech synthesis.
 * @property isSpeaking Indicates whether the service is currently speaking.
 * @property rms The RMS level for speech visualization.
 * @property spokenText The text currently being spoken.
 */
data class TextToSpeechUiState(
    val errorMessage: String? = null,
    val isSpeaking: Boolean = false,
    val rms: Float = 0f,
    val spokenText: String? = null,
)

/**
 * Service for handling text-to-speech operations using Android's TextToSpeech engine. It manages
 * initialization, speaking text, and error handling.
 *
 * @param context The application context.
 * @param errorHandler The error handler for reporting issues.
 */
class TextToSpeechService(private val context: Context, private val errorHandler: ErrorHandler) :
    OnInitListener {

  companion object {
    private const val DEFAULT_RMS = 20f
  }

  private var textToSpeech: TextToSpeech? = TextToSpeech(context, this)
  private var isInitialized = false
  private var pendingText: String? = null

  private val _uiState = MutableStateFlow(TextToSpeechUiState())

  val uiState: StateFlow<TextToSpeechUiState> = _uiState

  private val utteranceListener =
      object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
          _uiState.value =
              _uiState.value.copy(
                  isSpeaking = true,
                  rms = DEFAULT_RMS,
                  errorMessage = null,
                  spokenText = pendingText)
        }

        override fun onDone(utteranceId: String?) {
          _uiState.value = _uiState.value.copy(isSpeaking = false, rms = 0f, spokenText = null)
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
          _uiState.value =
              _uiState.value.copy(
                  isSpeaking = false,
                  rms = 0f,
                  errorMessage = "There was an error during speech synthesis.")
          errorHandler.addErrorToScreen(ErrorType.TEXT_TO_SPEECH_ERROR, Screen.Communication)
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
          _uiState.value =
              _uiState.value.copy(
                  isSpeaking = false,
                  rms = 0f,
                  errorMessage = "There was an error during speech synthesis.")
          errorHandler.addErrorToScreen(ErrorType.TEXT_TO_SPEECH_ERROR, Screen.Communication)
        }
      }

  /**
   * Called when the TextToSpeech engine is initialized.
   *
   * @param status The initialization status.
   */
  override fun onInit(status: Int) {
    isInitialized = status == TextToSpeech.SUCCESS
    if (isInitialized) {
      textToSpeech?.language = Locale.getDefault()
      textToSpeech?.setOnUtteranceProgressListener(utteranceListener)
      pendingText?.let {
        speak(it)
        pendingText = null
      }
    } else {
      _uiState.value =
          _uiState.value.copy(errorMessage = "There was an error during speech initialization.")
      errorHandler.addErrorToScreen(ErrorType.TEXT_TO_SPEECH_ERROR, Screen.Communication)
    }
  }

  /**
   * Speaks the provided text using the text-to-speech engine.
   *
   * @param text The text to speak.
   */
  fun speak(text: String) {
    if (text.isBlank()) return
    val engine = textToSpeech ?: return
    if (!isInitialized) {
      pendingText = text
      return
    }

    _uiState.value = _uiState.value.copy(isSpeaking = true, rms = DEFAULT_RMS, errorMessage = null)

    val utteranceId = System.currentTimeMillis().toString()
    val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

    if (result == TextToSpeech.ERROR) {
      _uiState.value =
          _uiState.value.copy(
              isSpeaking = false,
              rms = 0f,
              errorMessage = "There was an error during speech synthesis.")
      errorHandler.addErrorToScreen(ErrorType.TEXT_TO_SPEECH_ERROR, Screen.Communication)
    }
  }

  /** Destroys the text-to-speech engine and resets the state. */
  fun destroy() {
    textToSpeech?.shutdown()
    textToSpeech = null
    isInitialized = false
    pendingText = null
    _uiState.value = TextToSpeechUiState() // reset state
  }
}
