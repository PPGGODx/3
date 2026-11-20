package com.hybridmusic.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SpotifyBlack = Color(0xFF121212)
val SpotifySurface = Color(0xFF181818)
val SpotifyCard = Color(0xFF242424)
val SpotifyCardElevated = Color(0xFF2A2A2A)
val SpotifyGreen = Color(0xFF1ED760)
val SpotifyGreenDark = Color(0xFF1DB954)
val SpotifyTextPrimary = Color(0xFFFFFFFF)
val SpotifyTextSecondary = Color(0xFFB3B3B3)

private val SpotifyColorScheme = darkColorScheme(
    primary = SpotifyGreen,
    secondary = SpotifyGreenDark,
    background = SpotifyBlack,
    surface = SpotifySurface,
    surfaceVariant = SpotifyCard,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = SpotifyTextPrimary,
    onSurface = SpotifyTextPrimary,
    onSurfaceVariant = SpotifyTextSecondary
)

@Composable
fun HybridMusicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SpotifyColorScheme,
        content = content
    )
}
