package com.github.warnastrophy.core.ui.common

import androidx.lifecycle.ViewModel
import com.github.warnastrophy.core.util.ErrorDisplayManager
import kotlinx.coroutines.flow.StateFlow

// @HiltViewModel TODO: uncomment in next PR
class GlobalErrorViewModel(private val errorHandler: ErrorDisplayManager) : ViewModel() {
  val errorState: StateFlow<ErrorState> = errorHandler.errorState
}
