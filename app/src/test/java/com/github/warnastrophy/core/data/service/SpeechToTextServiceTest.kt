/** Made by Anas Sifi Mohamed and Gemini as assistant. */
package com.github.warnastrophy.core.data.service

import android.content.Context
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import java.util.concurrent.atomic.AtomicReference
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
import org.mockito.kotlin.*

@RunWith(MockitoJUnitRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class SpeechToTextServiceTest {

  @Mock private lateinit var mockContext: Context

  private lateinit var speechToTextService: SpeechToTextService

  @Before
  fun setUp() {
    speechToTextService = SpeechToTextService(mockContext)
  }

  private fun createMockBundle(matches: List<String>): Bundle {
    val bundle: Bundle = mock()
    whenever(bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION))
        .thenReturn(ArrayList(matches))
    return bundle
  }

  @Test
  fun `listenForConfirmation throws when speech recognition not available`() = runTest {
    mockStatic(SpeechRecognizer::class.java).use { staticMock ->
      staticMock
          .`when`<Boolean> { SpeechRecognizer.isRecognitionAvailable(mockContext) }
          .thenReturn(false)

      whenever(mockContext.getString(any())).thenReturn("Speech recognition not available")

      val exception =
          assertFailsWith<kotlinx.coroutines.CancellationException> {
            speechToTextService.listenForConfirmation()
          }

      assertEquals("Speech recognition not available", exception.cause?.message)
    }
  }

  @Test
  fun `listenForConfirmation returns true when yes detected`() = runTest {
    mockSpeechRecognizer().use { harness ->
      val deferred = async { speechToTextService.listenForConfirmation() }
      advanceUntilIdle()

      harness.listener().onResults(createMockBundle(listOf("yes")))
      advanceUntilIdle()

      assertTrue(deferred.await())
      verify(harness.recognizer, times(1)).startListening(any())
    }
  }

  @Test
  fun `listenForConfirmation returns true when yeah detected`() = runTest {
    mockSpeechRecognizer().use { harness ->
      val deferred = async { speechToTextService.listenForConfirmation() }
      advanceUntilIdle()

      harness.listener().onResults(createMockBundle(listOf("yeah")))
      advanceUntilIdle()

      assertTrue(deferred.await())
    }
  }

  @Test
  fun `listenForConfirmation returns false when no detected`() = runTest {
    mockSpeechRecognizer().use { harness ->
      val deferred = async { speechToTextService.listenForConfirmation() }
      advanceUntilIdle()

      harness.listener().onResults(createMockBundle(listOf("no")))
      advanceUntilIdle()

      assertFalse(deferred.await())
      verify(harness.recognizer, times(1)).startListening(any())
    }
  }

  @Test
  fun `listenForConfirmation restarts when null results received`() = runTest {
    mockSpeechRecognizer().use { harness ->
      val deferred = async { speechToTextService.listenForConfirmation() }
      advanceUntilIdle()

      harness.listener().onResults(null)
      advanceUntilIdle()

      verify(harness.recognizer, times(2)).startListening(any())

      harness.listener().onResults(createMockBundle(listOf("yes")))
      advanceUntilIdle()

      assertTrue(deferred.await())
    }
  }

  @Test
  fun `listenForConfirmation restarts when empty results received`() = runTest {
    mockSpeechRecognizer().use { harness ->
      val deferred = async { speechToTextService.listenForConfirmation() }
      advanceUntilIdle()

      harness.listener().onResults(createMockBundle(emptyList()))
      advanceUntilIdle()

      verify(harness.recognizer, times(2)).startListening(any())

      harness.listener().onResults(createMockBundle(listOf("no")))
      advanceUntilIdle()

      assertFalse(deferred.await())
    }
  }

  @Test
  fun `listenForConfirmation restarts when confirmation is unclear`() = runTest {
    mockSpeechRecognizer().use { harness ->
      val deferred = async { speechToTextService.listenForConfirmation() }
      advanceUntilIdle()

      harness.listener().onResults(createMockBundle(listOf("maybe")))
      advanceUntilIdle()

      verify(harness.recognizer, times(2)).startListening(any())

      harness.listener().onResults(createMockBundle(listOf("hello")))
      advanceUntilIdle()

      verify(harness.recognizer, times(3)).startListening(any())

      harness.listener().onResults(createMockBundle(listOf("yes")))
      advanceUntilIdle()

      assertTrue(deferred.await())
    }
  }

  @Test
  fun `listenForConfirmation retries after ERROR_NO_MATCH`() = runTest {
    mockSpeechRecognizer().use { harness ->
      val deferred = async { speechToTextService.listenForConfirmation() }
      advanceUntilIdle()

      harness.listener().onError(SpeechRecognizer.ERROR_NO_MATCH)
      advanceUntilIdle()

      verify(harness.recognizer, times(2)).startListening(any())

      harness.listener().onResults(createMockBundle(listOf("yes")))
      advanceUntilIdle()

      assertTrue(deferred.await())
    }
  }

  @Test
  fun `listenForConfirmation retries after ERROR_SPEECH_TIMEOUT`() = runTest {
    mockSpeechRecognizer().use { harness ->
      val deferred = async { speechToTextService.listenForConfirmation() }
      advanceUntilIdle()

      harness.listener().onError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
      advanceUntilIdle()

      verify(harness.recognizer, times(2)).startListening(any())

      harness.listener().onResults(createMockBundle(listOf("no")))
      advanceUntilIdle()

      assertFalse(deferred.await())
    }
  }

  @Test
  fun `listenForConfirmation retries after ERROR_NETWORK`() = runTest {
    mockSpeechRecognizer().use { harness ->
      val deferred = async { speechToTextService.listenForConfirmation() }
      advanceUntilIdle()

      harness.listener().onError(SpeechRecognizer.ERROR_NETWORK)
      advanceUntilIdle()

      verify(harness.recognizer, times(2)).startListening(any())

      harness.listener().onResults(createMockBundle(listOf("yes")))
      advanceUntilIdle()

      assertTrue(deferred.await())
    }
  }

  @Test
  fun `listenForConfirmation retries after multiple recoverable errors`() = runTest {
    mockSpeechRecognizer().use { harness ->
      val deferred = async { speechToTextService.listenForConfirmation() }
      advanceUntilIdle()

      harness.listener().onError(SpeechRecognizer.ERROR_NO_MATCH)
      advanceUntilIdle()

      harness.listener().onError(SpeechRecognizer.ERROR_NETWORK)
      advanceUntilIdle()

      harness.listener().onError(SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
      advanceUntilIdle()

      verify(harness.recognizer, times(4)).startListening(any())

      harness.listener().onResults(createMockBundle(listOf("no")))
      advanceUntilIdle()

      assertFalse(deferred.await())
    }
  }

  @Test
  fun `listenForConfirmation retries after other errors`() = runTest {
    mockSpeechRecognizer().use { harness ->
      val deferred = async { speechToTextService.listenForConfirmation() }
      advanceUntilIdle()

      harness.listener().onError(SpeechRecognizer.ERROR_CLIENT)
      advanceUntilIdle()

      verify(harness.recognizer, times(2)).startListening(any())

      harness.listener().onResults(createMockBundle(listOf("yes")))
      advanceUntilIdle()

      assertTrue(deferred.await())
    }
  }

  @Test
  fun `listenForConfirmation cancels and destroys recognizer`() = runTest {
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
  fun `destroy stops and releases speech recognizer`() = runTest {
    mockSpeechRecognizer().use { harness ->
      val deferred = async { speechToTextService.listenForConfirmation() }
      advanceUntilIdle()

      speechToTextService.destroy()
      advanceUntilIdle()

      deferred.cancel()

      verify(harness.recognizer).stopListening()
      verify(harness.recognizer).destroy()
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
    assertTrue(invokeParseConfirmation("YEAH")!!)
    assertTrue(invokeParseConfirmation(" Yeah ")!!)
  }

  @Test
  fun `parseConfirmation returns false for no variants`() {
    assertFalse(invokeParseConfirmation("no")!!)
    assertFalse(invokeParseConfirmation("NO")!!)
    assertFalse(invokeParseConfirmation(" No ")!!)
  }

  @Test
  fun `parseConfirmation returns null for invalid inputs`() {
    assertNull(invokeParseConfirmation("hello"))
    assertNull(invokeParseConfirmation("maybe"))
    assertNull(invokeParseConfirmation(""))
    assertNull(invokeParseConfirmation(null))
    assertNull(invokeParseConfirmation("yep"))
  }

  @Test
  fun `RecognitionListener empty callbacks do not crash`() = runTest {
    mockSpeechRecognizer().use { harness ->
      async { speechToTextService.listenForConfirmation() }
      advanceUntilIdle()

      harness.listener().onReadyForSpeech(null)
      harness.listener().onBeginningOfSpeech()
      harness.listener().onRmsChanged(0.5f)
      harness.listener().onBufferReceived(null)
      harness.listener().onEndOfSpeech()
      harness.listener().onPartialResults(null)
      harness.listener().onEvent(0, null)

      harness.listener().onResults(createMockBundle(listOf("yes")))
      advanceUntilIdle()
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
