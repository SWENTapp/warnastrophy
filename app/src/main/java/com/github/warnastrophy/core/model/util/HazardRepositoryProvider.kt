package com.github.warnastrophy.core.model.util

/** Provides a singleton instance of [HazardsRepository] and manages location polygon settings. */
object HazardRepositoryProvider {
  private val _repository: HazardsRepository by lazy { HazardsRepository() }

  var repository: HazardsRepository = _repository

  const val USA_POLYGON: String =
      "POLYGON((-125.0 50.0,-125.0 24.0,-66.0 24.0,-66.0 50.0,-125.0 50.0))"

  const val BRAZIL_POLYGON: String =
      "POLYGON((-73.99 -33.75,-34.79 -33.75,-34.79 5.27,-73.99 5.27,-73.99 -33.75))"

  const val WORLD_POLYGON: String =
      "POLYGON((-180.0 90.0,-180.0 -90.0,180.0 -90.0,180.0 90.0,-180.0 90.0))"

  /** The polygon defining the area for which hazards are fetched. Default is the entire world. */
  var locationPolygon: String = BRAZIL_POLYGON
}
