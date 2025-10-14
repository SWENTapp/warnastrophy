package com.github.warnastrophy.core.model.util

object HazardRepositoryProvider {
  private val _repository: HazardsRepository by lazy { HazardsRepository() }

  var repository: HazardsRepository = _repository

  const val locationPolygon: String =
      "POLYGON((-124.848974 49.384358,-124.848974 24.396308,-66.93457 24.396308," +
          "-66.93457 49.384358,-124.848974 49.384358))"
}
