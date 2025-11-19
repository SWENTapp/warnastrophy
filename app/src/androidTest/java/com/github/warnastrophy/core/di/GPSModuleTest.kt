package com.github.warnastrophy.core.di

import com.github.warnastrophy.core.domain.model.PositionService
import com.github.warnastrophy.core.ui.map.GpsServiceMock
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Hilt testing module providing mock implementations for GPS service.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [GPSModule::class])
object GPSModuleTest {
  @Singleton
  @Provides
  fun provideMockGpsService(): PositionService {
    return GpsServiceMock()
  }
}
