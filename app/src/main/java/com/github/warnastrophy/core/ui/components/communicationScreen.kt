/**
 * Communication screen UI for voice interactions.
 *
 * This file provides composable UI used when the app is in a voice communication mode. The screen
 * collects UI state from a provided [SpeechToTextService] and renders:
 * - a back navigation icon;
 * - a status card showing whether the service is listening, the last recognized text and any error;
 * - an animated microphone button that pulses with the RMS level and starts/stops listening.
 *
 * Note: the composables in this file are side-effect free except for the button callback which
 * should start/stop the speech service. The speech service itself owns coroutines and platform
 * resources such as the Android SpeechRecognizer.
 */
package com.github.warnastrophy.core.ui.components

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
import com.github.warnastrophy.core.data.service.SpeechToTextService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Top-level communication screen.
 *
 * The screen collects the [SpeechRecognitionUiState] from the given [speechToTextService] and
 * displays a listening status card and a large animated microphone button. The microphone button
 * callback should call the service to start/stop listening; the service exposes its state as a Flow
 * so the UI updates reactively.
 *
 * Parameters:
 *
 * @param speechToTextService service exposing `uiState: Flow<SpeechRecognitionUiState>` and
 *   `listenForConfirmation()` entry point used to start recognition.
 * @param modifier optional [Modifier] applied to the root Surface.
 * @param onBackClick callback invoked when the back arrow is pressed.
 */
@Composable
fun CommunicationScreen(
    speechToTextService: SpeechToTextService,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
) {
  val uiStateFlow = speechToTextService.uiState.collectAsState()
  val uiState = uiStateFlow.value

  Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(modifier = Modifier.padding(24.dp).fillMaxSize()) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.size(8.dp))

        Text(
            text = stringResource(R.string.communication_screen_title),
            style = MaterialTheme.typography.titleLarge)
      }

      Spacer(modifier = Modifier.height(32.dp))

      ListeningStatusCard(uiState = uiState)

      Spacer(modifier = Modifier.height(32.dp))

      AnimatedMicButton(
          uiState = uiState,
          onMicClick = {
            // Replace CoroutineScope.launch with runBlocking for testing purposes
            CoroutineScope(Dispatchers.Main).launch {
              if (!uiState.isListening) {
                speechToTextService.listenForConfirmation()
              }
            }
          })
    }
  }
}

/**
 * Small card that shows the current listening status, the last recognized text and an error message
 * if present.
 *
 * This composable is pure (no side-effects) and only renders the provided [uiState].
 *
 * @param uiState UI state containing `isListening`, `recognizedText`, `rmsLevel` and
 *   `errorMessage`.
 */
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

/**
 * Animated circular microphone button.
 *
 * The central button pulses based on the uiState rms level using a simple spring animation.
 * Pressing the button triggers [onMicClick]. The visual state (pulsing and displayed icon) are
 * driven entirely by the supplied [uiState].
 *
 * @param uiState state used to compute the animation scale (rmsLevel) and the "listening" label.
 * @param onMicClick callback invoked when the user taps the central microphone control.
 */
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
