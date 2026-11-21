package com.hybridmusic.player.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class RgbGlassPalette(
    val hue: Float,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val specularRim: Color,
    val innerGlow: Color,
    val glassSurface: Color,
    val glassSurfaceElevated: Color,
    val backdropDeep: Color,
    val textPrimary: Color,
    val textSecondary: Color
)

val LocalRgbPalette = staticCompositionLocalOf {
    RgbGlassPalette(
        hue = 210f,
        primary = Color(0xFF38BDF8),
        secondary = Color(0xFFA855F7),
        tertiary = Color(0xFF10B981),
        specularRim = Color.White.copy(alpha = 0.65f),
        innerGlow = Color(0xFF38BDF8).copy(alpha = 0.24f),
        glassSurface = Color(0xFF0E1220).copy(alpha = 0.50f),
        glassSurfaceElevated = Color(0xFF151A2E).copy(alpha = 0.66f),
        backdropDeep = Color(0xFF000000),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFFCBD5E1)
    )
}

object LiquidMotionTokens {
    val SnappySpring = spring<Float>(
        dampingRatio = 0.76f,
        stiffness = 300f
    )
    val FluidSpring = spring<Float>(
        dampingRatio = 0.68f,
        stiffness = 170f
    )
    val ElasticOvershootSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = 260f
    )
}

private val LiquidTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

@Composable
fun LiquidGlassRgbTheme(
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rgb_spectrum_cycle")
    val hue by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rgb_hue"
    )

    val primary = Color.hsv(hue, 0.80f, 1.0f)
    val secondary = Color.hsv((hue + 55f) % 360f, 0.78f, 0.98f)
    val tertiary = Color.hsv((hue + 145f) % 360f, 0.76f, 0.95f)
    val specular = Color.hsv(hue, 0.30f, 1.0f).copy(alpha = 0.72f)

    val palette = RgbGlassPalette(
        hue = hue,
        primary = primary,
        secondary = secondary,
        tertiary = tertiary,
        specularRim = specular,
        innerGlow = primary.copy(alpha = 0.24f),
        glassSurface = Color(0xFF0E1220).copy(alpha = 0.50f),
        glassSurfaceElevated = Color(0xFF151A2E).copy(alpha = 0.66f),
        backdropDeep = Color(0xFF000000),
        textPrimary = Color(0xFFF8FAFC),
        textSecondary = Color(0xFFCBD5E1)
    )

    val colorScheme = darkColorScheme(
        primary = primary,
        secondary = secondary,
        tertiary = tertiary,
        background = Color.Black,
        surface = palette.glassSurface,
        onPrimary = Color(0xFF04060C),
        onSecondary = Color.White,
        onBackground = palette.textPrimary,
        onSurface = palette.textPrimary
    )

    CompositionLocalProvider(LocalRgbPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LiquidTypography,
            content = content
        )
    }
}
