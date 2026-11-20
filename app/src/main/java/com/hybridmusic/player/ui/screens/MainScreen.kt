package com.hybridmusic.player.ui.screens

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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.runtime.remember
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
import com.hybridmusic.player.innertube.HomeShelf
import com.hybridmusic.player.innertube.TrackItem
import com.hybridmusic.player.innertube.YtPlaylistCard
import com.hybridmusic.player.player.PlaybackUiState
import com.hybridmusic.player.ui.theme.SpotifyBlack
import com.hybridmusic.player.ui.theme.SpotifyCard
import com.hybridmusic.player.ui.theme.SpotifyCardElevated
import com.hybridmusic.player.ui.theme.SpotifyGreen
import com.hybridmusic.player.ui.theme.SpotifySurface
import com.hybridmusic.player.ui.theme.SpotifyTextPrimary
import com.hybridmusic.player.ui.theme.SpotifyTextSecondary
import com.hybridmusic.player.ui.viewmodel.BrowseCategory
import com.hybridmusic.player.ui.viewmodel.MainViewModel
import java.util.Calendar

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val isFullPlayerOpen by viewModel.isFullPlayerOpen.collectAsState()
    val statusBanner by viewModel.statusBanner.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(SpotifyBlack)) {
        // Host 1dp สำหรับ Ad-Free Stream Engine สำรองอัตโนมัติ
        viewModel.playerController.getEmbedWebView()?.let { wv ->
            AndroidView(factory = { wv }, modifier = Modifier.size(1.dp))
        }

        Scaffold(
            containerColor = SpotifyBlack,
            bottomBar = {
                Column(modifier = Modifier.background(SpotifyBlack).navigationBarsPadding()) {
                    playbackState.currentTrack?.let { current ->
                        SpotifyMiniPlayer(
                            track = current,
                            isPlaying = playbackState.isPlaying,
                            isBuffering = playbackState.isBuffering,
                            positionMs = playbackState.positionMs,
                            durationMs = playbackState.durationMs,
                            onOpenFullPlayer = { viewModel.openFullPlayer(true) },
                            onTogglePlayPause = viewModel.playerController::togglePlayPause,
                            onNext = viewModel.playerController::playNextInQueue
                        )
                    }
                    NavigationBar(
                        containerColor = Color(0xF0121212),
                        tonalElevation = 0.dp
                    ) {
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
                            icon = { Icon(Icons.Default.QueueMusic, contentDescription = "คิวเพลง") },
                            label = { Text("คิว & ล่าสุด", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
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
                when (selectedTab) {
                    0 -> SpotifyHomeTab(viewModel = viewModel)
                    1 -> SpotifySearchTab(viewModel = viewModel)
                    else -> SpotifyQueueTab(viewModel = viewModel)
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

        // หน้าจอเล่นเพลงเต็มรูปแบบสไตล์ Spotify
        AnimatedVisibility(
            visible = isFullPlayerOpen && playbackState.currentTrack != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            playbackState.currentTrack?.let { current ->
                SpotifyFullPlayerScreen(
                    track = current,
                    state = playbackState,
                    onClose = { viewModel.openFullPlayer(false) },
                    onTogglePlayPause = viewModel.playerController::togglePlayPause,
                    onSeek = viewModel.playerController::seekTo,
                    onNext = viewModel.playerController::playNextInQueue,
                    onPrevious = viewModel.playerController::playPreviousInQueue,
                    onToggleShuffle = viewModel.playerController::toggleShuffle,
                    onCycleRepeat = viewModel.playerController::cycleRepeatMode
                )
            }
        }
    }
}

@Composable
private fun SpotifyHomeTab(viewModel: MainViewModel) {
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
        // Header + Ad-Free Shield
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
                    Text(
                        text = greeting,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SpotifyTextPrimary
                    )
                    Text(
                        text = "YouTube Music • แตะเพลงไหนเล่นทันทีใน 1 คลิก",
                        fontSize = 12.sp,
                        color = SpotifyTextSecondary
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = SpotifyGreen.copy(alpha = 0.16f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
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

        // Spotify-Style Mood / Genre Filter Pills
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(homeFeed.moods) { idx, mood ->
                    val selected = idx == selectedMoodIndex
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.loadHomeFeed(idx) },
                        label = {
                            Text(
                                text = mood.title,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
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
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SpotifyGreen)
                }
            }
        } else {
            // 1. Spotify 2-Column Quick-Pick Grid (6 การ์ดบนสุด กดเล่นใน 1 คลิก)
            if (homeFeed.quickPicks.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        homeFeed.quickPicks.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pair.forEach { track ->
                                    val isCurrent = track.videoId == playbackState.currentTrack?.videoId
                                    QuickPickGridCard(
                                        track = track,
                                        isCurrent = isCurrent,
                                        modifier = Modifier.weight(1f),
                                        onClick = { viewModel.playTrackOneClick(track, homeFeed.quickPicks) }
                                    )
                                }
                                if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // 2. YouTube Music Horizontal Carousels พร้อมปุ่ม Play สีเขียวสไตล์ Spotify
            items(homeFeed.shelves) { shelf ->
                SpotifyHorizontalShelf(
                    shelf = shelf,
                    currentVideoId = playbackState.currentTrack?.videoId,
                    onPlayTrack = { track -> viewModel.playTrackOneClick(track, shelf.tracks) },
                    onPlayPlaylist = { pl -> viewModel.playPlaylistOneClick(pl) }
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun QuickPickGridCard(
    track: TrackItem,
    isCurrent: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isCurrent) SpotifyCardElevated else SpotifyCard,
        modifier = modifier
            .height(56.dp)
            .clickable { onClick() }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = track.thumbnailUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp)
            )
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
            if (isCurrent) {
                Icon(
                    Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = SpotifyGreen,
                    modifier = Modifier.size(16.dp).padding(end = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun SpotifyHorizontalShelf(
    shelf: HomeShelf,
    currentVideoId: String?,
    onPlayTrack: (TrackItem) -> Unit,
    onPlayPlaylist: (YtPlaylistCard) -> Unit
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
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(shelf.tracks, key = { "t_${it.videoId}" }) { track ->
                val isCurrent = track.videoId == currentVideoId
                Column(
                    modifier = Modifier
                        .width(142.dp)
                        .clickable { onPlayTrack(track) }
                ) {
                    Box(modifier = Modifier.size(142.dp)) {
                        AsyncImage(
                            model = track.thumbnailUrl,
                            contentDescription = track.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                        )
                        // ปุ่ม Play สีเขียวสไตล์ Spotify มุมขวาล่าง (กดเล่น 1 คลิก)
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
                                contentDescription = "เล่นทันที",
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = track.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrent) SpotifyGreen else SpotifyTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        fontSize = 11.sp,
                        color = SpotifyTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            items(shelf.playlists, key = { "p_${it.browseId}" }) { pl ->
                Column(
                    modifier = Modifier
                        .width(142.dp)
                        .clickable { onPlayPlaylist(pl) }
                ) {
                    Box(modifier = Modifier.size(142.dp)) {
                        AsyncImage(
                            model = pl.thumbnailUrl,
                            contentDescription = pl.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
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
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "เล่นเพลย์ลิสต์",
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = pl.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SpotifyTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = pl.subtitle,
                        fontSize = 11.sp,
                        color = SpotifyTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SpotifySearchTab(viewModel: MainViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "ค้นหา",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SpotifyTextPrimary
            )
            Surface(
                shape = CircleShape,
                color = SpotifyCard,
                modifier = Modifier.clickable {
                    val pasted = clipboard.getText()?.text.orEmpty()
                    if (pasted.isNotBlank()) {
                        viewModel.onSearchQueryChange(pasted)
                        focusManager.clearFocus()
                        viewModel.submitSearchOrLink(pasted)
                    }
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ContentPaste, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("วางลิงก์ YT Music", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SpotifyTextPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = viewModel::onSearchQueryChange,
            placeholder = {
                Text("ศิลปิน เพลง หรือวางลิงก์ YT Music...", fontSize = 14.sp, color = Color.DarkGray)
            },
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

        Spacer(modifier = Modifier.height(14.dp))

        if (isSearching) {
            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SpotifyGreen)
            }
        } else if (searchResults.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ผลการค้นหา (${searchResults.size} เพลง)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SpotifyTextSecondary
                )
                Button(
                    onClick = { viewModel.playTrackOneClick(searchResults.first(), searchResults) },
                    colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen, contentColor = Color.Black),
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("เล่นทั้งหมด", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults, key = { it.videoId }) { track ->
                    SpotifyTrackRow(
                        track = track,
                        isPlayingCurrent = track.videoId == playbackState.currentTrack?.videoId,
                        onClick = { viewModel.playTrackOneClick(track, searchResults) }
                    )
                }
            }
        } else {
            Text(
                text = "เลือกแนวเพลงยอดนิยม (แตะเพื่อค้นหาทันที)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = SpotifyTextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(viewModel.browseCategories.chunked(2)) { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { cat ->
                            CategoryBoxCard(
                                category = cat,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.selectCategory(cat) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBoxCard(
    category: BrowseCategory,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(category.colorHex),
        modifier = modifier
            .height(84.dp)
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            Text(
                text = category.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.align(Alignment.TopStart)
            )
        }
    }
}

@Composable
private fun SpotifyQueueTab(viewModel: MainViewModel) {
    val playbackState by viewModel.playbackState.collectAsState()
    val recentTracks by viewModel.recentTracks.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "คิวเพลง & เพลงที่เล่นล่าสุด",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SpotifyTextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (playbackState.queue.isNotEmpty()) {
            item {
                Text(
                    text = "คิวกำลังเล่น (${playbackState.queue.size} เพลง)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SpotifyGreen
                )
            }
            items(playbackState.queue, key = { "q_${it.videoId}" }) { track ->
                SpotifyTrackRow(
                    track = track,
                    isPlayingCurrent = track.videoId == playbackState.currentTrack?.videoId,
                    onClick = { viewModel.playTrackOneClick(track, playbackState.queue) }
                )
            }
        }

        if (recentTracks.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "เล่นล่าสุด (${recentTracks.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = SpotifyTextSecondary
                )
            }
            items(recentTracks, key = { "r_${it.videoId}" }) { track ->
                SpotifyTrackRow(
                    track = track,
                    isPlayingCurrent = track.videoId == playbackState.currentTrack?.videoId,
                    onClick = { viewModel.playTrackOneClick(track, recentTracks) }
                )
            }
        } else if (playbackState.queue.isEmpty()) {
            item {
                Text(
                    text = "ยังไม่มีรายการในคิว แตะเลือกเพลงจากหน้าหลักเพื่อเริ่มฟังได้ทันที",
                    fontSize = 13.sp,
                    color = SpotifyTextSecondary,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun SpotifyTrackRow(
    track: TrackItem,
    isPlayingCurrent: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isPlayingCurrent) SpotifyCard else Color.Transparent)
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = track.thumbnailUrl,
            contentDescription = track.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
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
            Text(
                text = track.artist,
                fontSize = 12.sp,
                color = SpotifyTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isPlayingCurrent) {
            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = SpotifyGreen)
        } else {
            Icon(Icons.Default.PlayArrow, contentDescription = "เล่น", tint = SpotifyTextSecondary)
        }
    }
}

@Composable
private fun SpotifyMiniPlayer(
    track: TrackItem,
    isPlaying: Boolean,
    isBuffering: Boolean,
    positionMs: Long,
    durationMs: Long,
    onOpenFullPlayer: () -> Unit,
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
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = track.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(6.dp))
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SpotifyTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        fontSize = 11.sp,
                        color = SpotifyTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.SkipNext, contentDescription = "ถัดไป", tint = SpotifyTextPrimary)
                }
            }
            // แถบความคืบหน้าสีเขียวบางๆ ด้านล่าง Mini-Player สไตล์ Spotify
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(SpotifyGreen)
                )
            }
        }
    }
}

@Composable
private fun SpotifyFullPlayerScreen(
    track: TrackItem,
    state: PlaybackUiState,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = SpotifyBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF284B37), SpotifySurface, SpotifyBlack)
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "ย่อลง", tint = SpotifyTextPrimary, modifier = Modifier.size(30.dp))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("กำลังเล่นจาก YOUTUBE MUSIC", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SpotifyTextSecondary)
                    Text(state.engineBadge, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SpotifyGreen)
                }
                Spacer(modifier = Modifier.size(48.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            AsyncImage(
                model = track.thumbnailUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp))
            )

            Spacer(modifier = Modifier.weight(1f))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = track.title,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = SpotifyTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = track.artist,
                    fontSize = 15.sp,
                    color = SpotifyTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val safeDuration = state.durationMs.coerceAtLeast(1L)
            Slider(
                value = state.positionMs.coerceIn(0L, safeDuration).toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..safeDuration.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                )
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(state.positionMs), fontSize = 11.sp, color = SpotifyTextSecondary)
                Text(formatTime(state.durationMs), fontSize = 11.sp, color = SpotifyTextSecondary)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "สุ่มเพลง",
                        tint = if (state.shuffleEnabled) SpotifyGreen else SpotifyTextSecondary
                    )
                }
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "ก่อนหน้า", tint = Color.White, modifier = Modifier.size(36.dp))
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(66.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { onTogglePlayPause() }
                ) {
                    if (state.isBuffering) {
                        CircularProgressIndicator(color = Color.Black, strokeWidth = 2.5.dp, modifier = Modifier.size(26.dp))
                    } else {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "เล่น/หยุด",
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Default.SkipNext, contentDescription = "ถัดไป", tint = Color.White, modifier = Modifier.size(36.dp))
                }
                IconButton(onClick = onCycleRepeat) {
                    val icon = if (state.repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat
                    val tint = if (state.repeatMode != Player.REPEAT_MODE_OFF) SpotifyGreen else SpotifyTextSecondary
                    Icon(icon, contentDescription = "เล่นซ้ำ", tint = tint)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}
