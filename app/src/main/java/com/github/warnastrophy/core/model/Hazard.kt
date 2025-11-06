package com.github.warnastrophy.core.model

data class Hazard(
    val id: Int? = null,
    val type: String? = null,
    val description: String? = null,
    val severityText: String? = null,
    val country: String? = null,
    val date: String? = null,
    val bbox: List<Double>? = null,
    val severity: Double? = null, // Maybe no need because we have already alertLevel
    val severityUnit: String? = null,
    val reportUrl: String? = null,
    val alertLevel: Int? = null,
    val coordinates: List<Location>? = null,
    val affectedZoneWkt: String? = null
)
