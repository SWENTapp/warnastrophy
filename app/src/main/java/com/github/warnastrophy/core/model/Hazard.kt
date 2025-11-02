package com.github.warnastrophy.core.model

import org.locationtech.jts.geom.Geometry

data class Hazard(
    val id: Int?,
    val type: String?,
    val description: String?,
    val country: String?,
    val date: String?,
    val bbox: List<Double>? = null,
    val severity: Double?, // Maybe no need because we have already alertLevel
    val severityUnit: String?,
    val articleUrl: String?,
    val alertLevel: Int?,
    val centroid: List<Location>?,
    val affectedZoneWkt: String? = null,
    val geometry: Geometry? = null,
)
