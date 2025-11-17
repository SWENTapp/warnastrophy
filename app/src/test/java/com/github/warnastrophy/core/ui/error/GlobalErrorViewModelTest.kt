// This file is made by Dao Nguyen Ninh and Gemini assistance
package com.github.warnastrophy.core.ui.error

import com.github.warnastrophy.core.domain.error.Error
import com.github.warnastrophy.core.domain.error.ErrorDisplayManager
import com.github.warnastrophy.core.domain.error.ErrorState
import com.github.warnastrophy.core.ui.common.GlobalErrorViewModel
import com.github.warnastrophy.core.ui.navigation.Screen
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GlobalErrorViewModelTest {
  private lateinit var fakeErrorHandler: ErrorDisplayManager

  private lateinit var mockErrorStateFlow: MutableStateFlow<ErrorState>

  private lateinit var viewModel: GlobalErrorViewModel

  @Before
  fun setup() {
    mockErrorStateFlow = MutableStateFlow(ErrorState())
    fakeErrorHandler = mockk<ErrorDisplayManager>(relaxed = true)
    every { fakeErrorHandler.errorState } returns mockErrorStateFlow
    viewModel = GlobalErrorViewModel(fakeErrorHandler)
  }

  @Test
  fun `errorState should reflect errors added to the handler`() = runTest {
    assertEquals(ErrorState(), viewModel.errorState.value)

    val testError = Error("Access Denied", Screen.Profile)
    val newState = ErrorState(errors = listOf(testError))

    mockErrorStateFlow.value = newState

    assertEquals(newState, viewModel.errorState.value)

    mockErrorStateFlow.value = ErrorState()

    assertEquals(ErrorState(), viewModel.errorState.value)
  }
}
