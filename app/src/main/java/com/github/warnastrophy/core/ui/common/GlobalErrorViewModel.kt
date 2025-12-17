package com.github.warnastrophy.core.ui.common

import androidx.lifecycle.ViewModel
import com.github.warnastrophy.core.util.ErrorDisplayManager
import kotlinx.coroutines.flow.StateFlow

class GlobalErrorViewModel(errorHandler: ErrorDisplayManager) : ViewModel() {
  val errorState: StateFlow<ErrorState> = errorHandler.errorState
}
