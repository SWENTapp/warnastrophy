package com.github.warnastrophy.core.data.service

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.github.warnastrophy.R
import com.github.warnastrophy.core.ui.common.ErrorHandler
import com.github.warnastrophy.core.ui.common.ErrorType
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class TextToSpeechServiceTest {

  @Mock private lateinit var mockContext: Context

  @Before
  fun setUp() {
    whenever(mockContext.getString(any())).thenAnswer { "res-${it.arguments.first()}" }
  }

  @Test
  fun `speak with blank text reports error and skips engine`() {
    val errorHandler = ErrorHandler()
    val fakeEngine = FakeEngine()
    val service = createService(fakeEngine, errorHandler)

    service.onInit(TextToSpeech.SUCCESS)
    service.speak("   ")

    assertEquals("res-${R.string.error_text_to_speech_empty_input}", service.uiState.value.errorMessage)
    assertNull(fakeEngine.lastSpokenText)
    assertTrue(
        errorHandler.state.value.errors.any {
          it.type == ErrorType.TEXT_TO_SPEECH_ERROR && it.screenTypes.contains(com.github.warnastrophy.core.ui.navigation.Screen.Dashboard)
        })
  }

  @Test
  fun `speak queues until initialization completes`() {
    val fakeEngine = FakeEngine()
    val service = createService(fakeEngine)

    service.speak("hello")
    assertNull(fakeEngine.lastSpokenText)

    service.onInit(TextToSpeech.SUCCESS)
    assertEquals("hello", fakeEngine.lastSpokenText)
  }

  @Test
  fun `successful speak updates ui state via progress callbacks`() {
    val fakeEngine = FakeEngine()
    val service = createService(fakeEngine)

    service.onInit(TextToSpeech.SUCCESS)
    service.speak("world")
    assertNull(service.uiState.value.lastText)

    fakeEngine.listener?.onStart("id")
    assertTrue(service.uiState.value.isSpeaking)

    fakeEngine.listener?.onDone("id")
    assertEquals("world", service.uiState.value.lastText)
    assertEquals(0f, service.uiState.value.rmsLevel)
    assertTrue(!service.uiState.value.isSpeaking)
  }

  @Test
  fun `engine speak error surfaces to ui`() {
    val fakeEngine = FakeEngine().apply { speakResult = TextToSpeech.ERROR }
    val errorHandler = ErrorHandler()
    val service = createService(fakeEngine, errorHandler)

    service.onInit(TextToSpeech.SUCCESS)
    service.speak("issue")

    assertEquals("res-${R.string.error_text_to_speech_failed}", service.uiState.value.errorMessage)
    assertTrue(
        errorHandler.state.value.errors.any { it.type == ErrorType.TEXT_TO_SPEECH_ERROR })
  }

  @Test
  fun `language not supported during init reports error`() {
    val fakeEngine = FakeEngine(languageResult = TextToSpeech.LANG_NOT_SUPPORTED)
    val errorHandler = ErrorHandler()

    val service = createService(fakeEngine, errorHandler)

    assertEquals(
        "res-${R.string.error_text_to_speech_unavailable}", service.uiState.value.errorMessage)
    assertTrue(
        errorHandler.state.value.errors.any { it.type == ErrorType.TEXT_TO_SPEECH_ERROR })
  }

  @Test
  fun `updateRmsLevel clamps negative values`() {
    val service = createService(FakeEngine())

    service.updateRmsLevel(-5f)
    assertEquals(0f, service.uiState.value.rmsLevel)
  }

  @Test
  fun `destroy stops engine and resets state`() {
    val fakeEngine = FakeEngine()
    val service = createService(fakeEngine)

    service.onInit(TextToSpeech.SUCCESS)
    service.speak("bye")
    fakeEngine.listener?.onStart("id")

    service.destroy()

    assertTrue(fakeEngine.stopCalled)
    assertTrue(fakeEngine.shutdownCalled)
    assertEquals(TextToSpeechUiState(), service.uiState.value)
  }

  @Test
  fun `progress listener error routes through handler`() {
    val fakeEngine = FakeEngine()
    val errorHandler = ErrorHandler()
    val service = createService(fakeEngine, errorHandler)

    service.onInit(TextToSpeech.SUCCESS)
    service.speak("test")

    fakeEngine.listener?.onError("id")

    assertEquals("res-${R.string.error_text_to_speech_failed}", service.uiState.value.errorMessage)
    assertTrue(
        errorHandler.state.value.errors.any { it.type == ErrorType.TEXT_TO_SPEECH_ERROR })
  }

  private fun createService(
      engine: FakeEngine,
      handler: ErrorHandler = ErrorHandler()
  ): TextToSpeechService {
    return TextToSpeechService(mockContext, handler) { _, _ -> engine }
  }

  private class FakeEngine(private var languageResult: Int = TextToSpeech.LANG_AVAILABLE) :
      TextToSpeechEngine {
    var listener: UtteranceProgressListener? = null
    var lastSpokenText: String? = null
    var stopCalled = false
    var shutdownCalled = false
    var speakResult: Int = TextToSpeech.SUCCESS

    override fun setLanguage(locale: Locale): Int = languageResult

    override fun setOnUtteranceProgressListener(listener: UtteranceProgressListener) {
      this.listener = listener
    }

    override fun speak(text: String, queueMode: Int, params: Bundle?, utteranceId: String): Int {
      lastSpokenText = text
      return speakResult
    }

    override fun stop() {
      stopCalled = true
    }

    override fun shutdown() {
      shutdownCalled = true
    }
  }
}

