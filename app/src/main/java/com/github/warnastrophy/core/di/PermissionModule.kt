package com.github.warnastrophy.core.di

import android.content.Context
import com.github.warnastrophy.core.permissions.PermissionManager
import com.github.warnastrophy.core.permissions.PermissionManagerInterface
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PermissionModule {
  @Provides
  @Singleton
  fun providePermissionManagerInterface(
      @ApplicationContext context: Context
  ): PermissionManagerInterface {
    return PermissionManager(context)
  }
}
