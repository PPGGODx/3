package com.hybridmusic.player.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.hybridmusic.player.innertube.TrackItem
import com.hybridmusic.player.ui.viewmodel.MainViewModel

private val BgDark = Color(0xFF0A0508)
private val CardGlass = Color(0xCC1A0C13)
private val Crimson = Color(0xFFE50914)
private val CrimsonBright = Color(0xFFFF4D6D)
private val Emerald = Color(0xFF10B981)
private val TextMain = Color(0xFFF5F3F7)
private val TextSub = Color(0xFFB8A9B2)

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val inputText by viewModel.inputText.collectAsState()
    val trackList by viewModel.trackList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    val clipboard = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current

    Scaffold(
        containerColor = BgDark,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("PlayZongX", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = TextMain)
                    Text("Step 1 • YT Music Link Player", fontSize = 11.sp, color = TextSub)
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Emerald.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, Emerald.copy(alpha = 0.55f))
                ) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = Emerald, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ไม่มีโฆษณา 100%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Emerald)
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            viewModel.playerController.getEmbedWebView()?.let { wv ->
                AndroidView(factory = { wv }, modifier = Modifier.size(1.dp))
            }

            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = CardGlass,
                    border = BorderStroke(1.dp, Crimson.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("วางลิงก์เพลงจาก YT Music (หรือพิมพ์ชื่อเพลง)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CrimsonBright)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = viewModel::onInputChange,
                            placeholder = { Text("https://music.youtube.com/watch?v=...", fontSize = 13.sp, color = TextSub.copy(alpha = 0.7f)) },
                            leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = CrimsonBright) },
                            trailingIcon = {
                                if (inputText.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onInputChange("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "ล้าง", tint = TextSub)
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Crimson,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedTextColor = TextMain,
                                unfocusedTextColor = TextMain
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = {
                                focusManager.clearFocus()
                                viewModel.submitLinkOrSearch()
                            }),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    val pasted = clipboard.getText()?.text.orEmpty()
                                    if (pasted.isNotBlank()) {
                                        viewModel.onInputChange(pasted)
                                        focusManager.clearFocus()
                                        viewModel.submitLinkOrSearch(pasted)
                                    }
                                },
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = null, tint = TextMain, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("วางลิงก์ & เล่น", color = TextMain, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    viewModel.submitLinkOrSearch()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Crimson, contentColor = Color.White),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("เล่นเพลง", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                statusMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(msg, fontSize = 12.sp, color = CrimsonBright)
                }

                playbackState.errorMessage?.let { err ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(shape = RoundedCornerShape(10.dp), color = Crimson.copy(alpha = 0.2f), border = BorderStroke(1.dp, Crimson)) {
                        Text(err, modifier = Modifier.padding(10.dp), fontSize = 12.sp, color = Color.White)
                    }
                }

                playbackState.currentTrack?.let { current ->
                    Spacer(modifier = Modifier.height(12.dp))
                    NowPlayingCard(
                        track = current,
                        isPlaying = playbackState.isPlaying,
                        isBuffering = playbackState.isBuffering,
                        positionMs = playbackState.positionMs,
                        durationMs = playbackState.durationMs,
                        shuffleEnabled = playbackState.shuffleEnabled,
                        repeatMode = playbackState.repeatMode,
                        engineBadge = playbackState.engineBadge,
                        onTogglePlayPause = viewModel.playerController::togglePlayPause,
                        onSeek = viewModel.playerController::seekTo,
                        onNext = viewModel.playerController::playNextInQueue,
                        onPrevious = viewModel.playerController::playPreviousInQueue,
                        onToggleShuffle = viewModel.playerController::toggleShuffle,
                        onCycleRepeat = viewModel.playerController::cycleRepeatMode
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Crimson)
                    }
                } else if (trackList.isNotEmpty()) {
                    Text("รายการเพลง (${trackList.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextSub)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(trackList, key = { _, item -> item.videoId }) { _, item ->
                            val isActive = item.videoId == playbackState.currentTrack?.videoId
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isActive) Crimson.copy(alpha = 0.22f) else CardGlass,
                                border = BorderStroke(1.dp, if (isActive) Crimson else Color.White.copy(alpha = 0.1f)),
                                modifier = Modifier.fillMaxWidth().clickable { viewModel.playTrack(item) }
                            ) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    AsyncImage(
                                        model = item.thumbnailUrl,
                                        contentDescription = item.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(46.dp).clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            item.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isActive) CrimsonBright else TextMain,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(item.artist, fontSize = 11.sp, color = TextSub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Icon(Icons.Default.PlayArrow, contentDescription = "เล่น", tint = CrimsonBright)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NowPlayingCard(
    track: TrackItem,
    isPlaying: Boolean,
    isBuffering: Boolean,
    positionMs: Long,
    durationMs: Long,
    shuffleEnabled: Boolean,
    repeatMode: Int,
    engineBadge: String,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CardGlass,
        border = BorderStroke(1.dp, Crimson.copy(alpha = 0.65f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(engineBadge, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Emerald)
                    Text(track.title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, fontSize = 12.sp, color = TextSub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            val safeDuration = durationMs.coerceAtLeast(1L)
            Slider(
                value = positionMs.coerceIn(0L, safeDuration).toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..safeDuration.toFloat(),
                colors = SliderDefaults.colors(thumbColor = Crimson, activeTrackColor = Crimson, inactiveTrackColor = Color.White.copy(alpha = 0.2f))
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(positionMs), fontSize = 11.sp, color = CrimsonBright)
                Text(formatTime(durationMs), fontSize = 11.sp, color = TextSub)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = if (shuffleEnabled) Crimson else TextSub)
                }
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = TextMain)
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(Crimson, Color(0xFF8B0018))))
                        .clickable { onTogglePlayPause() }
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = TextMain)
                }
                IconButton(onClick = onCycleRepeat) {
                    val icon = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat
                    val tint = if (repeatMode != Player.REPEAT_MODE_OFF) Crimson else TextSub
                    Icon(icon, contentDescription = "Repeat", tint = tint)
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}
