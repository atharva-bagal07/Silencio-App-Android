package com.example.silencio.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SilencioDarkColorScheme = darkColorScheme(
    // Backgrounds
    background = Background,
    surface = Surface,
    surfaceVariant = SurfaceVariant,

    // Primary — buttons, active states, progress
    primary = AccentBlue,
    onPrimary = TextPrimary,
    primaryContainer = AccentBlueDim,
    onPrimaryContainer = TextPrimary,

    // Secondary — inactive states
    secondary = StatusMonitoring,
    onSecondary = TextPrimary,

    // Text
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,

    // Outline — dividers, borders
    outline = Divider,
    outlineVariant = SurfaceVariant,

    // Error
    error = Color(0xFFCF6679),
    onError = TextPrimary
)

@Composable
fun SilencioTheme(
    content: @Composable () -> Unit
) {
    // Dark mode only — always.
    // Silencio is a dark mode app by design.
    // No light mode. No system theme switching.
    MaterialTheme(
        colorScheme = SilencioDarkColorScheme,
        typography = Typography,
        content = content
    )
}