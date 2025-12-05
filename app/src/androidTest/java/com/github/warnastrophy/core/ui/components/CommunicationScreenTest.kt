package com.github.warnastrophy.core.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.warnastrophy.R
import com.github.warnastrophy.core.data.service.SpeechRecognitionUiState
import com.github.warnastrophy.core.data.service.SpeechToTextService
import com.github.warnastrophy.core.util.BaseComposeTest
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CommunicationScreenTest : BaseComposeTest() {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  private val uiStateFlow = MutableStateFlow(SpeechRecognitionUiState())

  @MockK(relaxed = true) private lateinit var speechToTextService: SpeechToTextService

  @Before
  fun setUp() {
    MockKAnnotations.init(this, relaxUnitFun = true)
    every { speechToTextService.uiState } returns uiStateFlow
    coEvery { speechToTextService.listenForConfirmation() } returns true
  }

  @After
  fun tearDown() {
    unmockkAll()
  }

  @Test
  fun displaysIdleStateWhenNotListening() {
    setScreen()

    uiStateFlow.value = SpeechRecognitionUiState(isListening = false)
    composeRule.waitForIdle()

    composeRule.onNodeWithText(string(R.string.communication_idle_label)).assertIsDisplayed()
    composeRule.onNodeWithText(string(R.string.communication_no_input_hint)).assertIsDisplayed()
  }

  @Test
  fun displaysListeningStateWithRecognizedText() {
    setScreen()

    uiStateFlow.value =
        SpeechRecognitionUiState(isListening = true, recognizedText = "Listening to you")
    composeRule.waitForIdle()

    composeRule.onNodeWithText(string(R.string.communication_listening_label)).assertIsDisplayed()
    composeRule.onNodeWithText("Listening to you").assertIsDisplayed()
  }

  @Test
  fun showsErrorMessageWhenProvided() {
    setScreen()

    uiStateFlow.value =
        SpeechRecognitionUiState(isListening = true, errorMessage = "Microphone permission missing")
    composeRule.waitForIdle()

    composeRule.onNodeWithText("Microphone permission missing").assertExists()
  }

  @Test
  fun clickingMicLaunchesListeningWhenIdle() {
    setScreen()

    composeRule
        .onNodeWithContentDescription(string(R.string.communication_listen_button))
        .performClick()
    composeRule.waitForIdle()

    coVerify(exactly = 1) { speechToTextService.listenForConfirmation() }
  }

  @Test
  fun clickingMicDoesNothingWhenAlreadyListening() {
    setScreen()

    uiStateFlow.value = SpeechRecognitionUiState(isListening = true)
    composeRule.waitForIdle()

    composeRule
        .onNodeWithContentDescription(string(R.string.communication_listen_button))
        .performClick()
    composeRule.waitForIdle()

    coVerify(exactly = 0) { speechToTextService.listenForConfirmation() }
  }

  private fun setScreen(onBackClick: () -> Unit = {}) {
    composeRule.setContent {
      CommunicationScreen(
          speechToTextService = speechToTextService,
          onBackClick = onBackClick,
      )
    }
    composeRule.waitForIdle()
  }

  private fun string(resId: Int): String = composeRule.activity.getString(resId)
}
