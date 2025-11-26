package com.github.warnastrophy.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ============================================================================
// Extended Color Scheme (Custom colors in addition with the one provided by Material Design 3)
// ============================================================================

/**
 * Custom extended color scheme containing additional color sets for the application.
 *
 * This scheme is used to manage custom color sets such as danger levels, news card colors, and map
 * preview colors, as well as general background colors.
 */
data class ExtendedColorScheme(
    val dangerLevels: DangerLevelColors, // Danger level colors (green, yellow, amber, red)
    val newsCard: NewsCardColors, // Colors used in news card components
    val mapPreview: MapPreviewColors, // Colors for map preview
    val backgroundGrey: Color, // Grey background color for light and dark themes
    val backgroundOffWhite: Color, // Off-white background color for light and dark themes
    val backgroundLightGreen: Color, // Light green background color for light and dark themes
    val backgroundLightBlue: Color // Light blue background color for light and dark themes
)

/** Provides the current extended color scheme. */
val LocalExtendedColorScheme = staticCompositionLocalOf {
  ExtendedColorScheme(
      dangerLevels = DangerLevelColorsLight,
      newsCard = NewsCardColorsLight,
      mapPreview = MapPreviewColorsLight,
      backgroundGrey = BackgroundGreyLight,
      backgroundOffWhite = BackgroundOffWhiteLight,
      backgroundLightGreen = BackgroundLightGreenLight,
      backgroundLightBlue = BackgroundLightBlueLight)
}

/** Extension property to access the extended color scheme in the [MaterialTheme]. */
val MaterialTheme.extendedColors: ExtendedColorScheme
  @Composable get() = LocalExtendedColorScheme.current

// ============================================================================
// Color Schemes
// ============================================================================

/** Default dark theme color scheme. Uses darker and more desaturated colors for the dark mode. */
private val DarkColorScheme =
    darkColorScheme(
        primary = Purple80,
        secondary = PurpleGrey80,
        tertiary = Pink80,
        background = BackgroundBlack,
        surface = BackgroundGreyDark,
        error = DangerLevelRedDark,
        errorContainer = BackgroundLightRedLight,
        onError = TextWhite,
        onErrorContainer = TextDarkRedDark,
        secondaryContainer = BackgroundLightBlueDark,
        onSecondaryContainer = TextWhite,
        onSurface = Color.White,
        onSurfaceVariant = Color.White)

/** Default light theme color scheme. Uses lighter and more vibrant colors for the light mode. */
private val LightColorScheme =
    lightColorScheme(
        primary = Purple40,
        secondary = PurpleGrey40,
        tertiary = Pink40,
        background = BackgroundWhite,
        surface = BackgroundWhite,
        error = DangerLevelRedDark,
        errorContainer = BackgroundLightRedLight,
        onError = TextWhite,
        onErrorContainer = TextDarkRedLight,
        secondaryContainer = BackgroundLightBlueLight,
        onSecondaryContainer = TextBlack,
        onSurface = Color.Black,
        onSurfaceVariant = Color.Black)

/**
 * Extended color scheme for the light theme. Includes additional custom colors used for specific
 * components like news cards and map previews.
 */
private val ExtendedLightColorScheme =
    ExtendedColorScheme(
        dangerLevels = DangerLevelColorsLight,
        newsCard = NewsCardColorsLight,
        mapPreview = MapPreviewColorsLight,
        backgroundGrey = BackgroundGreyLight,
        backgroundOffWhite = BackgroundOffWhiteLight,
        backgroundLightGreen = BackgroundLightGreenLight,
        backgroundLightBlue = BackgroundLightBlueLight)

/**
 * Extended color scheme for the dark theme. Includes additional custom colors used for specific
 * components like news cards and map previews.
 */
private val ExtendedDarkColorScheme =
    ExtendedColorScheme(
        dangerLevels = DangerLevelColorsDark,
        newsCard = NewsCardColorsDark,
        mapPreview = MapPreviewColorsDark,
        backgroundGrey = BackgroundGreyDark,
        backgroundOffWhite = BackgroundOffWhiteDark,
        backgroundLightGreen = BackgroundLightGreenDark,
        backgroundLightBlue = BackgroundLightBlueDark)

// ============================================================================
// Theme
// ============================================================================

/**
 * Composable function to provide the app's theme based on system settings (light/dark theme).
 *
 * This function sets up the color scheme dynamically, supporting Android 12+ dynamic theming, and
 * updates the status bar color based on the selected theme.
 *
 * @param darkTheme Boolean flag to indicate whether to apply the dark theme or not.
 * @param dynamicColor Boolean flag to enable dynamic color schemes (Android 12+).
 * @param content The composable content that will be displayed using the theme.
 */
@Composable
fun MainAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
  val colorScheme =
      when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
          val context = LocalContext.current
          if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
      }

  val extendedColorScheme =
      if (darkTheme) {
        ExtendedDarkColorScheme
      } else {
        ExtendedLightColorScheme
      }

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      window.statusBarColor = colorScheme.primary.toArgb()
      WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
    }
  }

  CompositionLocalProvider(LocalExtendedColorScheme provides extendedColorScheme) {
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}
