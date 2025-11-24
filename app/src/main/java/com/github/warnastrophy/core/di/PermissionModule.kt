package com.github.warnastrophy.core.di

import android.content.Context
import com.github.warnastrophy.core.data.permissions.PermissionManager
import com.github.warnastrophy.core.data.permissions.PermissionManagerInterface
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Hilt module that provides the binding for the application's permission management system. */
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
