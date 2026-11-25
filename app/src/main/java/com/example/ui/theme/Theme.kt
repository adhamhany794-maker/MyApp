package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat
import com.example.models.AppLanguage
import com.example.models.ThemeMode

private val LightColorScheme = lightColorScheme(
    primary = BrandOrange,
    onPrimary = Color.White,
    primaryContainer = BrandOrangeContainerLight,
    onPrimaryContainer = OnBrandOrangeContainerLight,
    secondary = EmeraldAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3F7EA),
    onSecondaryContainer = Color(0xFF0A4723),
    tertiary = Color(0xFF3B82F6),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE8F1FF),
    onTertiaryContainer = Color(0xFF1E3A8A),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = StatusCrimson,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandOrange,
    onPrimary = Color.White,
    primaryContainer = BrandOrangeContainerDark,
    onPrimaryContainer = OnBrandOrangeContainerDark,
    secondary = EmeraldSoft,
    onSecondary = Color(0xFF042416),
    secondaryContainer = Color(0xFF123023),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = Color(0xFF60A5FA),
    onTertiary = Color(0xFF0B2545),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A)
)

val LocalIsDarkTheme = staticCompositionLocalOf { true }

@Composable
fun SmartCompanionTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    language: AppLanguage = AppLanguage.ENGLISH,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val isArabic = language == AppLanguage.ARABIC
    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalIsDarkTheme provides isDark
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = buildAppTypography(isArabic),
            shapes = AppShapes,
            content = content
        )
    }
}
