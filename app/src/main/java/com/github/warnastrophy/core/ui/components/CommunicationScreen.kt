package com.github.warnastrophy.core.ui.components

import VoiceCommunicationViewModelInterface
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.warnastrophy.R
import com.github.warnastrophy.core.data.service.SpeechRecognitionUiState
import com.github.warnastrophy.core.data.service.TextToSpeechUiState

object CommunicationScreenTags {
  const val TITLE = "communication-screen-title"
  const val STATUS_CARD = "communication-status-card"
  const val STATUS_LABEL = "communication-status-label"
  const val STATUS_TEXT = "communication-status-text"
  const val STATUS_ERROR = "communication-status-error"
  const val MIC_BUTTON = "communication-mic-button"
}

/**
 * Displays the communication screen for voice interactions, including listening and speaking
 * status, a sample speak button, and an animated microphone button.
 *
 * @param viewModel The ViewModel managing voice communication state.
 * @param modifier Modifier for the composable.
 */
@Composable
fun CommunicationScreen(
    viewModel: VoiceCommunicationViewModelInterface,
    modifier: Modifier = Modifier,
) {
  val uiState by viewModel.uiState.collectAsState()
  val speechState = uiState.speechState
  val textToSpeechState = uiState.textToSpeechState
  val rms =
      if (speechState.isListening) {
        speechState.rmsLevel
      } else if (textToSpeechState.isSpeaking) {
        textToSpeechState.rms
      } else {
        0f
      }

  Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(modifier = Modifier.padding(24.dp).fillMaxSize()) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            modifier = Modifier.testTag(CommunicationScreenTags.TITLE),
            text = stringResource(R.string.communication_screen_title),
            style = MaterialTheme.typography.titleLarge)
      }

      Spacer(modifier = Modifier.height(32.dp))

      VoiceStatusCard(speechUiState = speechState, listenUiState = textToSpeechState)

      Spacer(modifier = Modifier.height(16.dp))

      Spacer(modifier = Modifier.height(32.dp))

      AnimatedMicButton(rms = rms)
    }
  }
}

/**
 * Displays the current voice status card, showing listening or speaking state, text, and errors.
 *
 * @param speechUiState The state of speech recognition.
 * @param listenUiState The state of text-to-speech.
 */
@Composable
private fun VoiceStatusCard(
    speechUiState: SpeechRecognitionUiState,
    listenUiState: TextToSpeechUiState
) {
  var voiceText: String
  var voiceLabel: String
  var error: String?

  if (speechUiState.isListening) {
    voiceText = speechUiState.recognizedText ?: ""
    voiceLabel = stringResource(R.string.communication_listening_label)
    error = speechUiState.errorMessage
  } else if (listenUiState.isSpeaking) {
    voiceText = listenUiState.spokenText ?: ""
    voiceLabel = stringResource(R.string.communication_tts_speaking_label)
    error = listenUiState.errorMessage
  } else {
    voiceText = ""
    voiceLabel = stringResource(R.string.communication_no_input_hint)
    error = listenUiState.errorMessage
  }
  ElevatedCard(
      modifier = Modifier.fillMaxWidth().testTag(CommunicationScreenTags.STATUS_CARD),
      shape = RoundedCornerShape(24.dp),
      colors =
          CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(
            modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
              Text(
                  modifier = Modifier.testTag(CommunicationScreenTags.STATUS_LABEL),
                  text = voiceLabel,
                  style = MaterialTheme.typography.titleMedium)
              Text(
                  modifier = Modifier.testTag(CommunicationScreenTags.STATUS_TEXT),
                  text = voiceText,
                  style = MaterialTheme.typography.bodyLarge,
                  color = MaterialTheme.colorScheme.onSecondaryContainer,
                  textAlign = TextAlign.Start)
              if (error != null)
                  Text(
                      modifier = Modifier.testTag(CommunicationScreenTags.STATUS_ERROR),
                      text = error,
                      color = MaterialTheme.colorScheme.error)
            }
      }
}

/**
 * Displays an animated microphone button that scales based on RMS level and handles click to
 * start/stop listening.
 *
 * @param rms The RMS level for animation scaling.
 * @param
 */
@Composable
private fun AnimatedMicButton(rms: Float) {
  val animatedScale by
      animateFloatAsState(
          targetValue = remember(rms) { 1f + (rms / 10f) }.coerceIn(1f, 1.5f),
          animationSpec =
              spring(
                  dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
          label = "mic-scale")

  Box(
      modifier = Modifier.fillMaxSize().testTag(CommunicationScreenTags.MIC_BUTTON),
      contentAlignment = Alignment.Center) {
        Box(
            modifier =
                Modifier.size(220.dp)
                    .clip(CircleShape)
                    .background(
                        brush =
                            Brush.radialGradient(
                                colors =
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                        Color.Transparent))))
        Box(
            modifier =
                Modifier.size((140.dp * animatedScale))
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)))
        Box(
            modifier =
                Modifier.size(130.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center) {
              Icon(
                  imageVector = Icons.Default.PlayArrow,
                  contentDescription = stringResource(R.string.communication_listen_button),
                  tint = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.size(48.dp))
            }
      }
}
