package com.github.warnastrophy.core.util

import com.github.warnastrophy.core.ui.common.ErrorHandler
import com.github.warnastrophy.core.ui.common.ErrorType
import com.github.warnastrophy.core.ui.common.getScreenErrors
import com.github.warnastrophy.core.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorHandlerTest {

  @Test
  fun addError_preventsDuplicates() {
    val handler = ErrorHandler()
    handler.addErrorToScreen(ErrorType.LOCATION_ERROR, Screen.Map)
    assertEquals(1, handler.state.value.errors.size)

    // adding same error for same screen again should not create a duplicate
    handler.addErrorToScreen(ErrorType.LOCATION_ERROR, Screen.Map)
    assertEquals(1, handler.state.value.errors.size)
  }

  @Test
  fun clearError_removesOnlyMatchingTypeAndScreen() {
    val handler = ErrorHandler()
    handler.addErrorToScreen(ErrorType.LOCATION_ERROR, Screen.Map)
    handler.addErrorToScreen(ErrorType.LOCATION_ERROR, Screen.Dashboard)
    handler.addErrorToScreen(ErrorType.FOREGROUND_ERROR, Screen.Map)
    // clear LOCATION_ERROR for Screen.Map only
    handler.clearErrorFromScreen(ErrorType.LOCATION_ERROR, Screen.Map)

    val remaining = handler.state.value.errors
    // should still contain LOCATION_ERROR for Dashboard and FOREGROUND_ERROR for Map
    assertTrue(
        remaining.any {
          it.type == ErrorType.LOCATION_ERROR && it.screenTypes.contains(Screen.Dashboard)
        })
    assertTrue(
        remaining.any {
          it.type == ErrorType.FOREGROUND_ERROR && it.screenTypes.contains(Screen.Map)
        })
    // should not contain LOCATION_ERROR for Map
    assertTrue(
        remaining.none {
          it.type == ErrorType.LOCATION_ERROR && it.screenTypes.contains(Screen.Map)
        })
  }

  @Test
  fun clearScreenErrors_removesAllForScreen() {
    val handler = ErrorHandler()
    handler.addErrorToScreen(ErrorType.LOCATION_ERROR, Screen.Map)
    handler.addErrorToScreen(ErrorType.FOREGROUND_ERROR, Screen.Map)
    handler.addErrorToScreen(ErrorType.HAZARD_FETCHING_ERROR, Screen.Dashboard)

    handler.clearScreenErrors(Screen.Map)

    val remaining = handler.state.value.errors
    assertEquals(1, remaining.size)
    assertTrue(remaining.first().screenTypes.contains(Screen.Dashboard))
  }

  @Test
  fun clearAll_resetsState() {
    val handler = ErrorHandler()
    handler.addErrorToScreen(ErrorType.LOCATION_ERROR, Screen.Map)
    handler.clearAll()
    assertEquals(0, handler.state.value.errors.size)
  }

  @Test
  fun getScreenErrors_returnsCorrectErrors() {
    val handler = ErrorHandler()
    handler.addErrorToScreen(ErrorType.LOCATION_ERROR, Screen.Map)
    handler.addErrorToScreen(ErrorType.LOCATION_UPDATE_ERROR, Screen.Map)
    handler.addErrorToScreen(ErrorType.FOREGROUND_ERROR, Screen.Dashboard)
    handler.addErrorToScreen(ErrorType.HAZARD_FETCHING_ERROR, Screen.Profile)

    val errors = handler.state.value.getScreenErrors(Screen.Map)
    // messages should be joined with newline and contain both messages
    assertEquals(2, errors.size)
    assertTrue(errors.any { it.type == ErrorType.LOCATION_ERROR })
    assertTrue(errors.any { it.type == ErrorType.LOCATION_UPDATE_ERROR })

    val dashboardErrors = handler.state.value.getScreenErrors(Screen.Dashboard)
    assertEquals(1, dashboardErrors.size)
    assertEquals(ErrorType.FOREGROUND_ERROR, dashboardErrors.first().type)

    val profileErrors = handler.state.value.getScreenErrors(Screen.Profile)
    assertEquals(1, profileErrors.size)
    assertEquals(ErrorType.HAZARD_FETCHING_ERROR, profileErrors.first().type)
  }
}
