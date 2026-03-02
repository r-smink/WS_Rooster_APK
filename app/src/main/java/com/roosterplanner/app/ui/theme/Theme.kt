package com.roosterplanner.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Sky,
    onPrimary = Color.White,
    secondary = Mint,
    onSecondary = Color.White,
    background = Sand,
    surface = Color.White,
    onBackground = Slate,
    onSurface = Slate,
)

private val DarkColors = darkColorScheme(
    primary = Sky,
    onPrimary = Color.White,
    secondary = Mint,
    onSecondary = Color.Black,
    background = Color(0xFF0F172A),
    surface = Color(0xFF111827),
    onBackground = Color(0xFFE5E7EB),
    onSurface = Color(0xFFE5E7EB),
)

@Composable
fun RoosterPlannerTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
