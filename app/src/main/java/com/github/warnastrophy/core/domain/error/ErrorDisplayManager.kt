package com.github.warnastrophy.core.domain.error

import com.github.warnastrophy.core.ui.common.ErrorState
import kotlinx.coroutines.flow.StateFlow

/**
 * An interface defining the contract for a component responsible for managing and exposing the
 * current application error state.
 *
 * It uses a [StateFlow] to allow collectors to react to changes in the error status.
 */
interface ErrorDisplayManager {
  val errorState: StateFlow<ErrorState>
}
