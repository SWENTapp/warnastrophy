package com.github.warnastrophy.core.ui.components

import MockVoiceCommunicationViewModel
import VoiceCommunicationUiState
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.warnastrophy.R
import com.github.warnastrophy.core.data.service.SpeechRecognitionUiState
import com.github.warnastrophy.core.data.service.TextToSpeechUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CommunicationScreenTest {

  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun listeningState_displaysListeningLabelTextAndError() {
    val listeningState =
        VoiceCommunicationUiState(
            speechState =
                SpeechRecognitionUiState(
                    isListening = true,
                    rmsLevel = 3.2f,
                    recognizedText = "Heard message",
                    errorMessage = "microphone unavailable"))
    val viewModel = MockVoiceCommunicationViewModel(listeningState)
    composeRule.setContent { CommunicationScreen(viewModel) }

    composeRule
        .onNodeWithTag(CommunicationScreenTags.STATUS_LABEL)
        .assertTextEquals(composeRule.activity.getString(R.string.communication_listening_label))
    composeRule.onNodeWithTag(CommunicationScreenTags.STATUS_TEXT).assertTextEquals("Heard message")
    composeRule
        .onNodeWithTag(CommunicationScreenTags.STATUS_ERROR)
        .assertTextEquals("microphone unavailable")
    composeRule.onNodeWithTag(CommunicationScreenTags.MIC_BUTTON).assertIsDisplayed()
  }

  @Test
  fun speakingState_showsSpeakingTextAndNoError() {
    val speakingState =
        VoiceCommunicationUiState(
            textToSpeechState =
                TextToSpeechUiState(isSpeaking = true, rms = 1.7f, spokenText = "Playing sample"))
    val viewModel = MockVoiceCommunicationViewModel(speakingState)
    composeRule.setContent { CommunicationScreen(viewModel) }

    composeRule
        .onNodeWithTag(CommunicationScreenTags.STATUS_LABEL)
        .assertTextEquals(composeRule.activity.getString(R.string.communication_tts_speaking_label))
    composeRule
        .onNodeWithTag(CommunicationScreenTags.STATUS_TEXT)
        .assertTextEquals("Playing sample")
    composeRule.onNodeWithTag(CommunicationScreenTags.STATUS_ERROR).assertDoesNotExist()
  }

  @Test
  fun idleState_displaysDefaultMessage() {
    val idleState = VoiceCommunicationUiState()
    val viewModel = MockVoiceCommunicationViewModel(idleState)
    composeRule.setContent { CommunicationScreen(viewModel) }

    composeRule
        .onNodeWithTag(CommunicationScreenTags.STATUS_LABEL)
        .assertTextEquals(composeRule.activity.getString(R.string.communication_no_input_hint))
    composeRule.onNodeWithTag(CommunicationScreenTags.STATUS_TEXT).assertTextEquals("")
  }
}
