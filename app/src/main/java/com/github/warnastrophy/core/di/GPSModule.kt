package com.github.warnastrophy.core.di

import com.github.warnastrophy.core.data.service.GpsService
import com.github.warnastrophy.core.data.service.PositionService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Hilt module that provides the binding for the application's position tracking service. */
@Module
@InstallIn(SingletonComponent::class)
abstract class GPSModule {
  @Singleton @Binds abstract fun bindPositionService(gpsService: GpsService): PositionService
}
