package com.github.warnastrophy.core.data.service

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.OnInitListener
import android.speech.tts.UtteranceProgressListener
import com.github.warnastrophy.R
import com.github.warnastrophy.core.ui.common.ErrorHandler
import com.github.warnastrophy.core.ui.common.ErrorType
import com.github.warnastrophy.core.ui.navigation.Screen
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Snapshot of the text-to-speech playback state exposed to the UI layer. */
data class TextToSpeechUiState(
    val isSpeaking: Boolean = false,
    val rmsLevel: Float = 0f,
    val lastText: String? = null,
    val errorMessage: String? = null
)

/**
 * Service responsible for driving the Android text-to-speech engine and exposing a UI-friendly
 * state.
 */
class TextToSpeechService(
    private val context: Context,
    private val errorHandler: ErrorHandler = ErrorHandler()
) : OnInitListener {

  private val _uiState = MutableStateFlow(TextToSpeechUiState())
  val uiState: StateFlow<TextToSpeechUiState> = _uiState.asStateFlow()

  private val engine: TextToSpeechEngine = AndroidTextToSpeechEngine(context, this)
  private var pendingUtterance: String? = null
  private var isInitialized = false

  private val progressListener =
      object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
          _uiState.update { it.copy(isSpeaking = true, errorMessage = null) }
        }

        override fun onDone(utteranceId: String?) {
          _uiState.update { it.copy(isSpeaking = false, rmsLevel = 0f) }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
          // not used
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
          handleError(R.string.error_text_to_speech_failed)
        }

        override fun onStop(utteranceId: String?, interrupted: Boolean) {
          _uiState.update { it.copy(isSpeaking = false, rmsLevel = 0f) }
        }
      }

  init {
    engine.setOnUtteranceProgressListener(progressListener)
    val languageResult = engine.setLanguage(Locale.ENGLISH)
    if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
        languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
      handleError(R.string.error_text_to_speech_unavailable)
    }
  }

  override fun onInit(status: Int) {
    isInitialized = status == TextToSpeech.SUCCESS
    if (!isInitialized) {
      handleError(R.string.error_text_to_speech_unavailable)
      return
    }

    val queuedText = pendingUtterance
    pendingUtterance = null
    if (!queuedText.isNullOrBlank()) {
      speak(queuedText)
    }
  }

  /** Starts speaking the provided [text], queuing it if the engine is not ready yet. */
  fun speak(text: String) {
    if (text.isBlank()) {
      handleError(R.string.error_text_to_speech_empty_input)
      return
    }

    if (!isInitialized) {
      pendingUtterance = text
      return
    }

    val params = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f) }
    val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, nextUtteranceId())

    if (result == TextToSpeech.ERROR) {
      handleError(R.string.error_text_to_speech_failed)
    } else {
      _uiState.update { it.copy(lastText = text, errorMessage = null) }
    }
  }

  /** Updates the RMS level so that UI components (e.g. mic animations) stay in sync. */
  fun updateRmsLevel(rmsDb: Float) {
    _uiState.update { it.copy(rmsLevel = rmsDb.coerceAtLeast(0f)) }
  }

  /** Releases the underlying engine and resets the public state. */
  fun destroy() {
    pendingUtterance = null
    engine.stop()
    engine.shutdown()
    isInitialized = false
    _uiState.value = TextToSpeechUiState()
  }

  private fun handleError(messageRes: Int) {
    val message = context.getString(messageRes)
    _uiState.update { it.copy(isSpeaking = false, rmsLevel = 0f, errorMessage = message) }
    errorHandler.addErrorToScreen(ErrorType.TEXT_TO_SPEECH_ERROR, Screen.Dashboard)
  }

  private fun nextUtteranceId(): String = System.currentTimeMillis().toString()
}

internal interface TextToSpeechEngine {
  fun setLanguage(locale: Locale): Int

  fun setOnUtteranceProgressListener(listener: UtteranceProgressListener)

  fun speak(text: String, queueMode: Int, params: Bundle?, utteranceId: String): Int

  fun stop()

  fun shutdown()
}

private class AndroidTextToSpeechEngine(context: Context, listener: OnInitListener) :
    TextToSpeechEngine {
  private val delegate = TextToSpeech(context, listener)

  override fun setLanguage(locale: Locale): Int = delegate.setLanguage(locale)

  override fun setOnUtteranceProgressListener(listener: UtteranceProgressListener) {
    delegate.setOnUtteranceProgressListener(listener)
  }

  override fun speak(text: String, queueMode: Int, params: Bundle?, utteranceId: String): Int {
    return delegate.speak(text, queueMode, params, utteranceId)
  }

  override fun stop() {
    delegate.stop()
  }

  override fun shutdown() {
    delegate.shutdown()
  }
}
