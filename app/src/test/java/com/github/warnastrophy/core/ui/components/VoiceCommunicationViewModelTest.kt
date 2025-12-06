package com.github.warnastrophy.core.ui.components

import VoiceCommunicationViewModel
import com.github.warnastrophy.core.data.service.SpeechRecognitionUiState
import com.github.warnastrophy.core.data.service.SpeechToTextService
import com.github.warnastrophy.core.data.service.TextToSpeechService
import com.github.warnastrophy.core.data.service.TextToSpeechUiState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceCommunicationViewModelTest {
  private lateinit var speechToTextService: SpeechToTextService
  private lateinit var textToSpeechService: TextToSpeechService
  private lateinit var speechFlow: MutableStateFlow<SpeechRecognitionUiState>
  private lateinit var ttsFlow: MutableStateFlow<TextToSpeechUiState>
  private lateinit var viewModel: VoiceCommunicationViewModel

  @Before
  fun setup() {
    speechFlow = MutableStateFlow(SpeechRecognitionUiState())
    ttsFlow = MutableStateFlow(TextToSpeechUiState())

    speechToTextService = mockk(relaxed = true)
    textToSpeechService = mockk(relaxed = true)

    coEvery { speechToTextService.listenForConfirmation() } returns true
    justRun { speechToTextService.destroy() }
    justRun { textToSpeechService.destroy() }
    justRun { textToSpeechService.speak(any()) }

    every { speechToTextService.uiState } returns speechFlow
    every { textToSpeechService.uiState } returns ttsFlow

    viewModel = VoiceCommunicationViewModel(speechToTextService, textToSpeechService)
  }

  @Test
  fun `combined state reflects each service updates`() = runTest {
    speechFlow.value = SpeechRecognitionUiState(isListening = true)
    ttsFlow.value = TextToSpeechUiState(isSpeaking = true)
    advanceUntilIdle()

    val currentState = viewModel.uiState.value
    assertTrue(currentState.speechState.isListening)
    assertTrue(currentState.textToSpeechState.isSpeaking)
  }

  @Test
  fun `startListening calls listen when not already listening`() = runTest {
    speechFlow.value = SpeechRecognitionUiState(isListening = false)
    viewModel.startListening()
    advanceUntilIdle()

    coVerify(exactly = 1) { speechToTextService.listenForConfirmation() }
  }

  @Test
  fun `startListening is no-op when already listening`() = runTest {
    speechFlow.value = SpeechRecognitionUiState(isListening = true)
    viewModel.startListening()
    advanceUntilIdle()

    coVerify(exactly = 0) { speechToTextService.listenForConfirmation() }
  }

  @Test
  fun `stopListening destroys speech service`() = runTest {
    viewModel.stopListening()
    verify(exactly = 1) { speechToTextService.destroy() }
  }

  @Test
  fun `speak delegates text to textToSpeechService`() = runTest {
    viewModel.speak("hello world")

    verify { textToSpeechService.speak("hello world") }
  }

  @Test
  fun `onCleared releases both services`() = runTest {
    viewModel.clear()

    verify { speechToTextService.destroy() }
    verify { textToSpeechService.destroy() }
  }
}
