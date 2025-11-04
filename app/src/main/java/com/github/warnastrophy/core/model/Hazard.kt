package com.github.warnastrophy.core.model

import org.locationtech.jts.geom.Geometry

data class Hazard(
    val id: Int?,
    val type: String?,
    val description: String?,
    val country: String?,
    val date: String?,
    val bbox: List<Double>?,
    val severity: Double?, // Maybe no need because we have already alertLevel
    val severityUnit: String?,
    val articleUrl: String?,
    val alertLevel: Double?, // TODO: this should be an double
    val centroid: List<Location>?,
    val affectedZone: Geometry?,
)
