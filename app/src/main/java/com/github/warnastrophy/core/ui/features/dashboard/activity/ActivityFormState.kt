package com.github.warnastrophy.core.ui.features.dashboard.activity

import com.github.warnastrophy.core.data.service.MovementConfig
import kotlin.time.Duration

/**
 * Represents the unified state of the UI for activity forms (add/edit). This state is shared
 * between AddActivity and EditActivity screens.
 *
 * @property activityName The current text input for the activity's name.
 * @property errorMsg A general error message to display, usually for repository/network failures.
 * @property invalidActivityName A specific error message for input validation failure on the
 *   activity name field.
 * @property preDangerThresholdStr String representation of the preDangerThreshold value.
 * @property preDangerTimeoutStr String representation of the preDangerTimeout value.
 * @property dangerAverageThresholdStr String representation of the dangerAverageThreshold value.
 */
data class ActivityFormState(
    val activityName: String = "",
    val errorMsg: String? = null,
    val invalidActivityName: String? = null,
    val preDangerThresholdStr: String = "50.0",
    val preDangerTimeoutStr: String = "10s",
    val dangerAverageThresholdStr: String = "1.0"
) {
  val preDangerThreshold: Double?
    get() = preDangerThresholdStr.toDoubleOrNull()?.takeIf { it >= 0 }

  val preDangerTimeout: Duration?
    get() =
        try {
          Duration.parse(preDangerTimeoutStr).takeIf { it >= Duration.ZERO }
        } catch (_: IllegalArgumentException) {
          null
        }

  val dangerAverageThreshold: Double?
    get() = dangerAverageThresholdStr.toDoubleOrNull()?.takeIf { it >= 0 }

  val isPreDangerThresholdError: Boolean
    get() = preDangerThreshold == null

  val isPreDangerTimeoutError: Boolean
    get() = preDangerTimeout == null

  val isDangerAverageThresholdError: Boolean
    get() = dangerAverageThreshold == null

  val isValid: Boolean
    get() =
        activityName.isNotBlank() &&
            preDangerThreshold != null &&
            dangerAverageThreshold != null &&
            preDangerTimeout != null

  fun toMovementConfig(): MovementConfig? {
    val threshold = preDangerThreshold ?: return null
    val timeout = preDangerTimeout ?: return null
    val avgThreshold = dangerAverageThreshold ?: return null

    return MovementConfig(
        preDangerThreshold = threshold,
        preDangerTimeout = timeout,
        dangerAverageThreshold = avgThreshold)
  }
}
