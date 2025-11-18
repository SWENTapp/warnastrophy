package com.github.warnastrophy.core.di

import com.github.warnastrophy.core.data.repository.HazardsDataSource
import com.github.warnastrophy.core.data.repository.HazardsRepository
import com.github.warnastrophy.core.domain.model.HazardsDataService
import com.github.warnastrophy.core.domain.model.HazardsService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides the bindings for the application's hazard data and service layers. This
 * module ensures that components requiring hazard information receive Singleton instances of both
 * the data source (HazardsDataSource) and the domain service (HazardsDataService).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class HazardsModule {
  @Binds
  @Singleton
  abstract fun bindHazardsDataSource(repository: HazardsRepository): HazardsDataSource

  @Binds
  @Singleton
  abstract fun bindHazardDataService(hazardsService: HazardsService): HazardsDataService
}
