package com.github.warnastrophy.core.di

import com.github.warnastrophy.core.data.repository.HazardsDataSource
import com.github.warnastrophy.core.domain.model.Hazard
import com.github.warnastrophy.core.domain.model.HazardsDataService
import com.github.warnastrophy.core.ui.map.HazardServiceMock
import com.github.warnastrophy.core.ui.map.HazardsRepositoryMock
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/** Hilt testing module providing mock implementations for hazard repository and hazard service.. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [HazardsModule::class])
object HazardsModuleTest {
  private val hazards =
      listOf(
          Hazard(
              id = 1,
              type = null,
              description = null,
              country = null,
              date = null,
              severity = null,
              severityUnit = null,
              articleUrl = null,
              alertLevel = null,
              centroid = null,
              bbox = null,
              affectedZone = null),
          Hazard(
              id = 2,
              type = null,
              description = null,
              country = null,
              date = null,
              severity = null,
              severityUnit = null,
              articleUrl = null,
              bbox = null,
              alertLevel = null,
              affectedZone = null,
              centroid = null))

  @Singleton
  @Provides
  fun provideHazardsRepository(): HazardsDataSource {
    return HazardsRepositoryMock(hazards)
  }

  @Singleton
  @Provides
  fun provideHazardsService(): HazardsDataService {
    return HazardServiceMock()
  }
}
