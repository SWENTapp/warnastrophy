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

data class TextToSpeechUiState(
    val error: ErrorType? = null,
    val isSpeaking: Boolean = false,
    val rms: Float = 0f
)

class TextToSpeechService(private val context: Context, private val errorHandler: ErrorHandler) :
    OnInitListener {

  companion object {
    private const val DEFAULT_RMS = 0.5f
  }

  private var textToSpeech: TextToSpeech? = TextToSpeech(context, this)
  private var isInitialized = false
  private var pendingText: String? = null

  private val _uiState = MutableStateFlow(TextToSpeechUiState())

  val uiState: StateFlow<TextToSpeechUiState> = _uiState

  private val utteranceListener =
      object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
          _uiState.value = _uiState.value.copy(isSpeaking = true, rms = DEFAULT_RMS, error = null)
        }

        override fun onDone(utteranceId: String?) {
          _uiState.value = _uiState.value.copy(isSpeaking = false, rms = 0f)
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
          _uiState.value =
              _uiState.value.copy(
                  isSpeaking = false, rms = 0f, error = ErrorType.TEXT_TO_SPEECH_ERROR)
          errorHandler.addErrorToScreen(ErrorType.TEXT_TO_SPEECH_ERROR, Screen.Communication)
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
          _uiState.value =
              _uiState.value.copy(
                  isSpeaking = false, rms = 0f, error = ErrorType.TEXT_TO_SPEECH_ERROR)
          errorHandler.addErrorToScreen(ErrorType.TEXT_TO_SPEECH_ERROR, Screen.Communication)
        }
      }

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
      _uiState.value = _uiState.value.copy(error = ErrorType.TEXT_TO_SPEECH_ERROR)
      errorHandler.addErrorToScreen(ErrorType.TEXT_TO_SPEECH_ERROR, Screen.Communication)
    }
  }

  fun speak(text: String) {
    if (text.isBlank()) return
    val engine = textToSpeech ?: return
    if (!isInitialized) {
      pendingText = text
      return
    }

    _uiState.value = _uiState.value.copy(isSpeaking = true, rms = DEFAULT_RMS, error = null)

    val utteranceId = System.currentTimeMillis().toString()
    val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

    if (result == TextToSpeech.ERROR) {
      _uiState.value =
          _uiState.value.copy(isSpeaking = false, rms = 0f, error = ErrorType.TEXT_TO_SPEECH_ERROR)
      errorHandler.addErrorToScreen(ErrorType.TEXT_TO_SPEECH_ERROR, Screen.Communication)
    }
  }

  fun destroy() {
    textToSpeech?.shutdown()
    textToSpeech = null
    isInitialized = false
    pendingText = null
    _uiState.value = TextToSpeechUiState() // reset state
  }
}
