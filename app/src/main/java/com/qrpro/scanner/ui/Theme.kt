package com.qrpro.scanner.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF2DE0C4),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF0A4A41),
    onPrimaryContainer = Color(0xFF9CF5E3),
    secondary = Color(0xFF9DB7FF),
    onSecondary = Color(0xFF0D2457),
    secondaryContainer = Color(0xFF22356B),
    onSecondaryContainer = Color(0xFFDCE5FF),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFE6EAF2),
    surface = Color(0xFF0F1729),
    onSurface = Color(0xFFE6EAF2),
    surfaceVariant = Color(0xFF1A2438),
    onSurfaceVariant = Color(0xFFA9B4CA),
    outline = Color(0xFF3A4660),
    outlineVariant = Color(0xFF273248),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun QRScannerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
