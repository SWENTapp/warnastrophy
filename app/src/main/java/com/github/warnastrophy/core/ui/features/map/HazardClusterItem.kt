package com.github.warnastrophy.core.ui.features.map

import com.github.warnastrophy.core.model.Hazard
import com.github.warnastrophy.core.model.Location
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

/**
 * A wrapper class that makes [Hazard] objects compatible with Google Maps clustering.
 *
 * This class implements [ClusterItem] to enable automatic clustering of hazard markers
 * when they are close together on the map at lower zoom levels.
 *
 * @property hazard The underlying hazard data.
 */
data class HazardClusterItem(
    val hazard: Hazard
) : ClusterItem {

    private val position: LatLng = hazard.centroid?.centroid?.let { point ->
        Location.toLatLng(Location(point.y, point.x))
    } ?: LatLng(0.0, 0.0)

    override fun getPosition(): LatLng = position

    override fun getTitle(): String? = hazard.description

    override fun getSnippet(): String? = formatSeveritySnippet(hazard)

    override fun getZIndex(): Float? = null
}

