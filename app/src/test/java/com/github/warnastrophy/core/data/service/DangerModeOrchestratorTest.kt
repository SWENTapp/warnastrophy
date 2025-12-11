package com.github.warnastrophy.core.data.service

import com.github.warnastrophy.core.data.repository.MotionData
import com.github.warnastrophy.core.data.repository.MovementSensorRepository
import com.github.warnastrophy.core.data.repository.UserPreferences
import com.github.warnastrophy.core.data.repository.UserPreferencesRepository
import com.github.warnastrophy.core.domain.model.EmergencyMessage
import com.github.warnastrophy.core.model.Hazard
import com.github.warnastrophy.core.permissions.AppPermissions
import com.github.warnastrophy.core.permissions.PermissionManagerInterface
import com.github.warnastrophy.core.permissions.PermissionResult
import com.github.warnastrophy.core.ui.common.ErrorHandler
import com.github.warnastrophy.core.ui.common.ErrorType
import com.github.warnastrophy.core.ui.common.getScreenErrors
import com.github.warnastrophy.core.ui.navigation.Screen
import io.mockk.every
import io.mockk.mockk
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class DangerModeOrchestratorTest {

  private lateinit var orchestrator: DangerModeOrchestrator
  private lateinit var dangerModeService: DangerModeService
  private lateinit var movementService: MovementService
  private lateinit var hazardFlow: MutableStateFlow<Hazard?>
  private lateinit var mockSmsSender: MockSmsSender
  private lateinit var mockCallSender: MockCallSender
  private lateinit var errorHandler: ErrorHandler
  private lateinit var preferencesFlow: MutableStateFlow<UserPreferences>
  private lateinit var mockMovementRepository: MovementSensorRepository
  private lateinit var dataFlow: MutableSharedFlow<MotionData>
  private lateinit var testTimeSource: TestTimeSource

  private val fakePermissionManager =
      object : PermissionManagerInterface {
        override fun getPermissionResult(permissionType: AppPermissions) = PermissionResult.Granted

        override fun getPermissionResult(
            permissionType: AppPermissions,
            activity: android.app.Activity
        ) = PermissionResult.Granted

        override fun markPermissionsAsAsked(permissionType: AppPermissions) {}

        override fun isPermissionAskedBefore(permissionType: AppPermissions) = false
      }

  @Before
  fun setUp() {
    mockSmsSender = MockSmsSender()
    mockCallSender = MockCallSender()
    errorHandler = ErrorHandler()
    preferencesFlow = MutableStateFlow(UserPreferences.default())
    dataFlow = MutableSharedFlow(replay = 0, extraBufferCapacity = 50)
    mockMovementRepository = mockk(relaxed = true)
    every { mockMovementRepository.data } returns dataFlow
    testTimeSource = TestTimeSource()
  }

  private fun TestScope.initOrchestrator(emergencyPhoneNumber: String = "1234567890") {
    val dispatcher = StandardTestDispatcher(testScheduler)
    val scope = CoroutineScope(dispatcher)

    hazardFlow = MutableStateFlow(null)
    dangerModeService =
        DangerModeService(
            activeHazardFlow = hazardFlow,
            serviceScope = scope,
            permissionManager = fakePermissionManager)

    movementService =
        MovementService(
            repository = mockMovementRepository,
            timeSource = testTimeSource,
            dispatcher = dispatcher)
    val mockGpsService = MockGpsServiceImpl(errorHandler)
    val mockPreferencesRepo = MockUserPreferencesRepository(preferencesFlow)

    orchestrator =
        DangerModeOrchestrator(
            dangerModeService = dangerModeService,
            movementService = movementService,
            userPreferencesRepository = mockPreferencesRepo,
            gpsService = mockGpsService,
            errorHandler = errorHandler,
            smsSender = mockSmsSender,
            callSender = mockCallSender,
            emergencyPhoneNumber = emergencyPhoneNumber,
            dispatcher = dispatcher)
  }

  // ==================== Voice Confirmation Tests ====================

  @Test
  fun `setVoiceConfirmationEnabled updates internal state`() = runTest {
    initOrchestrator()
    orchestrator.setVoiceConfirmationEnabled(true)
  }

  @Test
  fun `debugTriggerVoiceConfirmation shows voice screen with SendSmsAndCall`() = runTest {
    initOrchestrator()
    orchestrator.debugTriggerVoiceConfirmation()
    advanceUntilIdle()

    assertTrue(orchestrator.showVoiceConfirmationScreen.value)
    assertTrue(orchestrator.state.value.isWaitingForConfirmation)
    assertTrue(orchestrator.state.value.pendingAction is PendingEmergencyAction.SendSmsAndCall)
  }

  // ==================== Confirmation Flow Tests ====================

  @Test
  fun `onConfirmation executes pending action and hides screen`() = runTest {
    initOrchestrator()
    orchestrator.debugTriggerVoiceConfirmation()
    advanceUntilIdle()

    assertTrue(orchestrator.showVoiceConfirmationScreen.value)
    assertFalse(mockSmsSender.smsSent)

    orchestrator.onConfirmation()
    advanceUntilIdle()

    assertFalse(orchestrator.showVoiceConfirmationScreen.value)
    assertTrue(mockSmsSender.smsSent)
    assertTrue(mockCallSender.callPlaced)
  }

  @Test
  fun `onCancellation hides screen and sets cancelled state`() = runTest {
    initOrchestrator()
    orchestrator.debugTriggerVoiceConfirmation()
    advanceUntilIdle()

    assertTrue(orchestrator.showVoiceConfirmationScreen.value)

    orchestrator.onCancellation()
    // Don't advance past the 5 second delay which resets state
    testScheduler.runCurrent()

    assertFalse(orchestrator.showVoiceConfirmationScreen.value)
    assertFalse(mockSmsSender.smsSent)
    val result = orchestrator.state.value.lastActionTaken
    assertTrue("Expected Cancelled but got $result", result is EmergencyActionResult.Cancelled)
  }

  @Test
  fun `onCancellation with reportAsError adds error to handler`() = runTest {
    initOrchestrator()
    orchestrator.debugTriggerVoiceConfirmation()
    advanceUntilIdle()

    orchestrator.onCancellation(reportAsError = true)
    advanceUntilIdle()

    val errors = errorHandler.state.value.getScreenErrors(Screen.Dashboard)
    assertTrue(errors.any { it.type == ErrorType.EMERGENCY_ACTION_CANCELLED })
  }

  // ==================== Emergency Phone Number Tests ====================

  @Test
  fun `getEmergencyPhoneNumber returns configured number`() = runTest {
    initOrchestrator(emergencyPhoneNumber = "1234567890")
    assertEquals("1234567890", orchestrator.getEmergencyPhoneNumber())
  }

  // ==================== State Tests ====================

  @Test
  fun `initial state has no pending action`() = runTest {
    initOrchestrator()
    assertNull(orchestrator.state.value.pendingAction)
    assertFalse(orchestrator.state.value.isWaitingForConfirmation)
    assertNull(orchestrator.state.value.lastActionTaken)
  }

  @Test
  fun `successful action sets success result after confirmation`() = runTest {
    initOrchestrator()
    orchestrator.debugTriggerVoiceConfirmation()
    advanceUntilIdle()

    orchestrator.onConfirmation()
    // Run the coroutine but don't advance past the 60 second reset delay
    testScheduler.runCurrent()

    val result = orchestrator.state.value.lastActionTaken
    assertTrue("Expected Success but got $result", result is EmergencyActionResult.Success)
    assertEquals("SMS and Call", (result as EmergencyActionResult.Success).actionType)
  }

  // ==================== Error Handling Tests ====================

  @Test
  fun `SMS failure sets failure result`() = runTest {
    mockSmsSender.shouldThrowException = true
    initOrchestrator()
    orchestrator.debugTriggerVoiceConfirmation()
    advanceUntilIdle()

    orchestrator.onConfirmation()
    // Run the coroutine but don't advance past the 60 second reset delay
    testScheduler.runCurrent()

    val result = orchestrator.state.value.lastActionTaken
    assertTrue("Expected Failure but got $result", result is EmergencyActionResult.Failure)
  }

  @Test
  fun `Call failure sets failure result`() = runTest {
    mockCallSender.shouldThrowException = true
    initOrchestrator()
    orchestrator.debugTriggerVoiceConfirmation()
    advanceUntilIdle()

    orchestrator.onConfirmation()
    // Run the coroutine but don't advance past the 60 second reset delay
    testScheduler.runCurrent()

    val result = orchestrator.state.value.lastActionTaken
    assertTrue("Expected Failure but got $result", result is EmergencyActionResult.Failure)
  }

  // ==================== Monitoring Tests ====================

  @Test
  fun `stopMonitoring resets state`() = runTest {
    initOrchestrator()
    orchestrator.debugTriggerVoiceConfirmation()
    advanceUntilIdle()

    assertTrue(orchestrator.showVoiceConfirmationScreen.value)

    orchestrator.stopMonitoring()

    assertFalse(orchestrator.showVoiceConfirmationScreen.value)
    assertNull(orchestrator.state.value.pendingAction)
  }

  @Test
  fun `voice confirmation screen shows when danger mode and movement service both detect danger`() =
      runTest {
        initOrchestrator()
        orchestrator.setVoiceConfirmationEnabled(true)

        // Set preferences to enable all required conditions
        preferencesFlow.value =
            UserPreferences.default()
                .copy(
                    dangerModePreferences =
                        com.github.warnastrophy.core.data.repository.DangerModePreferences(
                            alertMode = true,
                            inactivityDetection = true,
                            automaticSms = true,
                            automaticCalls = false))

        // Set hazard flow - this will auto-activate danger mode with the hazard
        hazardFlow.value =
            Hazard(id = 1, type = "EQ", description = "Test hazard", alertLevel = 2.0)
        testScheduler.advanceUntilIdle()

        // Start movement service and orchestrator
        movementService.startListening()
        orchestrator.startMonitoring()
        testScheduler.advanceUntilIdle()

        // Trigger danger state through motion data sequence:
        // 1. High acceleration to enter PreDanger
        dataFlow.emit(createMotionData(accelerationMagnitude = 100.0))
        testScheduler.advanceUntilIdle()

        // 2. Low acceleration to enter PreDangerAcc
        dataFlow.emit(createMotionData(accelerationMagnitude = 0.5))
        testScheduler.advanceUntilIdle()

        // 3. Continue with low acceleration and advance time past the timeout (10s)
        repeat(12) {
          dataFlow.emit(createMotionData(accelerationMagnitude = 0.5))
          testTimeSource += 1.seconds // Advance the time source
          testScheduler.advanceUntilIdle()
        }
        testScheduler.advanceUntilIdle()

        // Check if we reached Danger state - if not, the test should tell us what state we're in
        val currentMovementState = movementService.movementState.value
        val dangerModeState = dangerModeService.state.value

        // Debug output
        println("Movement state: ${currentMovementState::class.simpleName}")
        println("Danger mode active: ${dangerModeState.isActive}")
        println("Activating hazard: ${dangerModeState.activatingHazard}")
        println("Voice confirmation screen: ${orchestrator.showVoiceConfirmationScreen.value}")

        // The voice confirmation should be shown if all conditions are met
        assertTrue(
            "Voice confirmation screen should be shown",
            orchestrator.showVoiceConfirmationScreen.value)
      }

  private fun createMotionData(accelerationMagnitude: Double): MotionData =
      MotionData(
          timestamp = System.currentTimeMillis(),
          acceleration = org.locationtech.jts.math.Vector3D(0.0, 0.0, 0.0),
          rotation = org.locationtech.jts.math.Vector3D(0.0, 0.0, 0.0),
          accelerationMagnitude = accelerationMagnitude)

  // ==================== executeEmergencyAction Tests ====================

  @Test
  fun `executeEmergencyAction handles IllegalArgumentException`() = runTest {
    mockSmsSender.shouldThrowIllegalArgument = true
    initOrchestrator()
    orchestrator.debugTriggerVoiceConfirmation()
    advanceUntilIdle()
    orchestrator.onConfirmation()
    testScheduler.runCurrent()

    assertTrue(orchestrator.state.value.lastActionTaken is EmergencyActionResult.Failure)
    assertTrue(
        errorHandler.state.value.getScreenErrors(Screen.Dashboard).any {
          it.type == ErrorType.NO_EMERGENCY_CONTACT
        })
  }

  @Test
  fun `executeEmergencyAction clears errors on success`() = runTest {
    errorHandler.addErrorToScreen(ErrorType.EMERGENCY_SMS_FAILED, Screen.Dashboard)
    initOrchestrator()
    orchestrator.debugTriggerVoiceConfirmation()
    advanceUntilIdle()
    orchestrator.onConfirmation()
    testScheduler.runCurrent()

    assertFalse(
        errorHandler.state.value.getScreenErrors(Screen.Dashboard).any {
          it.type == ErrorType.EMERGENCY_SMS_FAILED
        })
  }

  @Test
  fun `executeEmergencyAction adds SMS error on failure`() = runTest {
    mockSmsSender.shouldThrowException = true
    initOrchestrator()
    orchestrator.debugTriggerVoiceConfirmation()
    advanceUntilIdle()
    orchestrator.onConfirmation()
    testScheduler.runCurrent()

    assertTrue(
        errorHandler.state.value.getScreenErrors(Screen.Dashboard).any {
          it.type == ErrorType.EMERGENCY_SMS_FAILED
        })
  }
}

// ==================== Mock Classes ====================

class MockSmsSender : SmsSender {
  var smsSent = false
  var lastPhoneNumber: String? = null
  var lastMessage: EmergencyMessage? = null
  var shouldThrowException = false
  var shouldThrowIllegalArgument = false

  override fun sendSms(phoneNumber: String, message: EmergencyMessage) {
    if (shouldThrowIllegalArgument) throw IllegalArgumentException("Invalid phone")
    if (shouldThrowException) throw RuntimeException("SMS sending failed")
    smsSent = true
    lastPhoneNumber = phoneNumber
    lastMessage = message
  }
}

class MockCallSender : CallSender {
  var callPlaced = false
  var lastPhoneNumber: String? = null
  var shouldThrowException = false

  override fun placeCall(phoneNumber: String) {
    if (shouldThrowException) {
      throw RuntimeException("Call failed")
    }
    callPlaced = true
    lastPhoneNumber = phoneNumber
  }
}

class MockUserPreferencesRepository(
    private val preferencesFlow: MutableStateFlow<UserPreferences>
) : UserPreferencesRepository {
  override val getUserPreferences: Flow<UserPreferences> = preferencesFlow

  override suspend fun setAlertMode(enabled: Boolean) {
    preferencesFlow.value =
        preferencesFlow.value.copy(
            dangerModePreferences =
                preferencesFlow.value.dangerModePreferences.copy(alertMode = enabled))
  }

  override suspend fun setInactivityDetection(enabled: Boolean) {
    preferencesFlow.value =
        preferencesFlow.value.copy(
            dangerModePreferences =
                preferencesFlow.value.dangerModePreferences.copy(inactivityDetection = enabled))
  }

  override suspend fun setAutomaticSms(enabled: Boolean) {
    preferencesFlow.value =
        preferencesFlow.value.copy(
            dangerModePreferences =
                preferencesFlow.value.dangerModePreferences.copy(automaticSms = enabled))
  }

  override suspend fun setAutomaticCalls(enabled: Boolean) {
    preferencesFlow.value =
        preferencesFlow.value.copy(
            dangerModePreferences =
                preferencesFlow.value.dangerModePreferences.copy(automaticCalls = enabled))
  }

  override suspend fun setDarkMode(isDark: Boolean) {}
}

// Simple mock for PositionService
class MockGpsServiceImpl(override val errorHandler: ErrorHandler) : PositionService {
  private val _positionState = MutableStateFlow(GpsPositionState())
  override val positionState: StateFlow<GpsPositionState> = _positionState
  override val locationClient: com.google.android.gms.location.FusedLocationProviderClient
    get() = throw NotImplementedError("Not needed for tests")

  override fun requestCurrentLocation() {}

  override fun startLocationUpdates() {}

  override fun stopLocationUpdates() {}

  override fun startForegroundLocationUpdates(
      service: android.app.Service,
      channelId: String,
      channelName: String,
      notificationId: Int
  ) {}
}
