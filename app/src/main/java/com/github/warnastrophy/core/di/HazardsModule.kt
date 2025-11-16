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
