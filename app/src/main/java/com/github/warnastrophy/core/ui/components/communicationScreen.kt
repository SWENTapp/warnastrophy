package com.github.warnastrophy.core.ui.components

import VoiceCommunicationViewModel
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.warnastrophy.R
import com.github.warnastrophy.core.data.service.SpeechRecognitionUiState
import com.github.warnastrophy.core.data.service.TextToSpeechUiState

@Composable
fun CommunicationScreen(
    viewModel: VoiceCommunicationViewModel,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {}
) {
  val uiState by viewModel.uiState.collectAsState()
  val speechState = uiState.speechState
  val textToSpeechState = uiState.textToSpeechState

  Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(modifier = Modifier.padding(24.dp).fillMaxSize()) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = stringResource(R.string.communication_screen_title),
            style = MaterialTheme.typography.titleLarge)
      }

      Spacer(modifier = Modifier.height(32.dp))

      ListeningStatusCard(uiState = speechState)

      Spacer(modifier = Modifier.height(16.dp))

      TextToSpeechStatusCard(
          uiState = textToSpeechState,
          onSpeakClick = { viewModel.speak("Bonjour, comment puis-je vous aider ?") })

      Spacer(modifier = Modifier.height(32.dp))

      AnimatedMicButton(
          uiState = speechState,
          onMicClick = {
            if (speechState.isListening) {
              viewModel.stopListening()
            } else {
              viewModel.startListening()
            }
          })
    }
  }
}

@Composable
private fun ListeningStatusCard(uiState: SpeechRecognitionUiState) {
  ElevatedCard(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(24.dp),
      colors =
          CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(
            modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
              Text(
                  text =
                      if (uiState.isListening)
                          stringResource(R.string.communication_listening_label)
                      else stringResource(R.string.communication_idle_label),
                  style = MaterialTheme.typography.titleMedium)
              Text(
                  text =
                      uiState.recognizedText
                          ?: stringResource(R.string.communication_no_input_hint),
                  style = MaterialTheme.typography.bodyLarge,
                  color = MaterialTheme.colorScheme.onSecondaryContainer,
                  textAlign = TextAlign.Start)
              uiState.errorMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
            }
      }
}

@Composable
private fun TextToSpeechStatusCard(uiState: TextToSpeechUiState, onSpeakClick: () -> Unit) {
  ElevatedCard(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(24.dp),
      colors =
          CardDefaults.elevatedCardColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(
            modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
              Text(
                  text =
                      if (uiState.isSpeaking)
                          stringResource(R.string.communication_tts_speaking_label)
                      else stringResource(R.string.communication_tts_idle_label),
                  style = MaterialTheme.typography.titleMedium)
              Text(
                  text = stringResource(R.string.communication_tts_rms_label, uiState.rms),
                  style = MaterialTheme.typography.bodyMedium)
              uiState.error?.let {
                Text(
                    text = stringResource(R.string.communication_tts_error_label, it),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium)
              }
              Button(onClick = onSpeakClick, enabled = !uiState.isSpeaking) {
                Text(text = stringResource(R.string.communication_tts_sample_button))
              }
            }
      }
}

@Composable
private fun AnimatedMicButton(uiState: SpeechRecognitionUiState, onMicClick: () -> Unit) {
  val animatedScale by
      animateFloatAsState(
          targetValue =
              remember(uiState.rmsLevel) { 1f + (uiState.rmsLevel / 10f) }.coerceIn(1f, 1.5f),
          animationSpec =
              spring(
                  dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
          label = "mic-scale")

  Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClick = onMicClick),
        contentAlignment = Alignment.Center) {
          Icon(
              imageVector = Icons.Default.PlayArrow,
              contentDescription = stringResource(R.string.communication_listen_button),
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.size(48.dp))
        }
  }
}
