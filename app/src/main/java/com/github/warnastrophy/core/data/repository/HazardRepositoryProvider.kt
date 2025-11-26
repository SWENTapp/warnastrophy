package com.github.warnastrophy.core.data.repository

/** Provides a singleton instance of [HazardsRepository] and manages location polygon settings. */
object HazardRepositoryProvider {
  private var _repository: HazardsRepository? = null

  val repository: HazardsRepository
    get() {
      if (_repository == null) {
        _repository = HazardsRepository()
      }
      return _repository!!
    }

  const val USA_POLYGON: String =
      "POLYGON((-125.0 50.0,-125.0 24.0,-66.0 24.0,-66.0 50.0,-125.0 50.0))"

  const val WORLD_POLYGON: String =
      "POLYGON((-180.0 90.0,-180.0 -90.0,180.0 -90.0,180.0 90.0,-180.0 90.0))"

  /** The polygon defining the area for which hazards are fetched. Default is the entire world. */
  var locationPolygon: String = WORLD_POLYGON

  fun resetForTests() {
    _repository = null
  }
}
