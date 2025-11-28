/** Fait par anas sifi mohamed et Gemini comme assistant. */
package com.github.warnastrophy.core.data.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Un service pour gérer la reconnaissance vocale (Speech-to-Text).
 *
 * Ce service écoute l'entrée audio de l'utilisateur, la transcrit en texte et fournit une
 * fonctionnalité pour détecter une confirmation par "oui" ou "non".
 *
 * IMPORTANT : L'application qui utilise ce service doit déclarer la permission `<uses-permission
 * android:name="android.permission.RECORD_AUDIO" />` dans son AndroidManifest.xml et obtenir
 * l'autorisation de l'utilisateur au moment de l'exécution.
 *
 * @param context Le contexte de l'application, nécessaire pour initialiser le SpeechRecognizer.
 */
class SpeechToTextService(private val context: Context) {

  private var speechRecognizer: SpeechRecognizer? = null

  /**
   * Écoute l'entrée vocale de l'utilisateur jusqu'à ce qu'une confirmation ("oui" ou "non") soit
   * détectée.
   *
   * Cette fonction suspendue démarre le processus de reconnaissance vocale et continue d'écouter
   * jusqu'à ce que l'utilisateur dise une variante de "oui" ou "non" en anglais. Si l'utilisateur
   * dit "oui", "yes", ou "yeah", la fonction retourne `true`. Si l'utilisateur dit "non" ou "no",
   * la fonction retourne `false`. Pour tout autre mot, le service continue d'écouter.
   *
   * La reconnaissance s'arrête automatiquement si le coroutine est annulé.
   *
   * @return `true` si l'utilisateur confirme, `false` s'il refuse.
   * @throws Exception si le service de reconnaissance vocale n'est pas disponible sur l'appareil.
   */
  suspend fun listenForConfirmation(): Boolean = suspendCancellableCoroutine { continuation ->
    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
      continuation.cancel(Exception("Speech recognition service is not available on this device."))
      return@suspendCancellableCoroutine
    }

    speechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(context).apply {
          val speechRecognizerIntent =
              Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH.toString())
              }

          setRecognitionListener(
              object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                  val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                  if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0]
                    val confirmation = parseConfirmation(spokenText)

                    if (confirmation != null) {
                      if (continuation.isActive) {
                        continuation.resume(confirmation)
                      }
                    } else {
                      // Si ce n'est pas une confirmation, on continue d'écouter
                      startListening(speechRecognizerIntent)
                    }
                  } else {
                    startListening(speechRecognizerIntent)
                  }
                }

                override fun onError(error: Int) {
                  // On continue d'écouter sauf si l'erreur est fatale
                  if (error == SpeechRecognizer.ERROR_NO_MATCH ||
                      error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    startListening(speechRecognizerIntent)
                  } else {
                    // Pour d'autres erreurs, on pourrait vouloir annuler.
                    // Pour ce cas, nous choisissons de réessayer.
                    startListening(speechRecognizerIntent)
                  }
                }

                override fun onReadyForSpeech(params: Bundle?) {}

                override fun onBeginningOfSpeech() {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {}

                override fun onPartialResults(partialResults: Bundle?) {}

                override fun onEvent(eventType: Int, params: Bundle?) {}
              })

          startListening(speechRecognizerIntent)
        }

    continuation.invokeOnCancellation { destroy() }
  }

  /**
   * Analyse le texte transcrit pour vérifier s'il s'agit d'une confirmation positive ou négative.
   *
   * @param text Le texte à analyser.
   * @return `true` pour "yes", "yeah". `false` pour "no". `null` pour tout autre texte.
   */
  internal fun parseConfirmation(text: String?): Boolean? {
    return when (text?.lowercase(Locale.ROOT)?.trim()) {
      "yes",
      "yeah" -> true
      "no" -> false
      else -> null
    }
  }

  /**
   * Arrête la reconnaissance vocale et libère les ressources. Doit être appelé lorsque le service
   * n'est plus nécessaire pour éviter les fuites de mémoire.
   */
  fun destroy() {
    speechRecognizer?.stopListening()
    speechRecognizer?.destroy()
    speechRecognizer = null
  }
}
