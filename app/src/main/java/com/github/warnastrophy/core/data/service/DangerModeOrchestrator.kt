package com.github.warnastrophy.core.data.service

import android.content.Context
import android.util.Log
import com.github.warnastrophy.core.data.interfaces.ContactsRepository
import com.github.warnastrophy.core.data.interfaces.UserPreferencesRepository
import com.github.warnastrophy.core.data.provider.ContactRepositoryProvider
import com.github.warnastrophy.core.data.repository.DangerModePreferences
import com.github.warnastrophy.core.domain.model.EmergencyMessage
import com.github.warnastrophy.core.model.Location
import com.github.warnastrophy.core.ui.common.ErrorHandler
import com.github.warnastrophy.core.ui.common.ErrorType
import com.github.warnastrophy.core.ui.navigation.Screen
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Represents the current state of the danger mode orchestrator.
 *
 * @property isWaitingForConfirmation Whether the orchestrator is waiting for user confirmation.
 * @property pendingAction The action that is pending user confirmation, if any.
 * @property confirmationTimeoutSeconds Time remaining for confirmation, if applicable.
 */
data class OrchestratorState(
    val isWaitingForConfirmation: Boolean = false,
    val pendingAction: PendingEmergencyAction? = null,
    val confirmationTimeoutSeconds: Int = 0,
    val lastActionTaken: EmergencyActionResult? = null
)

/** Represents an emergency action that requires user confirmation. */
sealed class PendingEmergencyAction {
  data class SendSms(val phoneNumber: String, val message: EmergencyMessage) :
      PendingEmergencyAction()

  data class MakeCall(val phoneNumber: String) : PendingEmergencyAction()

  data class SendSmsAndCall(val phoneNumber: String, val message: EmergencyMessage) :
      PendingEmergencyAction()
}

/** Result of an emergency action execution. */
sealed class EmergencyActionResult {
  data class Success(val actionType: String) : EmergencyActionResult()

  data class Failure(val actionType: String, val error: String) : EmergencyActionResult()

  object Cancelled : EmergencyActionResult()
}

/**
 * Orchestrates the danger mode functionality by coordinating between:
 * - DangerModeService (danger zone detection, manual activation)
 * - MovementService (inactivity detection)
 * - UserPreferencesRepository (user settings for SMS/Call actions)
 * - SpeechToTextService (voice confirmation)
 * - CallService and SmsService (executing emergency actions)
 *
 * When the user is in a danger zone with danger mode active and inactivity is detected, this
 * orchestrator will:
 * 1. Check user preferences for what actions to take
 * 2. If voice confirmation is required, prompt the user
 * 3. Execute the appropriate emergency actions (SMS, Call, or both)
 *
 * @param dangerModeService Service tracking danger mode state and active hazards.
 * @param movementService Service tracking user movement/inactivity.
 * @param userPreferencesRepository Repository for user preferences.
 * @param gpsService Service providing current GPS position.
 * @param smsSender Implementation for sending SMS messages.
 * @param callSender Implementation for placing phone calls.
 * @param emergencyPhoneNumber The phone number to contact in emergencies.
 * @param dispatcher Coroutine dispatcher for background operations.
 */
class DangerModeOrchestrator(
    private val dangerModeService: DangerModeService = StateManagerService.dangerModeService,
    private val movementService: MovementService = StateManagerService.movementService,
    private val userPreferencesRepository: UserPreferencesRepository =
        StateManagerService.userPreferencesRepository,
    private val gpsService: PositionService = StateManagerService.gpsService,
    private val errorHandler: ErrorHandler = StateManagerService.errorHandler,
    private val contactsRepository: ContactsRepository? = null,
    private val smsSender: SmsSender? = null,
    private val callSender: CallSender? = null,
    private var emergencyPhoneNumber: String = DEFAULT_EMERGENCY_NUMBER,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
  companion object {
    private const val TAG = "DangerModeOrchestrator"
    private const val DEFAULT_EMERGENCY_NUMBER = ""
    private const val CONFIRMATION_TIMEOUT_SECONDS = 30
  }

  private val scope = CoroutineScope(SupervisorJob() + dispatcher)
  private var monitoringJob: Job? = null
  private var confirmationJob: Job? = null

  private var contactsRepo: ContactsRepository? = contactsRepository

  private val _state = MutableStateFlow(OrchestratorState())
  val state: StateFlow<OrchestratorState> = _state.asStateFlow()

  private val _showVoiceConfirmationScreen = MutableStateFlow(false)
  val showVoiceConfirmationScreen: StateFlow<Boolean> = _showVoiceConfirmationScreen.asStateFlow()

  private var _voiceConfirmationEnabled = false

  /** Sets whether voice confirmation is required before executing emergency actions. */
  fun setVoiceConfirmationEnabled(enabled: Boolean) {
    _voiceConfirmationEnabled = enabled
  }

  private var smsSenderInstance: SmsSender? = smsSender
  private var callSenderInstance: CallSender? = callSender

  /**
   * Initializes the orchestrator with context-dependent services. Must be called before starting
   * monitoring if SMS/Call services weren't provided in constructor.
   *
   * @param context Application context for initializing services.
   */
  fun initialize(context: Context) {
    if (smsSenderInstance == null) {
      smsSenderInstance = SmsManagerSender(context)
    }

    // Initialize contacts repository if not provided
    if (contactsRepo == null) {
      try {
        contactsRepo = ContactRepositoryProvider.repository
      } catch (e: Exception) {
        Log.w(TAG, "ContactsRepository not initialized yet")
      }
    }

    // Fetch emergency phone number from first contact
    scope.launch {
      fetchEmergencyPhoneNumber()
      // Initialize call sender after we have the phone number
      if (callSenderInstance == null) {
        callSenderInstance = CallIntentCaller(context, emergencyPhoneNumber)
      }
    }
  }

  /**
   * Fetches the emergency phone number from the first contact in the repository. Falls back to
   * DEFAULT_EMERGENCY_NUMBER if no contacts are available.
   */
  private suspend fun fetchEmergencyPhoneNumber() {
    try {
      // Try to get contacts repository if not already set
      if (contactsRepo == null) {
        try {
          contactsRepo = ContactRepositoryProvider.repository
        } catch (e: Exception) {
          Log.w(TAG, "ContactsRepository not initialized yet")
          return
        }
      }

      val contacts = contactsRepo?.getAllContacts()?.getOrNull()
      if (!contacts.isNullOrEmpty()) {
        emergencyPhoneNumber = contacts.first().phoneNumber
      } else {
        Log.w(TAG, "No emergency contacts found, using default number")
      }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to fetch emergency contacts", e)
    }
  }

  /**
   * Starts monitoring for danger conditions.
   *
   * This function combines the danger mode state, movement state, and user preferences to determine
   * when emergency actions should be triggered.
   */
  fun startMonitoring() {
    if (monitoringJob?.isActive == true) return

    monitoringJob =
        scope.launch {
          combine(
                  dangerModeService.state,
                  movementService.movementState,
                  userPreferencesRepository.getUserPreferences) {
                      dangerState,
                      movementState,
                      preferences ->
                    Triple(dangerState, movementState, preferences.dangerModePreferences)
                  }
              .collectLatest { (dangerState, movementState, preferences) ->
                evaluateDangerConditions(dangerState, movementState, preferences)
              }
        }
  }

  /** Stops monitoring for danger conditions. */
  fun stopMonitoring() {
    monitoringJob?.cancel()
    monitoringJob = null
    confirmationJob?.cancel()
    confirmationJob = null
    resetState()
  }

  /** Evaluates the current conditions and determines if emergency actions should be triggered. */
  private suspend fun evaluateDangerConditions(
      dangerState: DangerModeService.DangerModeState,
      movementState: MovementState,
      preferences: DangerModePreferences
  ) {
    // Check if all conditions are met for triggering emergency actions
    val shouldTrigger =
        dangerState.isActive &&
            dangerState.activatingHazard != null &&
            preferences.alertMode &&
            preferences.inactivityDetection &&
            movementState is MovementState.Danger

    if (!shouldTrigger) {
      // If we were waiting for confirmation but conditions changed, reset
      if (_state.value.isWaitingForConfirmation) {
        resetState()
      }
      return
    }

    // Don't trigger again if we're already waiting or have recently taken action
    if (_state.value.isWaitingForConfirmation || _state.value.lastActionTaken != null) {
      return
    }

    triggerEmergencyProtocol(preferences)
  }

  /** Triggers the emergency protocol based on user preferences. */
  private suspend fun triggerEmergencyProtocol(preferences: DangerModePreferences) {
    // Re-fetch emergency phone number in case it wasn't available during init
    if (emergencyPhoneNumber.isBlank()) {
      fetchEmergencyPhoneNumber()
    }

    // Check if we have a valid phone number
    if (emergencyPhoneNumber.isBlank()) {
      Log.w(TAG, "No emergency phone number configured - cannot trigger emergency protocol")
      errorHandler.addErrorToScreen(ErrorType.NO_EMERGENCY_CONTACT, Screen.Dashboard)
      return
    }

    val currentLocation = gpsService.positionState.value.position
    val emergencyMessage =
        EmergencyMessage(location = Location(currentLocation.latitude, currentLocation.longitude))

    val pendingAction =
        when {
          preferences.automaticSms && preferences.automaticCalls -> {
            PendingEmergencyAction.SendSmsAndCall(emergencyPhoneNumber, emergencyMessage)
          }
          preferences.automaticSms -> {
            PendingEmergencyAction.SendSms(emergencyPhoneNumber, emergencyMessage)
          }
          preferences.automaticCalls -> {
            PendingEmergencyAction.MakeCall(emergencyPhoneNumber)
          }
          else -> null
        }

    if (pendingAction == null) {
      return
    }

    _state.value =
        OrchestratorState(
            isWaitingForConfirmation = true,
            pendingAction = pendingAction,
            confirmationTimeoutSeconds = CONFIRMATION_TIMEOUT_SECONDS)

    // Check if voice confirmation is enabled
    if (_voiceConfirmationEnabled) {
      // Show voice confirmation screen
      _showVoiceConfirmationScreen.value = true
    } else {
      // Execute action directly without voice confirmation
      scope.launch { executeEmergencyAction(pendingAction) }
    }
  }

  /** Called when the user confirms the emergency action via voice ("yes"). */
  fun onConfirmation() {
    val pendingAction = _state.value.pendingAction ?: return

    _showVoiceConfirmationScreen.value = false

    scope.launch { executeEmergencyAction(pendingAction) }
  }

  /**
   * Called when the user cancels the emergency action via voice ("no") or timeout.
   *
   * @param reportAsError If true, reports the cancellation to the ErrorHandler.
   */
  fun onCancellation(reportAsError: Boolean = false) {
    _showVoiceConfirmationScreen.value = false
    _state.value = OrchestratorState(lastActionTaken = EmergencyActionResult.Cancelled)

    if (reportAsError) {
      errorHandler.addErrorToScreen(ErrorType.EMERGENCY_ACTION_CANCELLED, Screen.Dashboard)
    }

    // Reset after a delay to allow re-triggering if conditions persist
    scope.launch {
      kotlinx.coroutines.delay(5000)
      resetState()
    }
  }

  /** Executes the emergency action (SMS, Call, or both). */
  private fun executeEmergencyAction(action: PendingEmergencyAction) {
    try {
      when (action) {
        is PendingEmergencyAction.SendSms -> {
          smsSenderInstance?.sendSms(action.phoneNumber, action.message)
          _state.value = OrchestratorState(lastActionTaken = EmergencyActionResult.Success("SMS"))
          // Clear any previous SMS errors
          errorHandler.clearErrorFromScreen(ErrorType.EMERGENCY_SMS_FAILED, Screen.Dashboard)
          errorHandler.clearErrorFromScreen(ErrorType.NO_EMERGENCY_CONTACT, Screen.Dashboard)
        }
        is PendingEmergencyAction.MakeCall -> {
          callSenderInstance?.placeCall(action.phoneNumber)
          _state.value = OrchestratorState(lastActionTaken = EmergencyActionResult.Success("Call"))
          // Clear any previous call errors
          errorHandler.clearErrorFromScreen(ErrorType.EMERGENCY_CALL_FAILED, Screen.Dashboard)
          errorHandler.clearErrorFromScreen(ErrorType.NO_EMERGENCY_CONTACT, Screen.Dashboard)
        }
        is PendingEmergencyAction.SendSmsAndCall -> {
          smsSenderInstance?.sendSms(action.phoneNumber, action.message)
          callSenderInstance?.placeCall(action.phoneNumber)
          _state.value =
              OrchestratorState(lastActionTaken = EmergencyActionResult.Success("SMS and Call"))
          // Clear any previous errors
          errorHandler.clearErrorFromScreen(ErrorType.EMERGENCY_SMS_FAILED, Screen.Dashboard)
          errorHandler.clearErrorFromScreen(ErrorType.EMERGENCY_CALL_FAILED, Screen.Dashboard)
          errorHandler.clearErrorFromScreen(ErrorType.NO_EMERGENCY_CONTACT, Screen.Dashboard)
        }
      }
    } catch (e: IllegalArgumentException) {
      // Invalid phone number - likely no emergency contact configured
      Log.e(TAG, "Invalid phone number for emergency action", e)
      errorHandler.addErrorToScreen(ErrorType.NO_EMERGENCY_CONTACT, Screen.Dashboard)
      _state.value =
          OrchestratorState(
              lastActionTaken =
                  EmergencyActionResult.Failure(
                      action::class.simpleName ?: "Unknown", e.message ?: "Invalid phone number"))
    } catch (e: Exception) {
      Log.e(TAG, "Failed to execute emergency action", e)

      // Report error to ErrorHandler based on action type
      val errorType =
          when (action) {
            is PendingEmergencyAction.SendSms -> ErrorType.EMERGENCY_SMS_FAILED
            is PendingEmergencyAction.MakeCall -> ErrorType.EMERGENCY_CALL_FAILED
            is PendingEmergencyAction.SendSmsAndCall ->
                ErrorType.EMERGENCY_SMS_FAILED // Report SMS as primary
          }
      errorHandler.addErrorToScreen(errorType, Screen.Dashboard)

      _state.value =
          OrchestratorState(
              lastActionTaken =
                  EmergencyActionResult.Failure(
                      action::class.simpleName ?: "Unknown", e.message ?: "Unknown error"))
    }

    // Reset after a delay to allow re-triggering if conditions persist
    scope.launch {
      kotlinx.coroutines.delay(60000) // 1 minute cooldown
      resetState()
    }
  }

  /** Resets the orchestrator state. */
  private fun resetState() {
    _state.value = OrchestratorState()
    _showVoiceConfirmationScreen.value = false
  }

  /** Gets the current emergency phone number. This could be fetched from contacts in the future. */
  fun getEmergencyPhoneNumber(): String {
    // TODO: Fetch from emergency contacts repository
    return emergencyPhoneNumber
  }

  /** DEBUG ONLY: Manually trigger the voice confirmation screen for testing purposes. */
  fun debugTriggerVoiceConfirmation() {
    val currentLocation = gpsService.positionState.value.position
    val emergencyMessage =
        EmergencyMessage(location = Location(currentLocation.latitude, currentLocation.longitude))

    _state.value =
        OrchestratorState(
            isWaitingForConfirmation = true,
            pendingAction =
                PendingEmergencyAction.SendSmsAndCall(emergencyPhoneNumber, emergencyMessage),
            confirmationTimeoutSeconds = CONFIRMATION_TIMEOUT_SECONDS)
    _showVoiceConfirmationScreen.value = true
  }
}
