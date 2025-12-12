// kotlin
package com.github.warnastrophy.core.ui.components

import MockVoiceCommunicationViewModel
import VoiceCommunicationUiState
import VoiceCommunicationViewModel
import android.content.Context
import com.github.warnastrophy.core.data.service.MockSpeechToTextService
import com.github.warnastrophy.core.data.service.MockTextToSpeechService
import com.github.warnastrophy.core.data.service.SpeechRecognitionUiState
import com.github.warnastrophy.core.data.service.TextToSpeechUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceCommunicationViewModelTest {

  private val testDispatcher = StandardTestDispatcher()
  private lateinit var speechService: MockSpeechToTextService
  private lateinit var textService: MockTextToSpeechService
  private lateinit var context: Context

  private val defaultString: String = "bla bla bla"

  @Before
  fun setup() {
    context = mock()
    // fallback pour éviter NPE sur d'autres appels
    whenever(context.getString(any())).thenReturn(defaultString)
    Dispatchers.setMain(testDispatcher)
    speechService = MockSpeechToTextService()
    textService = MockTextToSpeechService()
  }

  @After
  fun teardown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `ui state merges data from both services on init`() =
      runTest(testDispatcher) {
        val viewModel = VoiceCommunicationViewModel(speechService, textService, context)
        advanceUntilIdle()

        assertEquals(speechService.uiState.value, viewModel.uiState.value.speechState)
        assertEquals(textService.uiState.value, viewModel.uiState.value.textToSpeechState)
      }

  @Test
  fun `speaks emergency message when confirmation is true`() =
      runTest(testDispatcher) {
        val viewModel = VoiceCommunicationViewModel(speechService, textService, context)
        viewModel.launch()
        advanceUntilIdle()

        setSpeechState(
            SpeechRecognitionUiState(
                isListening = false, recognizedText = "yes", isConfirmed = true))
        advanceUntilIdle()

        assertEquals("bla bla bla", textService.uiState.value.spokenText)
      }

  @Test
  fun `speaks cancel message when confirmation is false`() =
      runTest(testDispatcher) {
        val viewModel = VoiceCommunicationViewModel(speechService, textService, context)
        viewModel.launch()
        advanceUntilIdle()

        setSpeechState(
            SpeechRecognitionUiState(
                isListening = false, recognizedText = "no", isConfirmed = false))
        advanceUntilIdle()

        assertEquals(defaultString, textService.uiState.value.spokenText)
      }

  @Test
  fun `viewmodel reflects text to speech updates in combined state`() =
      runTest(testDispatcher) {
        val viewModel = VoiceCommunicationViewModel(speechService, textService, context)
        viewModel.launch()
        advanceUntilIdle()

        setTextState(TextToSpeechUiState(isSpeaking = false, rms = 5f, spokenText = defaultString))
        advanceUntilIdle()

        assertEquals(defaultString, viewModel.uiState.value.textToSpeechState.spokenText)
        assertEquals(5f, viewModel.uiState.value.textToSpeechState.rms)
      }

  @Test
  fun `resetConfirmation resets state and stops listening`() =
      runTest(testDispatcher) {
        val viewModel = VoiceCommunicationViewModel(speechService, textService, context)
        viewModel.launch()
        advanceUntilIdle()

        // Set some state
        setSpeechState(
            SpeechRecognitionUiState(
                isListening = true, recognizedText = "hello", isConfirmed = null))
        advanceUntilIdle()

        // Reset
        viewModel.resetConfirmation()
        advanceUntilIdle()

        // Verify speech state is reset (stopListening calls destroy on speechService)
        assertEquals(SpeechRecognitionUiState(), viewModel.uiState.value.speechState)
      }

  @Test
  fun `launch can be called again after resetConfirmation`() =
      runTest(testDispatcher) {
        val viewModel = VoiceCommunicationViewModel(speechService, textService, context)

        // First launch
        viewModel.launch()
        advanceUntilIdle()

        // Reset
        viewModel.resetConfirmation()
        advanceUntilIdle()

        // Second launch should work
        viewModel.launch()
        advanceUntilIdle()

        // Verify it spoke the confirmation request again
        assertEquals(defaultString, textService.uiState.value.spokenText)
      }

  @Test
  fun `multiple launches are prevented`() =
      runTest(testDispatcher) {
        val viewModel = VoiceCommunicationViewModel(speechService, textService, context)

        viewModel.launch()
        advanceUntilIdle()

        // Clear the spoken text
        setTextState(TextToSpeechUiState(spokenText = null))
        advanceUntilIdle()

        // Second launch should be ignored
        viewModel.launch()
        advanceUntilIdle()

        // spokenText should still be null (not spoken again)
        assertEquals(null, textService.uiState.value.spokenText)
      }

  @Test
  fun `stopListening destroys speech service`() =
      runTest(testDispatcher) {
        val viewModel = VoiceCommunicationViewModel(speechService, textService, context)
        viewModel.launch()
        advanceUntilIdle()

        viewModel.stopListening()
        advanceUntilIdle()

        // Speech service should be destroyed (reset state)
        assertEquals(SpeechRecognitionUiState(), speechService.uiState.value)
      }

  @Test
  fun `speak delegates to text to speech service`() =
      runTest(testDispatcher) {
        val viewModel = VoiceCommunicationViewModel(speechService, textService, context)

        viewModel.speak("Hello world")
        advanceUntilIdle()

        assertEquals("Hello world", textService.uiState.value.spokenText)
      }

  @Test
  fun `starts listening when TTS finishes speaking`() =
      runTest(testDispatcher) {
        val viewModel = VoiceCommunicationViewModel(speechService, textService, context)
        viewModel.launch()
        advanceUntilIdle()

        // Simulate TTS starting to speak
        setTextState(TextToSpeechUiState(isSpeaking = true, spokenText = defaultString))
        advanceUntilIdle()

        // Simulate TTS finishing
        setTextState(TextToSpeechUiState(isSpeaking = false, spokenText = defaultString))
        advanceUntilIdle()

        // Speech service should start listening
        assertEquals(true, speechService.uiState.value.isListening)
      }

  @Test
  fun `MockVoiceCommunicationViewModel resetConfirmation works`() =
      runTest(testDispatcher) {
        val mockViewModel =
            MockVoiceCommunicationViewModel(
                VoiceCommunicationUiState(
                    speechState = SpeechRecognitionUiState(isConfirmed = true)))

        mockViewModel.resetConfirmation()
        advanceUntilIdle()

        assertEquals(null, mockViewModel.uiState.value.speechState.isConfirmed)
      }

  @Test
  fun `MockVoiceCommunicationViewModel updateState works`() =
      runTest(testDispatcher) {
        val mockViewModel = MockVoiceCommunicationViewModel(VoiceCommunicationUiState())

        val newState =
            VoiceCommunicationUiState(
                speechState = SpeechRecognitionUiState(recognizedText = "test"))

        mockViewModel.updateState(newState)
        advanceUntilIdle()

        assertEquals("test", mockViewModel.uiState.value.speechState.recognizedText)
      }

  private fun setSpeechState(state: SpeechRecognitionUiState) {
    val field = speechService.javaClass.getDeclaredField("_uiState").apply { isAccessible = true }
    (field.get(speechService) as MutableStateFlow<SpeechRecognitionUiState>).value = state
  }

  private fun setTextState(state: TextToSpeechUiState) {
    val field = textService.javaClass.getDeclaredField("_uiState").apply { isAccessible = true }
    (field.get(textService) as MutableStateFlow<TextToSpeechUiState>).value = state
  }
}
