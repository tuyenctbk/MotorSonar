package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

private val DarkColorScheme = darkColorScheme(
    primary = AmberOrange,
    onPrimary = DeepBlack,
    primaryContainer = Color(0xFF322000),
    onPrimaryContainer = Color(0xFFFFDDB3),
    secondary = SteelGrey,
    onSecondary = CleanWhite,
    tertiary = SafeGreen,
    onTertiary = DeepBlack,
    background = DarkAsphalt,
    onBackground = CleanWhite,
    surface = CarbonCard,
    onSurface = Color.White,
    surfaceVariant = HighlightSleek,
    onSurfaceVariant = Color(0xFFA1A8B3),
    surfaceContainer = Color(0xFF131820),
    surfaceContainerHigh = Color(0xFF1C222B),
    outline = Color(0xFF2C3540),
    outlineVariant = Color(0xFF232D38)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFD97706),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF78350F),
    secondary = Color(0xFF64748B),
    onSecondary = Color.White,
    tertiary = Color(0xFF16A34A),
    onTertiary = Color.White,
    background = Color(0xFFF4F6F9),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    surfaceContainer = Color(0xFFF1F5F9),
    surfaceContainerHigh = Color(0xFFE2E8F0),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0)
)

@Composable
fun MyApplicationTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

