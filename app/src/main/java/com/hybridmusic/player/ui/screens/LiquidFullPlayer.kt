package com.hybridmusic.player.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.hybridmusic.player.innertube.TrackItem
import com.hybridmusic.player.ui.components.LiquidGlassIconButton
import com.hybridmusic.player.ui.components.LiquidGlassSurface
import com.hybridmusic.player.ui.components.LiquidRgbAuroraBackground
import com.hybridmusic.player.ui.components.LiquidSegmentedControl
import com.hybridmusic.player.ui.components.LiquidStretchSlider
import com.hybridmusic.player.ui.components.RgbLiveVisualizer
import com.hybridmusic.player.ui.theme.LocalRgbPalette
import com.hybridmusic.player.ui.viewmodel.MainViewModel

@Composable
fun LiquidMiniPlayerBar(
    viewModel: MainViewModel,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalRgbPalette.current
    val state by viewModel.playbackState.collectAsState()
    val track = state.currentTrack ?: return
    val progress = if (state.durationMs > 0L) {
        (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    LiquidGlassSurface(
        shape = CircleShape,
        elevated = true,
        accentGlow = true,
        onClick = onExpand,
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, palette.primary, CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (state.isBuffering) {
                    CircularProgressIndicator(
                        color = palette.primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                LiquidGlassIconButton(
                    icon = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Play/Pause",
                    isAccent = true,
                    size = 40.dp,
                    onClick = { viewModel.playerController.togglePlayPause() }
                )
                Spacer(modifier = Modifier.width(6.dp))
                LiquidGlassIconButton(
                    icon = Icons.Rounded.SkipNext,
                    contentDescription = "Next",
                    isAccent = false,
                    size = 38.dp,
                    onClick = { viewModel.playerController.playNextInQueue() }
                )
            }
            // เส้นบอกความคืบหน้า RGB ด้านล่างแคปซูล
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(2.5.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(2.5.dp)
                        .background(Brush.horizontalGradient(listOf(palette.primary, palette.secondary)))
                )
            }
        }
    }
}

@Composable
fun LiquidFullPlayerSheet(
    viewModel: MainViewModel,
    onRequestAddToPlaylist: (TrackItem) -> Unit
) {
    val isOpen by viewModel.isFullPlayerOpen.collectAsState()
    val state by viewModel.playbackState.collectAsState()
    val likedTracks by viewModel.likedTracks.collectAsState()
    val lyricsResult by viewModel.lyricsResult.collectAsState()
    val isLoadingLyrics by viewModel.isLoadingLyrics.collectAsState()
    val relatedResult by viewModel.relatedResult.collectAsState()
    val isLoadingRelated by viewModel.isLoadingRelated.collectAsState()

    val palette = LocalRgbPalette.current
    var subTab by remember { mutableIntStateOf(0) }

    AnimatedVisibility(
        visible = isOpen && state.currentTrack != null,
        enter = slideInVertically(initialOffsetY = { it }) + scaleIn(initialScale = 0.94f) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + scaleOut(targetScale = 0.96f) + fadeOut()
    ) {
        val track = state.currentTrack ?: return@AnimatedVisibility
        val isLiked = likedTracks.any { it.videoId == track.videoId }
        val progress = if (state.durationMs > 0L) {
            (state.positionMs.toFloat() / state.durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f

        Box(modifier = Modifier.fillMaxSize()) {
            LiquidRgbAuroraBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Bar + Engine Badge สี RGB
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LiquidGlassIconButton(
                        icon = Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Minimize",
                        onClick = { viewModel.openFullPlayer(false) }
                    )

                    LiquidGlassSurface(shape = CircleShape, accentGlow = true) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RgbLiveVisualizer(isPlaying = state.isPlaying)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = state.engineBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.primary,
                                maxLines = 1
                            )
                        }
                    }

                    LiquidGlassIconButton(
                        icon = Icons.Rounded.PlaylistAdd,
                        contentDescription = "Add to Playlist",
                        onClick = { onRequestAddToPlaylist(track) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Segmented Control กระจกสลับหน้า
                LiquidSegmentedControl(
                    options = listOf("กำลังเล่น", "เนื้อเพลง", "คิว & แนะนำ"),
                    selectedIndex = subTab,
                    onSelect = { subTab = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (subTab) {
                    0 -> {
                        // หน้าปกเพลงในกรอบกระจก Concentric + แสง RGB
                        LiquidGlassSurface(
                            shape = RoundedCornerShape(32.dp),
                            elevated = true,
                            accentGlow = true,
                            modifier = Modifier
                                .fillMaxWidth(0.84f)
                                .aspectRatio(1f)
                        ) {
                            AsyncImage(
                                model = track.thumbnailUrl,
                                contentDescription = track.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp)
                                    .clip(RoundedCornerShape(22.dp))
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // ชื่อเพลง + ปุ่มหัวใจ & วิทยุ
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artist,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            LiquidGlassIconButton(
                                icon = Icons.Rounded.Radio,
                                contentDescription = "Radio",
                                onClick = { viewModel.startRadioForTrack(track) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            LiquidGlassIconButton(
                                icon = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Like",
                                isAccent = isLiked,
                                onClick = { viewModel.toggleLikeTrack(track) }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Liquid Stretch Seek Slider
                        LiquidStretchSlider(
                            progress = progress,
                            onSeekFinished = { frac ->
                                if (state.durationMs > 0L) {
                                    viewModel.playerController.seekTo((frac * state.durationMs).toLong())
                                }
                            }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTimeMs(state.positionMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                            Text(
                                text = formatTimeMs(state.durationMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // แผงปุ่มควบคุมการเล่นแบบ Liquid Glass
                        LiquidGlassSurface(
                            shape = CircleShape,
                            elevated = true,
                            accentGlow = true,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                LiquidGlassIconButton(
                                    icon = Icons.Rounded.Shuffle,
                                    contentDescription = "Shuffle",
                                    isAccent = state.shuffleEnabled,
                                    size = 42.dp,
                                    onClick = { viewModel.playerController.toggleShuffle() }
                                )
                                LiquidGlassIconButton(
                                    icon = Icons.Rounded.SkipPrevious,
                                    contentDescription = "Previous",
                                    size = 48.dp,
                                    onClick = { viewModel.playerController.playPreviousInQueue() }
                                )
                                LiquidGlassIconButton(
                                    icon = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    isAccent = true,
                                    size = 64.dp,
                                    onClick = { viewModel.playerController.togglePlayPause() }
                                )
                                LiquidGlassIconButton(
                                    icon = Icons.Rounded.SkipNext,
                                    contentDescription = "Next",
                                    size = 48.dp,
                                    onClick = { viewModel.playerController.playNextInQueue() }
                                )
                                LiquidGlassIconButton(
                                    icon = if (state.repeatMode == Player.REPEAT_MODE_ONE) {
                                        Icons.Rounded.RepeatOne
                                    } else {
                                        Icons.Rounded.Repeat
                                    },
                                    contentDescription = "Repeat",
                                    isAccent = state.repeatMode != Player.REPEAT_MODE_OFF,
                                    size = 42.dp,
                                    onClick = { viewModel.playerController.cycleRepeatMode() }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    1 -> {
                        // หน้าเนื้อเพลงในการ์ดกระจก
                        LiquidGlassSurface(
                            shape = RoundedCornerShape(28.dp),
                            elevated = true,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (isLoadingLyrics) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = palette.primary)
                                }
                            } else {
                                val lines = lyricsResult?.lines ?: listOf("กำลังโหลดเนื้อเพลง...")
                                LazyColumn(
                                    contentPadding = PaddingValues(22.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(lines) { line ->
                                        Text(
                                            text = line,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    item {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = lyricsResult?.source ?: "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = palette.textSecondary,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // หน้าคิวเพลง & เพลงแนะนำ
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Text(
                                    text = "คิวการเล่นถัดไป (${state.queue.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = palette.primary
                                )
                            }
                            items(state.queue) { qTrack ->
                                val isCur = qTrack.videoId == track.videoId
                                LiquidGlassSurface(
                                    shape = RoundedCornerShape(18.dp),
                                    elevated = isCur,
                                    accentGlow = isCur,
                                    onClick = { viewModel.playTrackOneClick(qTrack, state.queue) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = qTrack.thumbnailUrl,
                                            contentDescription = qTrack.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = qTrack.title,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCur) palette.primary else Color.White,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = qTrack.artist,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = palette.textSecondary,
                                                maxLines = 1
                                            )
                                        }
                                        if (isCur) {
                                            RgbLiveVisualizer(isPlaying = state.isPlaying)
                                        }
                                    }
                                }
                            }

                            if (!isLoadingRelated && relatedResult.tracks.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "เพลงที่คล้ายกัน",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = palette.secondary
                                    )
                                }
                                items(relatedResult.tracks.take(10)) { rel ->
                                    LiquidGlassSurface(
                                        shape = RoundedCornerShape(18.dp),
                                        onClick = { viewModel.playTrackOneClick(rel, relatedResult.tracks) }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(
                                                model = rel.thumbnailUrl,
                                                contentDescription = rel.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(rel.title, style = MaterialTheme.typography.labelMedium, color = Color.White, maxLines = 1)
                                                Text(rel.artist, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary, maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimeMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return "%d:%02d".format(min, sec)
}
