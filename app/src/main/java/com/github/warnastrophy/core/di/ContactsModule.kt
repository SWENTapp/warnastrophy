package com.github.warnastrophy.core.di

import android.content.Context
import com.github.warnastrophy.core.data.interfaces.ContactsRepository
import com.github.warnastrophy.core.data.localStorage.ContactsStorage
import com.github.warnastrophy.core.data.localStorage.contactDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Hilt module that provides the binding for the application's local contact data repository. */
@Module
@InstallIn(SingletonComponent::class)
object ContactsModule {
  @Singleton
  @Provides
  fun provideContactsRepository(@ApplicationContext context: Context): ContactsRepository {
    return ContactsStorage(context.contactDataStore)
  }
}
