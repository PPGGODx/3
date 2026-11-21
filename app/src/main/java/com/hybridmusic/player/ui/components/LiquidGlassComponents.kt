package com.hybridmusic.player.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hybridmusic.player.ui.theme.LiquidMotionTokens
import com.hybridmusic.player.ui.theme.LocalRgbPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * พื้นหลัง RGB Aurora เคลื่อนไหวช้าๆ ด้านหลังแผ่นกระจก เพื่อให้ Liquid Glass หักเหแสงได้มีมิติ
 */
@Composable
fun LiquidRgbAuroraBackground(
    modifier: Modifier = Modifier
) {
    val palette = LocalRgbPalette.current
    val infinite = rememberInfiniteTransition(label = "aurora_drift")

    val phase1 by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "blob1_phase"
    )
    val phase2 by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 19_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "blob2_phase"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        drawRect(color = Color.Black)
        val w = size.width
        val h = size.height

        // Orb 1: Primary RGB (บนซ้ายลอยวน)
        val c1 = Offset(
            x = w * (0.25f + 0.18f * cos(phase1)),
            y = h * (0.20f + 0.12f * sin(phase1))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.primary.copy(alpha = 0.34f),
                    palette.primary.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = c1,
                radius = w * 0.78f
            ),
            radius = w * 0.78f,
            center = c1
        )

        // Orb 2: Secondary RGB (กลางขวา)
        val c2 = Offset(
            x = w * (0.78f + 0.16f * sin(phase2)),
            y = h * (0.48f + 0.15f * cos(phase2))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.secondary.copy(alpha = 0.30f),
                    palette.secondary.copy(alpha = 0.06f),
                    Color.Transparent
                ),
                center = c2,
                radius = w * 0.82f
            ),
            radius = w * 0.82f,
            center = c2
        )

        // Orb 3: Tertiary RGB (ล่างซ้าย)
        val c3 = Offset(
            x = w * (0.35f + 0.20f * sin(phase1 + 1.2f)),
            y = h * (0.84f + 0.09f * cos(phase2))
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.tertiary.copy(alpha = 0.26f),
                    palette.tertiary.copy(alpha = 0.05f),
                    Color.Transparent
                ),
                center = c3,
                radius = w * 0.75f
            ),
            radius = w * 0.75f,
            center = c3
        )
    }
}

/**
 * Modifier หลักสำหรับสร้างชั้นวัสดุ Liquid Glass:
 * - Translucent Smoked Glass + RGB Tint Wash
 * - Volumetric Top-Left Inner Glow
 * - 1dp Specular Rim Gradient Border
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(24.dp),
    elevated: Boolean = false,
    accentGlow: Boolean = false,
    rimAlphaBoost: Float = 0f
): Modifier = composed {
    val palette = LocalRgbPalette.current
    val baseFill = if (elevated) palette.glassSurfaceElevated else palette.glassSurface
    val topSpecular = Color.White.copy(alpha = (0.56f + rimAlphaBoost).coerceAtMost(0.92f))
    val midRim = palette.specularRim.copy(alpha = (0.48f + rimAlphaBoost).coerceAtMost(0.88f))
    val bottomRim = Color.White.copy(alpha = 0.08f)

    this
        .shadow(
            elevation = if (elevated) 16.dp else 8.dp,
            shape = shape,
            ambientColor = if (accentGlow) palette.primary.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.35f),
            spotColor = if (accentGlow) palette.primary.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.45f)
        )
        .clip(shape)
        .background(baseFill)
        .drawBehind {
            // ชั้นแสงหักเห RGB ด้านในกระจก
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (accentGlow) 0.16f else 0.10f),
                        palette.primary.copy(alpha = if (accentGlow) 0.16f else 0.06f),
                        palette.secondary.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
            )
        }
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(topSpecular, midRim, bottomRim),
                start = Offset.Zero,
                end = Offset(900f, 900f)
            ),
            shape = shape
        )
}

/**
 * การ์ดหรือปุ่ม Liquid Glass ที่ยุบตัวและสว่างขอบกระจกเมื่อแตะ (Spring Overshoot)
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    elevated: Boolean = false,
    accentGlow: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.96f else 1f,
        animationSpec = LiquidMotionTokens.SnappySpring,
        label = "glass_press_scale"
    )
    val rimBoost by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.28f else 0f,
        animationSpec = LiquidMotionTokens.SnappySpring,
        label = "glass_rim_boost"
    )

    val clickableMod = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .liquidGlass(
                shape = shape,
                elevated = elevated,
                accentGlow = accentGlow,
                rimAlphaBoost = rimBoost
            )
            .then(clickableMod),
        content = content
    )
}

@Composable
fun LiquidGlassPillButton(
    text: String,
    icon: ImageVector? = null,
    isPrimaryRgb: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalRgbPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = LiquidMotionTokens.SnappySpring,
        label = "pill_btn_scale"
    )

    val bgBrush = if (isPrimaryRgb) {
        Brush.horizontalGradient(listOf(palette.primary, palette.secondary))
    } else {
        Brush.horizontalGradient(
            listOf(
                Color.White.copy(alpha = 0.14f),
                palette.primary.copy(alpha = 0.12f)
            )
        )
    }

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(bgBrush)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = if (isPrimaryRgb) 0.75f else 0.45f),
                        Color.White.copy(alpha = 0.10f)
                    )
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = if (isPrimaryRgb) Color(0xFF04060D) else palette.textPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isPrimaryRgb) Color(0xFF04060D) else palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun LiquidGlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    isAccent: Boolean = false
) {
    val palette = LocalRgbPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = LiquidMotionTokens.ElasticOvershootSpring,
        label = "icon_btn_scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(
                if (isAccent) {
                    Brush.linearGradient(listOf(palette.primary, palette.secondary))
                } else {
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.14f),
                            palette.primary.copy(alpha = 0.08f)
                        )
                    )
                }
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = if (isAccent) 0.80f else 0.45f),
                        Color.White.copy(alpha = 0.08f)
                    )
                ),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (isAccent) Color(0xFF05070E) else palette.textPrimary,
            modifier = Modifier.size(size * 0.50f)
        )
    }
}

/**
 * Floating Capsule Navigation Bar พร้อมเลนส์กระจกที่เลื่อนและยืดตัวระหว่างแท็บ (Lensing Indicator)
 */
@Composable
fun LiquidFloatingNavBar(
    items: List<Pair<String, ImageVector>>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalRgbPalette.current

    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = LiquidMotionTokens.FluidSpring,
        label = "nav_lens_pos"
    )

    // ยืดเลนส์กระจกกลางทางขณะเคลื่อนที่ข้ามแท็บ (Liquid Stretch)
    val travelFraction = kotlin.math.abs(animatedIndex - selectedIndex.toFloat())
    val lensStretchX = 1f + (travelFraction.coerceAtMost(0.5f) * 0.36f)
    val lensSquashY = 1f - (travelFraction.coerceAtMost(0.5f) * 0.12f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(66.dp)
            .liquidGlass(shape = CircleShape, elevated = true, accentGlow = true)
            .padding(6.dp)
    ) {
        val tabWidth = maxWidth / items.size

        // เลนส์กระจกโปร่งแสงเรืองขอบ RGB สำหรับแท็บที่เลือก
        Box(
            modifier = Modifier
                .offset(x = tabWidth * animatedIndex)
                .width(tabWidth)
                .fillMaxHeight()
                .graphicsLayer {
                    scaleX = lensStretchX
                    scaleY = lensSquashY
                }
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            palette.primary.copy(alpha = 0.34f),
                            palette.secondary.copy(alpha = 0.28f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.72f),
                            palette.primary.copy(alpha = 0.55f),
                            Color.White.copy(alpha = 0.12f)
                        )
                    ),
                    shape = CircleShape
                )
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { idx, (label, icon) ->
                val isSelected = idx == selectedIndex
                val itemScale by animateFloatAsState(
                    targetValue = if (isSelected) 1.05f else 0.96f,
                    animationSpec = LiquidMotionTokens.SnappySpring,
                    label = "tab_item_scale"
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(idx) }
                        .graphicsLayer {
                            scaleX = itemScale
                            scaleY = itemScale
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = if (isSelected) Color.White else palette.textSecondary.copy(alpha = 0.75f),
                        modifier = Modifier.size(21.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else palette.textSecondary.copy(alpha = 0.75f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Segmented Control แบบกระจกพร้อมหัวเลื่อน Morphing Glass Thumb
 */
@Composable
fun LiquidSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalRgbPalette.current
    val animatedIdx by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = LiquidMotionTokens.FluidSpring,
        label = "segmented_thumb"
    )
    val stretch = 1f + (kotlin.math.abs(animatedIdx - selectedIndex).coerceAtMost(0.5f) * 0.25f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .liquidGlass(shape = CircleShape, elevated = false)
            .padding(4.dp)
    ) {
        val segWidth = maxWidth / options.size
        Box(
            modifier = Modifier
                .offset(x = segWidth * animatedIdx)
                .width(segWidth)
                .fillMaxHeight()
                .graphicsLayer { scaleX = stretch }
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            palette.primary.copy(alpha = 0.38f),
                            palette.secondary.copy(alpha = 0.30f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.65f), Color.White.copy(alpha = 0.12f))
                    ),
                    shape = CircleShape
                )
        )

        Row(modifier = Modifier.fillMaxSize()) {
            options.forEachIndexed { idx, title ->
                val selected = idx == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelect(idx) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) Color.White else palette.textSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * แถบเลื่อนเวลาเพลงแบบ Liquid Stretch: หัวเลนส์กระจกยืดตัวเมื่อกำลังลากเปลี่ยนช่วงเพลง
 */
@Composable
fun LiquidStretchSlider(
    progress: Float,
    onSeekFinished: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalRgbPalette.current
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(progress.coerceIn(0f, 1f)) }
    val displayProgress = if (isDragging) dragProgress else progress.coerceIn(0f, 1f)

    val knobStretchX by animateFloatAsState(
        targetValue = if (isDragging) 1.45f else 1f,
        animationSpec = LiquidMotionTokens.ElasticOvershootSpring,
        label = "knob_stretch_x"
    )
    val knobSquashY by animateFloatAsState(
        targetValue = if (isDragging) 0.84f else 1f,
        animationSpec = LiquidMotionTokens.ElasticOvershootSpring,
        label = "knob_squash_y"
    )
    val trackHeight by animateFloatAsState(
        targetValue = if (isDragging) 9f else 6f,
        animationSpec = LiquidMotionTokens.SnappySpring,
        label = "track_height"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onSeekFinished(frac)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        isDragging = true
                        dragProgress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dragProgress = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    },
                    onDragEnd = {
                        isDragging = false
                        onSeekFinished(dragProgress)
                    },
                    onDragCancel = {
                        isDragging = false
                    }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val totalWidth = maxWidth
        // รางกระจกพื้นหลัง
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f))
                .border(0.5.dp, Color.White.copy(alpha = 0.25f), CircleShape)
        )
        // แถบความคืบหน้า RGB
        Box(
            modifier = Modifier
                .width(totalWidth * displayProgress)
                .height(trackHeight.dp)
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(listOf(palette.primary, palette.secondary))
                )
        )
        // หัวเลนส์กระจกยืดหยุ่น (Liquid Glass Lens Knob)
        val knobSize = 18.dp
        val knobOffset = ((totalWidth - knobSize) * displayProgress).coerceAtLeast(0.dp)
        Box(
            modifier = Modifier
                .offset(x = knobOffset)
                .size(knobSize)
                .graphicsLayer {
                    scaleX = knobStretchX
                    scaleY = knobSquashY
                }
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.90f))
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(listOf(palette.primary, palette.secondary)),
                    shape = CircleShape
                )
        )
    }
}

/**
 * คลื่นเสียง Equalizer สี RGB เต้นตามจังหวะเมื่อกำลังเล่นเพลง
 */
@Composable
fun RgbLiveVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalRgbPalette.current
    val infinite = rememberInfiniteTransition(label = "rgb_eq")
    val bar1 by infinite.animateFloat(
        initialValue = 0.25f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq1"
    )
    val bar2 by infinite.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.20f,
        animationSpec = infiniteRepeatable(tween(340, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq2"
    )
    val bar3 by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(510, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "eq3"
    )

    val levels = if (isPlaying) listOf(bar1, bar2, bar3) else listOf(0.30f, 0.30f, 0.30f)

    Canvas(modifier = modifier.size(18.dp)) {
        val barWidth = size.width / 5f
        levels.forEachIndexed { idx, level ->
            val barH = size.height * level
            val x = idx * (barWidth * 1.8f)
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(palette.primary, palette.secondary)),
                topLeft = Offset(x, size.height - barH),
                size = androidx.compose.ui.geometry.Size(barWidth, barH),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

/**
 * Toast แจ้งเตือนทรงแคปซูลกระจกเลื่อนลงมาจากด้านบน
 */
@Composable
fun LiquidToastBanner(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        if (!message.isNullOrBlank()) {
            LiquidGlassSurface(
                shape = CircleShape,
                elevated = true,
                accentGlow = true,
                onClick = onDismiss,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(LocalRgbPalette.current.primary)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
