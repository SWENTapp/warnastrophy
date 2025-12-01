/** Made by Anas Sifi Mohamed and Gemini as assistant. */
package com.github.warnastrophy.core.data.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.github.warnastrophy.R
import com.github.warnastrophy.core.ui.common.ErrorHandler
import com.github.warnastrophy.core.ui.common.ErrorType
import com.github.warnastrophy.core.ui.navigation.Screen
import java.util.Locale
import kotlin.coroutines.Continuation
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlinx.coroutines.isActive
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
class SpeechToTextService(
    private val context: Context,
    val errorHandler: ErrorHandler = ErrorHandler()
) {

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
          if (!matches.isNullOrEmpty()) {
            val spokenText = matches[0]
            val confirmation = parseConfirmation(spokenText)

            if (confirmation != null) {
              currentContinuation?.let { continuation ->
                if (continuation.context.isActive) {
                  continuation.resume(confirmation)
                  currentContinuation = null
                }
              }
            } else {
              speechRecognizer?.startListening(speechRecognizerIntent)
            }
          } else {
            speechRecognizer?.startListening(speechRecognizerIntent)
          }
        }

        override fun onError(error: Int) {
          // Continue listening unless the error is fatal
          speechRecognizer?.startListening(speechRecognizerIntent)
        }

        override fun onReadyForSpeech(params: Bundle?) {
          // No action needed
        }

        override fun onBeginningOfSpeech() {
          // No action needed
        }

        override fun onRmsChanged(rmsdB: Float) {
          // No action needed
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
      errorHandler.addErrorToScreen(ErrorType.SPEECH_RECOGNITION_ERROR, Screen.Dashboard)
      continuation.cancel(
          CancellationException(context.getString(R.string.error_speech_recognition_failed)))
      return@suspendCancellableCoroutine
    }

    currentContinuation = continuation

    speechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(context).apply {
          setRecognitionListener(recognitionListener)
          startListening(speechRecognizerIntent)
        }

    continuation.invokeOnCancellation {
      currentContinuation = null
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
    speechRecognizer?.stopListening()
    speechRecognizer?.destroy()
    speechRecognizer = null
  }
}
