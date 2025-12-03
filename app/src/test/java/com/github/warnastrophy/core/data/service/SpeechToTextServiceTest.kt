/** Made by Anas Sifi Mohamed and Gemini as assistant. */
package com.github.warnastrophy.core.data.service

import android.content.Context
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import com.github.warnastrophy.core.ui.common.ErrorHandler
import com.github.warnastrophy.core.ui.common.ErrorType
import com.github.warnastrophy.core.ui.navigation.Screen
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.use
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SpeechToTextServiceTest {

  @Mock private lateinit var mockContext: Context
  @Mock private lateinit var mockErrorHandler: ErrorHandler

  private lateinit var speechToTextService: SpeechToTextService

  @Before
  fun setUp() {
    whenever(mockContext.getString(any())).thenReturn("Speech recognition failed")
    speechToTextService = SpeechToTextService(mockContext, mockErrorHandler)
  }

  private fun createMockBundle(matches: List<String>): Bundle {
    val bundle: Bundle = mock()
    whenever(bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION))
        .thenReturn(ArrayList(matches))
    return bundle
  }

  @Test
  fun `listenForConfirmation reports error when recognition not available`() = runTest {
    mockStatic(SpeechRecognizer::class.java).use { staticMock ->
      staticMock
          .`when`<Boolean> { SpeechRecognizer.isRecognitionAvailable(mockContext) }
          .thenReturn(false)

      val exception =
          assertFailsWith<CancellationException> { speechToTextService.listenForConfirmation() }

      assertEquals("Speech recognition failed", exception.cause?.message)
      assertFalse(speechToTextService.uiState.value.isListening)
      verify(mockErrorHandler)
          .addErrorToScreen(ErrorType.SPEECH_RECOGNITION_ERROR, Screen.Communication)
    }
  }

  @Test
  fun `listenForConfirmation captures yes and stops listening`() = runTest {
    mockSpeechRecognizer().use { harness ->
      val job = async { speechToTextService.listenForConfirmation() }
      advanceUntilIdle()
      assertTrue(speechToTextService.uiState.value.isListening)

      harness.listener().onResults(createMockBundle(listOf("yes")))
      advanceUntilIdle()

      val state = speechToTextService.uiState.value
      assertFalse(state.isListening)
      assertEquals("yes", state.recognizedText)
      assertNull(state.errorMessage)

      job.cancelAndJoin()
    }
  }

  @Test
  fun `listenForConfirmation captures no and stops listening`() = runTest {
    mockSpeechRecognizer().use { harness ->
      val job = async { speechToTextService.listenForConfirmation() }
      advanceUntilIdle()

      harness.listener().onResults(createMockBundle(listOf("no")))
      advanceUntilIdle()

      val state = speechToTextService.uiState.value
      assertFalse(state.isListening)
      assertEquals("no", state.recognizedText)

      job.cancelAndJoin()
    }
  }

  @Test
  fun `listenForConfirmation keeps listening for unclear confirmation`() = runTest {
    mockSpeechRecognizer().use { harness ->
      val job = async { speechToTextService.listenForConfirmation() }
      advanceUntilIdle()

      harness.listener().onResults(createMockBundle(listOf("maybe")))
      advanceUntilIdle()

      val state = speechToTextService.uiState.value
      assertTrue(state.isListening)
      assertEquals("maybe", state.recognizedText)
      verify(harness.recognizer, times(2)).startListening(any())

      harness.listener().onResults(createMockBundle(listOf("yes")))
      advanceUntilIdle()
      assertFalse(speechToTextService.uiState.value.isListening)

      job.cancelAndJoin()
    }
  }

  @Test
  fun `listenForConfirmation restarts on null or empty results`() = runTest {
    mockSpeechRecognizer().use { harness ->
      val job = async { speechToTextService.listenForConfirmation() }
      advanceUntilIdle()

      harness.listener().onResults(null)
      advanceUntilIdle()
      assertTrue(speechToTextService.uiState.value.isListening)
      verify(harness.recognizer, times(2)).startListening(any())

      harness.listener().onResults(createMockBundle(emptyList()))
      advanceUntilIdle()
      verify(harness.recognizer, times(3)).startListening(any())

      harness.listener().onResults(createMockBundle(listOf("no")))
      advanceUntilIdle()
      assertFalse(speechToTextService.uiState.value.isListening)

      job.cancelAndJoin()
    }
  }

  @Test
  fun `listenForConfirmation retries after recoverable errors`() = runTest {
    mockSpeechRecognizer().use { harness ->
      val job = async { speechToTextService.listenForConfirmation() }
      advanceUntilIdle()

      harness.listener().onError(SpeechRecognizer.ERROR_NO_MATCH)
      advanceUntilIdle()
      harness.listener().onError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
      advanceUntilIdle()
      harness.listener().onError(SpeechRecognizer.ERROR_NETWORK)
      advanceUntilIdle()

      verify(harness.recognizer, times(4)).startListening(any())
      assertEquals("Speech recognition failed", speechToTextService.uiState.value.errorMessage)

      harness.listener().onResults(createMockBundle(listOf("yes")))
      advanceUntilIdle()
      assertFalse(speechToTextService.uiState.value.isListening)

      job.cancelAndJoin()
    }
  }

  @Test
  fun `listenForConfirmation retries after other errors`() = runTest {
    mockSpeechRecognizer().use { harness ->
      val job = async { speechToTextService.listenForConfirmation() }
      advanceUntilIdle()

      harness.listener().onError(SpeechRecognizer.ERROR_CLIENT)
      advanceUntilIdle()

      verify(harness.recognizer, times(2)).startListening(any())
      assertEquals("Speech recognition failed", speechToTextService.uiState.value.errorMessage)

      job.cancelAndJoin()
    }
  }

  @Test
  fun `listenForConfirmation cancellation cleans up recognizer`() = runTest {
    mockSpeechRecognizer().use { harness ->
      val job = async { speechToTextService.listenForConfirmation() }
      advanceUntilIdle()

      job.cancelAndJoin()
      advanceUntilIdle()

      verify(harness.recognizer).stopListening()
      verify(harness.recognizer).destroy()
    }
  }

  @Test
  fun `destroy stops and releases recognizer`() = runTest {
    mockSpeechRecognizer().use { harness ->
      val job = async { speechToTextService.listenForConfirmation() }
      advanceUntilIdle()

      assertTrue(speechToTextService.uiState.value.isListening)

      speechToTextService.destroy()
      advanceUntilIdle()

      assertFalse(speechToTextService.uiState.value.isListening)
      verify(harness.recognizer).stopListening()
      verify(harness.recognizer).destroy()

      job.cancelAndJoin()
    }
  }

  private fun invokeParseConfirmation(input: String?): Boolean? {
    val method =
        speechToTextService.javaClass.getDeclaredMethod("parseConfirmation", String::class.java)
    method.isAccessible = true
    return method.invoke(speechToTextService, input) as Boolean?
  }

  @Test
  fun `parseConfirmation returns true for yes variants`() {
    assertTrue(invokeParseConfirmation("yes")!!)
    assertTrue(invokeParseConfirmation("YES")!!)
    assertTrue(invokeParseConfirmation(" yes ")!!)
    assertTrue(invokeParseConfirmation("yeah")!!)
  }

  @Test
  fun `parseConfirmation returns false for no variants`() {
    assertFalse(invokeParseConfirmation("no")!!)
    assertFalse(invokeParseConfirmation("NO")!!)
    assertFalse(invokeParseConfirmation(" No ")!!)
  }

  @Test
  fun `parseConfirmation returns null for invalid input`() {
    assertNull(invokeParseConfirmation("hello"))
    assertNull(invokeParseConfirmation("maybe"))
    assertNull(invokeParseConfirmation(""))
    assertNull(invokeParseConfirmation(null))
  }

  @Test
  fun `recognition listener no-op callbacks do not crash`() = runTest {
    mockSpeechRecognizer().use { harness ->
      val job = async { speechToTextService.listenForConfirmation() }
      advanceUntilIdle()

      harness.listener().onReadyForSpeech(null)
      harness.listener().onBeginningOfSpeech()
      harness.listener().onRmsChanged(0.8f)
      harness.listener().onBufferReceived(null)
      harness.listener().onEndOfSpeech()
      harness.listener().onPartialResults(null)
      harness.listener().onEvent(0, null)

      harness.listener().onResults(createMockBundle(listOf("yes")))
      advanceUntilIdle()

      assertFalse(speechToTextService.uiState.value.isListening)
      assertEquals(0f, speechToTextService.uiState.value.rmsLevel)

      job.cancelAndJoin()
    }
  }

  private fun mockSpeechRecognizer(isAvailable: Boolean = true): SpeechRecognizerHarness {
    val recognizer: SpeechRecognizer = mock()
    val listenerRef = AtomicReference<RecognitionListener>()

    doAnswer {
          listenerRef.set(it.arguments[0] as RecognitionListener)
          null
        }
        .`when`(recognizer)
        .setRecognitionListener(any())

    doNothing().`when`(recognizer).startListening(any())
    doNothing().`when`(recognizer).stopListening()
    doNothing().`when`(recognizer).destroy()

    val staticMock = mockStatic(SpeechRecognizer::class.java)
    staticMock
        .`when`<Boolean> { SpeechRecognizer.isRecognitionAvailable(mockContext) }
        .thenReturn(isAvailable)
    staticMock
        .`when`<SpeechRecognizer> { SpeechRecognizer.createSpeechRecognizer(mockContext) }
        .thenReturn(recognizer)

    return SpeechRecognizerHarness(recognizer, listenerRef, staticMock)
  }

  private data class SpeechRecognizerHarness(
      val recognizer: SpeechRecognizer,
      private val listenerRef: AtomicReference<RecognitionListener>,
      private val staticMock: MockedStatic<SpeechRecognizer>
  ) : AutoCloseable {
    fun listener(): RecognitionListener =
        listenerRef.get() ?: error("RecognitionListener has not been registered yet.")

    override fun close() {
      staticMock.close()
    }
  }
}
