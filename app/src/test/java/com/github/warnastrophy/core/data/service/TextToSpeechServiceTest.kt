package com.github.warnastrophy.core.data.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.github.warnastrophy.core.ui.common.ErrorHandler
import com.github.warnastrophy.core.ui.common.ErrorType
import com.github.warnastrophy.core.ui.navigation.Screen
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TextToSpeechServiceTest {

  private lateinit var context: Context
  private lateinit var errorHandler: ErrorHandler
  private lateinit var service: TextToSpeechService
  private lateinit var tts: TextToSpeech

  @Before
  fun setup() {
    mockkConstructor(TextToSpeech::class)
    every { anyConstructed<TextToSpeech>().shutdown() } returns Unit
    every { anyConstructed<TextToSpeech>().setOnUtteranceProgressListener(any()) } returns 0
    every { anyConstructed<TextToSpeech>().language = any() } returns Unit

    context = mockk(relaxed = true)
    errorHandler = mockk(relaxed = true)
    tts = mockk(relaxed = true)

    service = TextToSpeechService(context, errorHandler)
    service.setField("textToSpeech", tts)
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun `speak blank text does nothing`() {
    service.setField("isInitialized", true)
    service.speak("   ")
    verify { tts wasNot Called }
  }

  @Test
  fun `pending text is saved before initialization`() {
    service.setField("isInitialized", false)
    service.speak("pending review")
    assertEquals("pending review", service.getField("pendingText"))
  }

  @Test
  fun `speak triggers engine when initialized`() {
    service.setField("isInitialized", true)
    every { tts.speak(any(), any(), any(), any()) } returns TextToSpeech.SUCCESS

    service.speak("ready to speak")

    verify { tts.speak("ready to speak", TextToSpeech.QUEUE_FLUSH, null, any()) }
    assertTrue(service.uiState.value.isSpeaking)
  }

  @Test
  fun `speak reports error when text to speech fails`() {
    service.setField("isInitialized", true)
    every { tts.speak(any(), any(), any(), any()) } returns TextToSpeech.ERROR

    service.speak("bad request")

    assertEquals("There was an error during speech synthesis.", service.uiState.value.errorMessage)
    verify(exactly = 1) {
      errorHandler.addErrorToScreen(ErrorType.TEXT_TO_SPEECH_ERROR, Screen.Communication)
    }
  }

  @Test
  fun `onInit success processes pending text`() {
    service.setField("pendingText", "queued message")
    every { tts.speak(any(), any(), any(), any()) } returns TextToSpeech.SUCCESS

    service.onInit(TextToSpeech.SUCCESS)

    verify { tts.setOnUtteranceProgressListener(any()) }
    verify { tts.speak("queued message", TextToSpeech.QUEUE_FLUSH, null, any()) }
    assertTrue(service.uiState.value.isSpeaking)
  }

  @Test
  fun `onInit failure reports initialization error`() {
    service.onInit(TextToSpeech.ERROR)
    assertEquals(
        "There was an error during speech initialization.", service.uiState.value.errorMessage)
    verify(exactly = 1) {
      errorHandler.addErrorToScreen(ErrorType.TEXT_TO_SPEECH_ERROR, Screen.Communication)
    }
  }

  @Test
  fun `utterance listener updates state lifecycle`() {
    val listener = service.getField<UtteranceProgressListener>("utteranceListener")
    service.setField("pendingText", "spoken text")

    listener.onStart("id")
    assertTrue(service.uiState.value.isSpeaking)
    assertEquals(20f, service.uiState.value.rms)
    assertEquals("spoken text", service.uiState.value.spokenText)

    listener.onDone("id")
    assertFalse(service.uiState.value.isSpeaking)
    assertNull(service.uiState.value.spokenText)

    listener.onError("id")
    assertEquals("There was an error during speech synthesis.", service.uiState.value.errorMessage)
    verify(exactly = 1) {
      errorHandler.addErrorToScreen(ErrorType.TEXT_TO_SPEECH_ERROR, Screen.Communication)
    }
  }

  @Test
  fun `destroy resets state and shuts down engine`() {
    service.setField("isInitialized", true)
    service.destroy()

    verify { tts.shutdown() }
    assertEquals(TextToSpeechUiState(), service.uiState.value)
  }

  private fun TextToSpeechService.setField(fieldName: String, value: Any?) {
    val field = TextToSpeechService::class.java.getDeclaredField(fieldName)
    field.isAccessible = true
    field.set(this, value)
  }

  private fun <T> TextToSpeechService.getField(fieldName: String): T {
    val field = TextToSpeechService::class.java.getDeclaredField(fieldName)
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST") return field.get(this) as T
  }
}
