package com.github.warnastrophy.core.ui.features.voiceconfirmation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.warnastrophy.R
import com.github.warnastrophy.core.data.service.SpeechToTextService
import kotlinx.coroutines.launch

/** Test tags for the VoiceConfirmationScreen. */
object VoiceConfirmationScreenTestTags {
  const val SCREEN = "voiceConfirmationScreen"
  const val TITLE = "voiceConfirmationTitle"
  const val INSTRUCTION = "voiceConfirmationInstruction"
  const val MIC_INDICATOR = "voiceConfirmationMicIndicator"
  const val RECOGNIZED_TEXT = "voiceConfirmationRecognizedText"
  const val CANCEL_BUTTON = "voiceConfirmationCancelButton"
  const val STATUS_TEXT = "voiceConfirmationStatusText"
}

/**
 * A full-screen composable that prompts the user for voice confirmation before executing emergency
 * actions (SMS, Call). The user can say "yes" to confirm or "no" to cancel.
 *
 * @param speechToTextService The service handling speech recognition.
 * @param onConfirmed Callback invoked when the user confirms with "yes".
 * @param onCancelled Callback invoked when the user cancels with "no" or manually.
 * @param actionDescription Description of the pending action (e.g., "send emergency SMS").
 * @param modifier Modifier for the composable.
 */
@Composable
fun VoiceConfirmationScreen(
    speechToTextService: SpeechToTextService,
    onConfirmed: () -> Unit,
    onCancelled: () -> Unit,
    actionDescription: String,
    modifier: Modifier = Modifier
) {
  val uiState by speechToTextService.uiState.collectAsState()
  val coroutineScope = rememberCoroutineScope()

  // Start listening when the screen is displayed
  LaunchedEffect(Unit) {
    coroutineScope.launch {
      val confirmed = speechToTextService.listenForConfirmation()
      if (confirmed) {
        onConfirmed()
      } else {
        onCancelled()
      }
    }
  }

  // Clean up when leaving the screen
  DisposableEffect(Unit) { onDispose { speechToTextService.destroy() } }

  Box(
      modifier =
          modifier
              .fillMaxSize()
              .background(MaterialTheme.colorScheme.errorContainer)
              .testTag(VoiceConfirmationScreenTestTags.SCREEN),
      contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
              // Emergency Icon/Title
              Text(
                  text = "⚠️",
                  fontSize = 64.sp,
                  modifier = Modifier.testTag(VoiceConfirmationScreenTestTags.TITLE))

              Spacer(modifier = Modifier.height(24.dp))

              Text(
                  text = stringResource(R.string.voice_confirmation_title),
                  style = MaterialTheme.typography.headlineMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                  textAlign = TextAlign.Center)

              Spacer(modifier = Modifier.height(16.dp))

              Text(
                  text = actionDescription,
                  style = MaterialTheme.typography.bodyLarge,
                  color = MaterialTheme.colorScheme.onErrorContainer,
                  textAlign = TextAlign.Center,
                  modifier = Modifier.testTag(VoiceConfirmationScreenTestTags.INSTRUCTION))

              Spacer(modifier = Modifier.height(32.dp))

              // Microphone indicator with pulsing animation
              MicrophoneIndicator(
                  isListening = uiState.isListening,
                  rmsLevel = uiState.rmsLevel,
                  modifier = Modifier.testTag(VoiceConfirmationScreenTestTags.MIC_INDICATOR))

              Spacer(modifier = Modifier.height(24.dp))

              // Status text
              Text(
                  text =
                      when {
                        uiState.isListening -> stringResource(R.string.voice_confirmation_listening)
                        uiState.errorMessage != null -> uiState.errorMessage!!
                        else -> stringResource(R.string.voice_confirmation_waiting)
                      },
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                  textAlign = TextAlign.Center,
                  modifier = Modifier.testTag(VoiceConfirmationScreenTestTags.STATUS_TEXT))

              Spacer(modifier = Modifier.height(8.dp))

              // Recognized text
              if (uiState.recognizedText != null) {
                Text(
                    text = "\"${uiState.recognizedText}\"",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag(VoiceConfirmationScreenTestTags.RECOGNIZED_TEXT))
              }

              Spacer(modifier = Modifier.height(16.dp))

              Text(
                  text = stringResource(R.string.voice_confirmation_instructions),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                  textAlign = TextAlign.Center)

              Spacer(modifier = Modifier.height(48.dp))

              // Manual cancel button
              Button(
                  onClick = {
                    speechToTextService.destroy()
                    onCancelled()
                  },
                  colors =
                      ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                  modifier =
                      Modifier.fillMaxWidth(0.6f)
                          .testTag(VoiceConfirmationScreenTestTags.CANCEL_BUTTON)) {
                    Text(
                        text = stringResource(R.string.voice_confirmation_cancel),
                        color = MaterialTheme.colorScheme.onError)
                  }
            }
      }
}

/**
 * Animated microphone indicator that pulses based on listening state and audio level.
 *
 * @param isListening Whether the speech recognizer is currently listening.
 * @param rmsLevel The current RMS audio level (0-10 typically).
 * @param modifier Modifier for the composable.
 */
@Composable
private fun MicrophoneIndicator(
    isListening: Boolean,
    rmsLevel: Float,
    modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "micPulse")

  val scale by
      infiniteTransition.animateFloat(
          initialValue = 1f,
          targetValue = if (isListening) 1.2f else 1f,
          animationSpec =
              infiniteRepeatable(
                  animation = tween(600, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
          label = "scale")

  // Calculate dynamic size based on RMS level
  val dynamicScale =
      if (isListening) {
        1f + (rmsLevel / 20f).coerceIn(0f, 0.3f)
      } else {
        1f
      }

  Box(
      modifier =
          modifier
              .size(120.dp)
              .scale(scale * dynamicScale)
              .background(
                  color = if (isListening) MaterialTheme.colorScheme.error else Color.Gray,
                  shape = CircleShape),
      contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Filled.Mic,
            contentDescription = "Microphone",
            modifier = Modifier.size(56.dp),
            tint = Color.White)
      }
}
