package com.github.warnastrophy.core.di

import com.github.warnastrophy.core.permissions.PermissionManagerInterface
import com.github.warnastrophy.core.ui.map.MockPermissionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/** Hilt testing module providing a mock implementation of the permission management logic. */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [PermissionModule::class])
object PermissionModuleTest {

  @Provides
  @Singleton
  fun providePermissionManagerTest(): PermissionManagerInterface {
    return MockPermissionManager()
  }
}
