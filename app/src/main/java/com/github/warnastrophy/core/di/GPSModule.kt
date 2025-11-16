package com.github.warnastrophy.core.di

import com.github.warnastrophy.core.domain.model.GpsService
import com.github.warnastrophy.core.domain.model.PositionService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class GPSModule {

  /**
   * Binds the concrete GpsService implementation to the PositionService interface. This tells Hilt:
   * "When someone asks for PositionService, use GpsService."
   */
  @Singleton @Binds abstract fun bindPositionService(gpsService: GpsService): PositionService
}
