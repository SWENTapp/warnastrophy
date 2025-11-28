package com.github.warnastrophy.core.ui.features.profile

import androidx.compose.runtime.compositionLocalOf

/**
 * A [CompositionLocal] that provides access to the [ThemeViewModel] instance within a composable
 * hierarchy.
 *
 * [LocalThemeViewModel] is used to allow composables to read the [ThemeViewModel] without needing
 * to pass it down explicitly through the composable hierarchy.
 *
 * @throws [IllegalStateException] if a composable attempts to access the [ThemeViewModel] without a
 *   parent providing it via [CompositionLocalProvider].
 */
val LocalThemeViewModel = compositionLocalOf<ThemeViewModel> { error("No ThemeViewModel provided") }
