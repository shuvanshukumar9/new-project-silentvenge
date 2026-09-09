package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NeonGreen,
    onPrimary = CyberBlack,
    primaryContainer = Color(0xFF00381A),
    onPrimaryContainer = NeonGreen,
    secondary = CyberCyan,
    onSecondary = CyberBlack,
    secondaryContainer = Color(0xFF00363F),
    onSecondaryContainer = CyberCyan,
    tertiary = TerminalYellow,
    onTertiary = CyberBlack,
    background = CyberBlack,
    onBackground = TextPrimary,
    surface = CyberDark,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurface,
    onSurfaceVariant = TextSecondary,
    outline = CyberBorder,
    error = TerminalRed,
    onError = Color.White
)

private val LightColorScheme = darkColorScheme(
    // Force sleek high-contrast dark cyberpunk theme even if device is light
    primary = NeonGreen,
    onPrimary = CyberBlack,
    background = CyberBlack,
    onBackground = TextPrimary,
    surface = CyberDark,
    onSurface = TextPrimary,
    outline = CyberBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false, // Keep signature cyber look consistent
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
