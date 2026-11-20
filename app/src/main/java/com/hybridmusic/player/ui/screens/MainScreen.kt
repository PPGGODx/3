package com.hybridmusic.player.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.hybridmusic.player.innertube.HomeShelf
import com.hybridmusic.player.innertube.LyricsResult
import com.hybridmusic.player.innertube.RelatedResult
import com.hybridmusic.player.innertube.TrackItem
import com.hybridmusic.player.innertube.UserPlaylist
import com.hybridmusic.player.innertube.YtPlaylistCard
import com.hybridmusic.player.player.PlaybackUiState
import com.hybridmusic.player.ui.theme.SpotifyBlack
import com.hybridmusic.player.ui.theme.SpotifyCard
import com.hybridmusic.player.ui.theme.SpotifyCardElevated
import com.hybridmusic.player.ui.theme.SpotifyGreen
import com.hybridmusic.player.ui.theme.SpotifySurface
import com.hybridmusic.player.ui.theme.SpotifyTextPrimary
import com.hybridmusic.player.ui.theme.SpotifyTextSecondary
import com.hybridmusic.player.ui.viewmodel.ActiveCollectionView
import com.hybridmusic.player.ui.viewmodel.BrowseCategory
import com.hybridmusic.player.ui.viewmodel.MainViewModel
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val isFullPlayerOpen by viewModel.isFullPlayerOpen.collectAsState()
    val statusBanner by viewModel.statusBanner.collectAsState()
    val activeCollection by viewModel.activeCollection.collectAsState()
    val likedTracks by viewModel.likedTracks.collectAsState()
    val userPlaylists by viewModel.userPlaylists.collectAsState()
    val savedYtPlaylists by viewModel.savedYtPlaylists.collectAsState()
    val sleepTimerSec by viewModel.sleepTimerRemainingSec.collectAsState()

    var trackMenuTarget by remember { mutableStateOf<TrackItem?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var pendingTrackForNewPlaylist by remember { mutableStateOf<TrackItem?>(null) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    fun shareTrack(track: TrackItem) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, track.title)
            putExtra(Intent.EXTRA_TEXT, "${track.title} - ${track.artist}\nhttps://music.youtube.com/watch?v=${track.videoId}")
        }
        context.startActivity(Intent.createChooser(sendIntent, "แชร์เพลงจาก PlayZongX"))
    }

    Box(modifier = Modifier.fillMaxSize().background(SpotifyBlack)) {
        viewModel.playerController.getEmbedWebView()?.let { wv ->
            AndroidView(factory = { wv }, modifier = Modifier.size(1.dp))
        }

        Scaffold(
            containerColor = SpotifyBlack,
            bottomBar = {
                Column(modifier = Modifier.background(SpotifyBlack).navigationBarsPadding()) {
                    playbackState.currentTrack?.let { current ->
                        val isLiked = likedTracks.any { it.videoId == current.videoId }
                        SpotifyMiniPlayer(
                            track = current,
                            isPlaying = playbackState.isPlaying,
                            isBuffering = playbackState.isBuffering,
                            isLiked = isLiked,
                            positionMs = playbackState.positionMs,
                            durationMs = playbackState.durationMs,
                            onOpenFullPlayer = { viewModel.openFullPlayer(true) },
                            onToggleLike = { viewModel.toggleLikeTrack(current) },
                            onTogglePlayPause = viewModel.playerController::togglePlayPause,
                            onNext = viewModel.playerController::playNextInQueue
                        )
                    }
                    NavigationBar(containerColor = Color(0xF5121212), tonalElevation = 0.dp) {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { viewModel.selectTab(0) },
                            icon = { Icon(Icons.Default.Home, contentDescription = "หน้าหลัก") },
                            label = { Text("หน้าหลัก", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SpotifyTextPrimary,
                                selectedTextColor = SpotifyTextPrimary,
                                unselectedIconColor = SpotifyTextSecondary,
                                unselectedTextColor = SpotifyTextSecondary,
                                indicatorColor = Color.Transparent
                            )
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { viewModel.selectTab(1) },
                            icon = { Icon(Icons.Default.Search, contentDescription = "ค้นหา") },
                            label = { Text("ค้นหา", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SpotifyTextPrimary,
                                selectedTextColor = SpotifyTextPrimary,
                                unselectedIconColor = SpotifyTextSecondary,
                                unselectedTextColor = SpotifyTextSecondary,
                                indicatorColor = Color.Transparent
                            )
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { viewModel.selectTab(2) },
                            icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "คลังเพลง") },
                            label = { Text("คลังเพลง", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SpotifyTextPrimary,
                                selectedTextColor = SpotifyTextPrimary,
                                unselectedIconColor = SpotifyTextSecondary,
                                unselectedTextColor = SpotifyTextSecondary,
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                val collection = activeCollection
                if (collection != null) {
                    CollectionDetailScreen(
                        collection = collection,
                        currentVideoId = playbackState.currentTrack?.videoId,
                        likedVideoIds = likedTracks.map { it.videoId }.toSet(),
                        isSavedYtPlaylist = collection.ytCard?.let { card ->
                            savedYtPlaylists.any { it.browseId == card.browseId }
                        } ?: false,
                        onBack = viewModel::closeCollection,
                        onPlayAll = {
                            if (collection.tracks.isNotEmpty()) {
                                viewModel.playTrackOneClick(collection.tracks.first(), collection.tracks)
                            }
                        },
                        onShuffleAll = { viewModel.shufflePlayList(collection.tracks) },
                        onToggleSaveYt = { collection.ytCard?.let { viewModel.toggleSaveYtPlaylist(it) } },
                        onPlayTrack = { track -> viewModel.playTrackOneClick(track, collection.tracks) },
                        onOpenTrackMenu = { trackMenuTarget = it }
                    )
                } else {
                    when (selectedTab) {
                        0 -> SpotifyHomeTab(
                            viewModel = viewModel,
                            onOpenTrackMenu = { trackMenuTarget = it }
                        )
                        1 -> SpotifySearchTab(
                            viewModel = viewModel,
                            onOpenTrackMenu = { trackMenuTarget = it }
                        )
                        else -> SpotifyLibraryTab(
                            viewModel = viewModel,
                            onOpenCreatePlaylist = {
                                pendingTrackForNewPlaylist = null
                                showCreatePlaylistDialog = true
                            },
                            onOpenTrackMenu = { trackMenuTarget = it }
                        )
                    }
                }

                statusBanner?.let { banner ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SpotifyCardElevated,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                            .clickable { viewModel.clearStatusBanner() }
                    ) {
                        Text(
                            text = banner,
                            color = SpotifyTextPrimary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }

        // หน้าจอเล่นเพลงเต็มรูปแบบพร้อม 3 แท็บแบบ YT Music (เพลงถัดไป | เนื้อเพลง | เพลงที่เกี่ยวข้อง)
        AnimatedVisibility(
            visible = isFullPlayerOpen && playbackState.currentTrack != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            playbackState.currentTrack?.let { current ->
                val isLiked = likedTracks.any { it.videoId == current.videoId }
                val lyricsResult by viewModel.lyricsResult.collectAsState()
                val isLoadingLyrics by viewModel.isLoadingLyrics.collectAsState()
                val relatedResult by viewModel.relatedResult.collectAsState()
                val isLoadingRelated by viewModel.isLoadingRelated.collectAsState()
                val autoplayEnabled by viewModel.autoplayRadioEnabled.collectAsState()

                YtMusicFullPlayerScreen(
                    track = current,
                    state = playbackState,
                    isLiked = isLiked,
                    autoplayEnabled = autoplayEnabled,
                    sleepTimerRemainingSec = sleepTimerSec,
                    lyricsResult = lyricsResult,
                    isLoadingLyrics = isLoadingLyrics,
                    relatedResult = relatedResult,
                    isLoadingRelated = isLoadingRelated,
                    onClose = { viewModel.openFullPlayer(false) },
                    onToggleLike = { viewModel.toggleLikeTrack(current) },
                    onStartRadio = { viewModel.startRadioForTrack(current) },
                    onToggleAutoplay = viewModel::toggleAutoplayRadio,
                    onOpenSleepTimer = { showSleepTimerDialog = true },
                    onShareTrack = { shareTrack(current) },
                    onOpenAddToPlaylist = { trackMenuTarget = current },
                    onTogglePlayPause = viewModel.playerController::togglePlayPause,
                    onSeek = viewModel.playerController::seekTo,
                    onNext = viewModel.playerController::playNextInQueue,
                    onPrevious = viewModel.playerController::playPreviousInQueue,
                    onToggleShuffle = viewModel.playerController::toggleShuffle,
                    onCycleRepeat = viewModel.playerController::cycleRepeatMode,
                    onSelectQueueTrack = { t -> viewModel.playTrackOneClick(t, playbackState.queue) },
                    onSelectRelatedTrack = { t -> viewModel.playTrackOneClick(t, relatedResult.tracks) },
                    onSelectRelatedPlaylist = { pl ->
                        viewModel.openFullPlayer(false)
                        viewModel.openYtPlaylist(pl)
                    }
                )
            }
        }

        // Bottom Sheet เมนูจัดการเพลง (เริ่มวิทยุ, ถูกใจ, เพิ่มลงเพลย์ลิสต์, แชร์)
        trackMenuTarget?.let { target ->
            val liked = likedTracks.any { it.videoId == target.videoId }
            ModalBottomSheet(
                onDismissRequest = { trackMenuTarget = null },
                containerColor = SpotifySurface
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).navigationBarsPadding()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(
                            model = target.thumbnailUrl,
                            contentDescription = target.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(target.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = SpotifyTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(target.artist, fontSize = 12.sp, color = SpotifyTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    SheetMenuItem(
                        icon = Icons.Default.Radio,
                        label = "เริ่มสถานีวิทยุจากเพลงนี้ (YT Music Radio)",
                        tint = SpotifyGreen
                    ) {
                        trackMenuTarget = null
                        viewModel.startRadioForTrack(target)
                    }
                    SheetMenuItem(
                        icon = if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        label = if (liked) "นำออกจากเพลงที่ถูกใจ" else "เพิ่มลงเพลงที่ถูกใจ",
                        tint = if (liked) SpotifyGreen else SpotifyTextPrimary
                    ) {
                        trackMenuTarget = null
                        viewModel.toggleLikeTrack(target)
                    }
                    SheetMenuItem(
                        icon = Icons.Default.Add,
                        label = "สร้างเพลย์ลิสต์ใหม่และเพิ่มเพลงนี้"
                    ) {
                        trackMenuTarget = null
                        pendingTrackForNewPlaylist = target
                        showCreatePlaylistDialog = true
                    }
                    userPlaylists.forEach { pl ->
                        SheetMenuItem(
                            icon = Icons.Default.PlaylistAdd,
                            label = "เพิ่มลงเพลย์ลิสต์ \"${pl.name}\""
                        ) {
                            trackMenuTarget = null
                            viewModel.addTrackToPlaylist(pl.id, target)
                        }
                    }
                    SheetMenuItem(
                        icon = Icons.Default.Share,
                        label = "แชร์ลิงก์เพลง YT Music"
                    ) {
                        trackMenuTarget = null
                        shareTrack(target)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        if (showCreatePlaylistDialog) {
            var newName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showCreatePlaylistDialog = false },
                containerColor = SpotifyCardElevated,
                title = { Text("สร้างเพลย์ลิสต์ใหม่", color = SpotifyTextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        placeholder = { Text("ชื่อเพลย์ลิสต์ของคุณ...") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SpotifyGreen,
                            focusedTextColor = SpotifyTextPrimary,
                            unfocusedTextColor = SpotifyTextPrimary
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newName.isNotBlank()) {
                                viewModel.createPlaylist(newName, pendingTrackForNewPlaylist)
                                showCreatePlaylistDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen, contentColor = Color.Black)
                    ) { Text("สร้าง", fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showCreatePlaylistDialog = false }) {
                        Text("ยกเลิก", color = SpotifyTextSecondary)
                    }
                }
            )
        }

        if (showSleepTimerDialog) {
            AlertDialog(
                onDismissRequest = { showSleepTimerDialog = false },
                containerColor = SpotifyCardElevated,
                title = { Text("ตั้งเวลาปิดเพลงอัตโนมัติ", color = SpotifyTextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15 to "15 นาที", 30 to "30 นาที", 45 to "45 นาที", 60 to "1 ชั่วโมง").forEach { (mins, label) ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SpotifyCard,
                                modifier = Modifier.fillMaxWidth().clickable {
                                    viewModel.setSleepTimerMinutes(mins)
                                    showSleepTimerDialog = false
                                }
                            ) {
                                Text(label, color = SpotifyTextPrimary, modifier = Modifier.padding(12.dp), fontSize = 14.sp)
                            }
                        }
                        if (sleepTimerSec != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF4A1818),
                                modifier = Modifier.fillMaxWidth().clickable {
                                    viewModel.setSleepTimerMinutes(0)
                                    showSleepTimerDialog = false
                                }
                            ) {
                                Text("ยกเลิกตัวตั้งเวลา", color = Color.White, modifier = Modifier.padding(12.dp), fontSize = 14.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSleepTimerDialog = false }) {
                        Text("ปิด", color = SpotifyGreen)
                    }
                }
            )
        }
    }
}

@Composable
private fun SheetMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = SpotifyTextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(label, color = SpotifyTextPrimary, fontSize = 14.sp)
    }
}

@Composable
private fun SpotifyHomeTab(
    viewModel: MainViewModel,
    onOpenTrackMenu: (TrackItem) -> Unit
) {
    val homeFeed by viewModel.homeFeed.collectAsState()
    val selectedMoodIndex by viewModel.selectedMoodIndex.collectAsState()
    val isLoadingHome by viewModel.isLoadingHome.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "สวัสดีตอนเช้า"
            in 12..17 -> "สวัสดีตอนบ่าย"
            else -> "สวัสดีตอนเย็น"
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E3A2A), SpotifyBlack, SpotifyBlack),
                    startY = 0f,
                    endY = 650f
                )
            ),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(greeting, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = SpotifyTextPrimary)
                    Text("YouTube Music • วิทยุอัตโนมัติ + ไม่มีโฆษณา", fontSize = 12.sp, color = SpotifyTextSecondary)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = SpotifyGreen.copy(alpha = 0.16f)) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ad-Free", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SpotifyGreen)
                        }
                    }
                    IconButton(onClick = { viewModel.loadHomeFeed(selectedMoodIndex) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "รีเฟรช", tint = SpotifyTextPrimary)
                    }
                }
            }
        }

        item {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(homeFeed.moods) { idx, mood ->
                    val selected = idx == selectedMoodIndex
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.loadHomeFeed(idx) },
                        label = { Text(mood.title, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SpotifyGreen,
                            selectedLabelColor = Color.Black,
                            containerColor = SpotifyCard,
                            labelColor = SpotifyTextPrimary
                        ),
                        border = null
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        if (isLoadingHome) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 80.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SpotifyGreen)
                }
            }
        } else {
            if (homeFeed.quickPicks.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        homeFeed.quickPicks.chunked(2).forEach { pair ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                pair.forEach { track ->
                                    val isCurrent = track.videoId == playbackState.currentTrack?.videoId
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isCurrent) SpotifyCardElevated else SpotifyCard,
                                        modifier = Modifier.weight(1f).height(56.dp).clickable {
                                            viewModel.playTrackOneClick(track)
                                        }
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            AsyncImage(model = track.thumbnailUrl, contentDescription = track.title, contentScale = ContentScale.Crop, modifier = Modifier.size(56.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = track.title,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCurrent) SpotifyGreen else SpotifyTextPrimary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f).padding(end = 6.dp)
                                            )
                                        }
                                    }
                                }
                                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            items(homeFeed.shelves) { shelf ->
                SpotifyHorizontalShelf(
                    shelf = shelf,
                    currentVideoId = playbackState.currentTrack?.videoId,
                    onPlayTrack = { track -> viewModel.playTrackOneClick(track, shelf.tracks) },
                    onOpenPlaylist = { pl -> viewModel.openYtPlaylist(pl) }
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun SpotifyHorizontalShelf(
    shelf: HomeShelf,
    currentVideoId: String?,
    onPlayTrack: (TrackItem) -> Unit,
    onOpenPlaylist: (YtPlaylistCard) -> Unit
) {
    Column {
        Text(
            text = shelf.title,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = SpotifyTextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(shelf.tracks, key = { "t_${it.videoId}" }) { track ->
                val isCurrent = track.videoId == currentVideoId
                Column(modifier = Modifier.width(142.dp).clickable { onPlayTrack(track) }) {
                    Box(modifier = Modifier.size(142.dp)) {
                        AsyncImage(
                            model = track.thumbnailUrl,
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(SpotifyGreen)
                        ) {
                            Icon(
                                imageVector = if (isCurrent) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                                contentDescription = "เล่น",
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(track.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isCurrent) SpotifyGreen else SpotifyTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, fontSize = 11.sp, color = SpotifyTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            items(shelf.playlists, key = { "p_${it.browseId}" }) { pl ->
                Column(modifier = Modifier.width(142.dp).clickable { onOpenPlaylist(pl) }) {
                    Box(modifier = Modifier.size(142.dp)) {
                        AsyncImage(
                            model = pl.thumbnailUrl,
                            contentDescription = pl.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(SpotifyGreen)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "เปิดเพลย์ลิสต์", tint = Color.Black, modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(pl.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SpotifyTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(pl.subtitle, fontSize = 11.sp, color = SpotifyTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun SpotifySearchTab(
    viewModel: MainViewModel,
    onOpenTrackMenu: (TrackItem) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val suggestions by viewModel.searchSuggestions.collectAsState()
    val filterIndex by viewModel.searchFilterIndex.collectAsState()
    val songResults by viewModel.searchSongsResult.collectAsState()
    val playlistResults by viewModel.searchPlaylistsResult.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    val clipboard = LocalClipboardManager.current
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text("ค้นหา", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = SpotifyTextPrimary)
            Surface(
                shape = CircleShape,
                color = SpotifyCard,
                modifier = Modifier.clickable {
                    val pasted = clipboard.getText()?.text.orEmpty()
                    if (pasted.isNotBlank()) {
                        focusManager.clearFocus()
                        viewModel.submitSearchOrLink(pasted)
                    }
                }
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("วางลิงก์เพลง / เพลย์ลิสต์", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SpotifyTextPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = viewModel::onSearchQueryChange,
            placeholder = { Text("ศิลปิน เพลง อัลบั้ม หรือวางลิงก์ YT Music...", fontSize = 13.sp, color = Color.DarkGray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "ล้าง", tint = Color.Black)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                focusedBorderColor = SpotifyGreen,
                unfocusedBorderColor = Color.White
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                focusManager.clearFocus()
                viewModel.submitSearchOrLink()
            }),
            modifier = Modifier.fillMaxWidth()
        )

        // คำแนะนำการค้นหาแบบ Real-Time (Autocomplete)
        if (suggestions.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SpotifyCardElevated,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            ) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    suggestions.forEach { sug ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    focusManager.clearFocus()
                                    viewModel.submitSearchOrLink(sug)
                                }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = SpotifyTextSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(sug, color = SpotifyTextPrimary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // แถบ Filter Pills แบบ YouTube Music (เพลง | เพลย์ลิสต์ | อัลบั้ม)
        if (searchQuery.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("เพลง", "เพลย์ลิสต์", "อัลบั้ม").forEachIndexed { idx, label ->
                    val selected = filterIndex == idx
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setSearchFilter(idx) },
                        label = { Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                        shape = CircleShape,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SpotifyGreen,
                            selectedLabelColor = Color.Black,
                            containerColor = SpotifyCard,
                            labelColor = SpotifyTextPrimary
                        ),
                        border = null
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isSearching) {
            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SpotifyGreen)
            }
        } else if (filterIndex == 0 && songResults.isNotEmpty()) {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(songResults, key = { it.videoId }) { track ->
                    SpotifyTrackRow(
                        track = track,
                        isPlayingCurrent = track.videoId == playbackState.currentTrack?.videoId,
                        onClick = { viewModel.playTrackOneClick(track, songResults) },
                        onOpenMenu = { onOpenTrackMenu(track) }
                    )
                }
            }
        } else if (filterIndex != 0 && playlistResults.isNotEmpty()) {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(playlistResults, key = { it.browseId }) { pl ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { viewModel.openYtPlaylist(pl) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = pl.thumbnailUrl,
                            contentDescription = pl.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(54.dp).clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(pl.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SpotifyTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(pl.subtitle, fontSize = 12.sp, color = SpotifyTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        } else {
            Text("เลือกแนวเพลงยอดนิยม", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SpotifyTextPrimary)
            Spacer(modifier = Modifier.height(10.dp))
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(viewModel.browseCategories.chunked(2)) { pair ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { cat ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(cat.colorHex),
                                modifier = Modifier.weight(1f).height(82.dp).clickable { viewModel.selectCategory(cat) }
                            ) {
                                Box(modifier = Modifier.padding(12.dp)) {
                                    Text(cat.title, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
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
private fun SpotifyLibraryTab(
    viewModel: MainViewModel,
    onOpenCreatePlaylist: () -> Unit,
    onOpenTrackMenu: (TrackItem) -> Unit
) {
    val likedTracks by viewModel.likedTracks.collectAsState()
    val userPlaylists by viewModel.userPlaylists.collectAsState()
    val savedYtPlaylists by viewModel.savedYtPlaylists.collectAsState()
    val recentTracks by viewModel.recentTracks.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("คลังเพลงของคุณ", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = SpotifyTextPrimary)
                IconButton(onClick = onOpenCreatePlaylist) {
                    Icon(Icons.Default.Add, contentDescription = "สร้างเพลย์ลิสต์", tint = SpotifyTextPrimary, modifier = Modifier.size(28.dp))
                }
            }
        }

        // การ์ดเพลงที่ถูกใจ (Liked Songs) สไตล์ Spotify
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SpotifyCard,
                modifier = Modifier.fillMaxWidth().clickable {
                    viewModel.openLocalCollection(
                        id = "liked",
                        title = "เพลงที่ถูกใจ",
                        subtitle = "${likedTracks.size} เพลง",
                        tracks = likedTracks
                    )
                }
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF450AF5), Color(0xFFC4EFD9))))
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("เพลงที่ถูกใจ (Liked Songs)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SpotifyTextPrimary)
                        Text("เพลย์ลิสต์ • ${likedTracks.size} เพลง", fontSize = 12.sp, color = SpotifyTextSecondary)
                    }
                }
            }
        }

        // เพลย์ลิสต์ที่สร้างเอง
        items(userPlaylists, key = { "upl_${it.id}" }) { pl ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        viewModel.openLocalCollection(
                            id = pl.id,
                            title = pl.name,
                            subtitle = "เพลย์ลิสต์ของคุณ • ${pl.tracks.size} เพลง",
                            tracks = pl.tracks
                        )
                    }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val cover = pl.tracks.firstOrNull()?.thumbnailUrl
                if (cover != null) {
                    AsyncImage(model = cover, contentDescription = pl.name, contentScale = ContentScale.Crop, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)))
                } else {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)).background(SpotifyCardElevated)) {
                        Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = SpotifyTextSecondary)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(pl.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SpotifyTextPrimary)
                    Text("${pl.tracks.size} เพลง", fontSize = 12.sp, color = SpotifyTextSecondary)
                }
                IconButton(onClick = { viewModel.deleteUserPlaylist(pl.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "ลบ", tint = SpotifyTextSecondary, modifier = Modifier.size(20.dp))
                }
            }
        }

        // เพลย์ลิสต์ที่บันทึกมาจาก YouTube Music
        items(savedYtPlaylists, key = { "syt_${it.browseId}" }) { card ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { viewModel.openYtPlaylist(card) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(model = card.thumbnailUrl, contentDescription = card.title, contentScale = ContentScale.Crop, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(card.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = SpotifyTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("YouTube Music • ${card.subtitle}", fontSize = 12.sp, color = SpotifyTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        if (recentTracks.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("ประวัติการฟังล่าสุด (${recentTracks.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = SpotifyTextPrimary)
            }
            items(recentTracks.take(15), key = { "hist_${it.videoId}" }) { track ->
                SpotifyTrackRow(
                    track = track,
                    isPlayingCurrent = track.videoId == playbackState.currentTrack?.videoId,
                    onClick = { viewModel.playTrackOneClick(track, recentTracks) },
                    onOpenMenu = { onOpenTrackMenu(track) }
                )
            }
        }
    }
}

@Composable
private fun CollectionDetailScreen(
    collection: ActiveCollectionView,
    currentVideoId: String?,
    likedVideoIds: Set<String>,
    isSavedYtPlaylist: Boolean,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onToggleSaveYt: () -> Unit,
    onPlayTrack: (TrackItem) -> Unit,
    onOpenTrackMenu: (TrackItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF234230), SpotifyBlack, SpotifyBlack)))
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "กลับ", tint = SpotifyTextPrimary)
                }
                Text("รายละเอียดเพลย์ลิสต์", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SpotifyTextPrimary)
            }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                if (collection.thumbnailUrl.isNotEmpty()) {
                    AsyncImage(
                        model = collection.thumbnailUrl,
                        contentDescription = collection.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(180.dp).clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
                Text(collection.title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = SpotifyTextPrimary)
                Text(collection.subtitle, fontSize = 12.sp, color = SpotifyTextSecondary)
                Spacer(modifier = Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = onPlayAll,
                        colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen, contentColor = Color.Black),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("เล่นทั้งหมด", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(onClick = onShuffleAll, shape = CircleShape) {
                        Icon(Icons.Default.Shuffle, contentDescription = null, tint = SpotifyTextPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("สุ่มเล่น", color = SpotifyTextPrimary)
                    }
                    if (collection.ytCard != null) {
                        IconButton(onClick = onToggleSaveYt) {
                            Icon(
                                imageVector = if (isSavedYtPlaylist) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = "บันทึก",
                                tint = if (isSavedYtPlaylist) SpotifyGreen else SpotifyTextPrimary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        if (collection.isLoading) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SpotifyGreen)
                }
            }
        } else {
            items(collection.tracks, key = { "col_${it.videoId}" }) { track ->
                Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)) {
                    SpotifyTrackRow(
                        track = track,
                        isPlayingCurrent = track.videoId == currentVideoId,
                        onClick = { onPlayTrack(track) },
                        onOpenMenu = { onOpenTrackMenu(track) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SpotifyTrackRow(
    track: TrackItem,
    isPlayingCurrent: Boolean,
    onClick: () -> Unit,
    onOpenMenu: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPlayingCurrent) SpotifyCard else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = track.thumbnailUrl,
            contentDescription = track.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPlayingCurrent) SpotifyGreen else SpotifyTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(track.artist, fontSize = 12.sp, color = SpotifyTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (isPlayingCurrent) {
            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.padding(end = 4.dp))
        }
        IconButton(onClick = onOpenMenu, modifier = Modifier.size(34.dp)) {
            Icon(Icons.Default.MoreVert, contentDescription = "ตัวเลือก", tint = SpotifyTextSecondary)
        }
    }
}

@Composable
private fun SpotifyMiniPlayer(
    track: TrackItem,
    isPlaying: Boolean,
    isBuffering: Boolean,
    isLiked: Boolean,
    positionMs: Long,
    durationMs: Long,
    onOpenFullPlayer: () -> Unit,
    onToggleLike: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit
) {
    val progress = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = SpotifyCardElevated,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onOpenFullPlayer() }
    ) {
        Column {
            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(track.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SpotifyTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.artist, fontSize = 11.sp, color = SpotifyTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = onToggleLike, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "ถูกใจ",
                        tint = if (isLiked) SpotifyGreen else SpotifyTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = onTogglePlayPause, modifier = Modifier.size(36.dp)) {
                    if (isBuffering) {
                        CircularProgressIndicator(color = SpotifyGreen, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = SpotifyTextPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                IconButton(onClick = onNext, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = "ถัดไป", tint = SpotifyTextPrimary)
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.White.copy(alpha = 0.15f))) {
                Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().background(SpotifyGreen))
            }
        }
    }
}

@Composable
private fun YtMusicFullPlayerScreen(
    track: TrackItem,
    state: PlaybackUiState,
    isLiked: Boolean,
    autoplayEnabled: Boolean,
    sleepTimerRemainingSec: Int?,
    lyricsResult: LyricsResult?,
    isLoadingLyrics: Boolean,
    relatedResult: RelatedResult,
    isLoadingRelated: Boolean,
    onClose: () -> Unit,
    onToggleLike: () -> Unit,
    onStartRadio: () -> Unit,
    onToggleAutoplay: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onShareTrack: () -> Unit,
    onOpenAddToPlaylist: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onSelectQueueTrack: (TrackItem) -> Unit,
    onSelectRelatedTrack: (TrackItem) -> Unit,
    onSelectRelatedPlaylist: (YtPlaylistCard) -> Unit
) {
    // 0 = เครื่องเล่นหลัก, 1 = เพลงถัดไป (Up Next), 2 = เนื้อเพลง (Lyrics), 3 = เพลงที่เกี่ยวข้อง (Related)
    var playerTab by remember { mutableIntStateOf(0) }

    Surface(modifier = Modifier.fillMaxSize(), color = SpotifyBlack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF284B37), SpotifySurface, SpotifyBlack)))
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Top Bar ของ Player
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "ย่อลง", tint = SpotifyTextPrimary, modifier = Modifier.size(30.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("YOUTUBE MUSIC • AD-FREE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SpotifyTextSecondary)
                    Text(
                        text = if (sleepTimerRemainingSec != null) "ปิดใน ${sleepTimerRemainingSec / 60}:${"%02d".format(sleepTimerRemainingSec % 60)}" else state.engineBadge,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SpotifyGreen
                    )
                }
                Row {
                    IconButton(onClick = onOpenSleepTimer) {
                        Icon(
                            Icons.Default.Bedtime,
                            contentDescription = "ตั้งเวลาปิด",
                            tint = if (sleepTimerRemainingSec != null) SpotifyGreen else SpotifyTextPrimary,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    IconButton(onClick = onShareTrack) {
                        Icon(Icons.Default.Share, contentDescription = "แชร์", tint = SpotifyTextPrimary, modifier = Modifier.size(21.dp))
                    }
                }
            }

            // ส่วนเนื้อหาหลักตามแท็บที่เลือก
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (playerTab) {
                    0 -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AsyncImage(
                                model = track.thumbnailUrl,
                                contentDescription = track.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth(0.84f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(track.title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = SpotifyTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(track.artist, fontSize = 14.sp, color = SpotifyTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                IconButton(onClick = onStartRadio) {
                                    Icon(Icons.Default.Radio, contentDescription = "วิทยุ", tint = SpotifyGreen)
                                }
                                IconButton(onClick = onOpenAddToPlaylist) {
                                    Icon(Icons.Default.PlaylistAdd, contentDescription = "เพิ่มลงเพลย์ลิสต์", tint = SpotifyTextPrimary)
                                }
                                IconButton(onClick = onToggleLike) {
                                    Icon(
                                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "ถูกใจ",
                                        tint = if (isLiked) SpotifyGreen else SpotifyTextPrimary
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        // แท็บเพลงถัดไป (Up Next Queue + Autoplay Radio Switch)
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("เล่นเพลงคล้ายกันอัตโนมัติ (Radio)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SpotifyTextPrimary)
                                    Text("เพิ่มเพลงแนะนำเข้าคิวอัตโนมัติเมื่อจบเพลง", fontSize = 11.sp, color = SpotifyTextSecondary)
                                }
                                Switch(
                                    checked = autoplayEnabled,
                                    onCheckedChange = { onToggleAutoplay() },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = SpotifyGreen)
                                )
                            }
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(state.queue, key = { "pq_${it.videoId}" }) { qTrack ->
                                    val isNow = qTrack.videoId == track.videoId
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isNow) SpotifyCardElevated else Color.Transparent)
                                            .clickable { onSelectQueueTrack(qTrack) }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(model = qTrack.thumbnailUrl, contentDescription = qTrack.title, contentScale = ContentScale.Crop, modifier = Modifier.size(42.dp).clip(RoundedCornerShape(6.dp)))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(qTrack.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isNow) SpotifyGreen else SpotifyTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(qTrack.artist, fontSize = 11.sp, color = SpotifyTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        if (isNow) Icon(Icons.Default.GraphicEq, contentDescription = null, tint = SpotifyGreen)
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // แท็บเนื้อเพลง (Lyrics)
                        if (isLoadingLyrics) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = SpotifyGreen)
                            }
                        } else {
                            val lines = lyricsResult?.lines ?: listOf("ไม่พบเนื้อเพลงสำหรับเพลงนี้")
                            LazyColumn(modifier = Modifier.fillMaxSize().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(lines) { line ->
                                    Text(
                                        text = line,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SpotifyTextPrimary,
                                        lineHeight = 28.sp
                                    )
                                }
                                item {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(lyricsResult?.source ?: "", fontSize = 11.sp, color = SpotifyTextSecondary)
                                }
                            }
                        }
                    }

                    3 -> {
                        // แท็บเพลงที่เกี่ยวข้อง (Related)
                        if (isLoadingRelated) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = SpotifyGreen)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    Text("เพลงที่คุณอาจชอบ", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SpotifyTextPrimary)
                                }
                                items(relatedResult.tracks, key = { "rel_${it.videoId}" }) { rTrack ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable { onSelectRelatedTrack(rTrack) }.padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(model = rTrack.thumbnailUrl, contentDescription = rTrack.title, contentScale = ContentScale.Crop, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(rTrack.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SpotifyTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(rTrack.artist, fontSize = 11.sp, color = SpotifyTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SpotifyGreen)
                                    }
                                }
                                if (relatedResult.playlists.isNotEmpty()) {
                                    item {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("เพลย์ลิสต์แนะนำ", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = SpotifyTextPrimary)
                                    }
                                    items(relatedResult.playlists, key = { "relpl_${it.browseId}" }) { pl ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable { onSelectRelatedPlaylist(pl) }.padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(model = pl.thumbnailUrl, contentDescription = pl.title, contentScale = ContentScale.Crop, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(pl.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = SpotifyTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(pl.subtitle, fontSize = 11.sp, color = SpotifyTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // แถบควบคุมเวลา + ปุ่มเล่น/หยุด
            val safeDuration = state.durationMs.coerceAtLeast(1L)
            Slider(
                value = state.positionMs.coerceIn(0L, safeDuration).toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..safeDuration.toFloat(),
                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.25f))
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(state.positionMs), fontSize = 11.sp, color = SpotifyTextSecondary)
                Text(formatTime(state.durationMs), fontSize = 11.sp, color = SpotifyTextSecondary)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(Icons.Default.Shuffle, contentDescription = "สุ่ม", tint = if (state.shuffleEnabled) SpotifyGreen else SpotifyTextSecondary)
                }
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "ก่อนหน้า", tint = Color.White, modifier = Modifier.size(34.dp))
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.White).clickable { onTogglePlayPause() }
                ) {
                    if (state.isBuffering) {
                        CircularProgressIndicator(color = Color.Black, strokeWidth = 2.5.dp, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "เล่น/หยุด",
                            tint = Color.Black,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Default.SkipNext, contentDescription = "ถัดไป", tint = Color.White, modifier = Modifier.size(34.dp))
                }
                IconButton(onClick = onCycleRepeat) {
                    val icon = if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat
                    val tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) SpotifyGreen else SpotifyTextSecondary
                    Icon(icon, contentDescription = "เล่นซ้ำ", tint = tint)
                }
            }

            // แถบสลับ 3 โหมดล่างสุดสไตล์ YouTube Music (เครื่องเล่น | เพลงถัดไป | เนื้อเพลง | เพลงที่เกี่ยวข้อง)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("เครื่องเล่น", "เพลงถัดไป", "เนื้อเพลง", "เกี่ยวข้อง").forEachIndexed { idx, label ->
                    val active = playerTab == idx
                    Surface(
                        shape = CircleShape,
                        color = if (active) SpotifyGreen.copy(alpha = 0.2f) else Color.Transparent,
                        modifier = Modifier.clickable { playerTab = idx }
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            color = if (active) SpotifyGreen else SpotifyTextSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}
