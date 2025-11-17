package com.github.warnastrophy.core.ui.common

import androidx.lifecycle.ViewModel
import com.github.warnastrophy.core.domain.error.ErrorDisplayManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class GlobalErrorViewModel @Inject constructor(private val errorHandler: ErrorDisplayManager) :
    ViewModel() {
  val errorState: StateFlow<ErrorState> = errorHandler.errorState
}
