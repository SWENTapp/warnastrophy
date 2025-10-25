package com.github.warnastrophy.core.model

data class Hazard(
    val id: Int?,
    val type: String?,
    val description: String?,
    val country: String?,
    val date: String?,
    val severity: Double?,
    val severityUnit: String?,
    val reportUrl: String?,
    val alertLevel: Int?,
    val coordinates: List<Location>?
)
