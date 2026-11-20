package com.hybridmusic.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFE50914),
    secondary = Color(0xFFFF4D6D),
    background = Color(0xFF0A0508),
    surface = Color(0xFF160B11),
    onPrimary = Color.White,
    onBackground = Color(0xFFF5F3F7),
    onSurface = Color(0xFFF5F3F7)
)

@Composable
fun HybridMusicTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkScheme, content = content)
}
