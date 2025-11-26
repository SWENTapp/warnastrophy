package com.github.warnastrophy.core.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.github.warnastrophy.core.util.AppConfig

/**
 * Extension property for [Context] to provide a singleton instance of
 * [androidx.datastore.core.DataStore] for user preferences. This DataStore is used to persist
 * simple key-value pairs, such as user settings, using the Jetpack DataStore library.
 *
 * The `preferencesDataStore` delegate ensures that there's only one instance of DataStore with the
 * name [AppConfig.PREF_FILE_NAME] per application process.
 *
 * Usage:
 * ```
 * context.userPrefsDataStore.edit { prefs ->
 *     prefs[USER_THEME_KEY] = "dark"
 * }
 * ```
 *
 * Note: This code was generated with the help of AI
 */
val Context.userPrefsDataStore by preferencesDataStore(name = AppConfig.PREF_FILE_NAME)
