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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceCommunicationViewModelTest {

  @get:Rule val mainDispatcherRule = MainDispatcherRule()

  private lateinit var speechToTextService: SpeechToTextService
  private lateinit var textToSpeechService: TextToSpeechService
  private lateinit var speechFlow: MutableStateFlow<SpeechRecognitionUiState>
  private lateinit var ttsFlow: MutableStateFlow<TextToSpeechUiState>
  private lateinit var viewModel: TestableVoiceCommunicationViewModel

  @Before
  fun setup() {
    speechFlow = MutableStateFlow(SpeechRecognitionUiState())
    ttsFlow = MutableStateFlow(TextToSpeechUiState())

    speechToTextService = mockk(relaxed = true)
    textToSpeechService = mockk(relaxed = true)

    every { speechToTextService.uiState } returns speechFlow
    every { textToSpeechService.uiState } returns ttsFlow

    coEvery { speechToTextService.listenForConfirmation() } returns true
    justRun { speechToTextService.destroy() }
    justRun { textToSpeechService.destroy() }
    justRun { textToSpeechService.speak(any()) }

    viewModel = TestableVoiceCommunicationViewModel(speechToTextService, textToSpeechService)
  }

  @Test
  fun `ui state mirrors upstream flows`() = runTest {
    val speechState = SpeechRecognitionUiState(isListening = true, recognizedText = "yes")
    val ttsState = TextToSpeechUiState(isSpeaking = true, spokenText = "hello")

    speechFlow.value = speechState
    ttsFlow.value = ttsState
    advanceUntilIdle()

    val combined = viewModel.uiState.value
    assertEquals(speechState, combined.speechState)
    assertEquals(ttsState, combined.textToSpeechState)
  }

  @Test
  fun `startListening invokes speech service when idle`() = runTest {
    speechFlow.value = SpeechRecognitionUiState(isListening = false)

    val result = viewModel.startListening()

    assertEquals(true, result)
    coVerify(exactly = 1) { speechToTextService.listenForConfirmation() }
  }

  @Test
  fun `startListening is skipped when already listening`() = runTest {
    speechFlow.value = SpeechRecognitionUiState(isListening = true)

    val result = viewModel.startListening()

    assertEquals(true, result)
    coVerify(exactly = 0) { speechToTextService.listenForConfirmation() }
  }

  @Test
  fun `stopListening destroys speech service`() = runTest {
    viewModel.stopListening()

    verify(exactly = 1) { speechToTextService.destroy() }
  }

  @Test
  fun `speak delegates to text to speech`() = runTest {
    val expected = "hello there"

    viewModel.speak(expected)

    verify { textToSpeechService.speak(expected) }
  }

  @Test
  fun `onCleared destroys both services`() = runTest {
    viewModel.invokeOnCleared()

    verify { speechToTextService.destroy() }
    verify { textToSpeechService.destroy() }
  }

  private class TestableVoiceCommunicationViewModel(
      speechToTextService: SpeechToTextService,
      textToSpeechService: TextToSpeechService
  ) : VoiceCommunicationViewModel(speechToTextService, textToSpeechService) {
    fun invokeOnCleared() = super.onCleared()
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()) :
    TestWatcher() {

  override fun starting(description: Description) {
    Dispatchers.setMain(dispatcher)
  }

  override fun finished(description: Description) {
    Dispatchers.resetMain()
  }
}
