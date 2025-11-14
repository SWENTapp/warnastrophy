package com.github.warnastrophy.core.model

import org.locationtech.jts.geom.Geometry

/**
 * Represents a single geographical hazard event retrieved from the API, serving as the core domain
 * model for the application.
 * * All fields are nullable as the GeoJSON data source may omit specific properties.
 *
 * @constructor Creates a new hazard data model instance.
 * @property id The unique identifier for the hazard event (e.g., eventid).
 * @property type The classification of the hazard (e.g., "EQ", "DR").
 * @property description A brief narrative summary of the hazard event.
 * @property country The country where the hazard is located.
 * @property date The date of the event.
 * @property bbox The bounding box defined as [minLon, minLat, maxLon, maxLat].
 * @property severity The raw magnitude or intensity value of the hazard.
 * @property severityUnit The unit associated with the severity (e.g., "M" for Magnitude).
 * @property articleUrl URL pointing to an external report or article for more details.
 * @property alertLevel A standardized score used for UI prioritization.
 * @property centroid The central point of the hazard.
 * @property affectedZone The affected zone of the hazard.
 */
data class Hazard(
    val id: Int? = null,
    val type: String? = null,
    val description: String? = null,
    val country: String? = null,
    val date: String? = null,
    val bbox: List<Double>? = null,
    val severity: Double? = null, // Maybe no need because we have already alertLevel
    val severityUnit: String? = null,
    val severityText: String? = null,
    val articleUrl: String? = null,
    val alertLevel: Double? = null,
    val centroid: Geometry? = null,
    val affectedZone: Geometry? = null,
)
