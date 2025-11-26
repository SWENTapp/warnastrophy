package com.github.warnastrophy.core.domain.model

/**
 * A data class representing an activity that user does when danger mode is on.
 *
 * @param id id of activity, created by
 *   [com.github.warnastrophy.core.data.repository.ActivityRepository]
 * @param activityName title of activity
 */
data class Activity(val id: String, val activityName: String)
