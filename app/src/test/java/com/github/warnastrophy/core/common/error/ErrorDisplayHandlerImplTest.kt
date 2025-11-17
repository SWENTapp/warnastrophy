// This file is made by Dao Nguyen Ninh and Gemini assistance
package com.github.warnastrophy.core.common.error

import com.github.warnastrophy.core.domain.error.ErrorDisplayHandlerImpl
import com.github.warnastrophy.core.domain.error.ErrorDisplayManager
import com.github.warnastrophy.core.ui.common.Error
import com.github.warnastrophy.core.ui.common.ErrorState
import com.github.warnastrophy.core.ui.navigation.Screen
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ErrorDisplayHandlerImplTest {
  private lateinit var handler: ErrorDisplayManager

  @Before
  fun setup() {
    // Initialize a fresh handler before each test
    handler = ErrorDisplayHandlerImpl()
  }

  @After
  fun tearDown() {
    handler.clearError()
  }

  @Test
  fun `initial errorState should be empty`() = runTest {
    // Ensure the initial value of the StateFlow is an empty ErrorState
    assertEquals(ErrorState(), handler.errorState.value)
  }

  @Test
  fun `addError should correctly add a new error to the state`() = runTest {
    val errorMessage = "Network failed"
    val screen = Screen.Dashboard

    val collectedStates = mutableListOf<ErrorState>()
    val collectorJob: Job = launch { handler.errorState.collect { collectedStates.add(it) } }

    testScheduler.advanceUntilIdle()

    handler.addError(errorMessage, screen)

    testScheduler.advanceUntilIdle()

    assertEquals(2, collectedStates.size)

    val emittedState = collectedStates.last()
    val expectedError = Error(errorMessage, screen)

    assertEquals(1, emittedState.errors.size)
    assertEquals(expectedError, emittedState.errors.first())

    collectorJob.cancel()
  }

  @Test
  fun `clearError should reset the state to empty`() = runTest {
    val error1 = "Server Unreachable"
    val screen1 = Screen.Dashboard
    handler.addError(error1, screen1)

    assertTrue(
        "Pre-condition failed: State should contain errors before clearing.",
        handler.errorState.value.errors.isNotEmpty())

    val collectedStates = mutableListOf<ErrorState>()
    val collectorJob: Job = launch { handler.errorState.collect { collectedStates.add(it) } }

    testScheduler.advanceUntilIdle()

    handler.clearError()

    testScheduler.advanceUntilIdle()

    val expectedClearedState = ErrorState(errors = emptyList())

    assertEquals(
        "The final state after clearError() must be an empty ErrorState.",
        expectedClearedState,
        collectedStates.last())

    collectorJob.cancel()
  }

  @Test
  fun `addError should ignore blank messages`() = runTest {
    val initialValue = handler.errorState.value

    assertEquals(
        "Pre-condition failed: Initial state should be empty.", 0, initialValue.errors.size)

    handler.addError("", Screen.Dashboard)

    handler.addError("  ", Screen.ContactList)

    assertEquals(
        "The state should remain the initial (empty) value after adding blank messages.",
        initialValue,
        handler.errorState.value)

    assertEquals(
        "The errors list size should still be zero.", 0, handler.errorState.value.errors.size)
  }

  @Test
  fun `addError should prevent adding duplicate errors on the same screen`() = runTest {
    val message = "Session Expired"
    val screen = Screen.Profile

    handler.addError(message, screen)

    val initialErrorCount = handler.errorState.value.errors.size
    assertEquals(
        "Pre-condition failed: Initial add should result in 1 error.", 1, initialErrorCount)

    handler.addError(message, screen)

    val finalErrorCount = handler.errorState.value.errors.size

    assertEquals(
        "The error count must not increase when adding a duplicate error.",
        initialErrorCount,
        finalErrorCount)

    assertEquals(
        "The errors list should contain exactly one instance of the specific error.",
        1,
        handler.errorState.value.errors.count { it.message == message && it.screenType == screen })
  }

  @Test
  fun `addError should allow different errors on the same screen`() = runTest {
    val screen = Screen.Map

    val messageA = "GPS Signal Lost"
    val messageB = "Map Data Failed to Load"

    handler.addError(messageA, screen)

    handler.addError(messageB, screen)

    val errorsList = handler.errorState.value.errors

    assertEquals("The errors list should contain exactly two errors.", 2, errorsList.size)

    assertTrue(
        "Error A should be present in the list.",
        errorsList.any { it.message == messageA && it.screenType == screen })

    assertTrue(
        "Error B should be present in the list.",
        errorsList.any { it.message == messageB && it.screenType == screen })
  }

  @Test
  fun `addError should allow the same message on different screens`() = runTest {
    val message = "Database connection lost"

    val screenA = Screen.Dashboard
    val screenB = Screen.Map

    handler.addError(message, screenA)

    handler.addError(message, screenB)

    val errorsList = handler.errorState.value.errors

    assertEquals("The errors list should contain exactly two errors.", 2, errorsList.size)

    assertTrue(
        "Error associated with Screen A should be present.",
        errorsList.any { it.message == message && it.screenType == screenA })

    assertTrue(
        "Error associated with Screen B should be present.",
        errorsList.any { it.message == message && it.screenType == screenB })

    assertEquals(
        "The total count of the specific error message should be 2 (one per screen).",
        2,
        errorsList.count { it.message == message })
  }
}
