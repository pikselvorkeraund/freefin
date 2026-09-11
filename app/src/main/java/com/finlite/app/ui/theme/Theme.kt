package com.finlite.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF00696E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9CF1F6),
    onPrimaryContainer = Color(0xFF002022),
    secondary = Color(0xFF4A6363),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCE8E8),
    onSecondaryContainer = Color(0xFF051F20),
    tertiary = Color(0xFF7C5635),
    onTertiary = Color.White,
    background = Color(0xFFF4FBFB),
    surface = Color(0xFFF4FBFB),
    surfaceVariant = Color(0xFFDAE4E5),
    outline = Color(0xFF6F7979),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF80D4DA),
    onPrimary = Color(0xFF00363A),
    primaryContainer = Color(0xFF004F53),
    secondary = Color(0xFFB0CCCB),
    tertiary = Color(0xFFEFBD94),
    background = Color(0xFF191C1C),
    surface = Color(0xFF191C1C),
)

@Composable
fun FinLiteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
