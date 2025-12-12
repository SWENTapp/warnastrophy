package com.github.warnastrophy.core.data.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * Lightweight stub for emergency actions. Per user request this service does not automatically
 * attach to danger-mode flow; it's provided so callers can instantiate and use it explicitly.
 */
@Suppress("unused")
class EmergencyActionsService(
    private val smsSender: SmsSender,
    private val callSender: CallSender,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
  /** No-op: do not subscribe to StateManagerService flows automatically. */
  fun start() {
    // Intentionally empty: caller may start logic explicitly if desired.
  }
}
