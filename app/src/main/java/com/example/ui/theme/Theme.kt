package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF001F26),
    primaryContainer = Color(0xFF00363F),
    onPrimaryContainer = Color(0xFF80F2FF),
    secondary = NeonAmber,
    onSecondary = Color(0xFF261A00),
    secondaryContainer = Color(0xFF402E00),
    onSecondaryContainer = Color(0xFFFFDF9E),
    tertiary = NeonPurple,
    background = GamingDarkBackground,
    onBackground = TextPrimary,
    surface = GamingSurface,
    onSurface = TextPrimary,
    surfaceVariant = GamingSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = GamingBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force sleek gaming dark theme by default
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
