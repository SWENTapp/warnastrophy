package com.github.warnastrophy.core.di

import com.github.warnastrophy.core.data.repository.ContactsRepository
import com.github.warnastrophy.core.data.repository.MockContactRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * Hilt testing module for providing mock contact repository
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [ContactsModule::class])
object ContactModuleTest {
  @Singleton
  @Provides
  fun provideMockContactsRepository(): ContactsRepository {
    return MockContactRepository()
  }
}
