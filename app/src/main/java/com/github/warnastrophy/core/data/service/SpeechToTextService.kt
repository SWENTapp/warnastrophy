/**
 * This file was written by Anas Sidi Mohamed with the assistance of ChatGPT.
 *
 * Author: Anas Sidi Mohamed Assistance: ChatGPT
 */
package com.github.warnastrophy.core.data.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.github.warnastrophy.R
import com.github.warnastrophy.core.ui.common.ErrorHandler
import com.github.warnastrophy.core.ui.common.ErrorType
import com.github.warnastrophy.core.ui.navigation.Screen
import java.util.Locale
import kotlin.coroutines.Continuation
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * A service to handle speech recognition (Speech-to-Text).
 *
 * This service listens to the user's audio input, transcribes it into text, and provides
 * functionality to detect a confirmation by "yes" or "no".
 *
 * IMPORTANT: The application using this service must declare the permission `<uses-permission
 * android:name="android.permission.RECORD_AUDIO" />` in its AndroidManifest.xml and obtain the
 * user's permission at runtime.
 *
 * @param context The application context, required to initialize the SpeechRecognizer.
 */
data class SpeechRecognitionUiState(
    val isListening: Boolean = false,
    val rmsLevel: Float = 0f,
    val recognizedText: String? = null,
    val errorMessage: String? = null
)

class SpeechToTextService(
    private val context: Context,
    val errorHandler: ErrorHandler = ErrorHandler()
) {
  private val _uiState = MutableStateFlow(SpeechRecognitionUiState())
  val uiState: StateFlow<SpeechRecognitionUiState> = _uiState.asStateFlow()

  private var speechRecognizer: SpeechRecognizer? = null
  private var currentContinuation: Continuation<Boolean>? = null
  private val speechRecognizerIntent =
      Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH.toString())
      }

  private val recognitionListener =
      object : RecognitionListener {
        override fun onResults(results: Bundle?) {
          val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
          if (matches.isNullOrEmpty()) {
            _uiState.update {
              it.copy(isListening = true, recognizedText = null, errorMessage = null)
            }
            restartListening()
            return
          }

          val spokenText = matches[0]
          when (val confirmation = parseConfirmation(spokenText)) {
            null -> {
              _uiState.update {
                it.copy(isListening = true, recognizedText = spokenText, errorMessage = null)
              }
              restartListening()
            }
            else -> completeListening(spokenText)
          }
        }

        override fun onError(error: Int) {
          _uiState.update {
            Log.d("SpeechToTextService", "Speech recognition error: $error")
            it.copy(errorMessage = context.getString(R.string.error_speech_recognition_failed))
          }
          restartListening()
        }

        override fun onReadyForSpeech(params: Bundle?) {
          // No action needed
        }

        override fun onBeginningOfSpeech() {
          // No action needed
        }

        override fun onRmsChanged(rmsdB: Float) {
          _uiState.update { it.copy(rmsLevel = rmsdB.coerceAtLeast(0f)) }
        }

        override fun onBufferReceived(buffer: ByteArray?) {
          // No action needed
        }

        override fun onEndOfSpeech() {
          // No action needed
        }

        override fun onPartialResults(partialResults: Bundle?) {
          // No action needed
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
          // No action needed
        }
      }

  private fun restartListening() {
    speechRecognizer?.startListening(speechRecognizerIntent)
  }

  private fun completeListening(spokenText: String) {
    val confirmed = parseConfirmation(spokenText) ?: false
    val continuation = currentContinuation
    currentContinuation = null

    _uiState.update {
      it.copy(isListening = false, rmsLevel = 0f, recognizedText = spokenText, errorMessage = null)
    }

    // Stop recognizer first
    speechRecognizer?.stopListening()
    speechRecognizer?.destroy()
    speechRecognizer = null

    // Resume the continuation with the result
    continuation?.resumeWith(Result.success(confirmed))
  }

  /**
   * Listens to the user's voice input until a confirmation ("yes" or "no") is detected.
   *
   * This suspended function starts the speech recognition process and continues listening until the
   * user says a variant of "yes" or "no" in English. If the user says "yes", "yeah", or "yep", the
   * function returns `true`. If the user says "no", the function returns `false`. For any other
   * word, the service continues listening.
   *
   * Recognition stops automatically if the coroutine is canceled.
   *
   * @return `true` if the user confirms, `false` if they decline.
   * @throws Exception if the speech recognition service is not available on the device.
   */
  suspend fun listenForConfirmation(): Boolean = suspendCancellableCoroutine { continuation ->
    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
      errorHandler.addErrorToScreen(ErrorType.SPEECH_RECOGNITION_ERROR, Screen.Communication)
      Log.d("SpeechToTextService", "Speech recognition not available")
      continuation.cancel(
          CancellationException(context.getString(R.string.error_speech_recognition_failed)))
      return@suspendCancellableCoroutine
    }

    _uiState.value =
        _uiState.value.copy(
            isListening = true, rmsLevel = 0f, recognizedText = null, errorMessage = null)
    currentContinuation = continuation

    speechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(context).apply {
          setRecognitionListener(recognitionListener)
          startListening(speechRecognizerIntent)
        }

    continuation.invokeOnCancellation {
      currentContinuation = null
      _uiState.value = _uiState.value.copy(isListening = false)
      destroy()
    }
  }

  /**
   * Analyzes the transcribed text to check if it is a positive or negative confirmation.
   *
   * @param text The text to analyze.
   * @return `true` for "yes", "yeah". `false` for "no". `null` for any other text.
   */
  private fun parseConfirmation(text: String?): Boolean? {
    return when (text?.lowercase(Locale.ROOT)?.trim()) {
      "yes",
      "yeah" -> true
      "no" -> false
      else -> null
    }
  }

  /**
   * Stops speech recognition and releases resources. Must be called when the service is no longer
   * needed to avoid memory leaks.
   */
  fun destroy() {
    currentContinuation = null
    speechRecognizer?.stopListening()
    speechRecognizer?.destroy()
    speechRecognizer = null
    _uiState.update { it.copy(isListening = false) }
  }
}
