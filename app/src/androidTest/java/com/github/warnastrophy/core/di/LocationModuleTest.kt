package com.github.warnastrophy.core.di

import com.google.android.gms.location.FusedLocationProviderClient
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton
import org.mockito.Mockito

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [LocationModule::class] // only replace the client provider
    )
object LocationModuleTest {
  @Provides
  @Singleton
  fun provideFakeFusedLocationClient(): FusedLocationProviderClient = Mockito.mock()
}
