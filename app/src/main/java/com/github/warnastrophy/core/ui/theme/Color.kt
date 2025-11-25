package com.github.warnastrophy.core.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// Material Design 3 - Base Colors
// ============================================================================
/** Base colors based on Material Design 3 for general theme customization. */
val Purple80 = Color(0xFFD0BCFF) // Light purple
val PurpleGrey80 = Color(0xFFCCC2DC) // Muted purple-grey
val Pink80 = Color(0xFFEFB8C8) // Soft pink

val Purple40 = Color(0xFF6650a4) // Darker purple
val PurpleGrey40 = Color(0xFF625b71) // Dark purple-grey
val Pink40 = Color(0xFF7D5260) // Muted dark pink

// ============================================================================
// Light Theme Colors
// ============================================================================
/** Colors used in the light theme, which include background, text, and accent colors. */

// Danger Level Colors (Light)
val DangerLevelGreenLight = Color(0xFF4CAF50) // Green for light mode
val DangerLevelYellowLight = Color(0xFFFFEB3B) // Yellow for light mode
val DangerLevelAmberLight = Color(0xFFFFC107) // Amber for light mode
val DangerLevelRedLight = Color(0xFFD32F2F) // Red for light mode

// Background Colors (Light)
val BackgroundGreyLight = Color(0xFFF5F5F5) // Light grey background
val BackgroundOffWhiteLight = Color(0xFFF6F4F4) // Off-white background
val BackgroundLightGreenLight = Color(0xFFC8E6C9) // Light green background
val BackgroundLightRedLight = Color(0xFFFFEBEE) // Light red background
val BackgroundLightBlueLight = Color(0xFFD3F4FF) // Light blue background
val BackgroundWhite = Color.White // White background

// Border & Shadow Colors (Light)
val BorderShadowGreyLight = Color(0xFFBDBDBD) // Light grey for borders and shadows

// Text Colors (Light)
val TextDarkRedLight = Color(0xFFDB2F2F) // Dark red text color
val TextDarkGreyLight = Color(0xFF616161) // Dark grey text color
val TextGreyLight = Color(0xFF9E9E9E) // Grey text color
val TextOrangeLight = Color(0xFF8A2301) // Orange text color
val TextBlack = Color.Black // Black text color

// Accent Colors (Light)
val AccentMapBlueLight = Color(0xFF1E88E5) // Blue accent color

// ============================================================================
// Dark Theme Colors
// ============================================================================
/**
 * Colors used in the dark theme, which include darker and more desaturated versions of the light
 * colors.
 */

// Danger Level Colors (Dark) - Darker and more desaturated versions
val DangerLevelGreenDark = Color(0xFF388E3C) // Darker green for dark mode
val DangerLevelYellowDark = Color(0xFFFBC02D) // Darker yellow for dark mode
val DangerLevelAmberDark = Color(0xFFFF8F00) // Darker amber for dark mode
val DangerLevelRedDark = Color(0xFFB71C1C) // Darker red for dark mode

// Background Colors (Dark)
val BackgroundGreyDark = Color(0xFF1E1E1E) // Dark grey background
val BackgroundOffWhiteDark = Color(0xFF2C2C2C) // Dark off-white background
val BackgroundLightGreyDark = Color(0xFF303030) // Light grey background for dark mode
val BackgroundLightGreenDark = Color(0xFF1B3A1F) // Dark green background for dark mode
val BackgroundLightRedDark = Color(0xFF3A1E1E) // Dark red background for dark mode
val BackgroundLightBlueDark = Color(0xFF1A2B35) // Dark blue background for dark mode
val BackgroundBlack = Color(0xFF121212) // Black background for dark mode

// Border & Shadow Colors (Dark)
val BorderShadowGreyDark = Color(0xFF424242) // Dark grey for borders and shadows

// Text Colors (Dark)
val TextDarkRedDark = Color(0xFFEF5350) // Dark red text color for dark mode
val TextDarkGreyDark = Color(0xFFBDBDBD) // Dark grey text color for dark mode
val TextGreyDark = Color(0xFF9E9E9E) // Grey text color for dark mode
val TextOrangeDark = Color(0xFFFF8A65) // Orange text color for dark mode
val TextWhite = Color.White // White text color for dark mode

// Accent Colors (Dark)
val AccentMapBlueDark = Color(0xFF42A5F5) // Blue accent color for dark mode

// ============================================================================
// Semantic Color Sets (to ease usage)
// ============================================================================

/** Data classes to group related colors together. */

/** Danger level color set, with green, yellow, amber, and red colors. */
data class DangerLevelColors(val green: Color, val yellow: Color, val amber: Color, val red: Color)

/** Color set for news cards, including colors for the border, header background, text, and body. */
data class NewsCardColors(
    val border: Color,
    val headerBackground: Color,
    val headerText: Color,
    val bodyBackground: Color,
    val weatherText: Color,
    val imageText: Color,
    val readArticleText: Color
)

/** Color set for map preview, including background and map marker colors. */
data class MapPreviewColors(val background: Color, val mapMarker: Color)

// ============================================================================
// Color Sets for Light Theme
// ============================================================================
/** Color sets used in the light theme. */
val DangerLevelColorsLight =
    DangerLevelColors(
        green = DangerLevelGreenLight,
        yellow = DangerLevelYellowLight,
        amber = DangerLevelAmberLight,
        red = DangerLevelRedLight)

val NewsCardColorsLight =
    NewsCardColors(
        border = BorderShadowGreyLight,
        headerBackground = BackgroundLightRedLight,
        headerText = TextDarkRedLight,
        bodyBackground = BackgroundOffWhiteLight,
        weatherText = TextDarkGreyLight,
        imageText = TextGreyLight,
        readArticleText = TextOrangeLight)

val MapPreviewColorsLight =
    MapPreviewColors(background = BackgroundWhite, mapMarker = AccentMapBlueLight)

// ============================================================================
// Color Sets for Dark Theme
// ============================================================================
/** Color sets used in the dark theme. */
val DangerLevelColorsDark =
    DangerLevelColors(
        green = DangerLevelGreenDark,
        yellow = DangerLevelYellowDark,
        amber = DangerLevelAmberDark,
        red = DangerLevelRedDark)

val NewsCardColorsDark =
    NewsCardColors(
        border = BorderShadowGreyDark,
        headerBackground = BackgroundLightRedDark,
        headerText = TextDarkRedDark,
        bodyBackground = BackgroundWhite,
        weatherText = TextDarkGreyDark,
        imageText = TextGreyDark,
        readArticleText = TextOrangeDark)

val MapPreviewColorsDark =
    MapPreviewColors(background = BackgroundLightGreyDark, mapMarker = AccentMapBlueDark)
