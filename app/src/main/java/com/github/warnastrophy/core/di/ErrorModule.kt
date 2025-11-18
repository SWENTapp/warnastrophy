package com.github.warnastrophy.core.di

import com.github.warnastrophy.core.domain.error.ErrorDisplayHandlerImpl
import com.github.warnastrophy.core.domain.error.ErrorDisplayManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ErrorModule {
  @Binds
  @Singleton
  abstract fun bindErrorManager(impl: ErrorDisplayHandlerImpl): ErrorDisplayManager
}
