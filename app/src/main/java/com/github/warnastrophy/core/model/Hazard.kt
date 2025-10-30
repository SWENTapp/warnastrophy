package com.github.warnastrophy.core.model

data class Hazard(
    val id: Int?,
    val type: String?,
    val description: String?,
    val country: String?,
    val date: String?,
    val bbox: List<Double>? = null,
    val severity: Double?, // Maybe no need because we have already alertLevel
    val severityUnit: String?,
    val reportUrl: String?,
    val alertLevel: Int?,
    val coordinates: List<Location>?,
    val affectedZoneWkt: String? = null
)
