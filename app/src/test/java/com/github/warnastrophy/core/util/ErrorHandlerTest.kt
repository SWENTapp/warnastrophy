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
    handler.addError(ErrorType.LOCATION_ERROR, Screen.Map)
    assertEquals(1, handler.state.value.errors.size)

    // adding same error for same screen again should not create a duplicate
    handler.addError(ErrorType.LOCATION_ERROR, Screen.Map)
    assertEquals(1, handler.state.value.errors.size)
  }

  @Test
  fun clearError_removesOnlyMatchingTypeAndScreen() {
    val handler = ErrorHandler()
    handler.addError(ErrorType.LOCATION_ERROR, Screen.Map)
    handler.addError(ErrorType.LOCATION_ERROR, Screen.Dashboard)
    handler.addError(ErrorType.FOREGROUND_ERROR, Screen.Map)
    // clear LOCATION_ERROR for Screen.Map only
    handler.clearError(ErrorType.LOCATION_ERROR, Screen.Map)

    val remaining = handler.state.value.errors
    // should still contain LOCATION_ERROR for Dashboard and FOREGROUND_ERROR for Map
    assertTrue(
        remaining.any { it.type == ErrorType.LOCATION_ERROR && it.screenType == Screen.Dashboard })
    assertTrue(
        remaining.any { it.type == ErrorType.FOREGROUND_ERROR && it.screenType == Screen.Map })
    // should not contain LOCATION_ERROR for Map
    assertTrue(
        remaining.none { it.type == ErrorType.LOCATION_ERROR && it.screenType == Screen.Map })
  }

  @Test
  fun clearScreenErrors_removesAllForScreen() {
    val handler = ErrorHandler()
    handler.addError(ErrorType.LOCATION_ERROR, Screen.Map)
    handler.addError(ErrorType.FOREGROUND_ERROR, Screen.Map)
    handler.addError(ErrorType.HAZARD_FETCHING_ERROR, Screen.Dashboard)

    handler.clearScreenErrors(Screen.Map)

    val remaining = handler.state.value.errors
    assertEquals(1, remaining.size)
    assertEquals(Screen.Dashboard, remaining.first().screenType)
  }

  @Test
  fun clearAll_resetsState() {
    val handler = ErrorHandler()
    handler.addError(ErrorType.LOCATION_ERROR, Screen.Map)
    handler.clearAll()
    assertEquals(0, handler.state.value.errors.size)
  }

  @Test
  fun getScreenErrors_returnsConcatenatedMessages() {
    val handler = ErrorHandler()
    handler.addError(ErrorType.LOCATION_ERROR, Screen.Map)
    handler.addError(ErrorType.LOCATION_UPDATE_ERROR, Screen.Map)

    val text = handler.state.value.getScreenErrors(Screen.Map)
    // messages should be joined with newline and contain both messages
    assertTrue(text.contains(ErrorType.LOCATION_ERROR.message))
    assertTrue(text.contains(ErrorType.LOCATION_UPDATE_ERROR.message))
    // ensure newline separator present
    assertTrue(text.contains("\n"))
  }
}
