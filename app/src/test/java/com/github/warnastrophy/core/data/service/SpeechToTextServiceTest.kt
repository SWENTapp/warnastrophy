// kotlin
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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedStatic
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

  @Mock lateinit var context: Context
  @Mock lateinit var errorHandler: ErrorHandler

  private lateinit var service: SpeechToTextService

  @Before
  fun setup() {
    whenever(context.getString(any())).thenReturn("Speech recognition failed")
  }

  private fun mockBundle(matches: List<String>?): Bundle? =
      matches?.let {
        mock<Bundle>().apply {
          whenever(getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION))
              .thenReturn(ArrayList(matches))
        }
      }

  @Test
  fun listenForConfirmation_failWhenUnavailable() = runTest {
    mockSpeechRecognizer(isAvailable = false).use {
      service = SpeechToTextService(context, errorHandler)

      val exception = assertFailsWith<CancellationException> { service.listenForConfirmation() }
      assertEquals("Speech recognition failed", exception.cause?.message)
      assertFalse(service.uiState.value.isListening)
      verify(errorHandler)
          .addErrorToScreen(ErrorType.SPEECH_RECOGNITION_ERROR, Screen.Communication)
    }
  }

  @Test
  fun listenForConfirmation_acceptsYes() = runTest {
    mockSpeechRecognizer().use { harness ->
      service = SpeechToTextService(context, errorHandler)

      val job = async { service.listenForConfirmation() }
      advanceUntilIdle()

      harness.listener().onResults(mockBundle(listOf("yes")))
      advanceUntilIdle()

      val state = service.uiState.value
      assertFalse(state.isListening)
      assertEquals("yes", state.recognizedText)
      assertEquals(true, state.isConfirmed)

      job.cancelAndJoin()
    }
  }

  @Test
  fun listenForConfirmation_acceptsNo() = runTest {
    mockSpeechRecognizer().use { harness ->
      service = SpeechToTextService(context, errorHandler)

      val job = async { service.listenForConfirmation() }
      advanceUntilIdle()

      harness.listener().onResults(mockBundle(listOf("no")))
      advanceUntilIdle()

      val state = service.uiState.value
      assertFalse(state.isListening)
      assertEquals("no", state.recognizedText)
      assertEquals(false, state.isConfirmed)

      job.cancelAndJoin()
    }
  }

  @Test
  fun listenForConfirmation_retriesOnUnclearInput() = runTest {
    mockSpeechRecognizer().use { harness ->
      service = SpeechToTextService(context, errorHandler)

      val job = async { service.listenForConfirmation() }
      advanceUntilIdle()

      harness.listener().onResults(mockBundle(listOf("maybe")))
      advanceUntilIdle()
      assertTrue(service.uiState.value.isListening)
      verify(harness.recognizer, times(2)).startListening(any())

      harness.listener().onResults(mockBundle(listOf("yes")))
      advanceUntilIdle()
      assertFalse(service.uiState.value.isListening)

      job.cancelAndJoin()
    }
  }

  @Test
  fun listenForConfirmation_cancelledStopsRecognizer() = runTest {
    mockSpeechRecognizer().use { harness ->
      service = SpeechToTextService(context, errorHandler)

      val job = async { service.listenForConfirmation() }
      advanceUntilIdle()

      job.cancelAndJoin()
      advanceUntilIdle()

      verify(harness.recognizer).stopListening()
      verify(harness.recognizer).destroy()
    }
  }

  @Test
  fun destroyStopsRecognition() = runTest {
    mockSpeechRecognizer().use { harness ->
      service = SpeechToTextService(context, errorHandler)

      val job = async { service.listenForConfirmation() }
      advanceUntilIdle()

      service.destroy()
      advanceUntilIdle()

      assertFalse(service.uiState.value.isListening)
      verify(harness.recognizer).stopListening()
      verify(harness.recognizer).destroy()

      job.cancelAndJoin()
    }
  }

  @Test
  fun parseConfirmationVariants() {
    mockSpeechRecognizer().use { harness ->
      service = SpeechToTextService(context, errorHandler)

      val method =
          service.javaClass.getDeclaredMethod("parseConfirmation", String::class.java).apply {
            isAccessible = true
          }

      fun parse(input: String?) = method.invoke(service, input) as Boolean?

      assertTrue(parse("yes")!!)
      assertTrue(parse("Yeah")!!)
      assertFalse(parse("no")!!)
      assertNull(parse("maybe"))
      assertNull(parse(""))
    }
  }

  private fun mockSpeechRecognizer(isAvailable: Boolean = true): SpeechRecognizerHarness {
    val recognizer: SpeechRecognizer = mock()
    val ref = AtomicReference<RecognitionListener>()

    doAnswer { invocation ->
          ref.set(invocation.arguments[0] as RecognitionListener)
          null
        }
        .whenever(recognizer)
        .setRecognitionListener(any())

    doNothing().whenever(recognizer).startListening(any())
    doNothing().whenever(recognizer).stopListening()
    doNothing().whenever(recognizer).destroy()

    val staticMock: MockedStatic<SpeechRecognizer> =
        org.mockito.Mockito.mockStatic(SpeechRecognizer::class.java).apply {
          `when`<Boolean> { SpeechRecognizer.isRecognitionAvailable(context) }
              .thenReturn(isAvailable)
          `when`<SpeechRecognizer> { SpeechRecognizer.createSpeechRecognizer(context) }
              .thenReturn(recognizer)
        }

    return SpeechRecognizerHarness(recognizer, ref, staticMock)
  }

  private data class SpeechRecognizerHarness(
      val recognizer: SpeechRecognizer,
      private val listenerRef: AtomicReference<RecognitionListener>,
      private val staticMock: MockedStatic<SpeechRecognizer>
  ) : AutoCloseable {
    fun listener(): RecognitionListener =
        listenerRef.get() ?: error("Recognizer listener has not been set.")

    override fun close() {
      staticMock.close()
    }
  }
}
