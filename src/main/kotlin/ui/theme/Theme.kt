package ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val md_theme_light_primary = Color(0xFF6750A4)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_secondary = Color(0xFF625B71)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_error = Color(0xFFB3261E)
val md_theme_light_background = Color(0xFFFFFBFE)
val md_theme_light_surface = Color(0xFFFFFBFE)

val md_theme_dark_primary = Color(0xFFD0BCFF)
val md_theme_dark_onPrimary = Color(0xFF381E72)
val md_theme_dark_secondary = Color(0xFFCCC2DC)
val md_theme_dark_onSecondary = Color(0xFF332D41)
val md_theme_dark_error = Color(0xFFF2B8B5)
val md_theme_dark_background = Color(0xFF1C1B1F)
val md_theme_dark_surface = Color(0xFF1C1B1F)

val EditorBackground = Color(0xFF2B2B2B)
val OutputBackground = Color(0xFF1E1E1E)
val CodeTextColor = Color(0xFFA9B7C6)
val KeywordColor = Color(0xFFCC7832)
val SuccessColor = Color(0xFF4CAF50)
val ErrorColor = Color(0xFFF44336)
val RunningColor = Color(0xFF2196F3)
val StringColor = Color(0xFF6A8759)
val CommentColor = Color(0xFF808080)

val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    error = md_theme_light_error,
    background = md_theme_light_background,
    surface = md_theme_light_surface
)

val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    error = md_theme_dark_error,
    background = md_theme_dark_background,
    surface = md_theme_dark_surface
)

