package com.github.warnastrophy.core.model

import com.github.warnastrophy.core.data.service.MovementConfig

/**
 * A data class representing an activity that user does when danger mode is on.
 *
 * @param id id of activity, created by
 *   [com.github.warnastrophy.core.data.repository.ActivityRepository]
 * @param activityName title of activity
 * @param movementConfig configuration related to movement detection for this activity
 */
data class Activity(
    val id: String,
    val activityName: String,
    val movementConfig: MovementConfig = MovementConfig()
)
